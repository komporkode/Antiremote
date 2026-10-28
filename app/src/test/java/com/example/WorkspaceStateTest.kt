package com.example

import com.example.data.model.WorkspaceInfo
import com.example.ui.viewmodel.DashboardTab
import org.junit.Assert.*
import org.junit.Test

class WorkspaceStateTest {
    @Test
    fun testDashboardTab_doesNotContainReview() {
        val tabNames = DashboardTab.values().map { it.name }
        assertFalse("DashboardTab should not contain REVIEW", tabNames.contains("REVIEW"))
        assertEquals(4, DashboardTab.values().size)
    }

    @Test
    fun testWorkspaceInfo_hasBranchAndPort() {
        val info = WorkspaceInfo(
            projectName = "Dengar",
            activeFileName = "App.tsx",
            cursorLine = 12,
            gitBranch = "feature/mobile-sync",
            devPort = 3000,
            isSync = true
        )
        assertEquals("Dengar", info.projectName)
        assertEquals("feature/mobile-sync", info.gitBranch)
        assertEquals(3000, info.devPort)
    }
}
