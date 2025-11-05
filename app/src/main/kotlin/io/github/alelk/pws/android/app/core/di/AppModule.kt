package io.github.alelk.pws.android.app.core.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.alelk.pws.data.repository.room.book.BookRepositoryImpl
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.PwsDatabaseProvider
import io.github.alelk.pws.domain.book.repository.BookRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

  @Provides
  @Singleton
  fun provideDatabase(@ApplicationContext context: Context): PwsDatabase = PwsDatabaseProvider.getDatabase(context)

  @Provides
  @Singleton
  fun provideBookRepository(database: PwsDatabase): BookRepository = BookRepositoryImpl(database.bookDao())

}