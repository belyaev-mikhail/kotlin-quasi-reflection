package ru.spbstu.kotlin.reflection.quasi

import org.jetbrains.annotations.NotNull
import org.junit.Test
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.reflect
import kotlin.reflect.memberProperties
import kotlin.test.assertEquals

@kotlin.annotation.Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class Whatever(val i: Int)

@kotlin.annotation.Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class Str(val k: String = "Hi")

@kotlin.annotation.Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class Many(vararg val contents: Int)

@Whatever(6) @Str("it > 50")
fun ddd(x: @Whatever(1) @Str("it > 50") Int) {}

data class Example(val x: @Whatever(666) Int)

class AnnotationsTest {

    val thisPkg = javaClass.`package`.name

    @Test
    fun simple() {
        fun ggg(@NotNull x: @Whatever(1) @UnsafeVariance Int) {
        }

        assertEquals(
            UncookedAnnotation(Whatever::class.java.canonicalName, mapOf("i" to 1)).asAnnotation<Whatever>(),
            buildTypeHolderFromInput(::ggg).annotations.first() as Whatever
        )
    }

    @Test
    fun default() {
        fun ggg(@NotNull x: @Str Int) {
        }

        assertEquals(
            UncookedAnnotation(Str::class.java.canonicalName, mapOf("k" to "Hi")).asAnnotation<Str>(),
            buildTypeHolderFromInput(::ggg).annotations.first() as Str
        )
    }



    @Test
    fun varg() {
        fun ggg(@NotNull x: @Many(2, 4, 56) Int) {
        }

        assertEquals(
                UncookedAnnotation(Many::class.java.canonicalName, mapOf("contents" to listOf<Any>(2, 4, 56))).asAnnotation<Many>(),
                buildTypeHolderFromInput(::ggg).annotations.first() as Many
        )
    }
}

