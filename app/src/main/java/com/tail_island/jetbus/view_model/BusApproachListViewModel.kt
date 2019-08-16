package com.tail_island.jetbus.view_model

import android.content.Context
import androidx.lifecycle.*
import com.tail_island.jetbus.R
import com.tail_island.jetbus.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BusApproachListViewModel(private val repository: Repository, private val context: Context): ViewModel() {
    val departureBusStopName = MutableLiveData<String>()

    val arrivalBusStopName = MutableLiveData<String>()

    val departureBusStop = Transformations.switchMap(departureBusStopName) {
        repository.getObservableBusStop(it)
    }

    val arrivalBusStop = Transformations.switchMap(arrivalBusStopName) {
        repository.getObservableBusStop(it)
    }

    val bookmark = MediatorLiveData<Bookmark?>().apply {
        var source: LiveData<Bookmark?>? = null

        fun update() {
            val departureBusStopValue = departureBusStop.value ?: return
            val arrivalBusStopValue   = arrivalBusStop.value   ?: return

            source?.let {
                removeSource(it)
            }

            source = repository.getObservableBookmarkByDepartureBusStopAndArrivalBusStop(departureBusStopValue, arrivalBusStopValue).also {
                addSource(it) { sourceValue ->
                    value = sourceValue
                }
            }
        }

        addSource(departureBusStop) { update() }
        addSource(arrivalBusStop)   { update() }
    }

    val routes = MediatorLiveData<List<Route>>().apply {
        var source: LiveData<List<Route>>? = null

        fun update() {
            val departureBusStopValue = departureBusStop.value ?: return
            val arrivalBusStopValue   = arrivalBusStop.value   ?: return

            source?.let {
                removeSource(it)
            }

            source = repository.getObservableRoutesByDepartureBusStopAndArrivalBusStop(departureBusStopValue, arrivalBusStopValue).also {
                addSource(it) { sourceValue ->
                    value = sourceValue
                }
            }
        }

        addSource(departureBusStop) { update() }
        addSource(arrivalBusStop)   { update() }
    }

    val routeBusStopPoles = MediatorLiveData<List<RouteBusStopPole>>().apply {
        var source: LiveData<List<RouteBusStopPole>>? = null

        fun update() {
            val routesValue           = routes.value           ?: return
            val departureBusStopValue = departureBusStop.value ?: return

            source?.let {
                removeSource(it)
            }

            source = repository.getObservableRouteBusStopPolesByRoutes(routesValue, departureBusStopValue).also {
                addSource(it) { sourceValue ->
                    value = sourceValue
                }
            }
        }

        addSource(routes)           { update() }
        addSource(departureBusStop) { update() }
    }

    val busStopPoles = Transformations.switchMap(routeBusStopPoles) {
        repository.getObservableBusStopPolesByRouteBusStopPoles(it)
    }

    val timeTables = MediatorLiveData<List<TimeTable>>().apply {
        var source: LiveData<List<TimeTable>>? = null

        fun update() {
            val routesValue            = routes.value           ?: return
            val departureBusStopValue  = departureBusStop.value ?: return

            source?.let {
                removeSource(it)
            }

            source = repository.getObservableTimeTablesByRoutesAndDepartureBusStop(routesValue, departureBusStopValue).also {
                addSource(it) { sourceValue ->
                    value = sourceValue
                }
            }

            viewModelScope.launch {
                repository.syncTimeTables(routesValue)
            }
        }

        addSource(routes)           { update() }
        addSource(departureBusStop) { update() }
    }

    val timeTableDetails = MediatorLiveData<List<TimeTableDetail>>().apply {
        var source: LiveData<List<TimeTableDetail>>? = null

        fun update() {
            val timeTablesValue   = timeTables.value   ?: return
            val busStopPolesValue = busStopPoles.value ?: return

            source?.let {
                removeSource(it)
            }

            source = repository.getObservableTimeTableDetailsByTimeTables(timeTablesValue, busStopPolesValue).also {
                addSource(it) { sourceValue ->
                    value = sourceValue
                }
            }
        }

        addSource(timeTables)   { update() }
        addSource(busStopPoles) { update() }
    }

    val buses = MediatorLiveData<List<Bus>>().apply {
        fun update() {
            val routesValue            = routes.value            ?: return
            val routeBusStopPolesValue = routeBusStopPoles.value ?: return

            viewModelScope.launch {
                value = repository.getBuses(routesValue, routeBusStopPolesValue)

                delay(15000)

                update()
            }
        }

        addSource(routes)            { update() }
        addSource(routeBusStopPoles) { update() }
    }

    val busApproaches = MediatorLiveData<List<BusApproach>>().apply {
        fun update() {
            val routesValue            = routes.value            ?: return
            val routeBusStopPolesValue = routeBusStopPoles.value ?: return
            val busStopPolesValue      = busStopPoles.value      ?: return
            val timeTablesValue        = timeTables.value
            val timeTableDetailsValue  = timeTableDetails.value
            val busesValue             = buses.value             ?: return

            value = busesValue.map { bus ->
                BusApproach(
                    bus.id,
                    timeTablesValue?.find { timeTable -> timeTable.routeId == bus.routeId }?.let { timeTable ->
                        timeTableDetailsValue?.filter { it.timeTableId == timeTable.id }?.sortedByDescending { it.order }?.takeWhile { it.busStopPoleId != bus.fromBusStopPoleId }?.zipWithNext()?.map { (next, prev) -> next.arrival - prev.arrival }?.sum()
                    },
                    routeBusStopPolesValue.sortedByDescending { it.order }.let { it.first().order - it.find { routeBusStopPole -> routeBusStopPole.busStopPoleId == bus.fromBusStopPoleId }!!.order },
                    routesValue.find { it.id == bus.routeId }!!.name,
                    busStopPolesValue.find { it.id == bus.fromBusStopPoleId }!!.busStopName
                )
            }.sortedWith(compareBy({ it.willArriveAfter ?: Int.MAX_VALUE }, { it.busStopCount }))
        }

        addSource(routes)            { update() }
        addSource(routeBusStopPoles) { update() }
        addSource(busStopPoles)      { update() }
        addSource(timeTables)        { update() }
        addSource(timeTableDetails)  { update() }
        addSource(buses)             { update() }
    }

    fun toggleBookmark() = viewModelScope.launch {
        val departureBusStopNameValue = departureBusStopName.value ?: return@launch
        val arrivalBusStopNameValue   = arrivalBusStopName.value   ?: return@launch

        repository.toggleBookmark(departureBusStopNameValue, arrivalBusStopNameValue)
    }
}
