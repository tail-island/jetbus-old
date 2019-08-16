package com.tail_island.jetbus.view_model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.tail_island.jetbus.busStopIndices
import com.tail_island.jetbus.model.Repository

class ArrivalBusStopListViewModel(private val repository: Repository): ViewModel() {
    val departureBusStopName = MutableLiveData<String>()

    val departureBusStop = Transformations.switchMap(departureBusStopName) {
        repository.getObservableBusStop(it)
    }

    val arrivalBusStops = Transformations.switchMap(departureBusStop) {
        repository.getObservableBusStopsByDepartureBusStop(it)
    }

    val arrivalBusStopIndices = Transformations.map(arrivalBusStops) {
        busStopIndices(it)
    }
}
