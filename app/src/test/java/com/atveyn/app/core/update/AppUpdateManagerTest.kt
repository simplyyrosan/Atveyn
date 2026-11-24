package com.atveyn.app.core.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {

    @Test
    fun testIsNewerVersion() {
        assertTrue(AppVersionComparator.isNewerVersion("1.6", "1.5"))
        assertTrue(AppVersionComparator.isNewerVersion("v1.6", "1.5"))
        assertTrue(AppVersionComparator.isNewerVersion("1.51", "1.5"))
        assertTrue(AppVersionComparator.isNewerVersion("v1.51", "1.5"))
        assertTrue(AppVersionComparator.isNewerVersion("2.0.0", "1.5"))
        assertTrue(AppVersionComparator.isNewerVersion("1.5.1", "1.5"))
        assertTrue(AppVersionComparator.isNewerVersion("1.5.0.1", "1.5.0"))

        assertFalse(AppVersionComparator.isNewerVersion("1.5", "1.5"))
        assertFalse(AppVersionComparator.isNewerVersion("v1.5", "1.5"))
        assertFalse(AppVersionComparator.isNewerVersion("1.51", "1.51"))
        assertFalse(AppVersionComparator.isNewerVersion("v1.51", "1.51"))
        assertFalse(AppVersionComparator.isNewerVersion("1.5.0", "1.5"))
        assertFalse(AppVersionComparator.isNewerVersion("1.4", "1.5"))
        assertFalse(AppVersionComparator.isNewerVersion("1.4.9", "1.5"))
        assertTrue(AppVersionComparator.isNewerVersion("1.52", "1.51"))
        assertTrue(AppVersionComparator.isNewerVersion("v1.52", "1.51"))
        assertTrue(AppVersionComparator.isNewerVersion("1.52", "1.5"))
        assertFalse(AppVersionComparator.isNewerVersion("1.52", "1.52"))
        assertFalse(AppVersionComparator.isNewerVersion("1.51", "1.52"))
        assertFalse(AppVersionComparator.isNewerVersion("1.5", "1.52"))
        assertFalse(AppVersionComparator.isNewerVersion("", "1.5"))
    }
}
