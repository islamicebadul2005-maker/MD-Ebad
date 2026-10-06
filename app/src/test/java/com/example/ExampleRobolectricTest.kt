package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.WireGuardConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("WireShield", appName)
    }

    @Test
    fun `wireguard conf parsing and formatting`() {
        val conf = """
            [Interface]
            PrivateKey = aGVsbG9fd2lyZWd1YXJkX3Rlc3Rfa2V5XzAxMjM0NQ==
            Address = 10.14.0.2/32
            DNS = 1.1.1.1
            MTU = 1420

            [Peer]
            PublicKey = c2VjdXJlX3B1YmxpY19rZXlfd2dfMDk4NzY1NDMyMQ==
            Endpoint = 198.51.100.15:51820
            AllowedIPs = 0.0.0.0/0
            PersistentKeepalive = 25
        """.trimIndent()

        val parsed = WireGuardConfig.parseConf(conf, "Test Node")
        assertEquals("Test Node", parsed.serverName)
        assertEquals("10.14.0.2/32", parsed.interfaceAddress)
        assertEquals("198.51.100.15:51820", parsed.peerEndpoint)
        assertEquals(1420, parsed.interfaceMtu)
        assertEquals(25, parsed.peerPersistentKeepalive)

        val exportedConf = parsed.toConfString()
        assertTrue(exportedConf.contains("[Interface]"))
        assertTrue(exportedConf.contains("[Peer]"))
        assertTrue(exportedConf.contains("198.51.100.15:51820"))
    }

    @Test
    fun `random key generation produces valid 32 byte base64`() {
        val key = WireGuardConfig.generateRandomKey()
        assertNotNull(key)
        assertTrue(key.length >= 40)
    }
}
