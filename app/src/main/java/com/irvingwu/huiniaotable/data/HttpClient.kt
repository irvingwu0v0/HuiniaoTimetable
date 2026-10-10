package com.irvingwu.huiniaotable.data

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.zip.GZIPInputStream

class HttpResult(val code: Int, val body: String, val url: String)

/**
 * 极简 HTTP 客户端：只用 JDK 自带的 [HttpURLConnection]，不引入额外网络库。
 *
 * Cookie 按域名隔离存放：访问节假日接口时绝不会带上教务系统的会话 cookie。
 */
class HttpClient {

    /** host -> (cookie 名 -> 值) */
    private val cookies = LinkedHashMap<String, LinkedHashMap<String, String>>()

    val cookieHeader: String
        get() = cookies.values.flatMap { it.entries }.joinToString("; ") { "${it.key}=${it.value}" }

    fun clearCookies() = cookies.clear()

    fun get(url: String, referer: String? = null): HttpResult =
        request("GET", url, null, referer)

    fun post(url: String, form: Map<String, String>, referer: String? = null): HttpResult =
        request("POST", url, encodeForm(form).toByteArray(Charsets.UTF_8), referer)

    private fun request(method: String, url: String, body: ByteArray?, referer: String?): HttpResult {
        val target = URL(url)
        val host = target.host
        val conn = (target.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 25_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "text/html,application/json,application/xhtml+xml,*/*;q=0.8")
            setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            setRequestProperty("Accept-Encoding", "gzip")
            setRequestProperty("Connection", "close")
            if (referer != null) setRequestProperty("Referer", referer)
            cookieHeaderFor(host).takeIf { it.isNotEmpty() }?.let { setRequestProperty("Cookie", it) }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                setRequestProperty("Content-Length", body.size.toString())
            }
        }

        try {
            body?.let { conn.outputStream.use { out -> out.write(it) } }
            val code = conn.responseCode
            storeCookies(host, conn.headerFields["Set-Cookie"] ?: emptyList())
            val stream: InputStream = if (code >= 400) (conn.errorStream ?: conn.inputStream) else conn.inputStream
            val decoded = stream?.let { maybeGunzip(it, conn.contentEncoding) } ?: ByteArray(0)
            val charset = charsetOf(conn.contentType) ?: Charsets.UTF_8
            return HttpResult(code, String(decoded, charset), conn.url.toString())
        } finally {
            conn.disconnect()
        }
    }

    private fun cookieHeaderFor(host: String): String =
        cookies.entries
            .filter { (domain, _) -> domainMatches(host, domain) }
            .flatMap { it.value.entries }
            .joinToString("; ") { "${it.key}=${it.value}" }

    private fun domainMatches(host: String, domain: String): Boolean =
        host.equals(domain, ignoreCase = true) || host.endsWith(".$domain", ignoreCase = true)

    private fun encodeForm(form: Map<String, String>): String =
        form.entries.joinToString("&") { "${enc(it.key)}=${enc(it.value)}" }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun maybeGunzip(stream: InputStream, contentEncoding: String?): ByteArray {
        val input = if (contentEncoding?.contains("gzip", ignoreCase = true) == true) {
            try {
                GZIPInputStream(stream)
            } catch (t: Throwable) {
                stream
            }
        } else {
            stream
        }
        return input.use { ins ->
            val out = ByteArrayOutputStream()
            val buf = ByteArray(16 * 1024)
            while (true) {
                val n = ins.read(buf)
                if (n <= 0) break
                out.write(buf, 0, n)
            }
            out.toByteArray()
        }
    }

    private fun charsetOf(contentType: String?): java.nio.charset.Charset? {
        val m = Regex("charset=\"?([A-Za-z0-9_\\-]+)\"?").find(contentType ?: "") ?: return null
        return try {
            java.nio.charset.Charset.forName(m.groupValues[1])
        } catch (t: Throwable) {
            null
        }
    }

    private fun storeCookies(requestHost: String, raw: List<String>) {
        for (header in raw) {
            val parts = header.split(';')
            val first = parts.firstOrNull()?.trim().orEmpty()
            val name = first.substringBefore('=', "").trim()
            val value = first.substringAfter('=', "").trim()
            if (name.isEmpty()) continue
            val domain = parts.drop(1)
                .firstOrNull { it.trim().startsWith("Domain=", ignoreCase = true) }
                ?.substringAfter('=')
                ?.trim()
                ?.removePrefix(".")
                ?.takeIf { it.isNotEmpty() }
                ?: requestHost
            val expired = parts.any {
                val p = it.trim()
                p.equals("Max-Age=0", ignoreCase = true) ||
                    p.startsWith("Expires=Thu, 01 Jan 1970", ignoreCase = true)
            }
            val bucket = cookies.getOrPut(domain) { LinkedHashMap() }
            if (expired) bucket.remove(name) else bucket[name] = value
        }
    }

    companion object {
        /** 和桌面浏览器一致的 UA，避免教务系统返回移动端阉割页面。 */
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    }
}
