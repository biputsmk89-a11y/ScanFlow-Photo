package com.scanflow.photocompressor.presentation

import com.scanflow.photocompressor.domain.model.AppPreferences
import com.scanflow.photocompressor.domain.model.ThemeMode
import com.scanflow.photocompressor.ui.navigation.Screen
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class NavigationAndThemeTest {

    @Test
    fun `default theme in AppPreferences is SYSTEM`() {
        val prefs = AppPreferences()
        assertEquals(ThemeMode.SYSTEM, prefs.theme)
    }

    @Test
    fun `theme mode supports exactly SYSTEM, LIGHT, and DARK`() {
        val modes = ThemeMode.values().map { it.name }
        assertEquals(3, modes.size)
        assertTrue(modes.contains("SYSTEM"))
        assertTrue(modes.contains("LIGHT"))
        assertTrue(modes.contains("DARK"))
    }

    @Test
    fun `Rule 79 - Bottom navigation is strictly limited to 3 items and not overcrowded`() {
        val bottomNav = Screen.bottomNavItems
        assertEquals("Bottom navigation must have exactly 3 items to avoid overcrowding", 3, bottomNav.size)
        assertEquals(listOf(Screen.Home, Screen.History, Screen.Settings), bottomNav)
    }

    @Test
    fun `Rule 79 - Detail tools reside in Home or feature navigation and are excluded from bottom nav`() {
        val detailToolRoutes = listOf(
            Screen.Compress.route,
            Screen.Batch.route,
            Screen.Resize.route,
            Screen.Crop.route,
            Screen.Rotate.route,
            Screen.Watermark.route,
            Screen.Convert.route,
            Screen.Pdf.route,
            Screen.Passport.route,
            Screen.Social.route,
            Screen.WhatsApp.route
        )

        val bottomRoutes = Screen.bottomNavItems.map { it.route }.toSet()

        detailToolRoutes.forEach { toolRoute ->
            assertFalse(
                "Detail tool '$toolRoute' must NOT be in bottom navigation bar",
                bottomRoutes.contains(toolRoute)
            )
        }
    }

    @Test
    fun `localization parity - theme strings exist in both English and Indonesian`() {
        val enKeys = extractStringKeys("app/src/main/res/values/strings.xml")
        val inKeys = extractStringKeys("app/src/main/res/values-in/strings.xml")

        val requiredThemeKeys = listOf(
            "theme_appearance_title",
            "theme_mode",
            "theme_system",
            "theme_system_desc",
            "theme_light",
            "theme_light_desc",
            "theme_dark",
            "theme_dark_desc"
        )

        requiredThemeKeys.forEach { key ->
            assertTrue("English strings must contain $key", enKeys.contains(key))
            assertTrue("Indonesian strings must contain $key", inKeys.contains(key))
        }
    }

    private fun extractStringKeys(relativePath: String): Set<String> {
        val file = if (File(relativePath).exists()) {
            File(relativePath)
        } else {
            File(relativePath.removePrefix("app/"))
        }
        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = docBuilder.parse(file)
        val stringNodes = doc.getElementsByTagName("string")

        val keys = mutableSetOf<String>()
        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i)
            val nameAttr = node.attributes.getNamedItem("name")?.nodeValue
            if (nameAttr != null) {
                keys.add(nameAttr)
            }
        }
        return keys
    }
}
