package com.tail_island.jetbus

import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {
    fun inject(mainActivity: MainActivity)
    fun inject(arrivalBusStopListFragment: ArrivalBusStopListFragment)
    fun inject(bookmarkListFragment: BookmarkListFragment)
    fun inject(busApproachListFragment: BusApproachListFragment)
    fun inject(departureBusStopListFragment: DepartureBusStopListFragment)
    fun inject(splashFragment: SplashFragment)
}
