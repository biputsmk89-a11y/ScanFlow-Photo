package com.scanflow.photocompressor.presentation

import com.scanflow.photocompressor.domain.account.AccountContract
import com.scanflow.photocompressor.domain.account.DefaultAccountContract
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class LocalizationAndAccessibilityTest {

    @Test
    fun `AccountContract guarantees no login required for core application`() {
        val contract: AccountContract = DefaultAccountContract()

        // Core app requires NO login
        assertFalse("Core app workflow must never require login", contract.isLoginRequiredForCore)

        // Only optional cloud services require an account
        assertTrue(contract.isAccountRequiredFor(AccountContract.CloudServiceRequirement.CLOUD_SYNC))
        assertTrue(contract.isAccountRequiredFor(AccountContract.CloudServiceRequirement.CLOUD_BACKUP))
        assertTrue(contract.isAccountRequiredFor(AccountContract.CloudServiceRequirement.AI_CLOUD_SERVICES))
        assertTrue(contract.isAccountRequiredFor(AccountContract.CloudServiceRequirement.MULTI_DEVICE_SYNC))
    }

    @Test
    fun `Localization resource files exist and contain core localization keys in English and Indonesian`() {
        val baseResDir = File("src/main/res")
        val defaultStringsFile = File(baseResDir, "values/strings.xml")
        val indonesianStringsFile = File(baseResDir, "values-in/strings.xml")

        assertTrue("Default strings.xml must exist", defaultStringsFile.exists())
        assertTrue("values-in/strings.xml for Bahasa Indonesia must exist", indonesianStringsFile.exists())

        val defaultKeys = parseStringResourceKeys(defaultStringsFile)
        val indonesianKeys = parseStringResourceKeys(indonesianStringsFile)

        assertTrue("Default strings must have keys", defaultKeys.isNotEmpty())
        assertTrue("Indonesian strings must have keys", indonesianKeys.isNotEmpty())

        // Essential keys that must exist in both
        val requiredKeys = listOf(
            "app_name",
            "privacy_badge",
            "no_account_badge",
            "nav_home",
            "nav_history",
            "nav_settings",
            "action_compress_photos_semantic",
            "action_optimize_whatsapp_semantic",
            "action_generate_passport_semantic",
            "action_generate_pdf_semantic",
            "passport_disclaimer",
            "metric_before",
            "metric_after",
            "metric_saved",
            "metric_reduction"
        )

        for (key in requiredKeys) {
            assertTrue("Default strings must contain '$key'", defaultKeys.contains(key))
            assertTrue("Bahasa Indonesia strings must contain '$key'", indonesianKeys.contains(key))
        }
    }

    private fun parseStringResourceKeys(file: File): Set<String> {
        val keys = mutableSetOf<String>()
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(file)
        doc.documentElement.normalize()

        val nList = doc.getElementsByTagName("string")
        for (i in 0 until nList.length) {
            val node = nList.item(i)
            val nameAttr = node.attributes.getNamedItem("name")?.nodeValue
            if (nameAttr != null) {
                keys.add(nameAttr)
            }
        }
        return keys
    }
}
