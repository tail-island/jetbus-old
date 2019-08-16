package com.tail_island.jetbus

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.room.Room
import com.tail_island.jetbus.model.AppDatabase
import com.tail_island.jetbus.model.Repository
import com.tail_island.jetbus.model.WebService
import com.tail_island.jetbus.view_model.*
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@Module
class AppModule(private val application: Application) {
    // context

    @Provides
    fun provideContext() = application as Context

    // database

    @Provides
    fun provideAppDatabase(context: Context) = Room.databaseBuilder(context, AppDatabase::class.java, "jetbus.db").build()

    // web services

    @Provides
    fun provideWebService(): WebService {
        val okHttpClient = OkHttpClient.Builder().apply {
            connectTimeout(180, TimeUnit.SECONDS)
            readTimeout(180, TimeUnit.SECONDS)
            writeTimeout(180, TimeUnit.SECONDS)
        }.build()

        val retrofit = Retrofit.Builder().apply {
            baseUrl("https://api.odpt.org")
            client(okHttpClient)
            addConverterFactory(GsonConverterFactory.create())
        }.build()

        return retrofit.create(WebService::class.java)
    }

    // view models.

    @Provides
    @IntoMap
    @ViewModelKey(ArrivalBusStopListViewModel::class)
    fun provideArrivalBusStopListViewModel(repository: Repository) = ArrivalBusStopListViewModel(repository) as ViewModel

    @Provides
    @IntoMap
    @ViewModelKey(BookmarkListViewModel::class)
    fun provideBookmarkListViewModel(repository: Repository) = BookmarkListViewModel(repository) as ViewModel

    @Provides
    @IntoMap
    @ViewModelKey(BusApproachListViewModel::class)
    fun provideBusApproachListViewModel(repository: Repository, context: Context) = BusApproachListViewModel(repository, context) as ViewModel

    @Provides
    @IntoMap
    @ViewModelKey(DepartureBusStopListViewModel::class)
    fun provideDepartureBusStopListViewModel(repository: Repository) = DepartureBusStopListViewModel(repository) as ViewModel

    @Provides
    @IntoMap
    @ViewModelKey(SplashViewModel::class)
    fun provideSplashViewModel(repository: Repository) = SplashViewModel(repository) as ViewModel
}
