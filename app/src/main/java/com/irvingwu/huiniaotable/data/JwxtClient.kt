package com.irvingwu.huiniaotable.data

import com.irvingwu.huiniaotable.t
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher

sealed interface LoginResult {
    data object Success : LoginResult
    data class Failure(val message: String) : LoginResult
}

class SessionExpiredException : Exception(t("登录状态已失效", "Session expired"))

/**
 * 强智教务系统客户端。支持通用 Base64 登录与新疆师范大学 RSA 登录。
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
     * 2. `POST /xk/LoginToXkLdap`，按学校要求以 RSA 或 Base64 构造 encoded；
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
        // 仅对已核验的学校启用 RSA：其他学校的模板可能含有不会执行的 RSA 分支。
        val rsa = isXjnu()
        val encoded = if (rsa) {
            val keyText = Regex("""\.setPublicKey\s*\(\s*["']([^"']+)["']\s*\)""")
                .find(html)?.groupValues?.get(1)
            require(!keyText.isNullOrBlank()) {
                t("登录页缺少 RSA 公钥，请稍后重试", "The sign-in page is missing its RSA public key. Please try again.")
            }
            val der = Base64.getDecoder().decode(
                keyText.replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "").replace(Regex("\\s+"), ""),
            )
            val key = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(der))
            fun encrypt(value: String): String {
                val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
                cipher.init(Cipher.ENCRYPT_MODE, key)
                return Base64.getEncoder().encodeToString(cipher.doFinal(value.toByteArray(Charsets.UTF_8)))
            }
            encrypt(studentId) + "%%%" + encrypt(password)
        } else {
            base64(studentId) + "%%%" + base64(password)
        }
        return mapOf(
            "loginMethod" to "LoginToXkLdap",
            "userAccount" to if (rsa) "" else studentId,
            "userPassword" to if (rsa) "" else password,
            "encoded" to encoded,
        )
    }

    private fun isLoginPage(html: String): Boolean = Jsoup.parse(html).selectFirst("form#loginForm") != null

    private fun isXjnu(): Boolean = URI(buildUrl()).host.equals("jwxt.xjnu.edu.cn", ignoreCase = true)

    private fun homeUrl(): String = buildUrl(if (isXjnu()) "framework/xsMain.htmlx" else "framework/xsMain.jsp")

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
