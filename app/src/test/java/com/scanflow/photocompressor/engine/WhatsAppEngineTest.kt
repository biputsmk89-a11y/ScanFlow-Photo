package com.scanflow.photocompressor.engine

import com.scanflow.photocompressor.domain.model.WhatsAppConfig
import com.scanflow.photocompressor.domain.model.WhatsAppTier
import com.scanflow.photocompressor.util.ReductionCalculator
import org.junit.Assert.*
import org.junit.Test

class WhatsAppEngineTest {

    @Test
    fun `WhatsAppTier includes Small, Balanced, High Quality, and Custom`() {
        val tiers = WhatsAppTier.values()
        assertEquals(4, tiers.size)
        assertTrue(tiers.contains(WhatsAppTier.SMALL))
        assertTrue(tiers.contains(WhatsAppTier.BALANCED))
        assertTrue(tiers.contains(WhatsAppTier.HIGH_QUALITY))
        assertTrue(tiers.contains(WhatsAppTier.CUSTOM))
    }

    @Test
    fun `WhatsAppTier descriptions do not promise absolute file size numbers`() {
        WhatsAppTier.values().forEach { tier ->
            assertFalse(
                "Tier ${tier.name} description should not promise absolute numbers with < or KB",
                tier.description.contains("<") || tier.description.contains("KB") || tier.description.contains("MB")
            )
        }
    }

    @Test
    fun `WhatsAppConfig default configuration is Balanced tier`() {
        val config = WhatsAppConfig()
        assertEquals(WhatsAppTier.BALANCED, config.tier)
    }

    @Test
    fun `Reduction metrics Before, After, Saved, and Reduction percent are computed correctly`() {
        val originalBytes = 5_000_000L // 5 MB
        val outputBytes = 1_000_000L   // 1 MB

        val savedBytes = ReductionCalculator.calculateSavedBytes(originalBytes, outputBytes)
        val reductionPercent = ReductionCalculator.calculateReductionPercent(originalBytes, outputBytes)

        assertEquals(4_000_000L, savedBytes)
        assertEquals(80.0, reductionPercent, 0.001)

        val formattedBefore = ReductionCalculator.formatBytes(originalBytes)
        val formattedAfter = ReductionCalculator.formatBytes(outputBytes)
        val formattedSaved = ReductionCalculator.formatBytes(savedBytes)

        assertTrue(formattedBefore.contains("MB"))
        assertTrue(formattedAfter.contains("MB") || formattedAfter.contains("KB"))
        assertTrue(formattedSaved.contains("MB"))
    }
}
