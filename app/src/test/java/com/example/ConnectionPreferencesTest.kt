package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.storage.ConnectionPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConnectionPreferencesTest {

    @Test
    fun testSaveAndRetrieveConnection() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = ConnectionPreferences(context)
        prefs.clearConnection()

        assertNull(prefs.getLastConnection())

        prefs.saveConnection("192.168.1.50", 9876, "882341")

        val saved = prefs.getLastConnection()
        assertNotNull(saved)
        assertEquals("192.168.1.50", saved?.host)
        assertEquals(9876, saved?.port)
        assertEquals("882341", saved?.pin)

        prefs.clearConnection()
        assertNull(prefs.getLastConnection())
    }
}
