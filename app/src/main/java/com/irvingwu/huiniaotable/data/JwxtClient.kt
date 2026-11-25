package com.irvingwu.huiniaotable.data

import com.irvingwu.huiniaotable.t
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.util.Base64

sealed interface LoginResult {
    data object Success : LoginResult
    data class Failure(val message: String) : LoginResult
}

class SessionExpiredException : Exception(t("登录状态已失效", "Session expired"))

/**
 * 强智教务系统客户端。
 */
class JwxtClient(
    private val http: HttpClient = HttpClient(),
    val baseUrl: String = Prefs.DEFAULT_JWXT_BASE_URL,
) {

    /** 接受域名、/jsxsd/ 根路径或完整的学生主页 URL。 */
    fun buildUrl(subPath: String = ""): String {
        val uri = URI(baseUrl.trim())
        require(uri.scheme in listOf("https", "http") && !uri.host.isNullOrBlank() && uri.userInfo == null) {
            t("请输入有效的教务系统 HTTP(S) 网址", "Enter a valid HTTP(S) academic system URL")
        }
        val segments = uri.path.orEmpty().split('/').filter { it.isNotEmpty() }
        val contextIndex = segments.indexOfFirst { it.equals("jsxsd", ignoreCase = true) }
        val contextPath = when {
            contextIndex >= 0 -> "/" + segments.take(contextIndex + 1).joinToString("/")
            segments.isEmpty() -> "/jsxsd"
            else -> "/" + segments.joinToString("/")
        }
        val root = URI(uri.scheme, null, uri.host, uri.port, "$contextPath/", null, null).toASCIIString()
        var path = subPath.trim().trimStart('/')
        val context = contextPath.substringAfterLast('/')
        if (path.substringBefore('/').equals(context, ignoreCase = true)) {
            path = path.substringAfter('/', "")
        }
        return root + path
    }

    /**
     * 登录。流程与网页版一致：
     * 1. 访问首页校验是否存在验证码；
     * 2. `POST /xk/LoginToXkLdap`，用网页同款 Base64 规则构造 encoded；
     * 3. 访问受保护页面确认会话生效。
     */
    fun login(studentId: String, password: String): LoginResult {
        http.clearCookies()
        val loginUrl = buildUrl("")
        val loginPage = http.get(loginUrl)
        requireSuccess(loginPage)
        if (Jsoup.parse(loginPage.body).selectFirst("#SafeCodeImg, input[name=SafeCode]") != null) {
            return LoginResult.Failure(
                t(
                    "教务系统要求输入验证码，请用浏览器登录后使用",
                    "The academic system requires a CAPTCHA. Please sign in with a browser first.",
                ),
            )
        }

        val postUrl = buildUrl("xk/LoginToXkLdap")
        val response = http.post(
            postUrl,
            buildLoginForm(loginPage.body, studentId, password),
            referer = loginUrl,
        )
        requireSuccess(response)

        if (isLoginPage(response.body)) {
            return LoginResult.Failure(
                extractLoginMessage(response.body) ?: t("学号或密码错误", "Incorrect student ID or password"),
            )
        }

        val probeUrl = homeUrl()
        val probe = http.get(probeUrl)
        requireSuccess(probe)
        if (isLoginPage(probe.body)) {
            return LoginResult.Failure(t("登录未生效，请稍后重试", "Sign-in did not complete. Please try again."))
        }
        return LoginResult.Success
    }

    /** 抓取指定学期的课表；[term] 为空时由教务系统决定（通常是当前学期）。 */
    fun fetchSchedule(term: String?): KbParser.Parsed {
        val url = buildString {
            append(buildUrl("xskb/xskb_list.do"))
            if (!term.isNullOrBlank()) append("?xnxq01id=").append(URLEncoder.encode(term, "UTF-8"))
        }
        val response = http.get(url, referer = homeUrl())
        if (response.code == 401 || response.code == 403 || isLoginPage(response.body)) throw SessionExpiredException()
        requireSuccess(response)
        return KbParser.parse(response.body, fallbackTerm = term.orEmpty())
    }

    internal fun buildLoginForm(html: String, studentId: String, password: String): Map<String, String> {
        val encoded = base64(studentId) + "%%%" + base64(password)
        return mapOf(
            "loginMethod" to "LoginToXkLdap",
            "userAccount" to studentId,
            "userPassword" to password,
            "encoded" to encoded,
        )
    }

    private fun isLoginPage(html: String): Boolean = Jsoup.parse(html).selectFirst("form#loginForm") != null

    private fun homeUrl(): String = buildUrl("framework/xsMain.jsp")

    private fun requireSuccess(response: HttpResult) {
        if (response.code !in 200..299) {
            throw IOException(t("教务系统请求失败", "Academic system request failed") + " (HTTP ${response.code})")
        }
    }

    private fun extractLoginMessage(html: String): String? {
        return Jsoup.parse(html).selectFirst("#showMsg")?.text()?.trim()?.takeIf { it.isNotBlank() }
    }

    /** 与网页版 `encodeInp`（标准 Base64）保持一致。 */
    private fun base64(value: String): String =
        Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))
}
