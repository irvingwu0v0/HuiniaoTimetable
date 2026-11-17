package com.irvingwu.huiniaotable

import com.irvingwu.huiniaotable.data.HttpClient
import com.sun.net.httpserver.HttpServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import java.net.InetSocketAddress

class HttpClientTest {
    @Test
    fun `重定向逐跳接收Cookie且按状态码转换请求方法`() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/redirect/") { exchange ->
            val code = exchange.requestURI.path.substringAfterLast('/').toInt()
            exchange.requestBody.use { it.readBytes() }
            exchange.responseHeaders.add("set-cookie", "session=redirect-session; Path=/")
            exchange.responseHeaders.add("Location", "/target")
            exchange.sendResponseHeaders(code, -1)
            exchange.close()
        }
        server.createContext("/target") { exchange ->
            val text = listOf(
                exchange.requestMethod,
                exchange.requestHeaders.getFirst("Cookie").orEmpty(),
                exchange.requestBody.use { String(it.readBytes(), Charsets.UTF_8) },
            ).joinToString("\n")
            val bytes = text.toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            val root = "http://127.0.0.1:${server.address.port}"
            for (code in listOf(301, 302, 303, 307, 308)) {
                val response = HttpClient().post("$root/redirect/$code", mapOf("value" to "a+b%&"))
                assertEquals("$root/target", response.url)
                val lines = response.body.split('\n')
                assertEquals(if (code in listOf(307, 308)) "POST" else "GET", lines[0])
                assertEquals("session=redirect-session", lines[1])
                assertEquals(if (code in listOf(307, 308)) "value=a%2Bb%25%26" else "", lines[2])
            }
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `终止无限重定向且拒绝跨源转发登录表单`() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/loop") { exchange ->
            exchange.responseHeaders.add("Location", "/loop")
            exchange.sendResponseHeaders(302, -1)
            exchange.close()
        }
        server.createContext("/cross-origin") { exchange ->
            exchange.requestBody.use { it.readBytes() }
            exchange.responseHeaders.add("Location", "http://localhost:${server.address.port}/target")
            exchange.sendResponseHeaders(307, -1)
            exchange.close()
        }
        server.start()
        try {
            val root = "http://127.0.0.1:${server.address.port}"
            assertThrows(IOException::class.java) { HttpClient().get("$root/loop") }
            assertThrows(IOException::class.java) {
                HttpClient().post("$root/cross-origin", mapOf("password" to "test-password"))
            }
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `不区分大小写保存多个会话Cookie并在后续请求发送`() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/login") { exchange ->
            exchange.responseHeaders.add("set-cookie", "session=test-session; Path=/; HttpOnly")
            exchange.responseHeaders.add("set-cookie", "route=test-route; Path=/")
            exchange.sendResponseHeaders(200, -1)
            exchange.close()
        }
        server.createContext("/probe") { exchange ->
            val bytes = exchange.requestHeaders.getFirst("Cookie").orEmpty().toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            val root = "http://127.0.0.1:${server.address.port}"
            val client = HttpClient()
            client.get("$root/login")
            val cookies = client.get("$root/probe").body.split("; ").toSet()
            assertEquals(setOf("session=test-session", "route=test-route"), cookies)
            client.clearCookies()
            assertTrue(client.get("$root/probe").body.isEmpty())
        } finally {
            server.stop(0)
        }
    }
}
