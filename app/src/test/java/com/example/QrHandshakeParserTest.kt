package com.example

import com.example.data.model.QrHandshakeData
import org.junit.Assert.*
import org.junit.Test

class QrHandshakeParserTest {

    @Test
    fun testParseJsonHandshakePayload() {
        val jsonPayload = """{"app":"antigravity-remote","ip":"192.168.1.10","port":8080,"wsUrl":"ws://192.168.1.10:8080","httpUrl":"http://192.168.1.10:8080"}"""
        val parsed = QrHandshakeData.parse(jsonPayload)

        assertNotNull(parsed)
        assertEquals("antigravity-remote", parsed?.app)
        assertEquals("192.168.1.10", parsed?.ip)
        assertEquals(8080, parsed?.port)
        assertEquals("ws://192.168.1.10:8080", parsed?.wsUrl)
    }

    @Test
    fun testParseJsonWithAuthToken() {
        val jsonWithToken = """{"ip":"192.168.1.25","port":9090,"token":"secret999"}"""
        val parsed = QrHandshakeData.parse(jsonWithToken)

        assertNotNull(parsed)
        assertEquals("192.168.1.25", parsed?.ip)
        assertEquals(9090, parsed?.port)
        assertEquals("secret999", parsed?.authToken)
    }

    @Test
    fun testParseWebSocketUrl() {
        val wsUrl = "ws://192.168.0.105:8080"
        val parsed = QrHandshakeData.parse(wsUrl)

        assertNotNull(parsed)
        assertEquals("192.168.0.105", parsed?.ip)
        assertEquals(8080, parsed?.port)
    }

    @Test
    fun testParsePlainHostPort() {
        val hostPort = "10.0.0.88:8080"
        val parsed = QrHandshakeData.parse(hostPort)

        assertNotNull(parsed)
        assertEquals("10.0.0.88", parsed?.ip)
        assertEquals(8080, parsed?.port)
    }

    @Test
    fun testParsePlainIpOnly() {
        val plainIp = "192.168.1.50"
        val parsed = QrHandshakeData.parse(plainIp)

        assertNotNull(parsed)
        assertEquals("192.168.1.50", parsed?.ip)
        assertEquals(9876, parsed?.port)
    }

    @Test
    fun testParseUpgradePort9876Json() {
        val jsonPayload = """{"ip":"192.168.1.10","port":9876}"""
        val parsed = QrHandshakeData.parse(jsonPayload)

        assertNotNull(parsed)
        assertEquals("192.168.1.10", parsed?.ip)
        assertEquals(9876, parsed?.port)
    }

    @Test
    fun testParseEmptyReturnsNull() {
        assertNull(QrHandshakeData.parse(""))
        assertNull(QrHandshakeData.parse("   "))
    }
}
