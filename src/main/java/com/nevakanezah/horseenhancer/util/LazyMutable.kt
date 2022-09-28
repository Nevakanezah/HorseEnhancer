package com.nevakanezah.horseenhancer.util

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

// https://stackoverflow.com/a/47948047
class LazyMutable<T>(val initializer: () -> T) : ReadWriteProperty<Any?, T> {
    private object UninitialisedValue
    private var prop: Any? = UninitialisedValue

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return if (prop == UninitialisedValue) {
            synchronized(this) {
                return if (prop == UninitialisedValue) initializer().also { prop = it } else prop as T
            }
        } else prop as T
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        synchronized(this) {
            prop = value
        }
    }
}
