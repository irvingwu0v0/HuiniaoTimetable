package com.irvingwu.huiniaotable.data

import java.net.URLEncoder
import java.util.Base64

sealed interface LoginResult {
    data object Success : LoginResult
    data class Failure(val message: String) : LoginResult
}

class SessionExpiredException : Exception("登录状态已失效")

/**
 * 正方教务系统客户端。支持用户自定义教务系统端点网址。
 */
class JwxtClient(
    private val http: HttpClient = HttpClient(),
    val baseUrl: String = Prefs.DEFAULT_JWXT_BASE_URL,
) {

    /** 组合教务系统内部相对路径。 */
    fun buildUrl(subPath: String = ""): String {
        val cleanBase = baseUrl.trim().trimEnd('/')
        val cleanSub = subPath.trim().trimStart('/')
        if (cleanSub.isEmpty()) return cleanBase

        val baseSegment = cleanBase.substringAfterLast('/', "")
        val subSegment = cleanSub.substringBefore('/', "")
        return if (baseSegment.isNotEmpty() && baseSegment.equals(subSegment, ignoreCase = true)) {
            val rest = cleanSub.substringAfter('/', "")
            if (rest.isEmpty()) cleanBase else "$cleanBase/$rest"
        } else {
            "$cleanBase/$cleanSub"
        }
    }

    /**
     * 登录。流程与网页版一致：
     * 1. 访问首页校验是否存在验证码；
     * 2. `POST /xk/LoginToXkLdap`，携带 `encoded = base64(学号)%%%base64(密码)`；
     * 3. 访问受保护页面确认会话生效。
     */
    fun login(studentId: String, password: String): LoginResult {
        http.clearCookies()
        val loginUrl = buildUrl("")
        val loginPage = http.get(loginUrl)
        if (loginPage.body.contains("id=\"SafeCodeImg\"") || loginPage.body.contains("name=\"SafeCode\"")) {
            return LoginResult.Failure("教务系统要求输入验证码，请用浏览器登录后使用")
        }

        val encoded = base64(studentId) + "%%%" + base64(password)
        val postUrl = buildUrl("xk/LoginToXkLdap")
        val response = http.post(
            postUrl,
            mapOf(
                "loginMethod" to "LoginToXkLdap",
                "userAccount" to studentId,
                "userPassword" to password,
                "encoded" to encoded,
            ),
            referer = loginUrl,
        )

        if (response.body.contains("id=\"loginForm\"")) {
            return LoginResult.Failure(extractLoginMessage(response.body) ?: "学号或密码错误")
        }

        val probeUrl = buildUrl("framework/xsMain.jsp")
        val probe = http.get(probeUrl)
        if (probe.body.contains("id=\"loginForm\"")) {
            return LoginResult.Failure("登录未生效，请稍后重试")
        }
        return LoginResult.Success
    }

    /** 抓取指定学期的课表；[term] 为空时由教务系统决定（通常是当前学期）。 */
    fun fetchSchedule(term: String?): KbParser.Parsed {
        val url = buildString {
            append(buildUrl("xskb/xskb_list.do"))
            if (!term.isNullOrBlank()) append("?xnxq01id=").append(URLEncoder.encode(term, "UTF-8"))
        }
        val response = http.get(url, referer = buildUrl("framework/xsMain.jsp"))
        if (response.body.contains("id=\"loginForm\"")) throw SessionExpiredException()
        return KbParser.parse(response.body, fallbackTerm = term.orEmpty())
    }

    private fun extractLoginMessage(html: String): String? {
        val raw = Regex("id=\"showMsg\"[^>]*>(.*?)</", RegexOption.DOT_MATCHES_ALL)
            .find(html)?.groupValues?.get(1)
            ?.replace(Regex("<[^>]+>"), "")
            ?.trim()
        return raw?.takeIf { it.isNotBlank() && it != "&nbsp;" }
    }

    /** 与网页版 `encodeInp`（标准 Base64）保持一致。 */
    private fun base64(value: String): String =
        Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))
}
