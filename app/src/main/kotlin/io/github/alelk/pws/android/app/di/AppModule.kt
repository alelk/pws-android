package io.github.alelk.pws.android.app.di

import android.content.Context
import io.github.alelk.pws.database.PwsDatabaseProvider
import io.github.alelk.pws.database.PwsDatabase
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
  fun provideDatabase(@ApplicationContext context: Context): PwsDatabase = PwsDatabaseProvider.getDatabase(context)
}