package com.tail_island.jetbus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tail_island.jetbus.model.BusStop
import dagger.MapKey
import javax.inject.Inject
import javax.inject.Provider
import kotlin.reflect.KClass

// ViewModel and DI...

@Retention(AnnotationRetention.RUNTIME)
@MapKey
annotation class ViewModelKey(val value: KClass<out ViewModel>)

class AppViewModelProviderFactory @Inject constructor(private val factories: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>): ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T: ViewModel> create(modelClass: Class<T>): T {
        return factories.entries.find { modelClass.isAssignableFrom(it.key) }!!.value.get() as T
    }
}

// index converter

private val indexConverter = "ぁぃぅぇぉがぎぐげござじずぜぞだぢっづでどばぱびぴぶぷべぺぼぽゃゅょゎゐゑゔゕゖ".zip("あいうえおかきくけこさしすせそたちつつてとははひひふふへへほほやゆよわいえうかけ").toMap()

fun convertIndex(index: Char): Char {
    return when (index) {
        in '\u30a1'..'\u30fa' -> index - 0x0060
        else                  -> index
    }.let {
        indexConverter[it] ?: it
    }
}

fun busStopIndices(busStops: List<BusStop>): List<Char> {
    return busStops.asSequence().map { busStop -> busStop.phoneticName?.firstOrNull() }.filterNotNull().map { convertIndex(it) }.distinct().toList()
}

fun busStopPosition(busStops: List<BusStop>, index: Char): Int {
    return busStops.indexOfFirst { busStop -> busStop.phoneticName?.firstOrNull()?.let { convertIndex(it) == index } ?: false }
}
