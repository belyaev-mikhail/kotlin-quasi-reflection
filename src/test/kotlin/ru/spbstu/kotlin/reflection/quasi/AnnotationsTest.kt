package ru.spbstu.kotlin.reflection.quasi

import org.intellij.lang.annotations.Language
import org.jetbrains.annotations.NotNull
import org.junit.Test
import java.util.*
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.reflect
import kotlin.reflect.memberFunctions
import kotlin.reflect.memberProperties
import kotlin.test.assertEquals

@kotlin.annotation.Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class Whatever(val i: Int)

@kotlin.annotation.Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class Str(val k: String = "Hi")

@kotlin.annotation.Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class Many(vararg val contents: Int)

@kotlin.annotation.Target(
        AnnotationTarget.EXPRESSION,
        AnnotationTarget.LOCAL_VARIABLE,
        AnnotationTarget.TYPE,
        AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.FUNCTION
)
annotation class Omni(val i: Int = 4)

@Whatever(6) @Str("it > 50")
fun ddd(x: @Whatever(1) @Str("it > 50") Int) {}

data class Example(val x: @Whatever(666) Int)

fun<T> generateInfinite(generator: () -> T): Sequence<T> = Sequence {
    object : Iterator<T> {
        override fun hasNext() = true
        override fun next() = generator()
    }
}

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

    @Test
    @Omni(2)
    fun uncontrolled() {
        @Omni(2)
        fun ggg(@Omni(2) x: @Omni(2) Int) {
        }

        assertEquals(
                (javaClass.kotlin.memberFunctions.find { it.name == "uncontrolled" })?.annotations?.first { it is Omni },
                buildTypeHolderFromInput(::ggg).annotations.first() as Omni
        )
    }

    fun Random.nextFloatInRange(min: Float = 0.0f, max: Float = Float.MAX_VALUE) = nextFloat() * (max - min) + min
    fun Random.nextIntInRange(min: Int = 0, max: Int = Int.MAX_VALUE) = nextInt(max - min) + min
    fun<T> Random.choices(table: Map<Int, () -> T>): () -> T {
        val recalc: TreeMap<Int, () -> T> = TreeMap()
        var sum: Int = 0
        for((k, v) in table.entries) {
            recalc[sum] = v
            sum += k
        }
        return {
            val i = nextInt(sum)
            recalc.floorEntry(i).value()
        }
    }
    fun<T> Random.choices(vararg kv: Pair<Int, () -> T>) = choices(mapOf(*kv))

    @Test
    fun whatever() {
        val tp = typeOf { declval<@Omni(2) List<@Omni(2) Int?>>() }
    }
}

