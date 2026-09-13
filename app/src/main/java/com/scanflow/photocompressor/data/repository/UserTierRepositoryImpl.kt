package com.scanflow.photocompressor.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.scanflow.photocompressor.domain.model.ProFeature
import com.scanflow.photocompressor.domain.model.UserTier
import com.scanflow.photocompressor.domain.repository.UserTierRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserTierRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserTierRepository {

    private object Keys {
        val IS_PRO_USER = booleanPreferencesKey("is_pro_user")
    }

    override val currentTier: Flow<UserTier> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val isPro = preferences[Keys.IS_PRO_USER] ?: false
            if (isPro) UserTier.PRO else UserTier.FREE
        }

    override suspend fun setUserTier(tier: UserTier) {
        dataStore.edit { preferences ->
            preferences[Keys.IS_PRO_USER] = (tier == UserTier.PRO)
        }
    }

    override fun isFeatureAvailable(feature: ProFeature, currentTier: UserTier): Boolean {
        return when (currentTier) {
            UserTier.PRO -> true
            UserTier.FREE -> false
        }
    }

    override suspend fun isPro(): Boolean {
        return currentTier.first() == UserTier.PRO
    }
}
