package com.islami.Aha.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.islami.Aha.data.local.AppDatabase
import com.islami.Aha.data.local.DailyIslamicContentDao
import com.islami.Aha.data.local.HabitCompletionDao
import com.islami.Aha.data.local.HabitDao
import com.islami.Aha.data.local.UserHabitDao
import com.islami.Aha.data.repository.AuthRepository
import com.islami.Aha.data.repository.DailyIslamicContentRepository
import com.islami.Aha.data.repository.UserHabitRepository
import com.islami.Aha.util.SecurePrefsProvider
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "aha_db"
        )
        .addMigrations(
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7,
            AppDatabase.MIGRATION_7_8,
            AppDatabase.MIGRATION_8_9,
            AppDatabase.MIGRATION_9_10,
            AppDatabase.MIGRATION_10_11
        )
        .build()
    }

    @Provides
    @Singleton
    fun provideHabitDao(appDatabase: AppDatabase): HabitDao {
        return appDatabase.habitDao()
    }

    @Provides
    @Singleton
    fun provideHabitCompletionDao(appDatabase: AppDatabase): HabitCompletionDao {
        return appDatabase.habitCompletionDao()
    }

    @Provides
    @Singleton
    fun provideDailyIslamicContentDao(appDatabase: AppDatabase): DailyIslamicContentDao {
        return appDatabase.dailyIslamicContentDao()
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return SecurePrefsProvider.get(context)
    }

    @Provides
    @Singleton
    fun provideUserHabitDao(appDatabase: AppDatabase): UserHabitDao {
        return appDatabase.userHabitDao()
    }

    @Provides
    @Singleton
    fun provideUserHabitRepository(
        appDatabase: AppDatabase,
        userHabitDao: UserHabitDao,
        habitCompletionDao: HabitCompletionDao
    ): UserHabitRepository {
        return UserHabitRepository(appDatabase, userHabitDao, habitCompletionDao)
    }

    @Provides
    @Singleton
    fun provideDailyIslamicContentRepository(
        dailyIslamicContentDao: DailyIslamicContentDao
    ): DailyIslamicContentRepository {
        return DailyIslamicContentRepository(dailyIslamicContentDao)
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
        sharedPreferences: SharedPreferences
    ): AuthRepository {
        return AuthRepository(firebaseAuth, sharedPreferences)
    }
}
