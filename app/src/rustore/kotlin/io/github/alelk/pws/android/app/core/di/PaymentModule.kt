package io.github.alelk.pws.android.app.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.rustore.sdk.pay.ProductInteractor
import ru.rustore.sdk.pay.PurchaseInteractor
import ru.rustore.sdk.pay.RuStorePayClient
import ru.rustore.sdk.pay.UserInteractor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PaymentModule {

  @Provides
  @Singleton
  fun providePurchaseInteractor(): PurchaseInteractor = RuStorePayClient.Companion.instance.getPurchaseInteractor()

  @Provides
  @Singleton
  fun provideUserInteractor(): UserInteractor = RuStorePayClient.Companion.instance.getUserInteractor()

  @Provides
  @Singleton
  fun provideProductInteractor(): ProductInteractor = RuStorePayClient.Companion.instance.getProductInteractor()

  @Provides
  @Singleton
  fun provideIntentInteractor() = RuStorePayClient.Companion.instance.getIntentInteractor()
}
