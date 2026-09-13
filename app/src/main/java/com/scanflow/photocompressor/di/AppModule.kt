package com.scanflow.photocompressor.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import com.scanflow.photocompressor.data.local.AppDatabase
import com.scanflow.photocompressor.data.local.HistoryDao
import com.scanflow.photocompressor.data.local.PresetDao
import com.scanflow.photocompressor.data.repository.HistoryRepositoryImpl
import com.scanflow.photocompressor.data.repository.ImageRepositoryImpl
import com.scanflow.photocompressor.data.repository.PresetRepositoryImpl
import com.scanflow.photocompressor.domain.repository.HistoryRepository
import com.scanflow.photocompressor.domain.repository.ImageRepository
import com.scanflow.photocompressor.domain.repository.PresetRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.scanflow.photocompressor.data.repository.PreferencesRepositoryImpl
import com.scanflow.photocompressor.domain.repository.PreferencesRepository

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver {
        return context.contentResolver
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
        .addMigrations(com.scanflow.photocompressor.data.local.Migrations.MIGRATION_1_2)
        .build()
    }

    @Provides
    fun provideHistoryDao(database: AppDatabase): HistoryDao {
        return database.historyDao()
    }

    @Provides
    fun providePresetDao(database: AppDatabase): PresetDao {
        return database.presetDao()
    }

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): androidx.work.WorkManager {
        return androidx.work.WorkManager.getInstance(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindImageRepository(impl: ImageRepositoryImpl): ImageRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindPresetRepository(impl: PresetRepositoryImpl): PresetRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindUserTierRepository(impl: com.scanflow.photocompressor.data.repository.UserTierRepositoryImpl): com.scanflow.photocompressor.domain.repository.UserTierRepository

    @Binds
    @Singleton
    abstract fun bindEntitlementRepository(impl: com.scanflow.photocompressor.data.repository.EntitlementRepositoryImpl): com.scanflow.photocompressor.domain.repository.EntitlementRepository

    @Binds
    @Singleton
    abstract fun bindBillingManager(impl: com.scanflow.photocompressor.data.billing.BillingManagerImpl): com.scanflow.photocompressor.domain.billing.BillingManager

    @Binds
    @Singleton
    abstract fun bindAdManager(impl: com.scanflow.photocompressor.data.ad.AdManagerImpl): com.scanflow.photocompressor.domain.ad.AdManager
}
