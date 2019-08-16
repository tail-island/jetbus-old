package com.tail_island.jetbus.model

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.tail_island.jetbus.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Repository @Inject constructor(
    context: Context,
    private val database: AppDatabase,
    private val webService: WebService
) {
    private val aclConsumerkey = context.getString(R.string.consumerKey)

    private suspend fun <T> getWebServiceBody(webServiceCall: () -> Call<T>): T? = withContext(Dispatchers.IO) {
        val response = webServiceCall().execute()

        if (!response.isSuccessful) {
            Log.e("Repository", "HTTP Error ${response.code()}.")
            return@withContext null
        }

        response.body()!!
    }

    suspend fun clearDatabase() = withContext(Dispatchers.IO) {
        database.withTransaction {
            database.getTimeTableDetailDao().clear()
            database.getTimeTableDao().clear()
            database.getRouteBusStopPoleDao().clear()
            database.getRouteDao().clear()
            database.getBusStopPoleDao().clear()
            database.getBusStopDao().clear()
        }
    }

    suspend fun syncDatabase(): Unit? = withContext(Dispatchers.IO) {
        try {
            if (database.getBusStopDao().getCount() > 0) {
                return@withContext Unit
            }

            val busStopPoleJsonArray = getWebServiceBody { webService.busstopPole(aclConsumerkey)     } ?: return@withContext null
            val routeJsonArray       = getWebServiceBody { webService.busroutePattern(aclConsumerkey) } ?: return@withContext null

            database.withTransaction {
                for (busStopPoleJsonObject in busStopPoleJsonArray.map { it.asJsonObject }.filter { it.get("odpt:operator").asString == "odpt.Operator:Toei" }) {
                    val busStop = database.getBusStopDao().getByName(busStopPoleJsonObject.get("dc:title").asString) ?: run {
                        BusStop(
                            busStopPoleJsonObject.get("dc:title").asString,
                            busStopPoleJsonObject.get("odpt:kana")?.asString
                        ).also {
                            database.getBusStopDao().add(it)
                        }
                    }

                    BusStopPole(
                        busStopPoleJsonObject.get("owl:sameAs").asString,
                        busStop.name,
                        busStopPoleJsonObject.get("geo:lat")?.asFloat,
                        busStopPoleJsonObject.get("geo:long")?.asFloat
                    ).also {
                        database.getBusStopPoleDao().add(it)
                    }
                }

                for (routeJsonObject in routeJsonArray.map { it.asJsonObject}.filter { it.get("odpt:operator").asString == "odpt.Operator:Toei" }) {
                    val route = Route(
                        routeJsonObject.get("owl:sameAs").asString,
                        routeJsonObject.get("dc:title").asString
                    ).also {
                        database.getRouteDao().add(it)
                    }

                    for (routeBusStopPoleJsonObject in routeJsonObject.get("odpt:busstopPoleOrder").asJsonArray.map { it.asJsonObject }) {
                        RouteBusStopPole(
                            route.id,
                            routeBusStopPoleJsonObject.get("odpt:index").asInt,
                            routeBusStopPoleJsonObject.get("odpt:busstopPole").asString
                        ).also {
                            it.id = database.getRouteBusStopPoleDao().add(it)
                        }
                    }
                }
            }

            Unit

        } catch (e: IOException) {
            Log.e("Repository", "${e.message}")
            null
        }
    }

    private suspend fun syncTimeTables(route: Route): Unit? {
        try {
            val timeTableJsonArray = getWebServiceBody { webService.busTimeTable(aclConsumerkey, route.id) } ?: return null

            for (timeTableJsonObject in timeTableJsonArray.map { it.asJsonObject }) {
                val timeTable = TimeTable(
                    timeTableJsonObject.get("owl:sameAs").asString,
                    timeTableJsonObject.get("odpt:busroutePattern").asString
                ).also {
                    database.getTimeTableDao().add(it)
                }

                for (timeTableDetailJsonObject in timeTableJsonObject.get("odpt:busTimetableObject").asJsonArray.map { it.asJsonObject }) {
                    TimeTableDetail(
                        timeTable.id,
                        timeTableDetailJsonObject.get("odpt:index").asInt,
                        timeTableDetailJsonObject.get("odpt:busstopPole").asString,
                        timeTableDetailJsonObject.get("odpt:arrivalTime").asString.split(":").let { (hour, minute) -> hour.toInt() * 60 * 60 + minute.toInt() * 60 }
                    ).also {
                        it.id = database.getTimeTableDetailDao().add(it)
                    }
                }
            }

            return Unit

        } catch (e: IOException) {
            Log.e("Repository", "${e.message}")
            return null
        }
    }

    suspend fun syncTimeTables(routes: Iterable<Route>): Unit? = withContext(Dispatchers.IO) {
        database.withTransaction {
            for (route in routes) {
                if (database.getTimeTableDao().getCountByRouteId(route.id) > 0) {
                    continue
                }

                syncTimeTables(route) ?: return@withTransaction null
            }
        }

        Unit
    }

    suspend fun getBuses(routes: Iterable<Route>, routeBusStopPoles: Iterable<RouteBusStopPole>) = withContext(Dispatchers.IO) {
        try {
            val busStopPoleIds = routeBusStopPoles.groupBy { it.routeId }.map { (routeId, routeBusStopPoles) -> Pair(routeId, routeBusStopPoles.map { it.busStopPoleId }.toSet()) }.toMap()

            getWebServiceBody { webService.bus(aclConsumerkey, routes.map { it.id }.joinToString(",")) }?.filter { bus ->
                bus.fromBusStopPoleId in busStopPoleIds.getValue(bus.routeId) && bus.fromBusStopPoleId != routeBusStopPoles.filter { it.routeId == bus.routeId }.sortedByDescending { it.order }.first().busStopPoleId
            }

        } catch (e: IOException) {
            Log.e("Repository", "${e.message}")
            null
        }
    }

    suspend fun toggleBookmark(departureBusStopName: String, arrivalBusStopName: String) = withContext(Dispatchers.IO) {
        val bookmark = database.getBookmarkDao().getByDepartureBusStopNameAndArrivalBusStopName(departureBusStopName, arrivalBusStopName)

        if (bookmark != null) {
            database.getBookmarkDao().remove(bookmark)
        } else {
            database.getBookmarkDao().add(Bookmark(departureBusStopName, arrivalBusStopName))
        }
    }

    fun getObservableBookmarkByDepartureBusStopAndArrivalBusStop(departureBusStop: BusStop, arrivalBusStop: BusStop) = database.getBookmarkDao().getObservableByDepartureBusStopNameAndArrivalBusStopName(departureBusStop.name, arrivalBusStop.name)
    fun getObservableBookmarks() = database.getBookmarkDao().getObservables()
    fun getObservableBusStop(busStopName: String) = database.getBusStopDao().getObservable(busStopName)
    fun getObservableBusStops() = database.getBusStopDao().getObservables()
    fun getObservableBusStopsByDepartureBusStop(departureBusStop: BusStop) = database.getBusStopDao().getObservablesByDepartureBusStopName(departureBusStop.name)
    fun getObservableBusStopPolesByRouteBusStopPoles(routeBusStopPoles: Iterable<RouteBusStopPole>) = database.getBusStopPoleDao().getObservablesByIds(routeBusStopPoles.map { it.busStopPoleId }.distinct())  // 複数の路線が同じバス停を含んでいる可能性があるので、distinct()しておきます。
    fun getObservableRouteBusStopPolesByRoutes(routes: Iterable<Route>, departureBusStop: BusStop) = database.getRouteBusStopPoleDao().getObservablesByRouteIdsAndDepartureBusStopName(routes.map { it.id }, departureBusStop.name)
    fun getObservableRoutesByDepartureBusStopAndArrivalBusStop(departureBusStop: BusStop, arrivalBusStop: BusStop) = database.getRouteDao().getObservablesByDepartureBusStopNameAndArrivalBusStopName(departureBusStop.name, arrivalBusStop.name)
    fun getObservableTimeTablesByRoutesAndDepartureBusStop(routes: Iterable<Route>, departureBusStop: BusStop) = database.getTimeTableDao().getObservablesByRouteIdsAndDepartureBusStopName(routes.map { it.id }, departureBusStop.name, Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 * 60 + it.get(Calendar.MINUTE) * 60 })
    fun getObservableTimeTableDetailsByTimeTables(timeTables: Iterable<TimeTable>, busStopPoles: Iterable<BusStopPole>) = database.getTimeTableDetailDao().getObservablesByTimeTableIdsAndBusStopPoles(timeTables.map { it.id }, busStopPoles.map { it.id })
}
