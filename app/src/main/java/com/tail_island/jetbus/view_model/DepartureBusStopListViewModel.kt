package com.tail_island.jetbus.view_model

import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.tail_island.jetbus.busStopIndices
import com.tail_island.jetbus.model.Repository

class DepartureBusStopListViewModel(repository: Repository): ViewModel() {
    val departureBusStops = repository.getObservableBusStops()

    val departureBusStopIndices = Transformations.map(departureBusStops) {
        busStopIndices(it)
    }
}
