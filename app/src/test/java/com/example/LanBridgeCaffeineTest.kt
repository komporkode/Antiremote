package com.example

import com.example.data.network.LanBridgeClient
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LanBridgeCaffeineTest {

    @Test
    fun testPcCaffeineToggle() {
        val client = LanBridgeClient.getInstance()
        
        // Initial state or set false
        client.setPcCaffeine(false, manual = true)
        assertFalse(client.isPcCaffeineActive.value)

        // Toggle to true
        val toggledOn = client.togglePcCaffeine()
        assertTrue(toggledOn)
        assertTrue(client.isPcCaffeineActive.value)

        // Toggle back to false
        val toggledOff = client.togglePcCaffeine()
        assertFalse(toggledOff)
        assertFalse(client.isPcCaffeineActive.value)
    }

    @Test
    fun testApprovalResponseMarksResolved() {
        val client = LanBridgeClient.getInstance()
        val testApprovalId = "test-approval-999"

        client.sendApprovalResponse(testApprovalId, true, "1. Option A")
        
        assertTrue(
            "resolvedApprovalIds must contain the answered approval ID",
            client.resolvedApprovalIds.contains(testApprovalId)
        )
    }
}
