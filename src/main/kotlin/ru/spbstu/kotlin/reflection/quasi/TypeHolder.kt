package ru.spbstu.kotlin.reflection.quasi

import ru.spbstu.kotlin.reflection.quasi.java.*
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.internal.impl.types.KotlinType
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.reflect

val KType.type: KotlinType by JvmReflectionDelegate

data class TypeHolder(val clazz: Class<*>, val arguments: List<TypeHolder>, val isNullable: Boolean) {
    private fun formatArgs() = if(arguments.isEmpty()) "" else arguments.joinToString(prefix = "<", postfix = ">")
    private fun formatQuestionMark() = if(isNullable) "?" else ""
    override fun toString() = "${clazz.simpleName}${formatArgs()}${formatQuestionMark()}"
}

fun buildTH(java: Type, kotlin: KotlinType): TypeHolder = run {
    when(java) {
        is Class<*> -> TypeHolder(java, emptyList(), kotlin.isMarkedNullable)
        is ParameterizedType -> {
            val javaArgs = java.actualTypeArguments
            val kotlinArgs = kotlin.arguments
            val resArgs = javaArgs.zip(kotlinArgs){ jArg, kArg ->
                buildTH(jArg, kArg.type)
            }
            TypeHolder(java.erasure, resArgs, kotlin.isMarkedNullable)
        }
        is WildcardType -> {
            val ub = java.upperBounds.firstOrNull()
            val lb = java.lowerBounds.firstOrNull()
            val retype = when(ub) {
                null, Any::class.java -> lb ?: Any::class.java
                else -> ub
            }
            buildTH(retype, kotlin)
        }
        is GenericArrayType -> {
            val javaArgs = java.genericComponentType
            val kotlinArgs = kotlin.arguments
            val resArgs = listOf(javaArgs).zip(kotlinArgs){ jArg, kArg ->
                buildTH(jArg, kArg.type)
            }
            TypeHolder(java.erasure, resArgs, kotlin.isMarkedNullable)
        }
        else -> TODO("Sorry, not supported yet: $java -> $kotlin")
    }
}

inline fun<reified T> buildTH(type: KotlinType): TypeHolder {
    val javatype = javaTypeOf<T>()
    return buildTH(javatype, type)
}

inline fun<reified T> buildTH(type: KType): TypeHolder {
    return buildTH<T>(type.type)
}

fun buildTHRaw(type: KType): TypeHolder {
    return buildTH(type.javaType, type.type)
}

inline fun<reified T> buildTH(noinline discriminator: (T) -> Unit): TypeHolder {
    return buildTH<T>(discriminator.reflect()?.parameters?.first()?.type!!)
}

inline fun<reified T> buildTH(noinline discriminator: () -> T): TypeHolder {
    return buildTH<T>(discriminator.reflect()?.returnType!!)
}