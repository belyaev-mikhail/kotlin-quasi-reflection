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
import kotlin.reflect.jvm.internal.impl.platform.JavaToKotlinClassMap

internal val KType.type: KotlinType by JvmReflectionDelegate

val KType.ktype: KotlinType
    get() = this.apply{ toString() }.type

enum class Mutability{ NONE, MUTABLE, IMMUTABLE }

data class TypeHolder(
        val clazz: Class<*>,
        val arguments: List<TypeHolder>,
        val isNullable: Boolean,
        val mutability: Mutability
) {
    private fun formatArgs() = if(arguments.isEmpty()) "" else arguments.joinToString(prefix = "<", postfix = ">")
    private fun formatQuestionMark() = if(isNullable) "?" else ""
    private fun formatMutability() = when(mutability){ Mutability.MUTABLE -> "[Mutable]"; else -> "" }
    override fun toString() = "${formatMutability()}${clazz.simpleName}${formatArgs()}${formatQuestionMark()}"
}

fun TypeHolder.nonNullable() = copy(isNullable = false)
fun TypeHolder.erasure() = copy(arguments = emptyList())

fun buildTypeHolder(java: Type, kotlin: KotlinType): TypeHolder = run {
    val j2k = JavaToKotlinClassMap.INSTANCE
    val mutability = when{
        j2k.isReadOnly(kotlin) -> Mutability.IMMUTABLE
        j2k.isMutable(kotlin) -> Mutability.MUTABLE
        else -> Mutability.NONE
    }

    when(java) {
        is Class<*> -> {
            if(java.isArray) {
                val javaArgs = java.componentType
                val kotlinArgs = kotlin.arguments
                val resArgs = listOf(javaArgs).zip(kotlinArgs){ jArg, kArg ->
                    buildTypeHolder(jArg, kArg.type)
                }
                TypeHolder(Array<Any?>::class.java, resArgs, kotlin.isMarkedNullable, mutability)
            } else TypeHolder(java, emptyList(), kotlin.isMarkedNullable, mutability)
        }
        is ParameterizedType -> {
            val javaArgs = java.actualTypeArguments
            val kotlinArgs = kotlin.arguments
            val resArgs = javaArgs.zip(kotlinArgs){ jArg, kArg ->
                buildTypeHolder(jArg, kArg.type)
            }
            TypeHolder(java.erasure, resArgs, kotlin.isMarkedNullable, mutability)
        }
        is WildcardType -> {
            val ub = java.upperBounds.firstOrNull()
            val lb = java.lowerBounds.firstOrNull()
            val retype = when(ub) {
                null, Any::class.java -> lb ?: Any::class.java
                else -> ub
            }
            buildTypeHolder(retype, kotlin)
        }
        is GenericArrayType -> {
            val javaArgs = java.genericComponentType
            val kotlinArgs = kotlin.arguments
            val resArgs = listOf(javaArgs).zip(kotlinArgs){ jArg, kArg ->
                buildTypeHolder(jArg, kArg.type)
            }
            TypeHolder(Array<Any?>::class.java, resArgs, kotlin.isMarkedNullable, mutability)
        }
        else -> TODO("Sorry, not supported yet: $java -> $kotlin")
    }
}

inline fun<reified T> buildTypeHolder(type: KotlinType): TypeHolder {
    val javatype = javaTypeOf<T>()
    return buildTypeHolder(javatype, type)
}

inline fun<reified T> buildTypeHolder(type: KType): TypeHolder {
    return buildTypeHolder<T>(type.ktype)
}

fun buildTHRaw(type: KType): TypeHolder {
    return buildTypeHolder(type.javaType, type.ktype)
}

inline fun<reified T, R> buildTypeHolderFromInput(noinline discriminator: (T) -> R): TypeHolder {
    return buildTypeHolder<T>(discriminator.reflect()?.parameters?.first()?.type!!)
}

inline fun<reified T> buildTypeHolderFromOutput(noinline discriminator: () -> T): TypeHolder {
    return buildTypeHolder<T>(discriminator.reflect()?.returnType!!)
}