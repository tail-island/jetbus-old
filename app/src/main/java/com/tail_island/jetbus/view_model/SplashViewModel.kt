package com.tail_island.jetbus.view_model

import androidx.lifecycle.ViewModel
import com.tail_island.jetbus.model.Repository

class SplashViewModel(private val repository: Repository): ViewModel() {
    suspend fun syncDatabase() = repository.syncDatabase()
}
