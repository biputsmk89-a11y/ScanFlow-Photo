package com.scanflow.photocompressor.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.scanflow.photocompressor.domain.model.EntitlementResult
import com.scanflow.photocompressor.domain.model.ProFeature
import com.scanflow.photocompressor.domain.model.UserEntitlement
import com.scanflow.photocompressor.domain.repository.EntitlementRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntitlementRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : EntitlementRepository {

    private val scope = CoroutineScope(Dispatchers.IO)

    private object Keys {
        val IS_PRO = booleanPreferencesKey("is_pro_user")
        val PRODUCT_ID = stringPreferencesKey("pro_product_id")
        val EXPIRY_TIME = longPreferencesKey("pro_expiry_time")
    }

    private val _entitlement = MutableStateFlow(UserEntitlement.FREE)
    override val entitlement: StateFlow<UserEntitlement> = _entitlement.asStateFlow()

    init {
        scope.launch {
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) emit(emptyPreferences()) else throw exception
                }
                .collect { preferences ->
                    val isPro = preferences[Keys.IS_PRO] ?: false
                    val productId = preferences[Keys.PRODUCT_ID]
                    val expiryTime = preferences[Keys.EXPIRY_TIME]
                    _entitlement.value = UserEntitlement(
                        isPro = isPro,
                        productId = productId,
                        expiryTime = expiryTime
                    )
                }
        }
    }

    override suspend fun refreshEntitlement(): UserEntitlement {
        val prefs = dataStore.data.first()
        val isPro = prefs[Keys.IS_PRO] ?: false
        val productId = prefs[Keys.PRODUCT_ID]
        val expiryTime = prefs[Keys.EXPIRY_TIME]
        val updated = UserEntitlement(isPro = isPro, productId = productId, expiryTime = expiryTime)
        _entitlement.value = updated
        return updated
    }

    override suspend fun setEntitlement(entitlement: UserEntitlement) {
        dataStore.edit { preferences ->
            preferences[Keys.IS_PRO] = entitlement.isPro
            if (entitlement.productId != null) {
                preferences[Keys.PRODUCT_ID] = entitlement.productId
            } else {
                preferences.remove(Keys.PRODUCT_ID)
            }
            if (entitlement.expiryTime != null) {
                preferences[Keys.EXPIRY_TIME] = entitlement.expiryTime
            } else {
                preferences.remove(Keys.EXPIRY_TIME)
            }
        }
        _entitlement.value = entitlement
    }

    override fun isFeatureUnlocked(feature: ProFeature): Boolean {
        return _entitlement.value.isActivePro
    }

    override fun checkFeatureAccess(feature: ProFeature): EntitlementResult<Unit> {
        return if (isFeatureUnlocked(feature)) {
            EntitlementResult.Allowed(Unit)
        } else {
            EntitlementResult.Restricted(
                requiredFeature = feature,
                message = "Feature '${feature.title}' requires ScanFlow Pro."
            )
        }
    }
}
