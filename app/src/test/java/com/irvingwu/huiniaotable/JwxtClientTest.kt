package com.irvingwu.huiniaotable

import com.irvingwu.huiniaotable.data.JwxtClient
import com.irvingwu.huiniaotable.data.LoginResult
import com.irvingwu.huiniaotable.data.SessionExpiredException
import com.sun.net.httpserver.HttpServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.InetSocketAddress
import java.security.KeyPairGenerator
import java.util.Base64
import javax.crypto.Cipher

class JwxtClientTest {
    private val xjnu = "https://jwxt.xjnu.edu.cn"

    @Test
    fun `入口网址统一到教务上下文`() {
        for (entry in listOf(
            xjnu, "$xjnu/", "$xjnu/jsxsd", "$xjnu/jsxsd/",
            "$xjnu/jsxsd/framework/xsMain.htmlx",
            "  $xjnu/jsxsd/framework/xsMain.htmlx?foo=1#section  ",
        )) {
            val client = JwxtClient(baseUrl = entry)
            assertEquals("$xjnu/jsxsd/", client.buildUrl())
            assertEquals("$xjnu/jsxsd/xk/LoginToXkLdap", client.buildUrl("xk/LoginToXkLdap"))
            assertEquals("$xjnu/jsxsd/xskb/xskb_list.do", client.buildUrl("/jsxsd/xskb/xskb_list.do"))
        }
        assertEquals(
            "https://example.edu.cn:8443/proxy/jsxsd/xskb/xskb_list.do",
            JwxtClient(baseUrl = "https://example.edu.cn:8443/proxy/jsxsd/framework/xsMain.jsp")
                .buildUrl("xskb/xskb_list.do"),
        )
        assertEquals(
            "https://example.edu.cn/custom/xk/LoginToXkLdap",
            JwxtClient(baseUrl = "https://example.edu.cn/custom/").buildUrl("xk/LoginToXkLdap"),
        )
    }

    @Test
    fun `拒绝无效网址与网址中的认证信息`() {
        for (url in listOf("", "jwxt.xjnu.edu.cn", "file:///tmp/jsxsd", "https://user:pass@example.edu.cn/")) {
            assertTrue(runCatching { JwxtClient(baseUrl = url).buildUrl() }.isFailure)
        }
    }

    @Test
    fun `新疆师范大学按页面公钥生成兼容JSEncrypt的随机密文`() {
        val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val key = Base64.getEncoder().encodeToString(pair.public.encoded)
        val html = "<script>encrypt.setPublicKey(\"$key\");</script>"
        val client = JwxtClient(baseUrl = "$xjnu/jsxsd/framework/xsMain.htmlx")
        val form = client.buildLoginForm(html, "test-student", "测试-password+%&")
        assertEquals("LoginToXkLdap", form["loginMethod"])
        assertEquals("", form["userAccount"])
        assertEquals("", form["userPassword"])
        val parts = form.getValue("encoded").split("%%%")
        assertEquals(2, parts.size)
        fun decrypt(value: String): String {
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.DECRYPT_MODE, pair.private)
            return String(cipher.doFinal(Base64.getDecoder().decode(value)), Charsets.UTF_8)
        }
        assertEquals("test-student", decrypt(parts[0]))
        assertEquals("测试-password+%&", decrypt(parts[1]))
        assertNotEquals(form["encoded"], client.buildLoginForm(html, "test-student", "测试-password+%&")["encoded"])
        val pemHtml = "encrypt.setPublicKey('-----BEGIN PUBLIC KEY-----\n$key\n-----END PUBLIC KEY-----');"
        assertEquals("test-student", decrypt(client.buildLoginForm(pemHtml, "test-student", "p").getValue("encoded").split("%%%")[0]))
    }

    @Test
    fun `RSA公钥缺失或损坏时停止登录`() {
        val client = JwxtClient(baseUrl = xjnu)
        for (html in listOf("<html>maintenance</html>", "encrypt.setPublicKey('invalid');")) {
            assertTrue(runCatching { client.buildLoginForm(html, "test-student", "test-password") }.isFailure)
        }
    }

    @Test
    fun `其他学校保留Base64协议且不执行模板中的RSA分支`() {
        val form = JwxtClient(baseUrl = "https://example.edu.cn/jsxsd/")
            .buildLoginForm("if (false) encrypt.setPublicKey('unused');", "student", "password")
        assertEquals("student", form["userAccount"])
        assertEquals("password", form["userPassword"])
        assertEquals("c3R1ZGVudA==%%%cGFzc3dvcmQ=", form["encoded"])
    }

    @Test
    fun `登录错误支持单引号表单与HTML实体`() = withServer { server, client ->
        server.createContext("/jsxsd/") { exchange ->
            val html = if (exchange.requestMethod == "POST") {
                "<form id='loginForm'><font id='showMsg'>Invalid &amp; expired</font></form>"
            } else "<form id='loginForm'></form>"
            val bytes = html.toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        assertEquals(LoginResult.Failure("Invalid & expired"), client.login("student", "password"))
    }

    @Test
    fun `服务器错误不能被误报为登录成功`() = withServer { server, client ->
        server.createContext("/jsxsd/") { exchange ->
            exchange.sendResponseHeaders(503, -1)
            exchange.close()
        }
        assertThrows(IOException::class.java) { client.login("student", "password") }
    }

    @Test
    fun `课表请求遇到登录页时报告会话过期`() = withServer { server, client ->
        server.createContext("/jsxsd/") { exchange ->
            val bytes = "<form id='loginForm'></form>".toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        assertThrows(SessionExpiredException::class.java) { client.fetchSchedule(null) }
    }

    private fun withServer(block: (HttpServer, JwxtClient) -> Unit) {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.start()
        try {
            block(server, JwxtClient(baseUrl = "http://127.0.0.1:${server.address.port}/jsxsd/"))
        } finally {
            server.stop(0)
        }
    }
}
