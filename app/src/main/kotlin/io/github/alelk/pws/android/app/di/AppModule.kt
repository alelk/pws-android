package io.github.alelk.pws.android.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.alelk.pws.database.BuildConfig
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.PwsDatabaseProvider
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

  init {
    if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
  }

  @Provides
  @Singleton
  fun provideDatabase(@ApplicationContext context: Context): PwsDatabase = PwsDatabaseProvider.getDatabase(context)
}