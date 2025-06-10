package com.skyroute.core.mqtt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.InputStream

class TlsConfigTest {

    private val caInput = InputStream.nullInputStream()

    @Test
    fun `isMutualTls should return false when only CA cert is provided`() {
        val tls = TlsConfig(caCertInput = caInput)

        assertFalse(tls.isMutualTls())
    }

    @Test
    fun `isMutualTls should return false when only client cert is provided`() {
        val tls = TlsConfig(
            caCertInput = caInput,
            clientCertInput = InputStream.nullInputStream(),
            clientKeyInput = null
        )

        assertFalse(tls.isMutualTls())
    }

    @Test
    fun `isMutualTls should return true when both client cert and key are provided`() {
        val cert = InputStream.nullInputStream()
        val key = InputStream.nullInputStream()
        val tls = TlsConfig(
            caCertInput = caInput,
            clientCertInput = cert,
            clientKeyInput = key,
            clientKeyPassword = "secret"
        )

        assertTrue(tls.isMutualTls())
    }

    @Test
    fun `clientKeyPassword can be null or non-null`() {
        val cert = InputStream.nullInputStream()
        val key = InputStream.nullInputStream()
        val tlsWithPassword = TlsConfig(caInput, cert, key, "pwd")
        val tlsWithoutPassword = TlsConfig(caInput, cert, key, null)

        assertEquals("pwd", tlsWithPassword.clientKeyPassword)
        assertNull(tlsWithoutPassword.clientKeyPassword)
    }
}
