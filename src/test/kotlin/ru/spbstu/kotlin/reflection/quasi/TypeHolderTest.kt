package ru.spbstu.kotlin.reflection.quasi

import org.junit.Test
import kotlin.reflect.memberProperties
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TypeHolderTest {

    val intTH = TypeHolder(Int::class.javaObjectType)

    inline fun listTH(element: TypeHolder) = TypeHolder(List::class.java, listOf(element), false, Mutability.IMMUTABLE)
    inline fun setTH(element: TypeHolder) = TypeHolder(Set::class.java, listOf(element), false, Mutability.IMMUTABLE)
    inline fun mapTH(key: TypeHolder, value: TypeHolder) = TypeHolder(Map::class.java, listOf(key, value), false, Mutability.IMMUTABLE)

    inline fun arrayTH(element: TypeHolder) = TypeHolder(Array<Any>::class.java, listOf(element), false, Mutability.NONE)

    @Test
    fun sanityCheck() {
        assertNotEquals(listTH(intTH), listTH(intTH).nullable)
        assertNotEquals(listTH(intTH.nullable), listTH(intTH))
        assertNotEquals(listTH(intTH).mutable, listTH(intTH))
    }

    @Test
    fun simple() {
        assertEquals(
                buildTypeHolderFromInput { x: List<Int> ->  },
                listTH(intTH)
        )

        assertEquals(
                buildTypeHolderFromInput { x: Set<Map<Int, Int>> ->  },
                setTH(mapTH(intTH, intTH))
        )
    }

    @Test
    fun nullable() {
        assertEquals(
                buildTypeHolderFromInput { x: List<Map<Int?, Int>>? ->  },
                listTH(mapTH(intTH.nullable, intTH)).nullable
        )

        assertEquals(
                buildTypeHolderFromInput { x: List<Map<Int, Set<Int?>>?>? ->  },
                listTH(mapTH(intTH, setTH(intTH.nullable)).nullable).nullable
        )
    }

    @Test
    fun mutable() {
        assertEquals(
                buildTypeHolderFromInput { x: MutableList<Map<Int, List<Int>>> ->  },
                listTH(mapTH(intTH, listTH(intTH))).mutable
        )

        assertEquals(
                buildTypeHolderFromInput { x: MutableList<Map<Int, MutableSet<Int>>> ->  },
                listTH(mapTH(intTH, setTH(intTH).mutable)).mutable
        )
    }

    @Test
    fun builders() {
        data class TestClass(val x: MutableList<Map<Int?, MutableSet<Int>>>?)
        assertEquals(
                listTH(mapTH(intTH.nullable, setTH(intTH).mutable)).mutable.nullable,
                buildTHRaw(TestClass::class.memberProperties.first().returnType)
        )
    }

    @Test
    fun arrays() {
        data class TestClass(val x: Array<List<Int>>)
        assertEquals(
                arrayTH(listTH(intTH)),
                buildTHRaw(TestClass::class.memberProperties.first().returnType)
        )
    }

    @Test
    fun parrays() {
        data class TestClass(val x: Array<Int>)
        assertEquals(
                arrayTH(intTH),
                buildTHRaw(TestClass::class.memberProperties.first().returnType)
        )
    }
}


