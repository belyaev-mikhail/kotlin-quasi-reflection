package ru.spbstu.kotlin.reflection.quasi

import java.lang.reflect.Proxy
import kotlin.reflect.jvm.internal.impl.renderer.DescriptorRenderer
import kotlin.reflect.jvm.internal.impl.resolve.constants.ArrayValue
import kotlin.reflect.jvm.internal.impl.resolve.constants.ConstantValue
import kotlin.reflect.jvm.internal.impl.types.KotlinType

data class UncookedAnnotation(val fullName: String, val arguments: Map<String, Any?>) {
    override fun toString() = "@$fullName(${arguments.asSequence().joinToString()})"
}

private val ConstantValue<*>.adjustedValue: Any?
                get() = when(this) { is ArrayValue -> value.map{it.value}; else -> value }

private val KotlinType.uncookedAnnotations: List<UncookedAnnotation>
        get() =
            this.annotations.map { desc ->
                UncookedAnnotation(
                        DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(desc.type),
                        desc.allValueArguments.asSequence().map { e -> Pair("${e.key.name}", e.value.adjustedValue) }.toMap()
                )
            }

inline fun<reified A: kotlin.Annotation> UncookedAnnotation.asAnnotation(): A? {
    val anno = UncookedAnnotation@this
    val classLoader = this.javaClass.classLoader
    try {
        val realClass = kclassForName(fullName, true, classLoader).java
        val expando = realClass.methods.asSequence().map { method ->
            method.name to when (method.name) {
                "toString" -> { proxy: Any, args: Array<Any>? ->
                    anno.toString()
                }
                "equals" -> { proxy: Any, args: Array<Any>? ->
                    val that = args!![0]
                    proxy.javaClass == that.javaClass
                         && arguments.all { e ->
                                java.util.Objects.deepEquals(
                                        that.javaClass.getDeclaredMethod(e.key)?.invoke(proxy),
                                        that.javaClass.getDeclaredMethod(e.key)?.invoke(that)
                                )
                            }
                }
                "hashCode" -> { proxy: Any, args: Array<Any>? -> anno.hashCode() }
                else -> {
                    if(method.returnType.isArray) {
                        // this is unfortunate, but we do not know the actual class before this point
                        val v = arguments[method.name] as List<*>
                        val ret = java.lang.reflect.Array.newInstance(method.returnType.componentType, v.size)
                        for(i in 0..v.size - 1) {
                            java.lang.reflect.Array.set(ret, i, v[i])
                        }
                        { proxy: Any, args: Array<Any>? -> ret }
                    } else {
                        val ret = arguments[method.name]
                        { proxy: Any, args: Array<Any>? -> ret }
                    }
                }
            }
        }.toMap()

        val ret = Proxy.newProxyInstance(classLoader, arrayOf(realClass)) {
            proxy, method, args ->
                val f = expando[method.name]
                f?.invoke(proxy, args)
        }
        return ret as A
    } catch(ex: ClassNotFoundException) {
        return null
    }
}

fun KotlinType.computeAnnotations() =
        uncookedAnnotations.map{ it.asAnnotation<Annotation>() }.filterNotNull()
