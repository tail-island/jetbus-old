package com.tail_island.jetbus.model

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BookmarkDao {
    @Insert
    fun add(bookmark: Bookmark)

    @Delete
    fun remove(bookmark: Bookmark)

    @Query("SELECT * FROM Bookmark WHERE departureBusStopName = :departureBusStopName AND arrivalBusStopName = :arrivalBusStopName LIMIT 1")
    fun getByDepartureBusStopNameAndArrivalBusStopName(departureBusStopName: String, arrivalBusStopName: String): Bookmark?

    @Query("SELECT * FROM Bookmark WHERE departureBusStopName = :departureBusStopName AND arrivalBusStopName = :arrivalBusStopName LIMIT 1")
    fun getObservableByDepartureBusStopNameAndArrivalBusStopName(departureBusStopName: String, arrivalBusStopName: String): LiveData<Bookmark?>

    @Query("SELECT Bookmark.* FROM Bookmark INNER JOIN BusStop AS DepartureBusStop ON DepartureBusStop.name = Bookmark.departureBusStopName ORDER BY DepartureBusStop.phoneticName")
    fun getObservables(): LiveData<List<Bookmark>>
}
