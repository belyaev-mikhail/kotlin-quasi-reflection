package ru.spbstu.kotlin.reflection.quasi.java

import kotlin.reflect.KProperty
import kotlin.reflect.memberProperties

object JvmReflectionDelegate {
    inline operator fun<reified T: Any?> getValue(thisRef: Any?, property: KProperty<*>): T = run {
        thisRef!!
        val method = thisRef.javaClass.declaredMethods.find {
            it.name == property.name
                    || it.name == "is${property.name.capitalize()}"
                    || it.name == "get${property.name.capitalize()}"
        } ?: throw NoSuchFieldException("${property.name}")
        method(thisRef) as T
    }

    inline operator fun<reified T: Any?> setValue(thisRef: Any?, property: KProperty<*>, newValue: T): Unit = run {
        thisRef!!
        val method = thisRef.javaClass.declaredMethods.find {
            it.name == "set${property.name.capitalize()}"
        } ?: throw NoSuchFieldException("${property.name}")
        method(thisRef, newValue)
    }
}

object KotlinReflectionDelegate {
    inline operator fun<reified T: Any?> getValue(thisRef: Any?, property: KProperty<*>): T = run {
        thisRef!!
        thisRef.javaClass.kotlin.memberProperties.find { it.name == property.name }?.get(thisRef) as T
    }
}