package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.service.BridgeNotificationService
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BridgeNotificationServiceTest {

    @Test
    fun testStartAndStopServiceLifecycle() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // 1. Static start helper
        BridgeNotificationService.start(context)

        // 2. Service lifecycle with ACTION_START
        val startIntent = Intent(context, BridgeNotificationService::class.java).apply {
            action = BridgeNotificationService.ACTION_START
        }
        val controller = Robolectric.buildService(BridgeNotificationService::class.java, startIntent)
        val service = controller.create().startCommand(0, 1).get()
        assertNotNull(service)

        // 3. Service with ACTION_STOP
        val stopIntent = Intent(context, BridgeNotificationService::class.java).apply {
            action = BridgeNotificationService.ACTION_STOP
        }
        controller.withIntent(stopIntent).startCommand(0, 2)
        controller.destroy()

        // 4. Static stop helper
        BridgeNotificationService.stop(context)
    }
}
