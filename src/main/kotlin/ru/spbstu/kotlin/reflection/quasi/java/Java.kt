package ru.spbstu.kotlin.reflection.quasi.java

import java.lang.reflect.*

abstract class SuperClassTypeToken<T>() {
    val type: Type = this.javaClass.genericSuperclass.let { it as ParameterizedType }.actualTypeArguments[0]
}

inline fun <reified T> javaTypeOf(): Type {
    val disc = object : SuperClassTypeToken<T>() {}
    return disc.type
}

val Type.erasure: Class<*>
    get() =
    when (this) {
        is Class<*> -> this
        is ParameterizedType -> this.rawType.erasure
        is WildcardType -> (this.upperBounds.firstOrNull()?.erasure ?: this.lowerBounds.firstOrNull()?.erasure) ?: Any::class.java
        is GenericArrayType -> java.lang.reflect.Array.newInstance(this.genericComponentType.erasure, 0).javaClass
        is TypeVariable<*> -> this.bounds.firstOrNull()?.erasure ?: Any::class.java
        else -> Any::class.java
    }