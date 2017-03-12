package ru.spbstu.kotlin.reflection.quasi

import ru.spbstu.kotlin.reflection.quasi.java.JvmReflectionDelegate
import kotlin.reflect.KType
import kotlin.reflect.jvm.internal.impl.types.KotlinType

private val KType.type: KotlinType by JvmReflectionDelegate

val KType.kotlinType: KotlinType
    get() = this.apply{ toString() }.type // toString() is needed for lazy initialization
