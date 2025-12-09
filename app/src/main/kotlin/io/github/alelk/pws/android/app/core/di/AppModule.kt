package io.github.alelk.pws.android.app.core.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.alelk.pws.data.repository.room.book.BookObserveRepositoryImpl
import io.github.alelk.pws.data.repository.room.bookstatistic.BookStatisticRepositoryImpl
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.PwsDatabaseProvider
import io.github.alelk.pws.domain.book.repository.BookObserveRepository
import io.github.alelk.pws.domain.book.usecase.ObserveBooksUseCase
import io.github.alelk.pws.domain.bookstatistic.repository.BookStatisticRepository
import io.github.alelk.pws.domain.bookstatistic.usecase.UpdateBookStatisticUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

  @Provides
  @Singleton
  fun provideDatabase(@ApplicationContext context: Context): PwsDatabase = PwsDatabaseProvider.getDatabase(context)

  @Provides
  fun provideBookRepository(database: PwsDatabase): BookObserveRepository = BookObserveRepositoryImpl(database.bookDao())

  @Provides
  fun provideBookStatisticRepository(database: PwsDatabase): BookStatisticRepository = BookStatisticRepositoryImpl(database.bookStatisticDao())

  @Provides
  fun provideObserveBooksUseCase(bookRepository: BookObserveRepository): ObserveBooksUseCase = ObserveBooksUseCase(bookRepository)

  @Provides
  fun provideUpdateBookStatisticUseCase(bookStatisticRepository: BookStatisticRepository): UpdateBookStatisticUseCase =
    UpdateBookStatisticUseCase(bookStatisticRepository)

}