package com.scanflow.photocompressor.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.scanflow.photocompressor.domain.model.EntitlementResult
import com.scanflow.photocompressor.domain.model.ProFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class EntitlementRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var repository: EntitlementRepositoryImpl

    @Before
    fun setup() {
        dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val testFile = File(tempFolder.root, "test_entitlements_${System.nanoTime()}.preferences_pb")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { testFile }
        )
        repository = EntitlementRepositoryImpl(testDataStore)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun allFeaturesAreUnlockedByDefaultInOfflineRelease() = runTest {
        val entitlement = repository.refreshEntitlement()
        assertTrue("Entitlement must evaluate to active pro by default", entitlement.isActivePro)

        for (feature in ProFeature.values()) {
            assertTrue("Feature $feature must be unlocked", repository.isFeatureUnlocked(feature))
            val access = repository.checkFeatureAccess(feature)
            assertTrue("Feature access for $feature must be Allowed", access is EntitlementResult.Allowed)
        }
    }
}
