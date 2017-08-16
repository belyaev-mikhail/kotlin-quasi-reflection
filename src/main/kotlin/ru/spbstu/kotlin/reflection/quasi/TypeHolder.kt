package ru.spbstu.kotlin.reflection.quasi

import ru.spbstu.kotlin.reflection.quasi.java.erasure
import ru.spbstu.kotlin.reflection.quasi.java.javaTypeOf
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.internal.impl.name.FqNameUnsafe
import kotlin.reflect.jvm.internal.impl.platform.JavaToKotlinClassMap
import kotlin.reflect.jvm.internal.impl.types.KotlinType
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.reflect

enum class Mutability{ NONE, MUTABLE, IMMUTABLE }

data class TypeHolder(
        val clazz: Class<*>,
        val arguments: List<TypeHolder> = listOf(),
        val isNullable: Boolean = false,
        val mutability: Mutability = Mutability.NONE,
        val annotations: List<Annotation> = listOf()
) {
    private fun formatArgs() = if(arguments.isEmpty()) "" else arguments.joinToString(prefix = "<", postfix = ">")
    private fun formatQuestionMark() = if(isNullable) "?" else ""
    private fun formatMutability() = when(mutability){ Mutability.MUTABLE -> "[Mutable]"; else -> "" }
    private fun formatAnnotations() = if(annotations.isEmpty()) "" else annotations.joinToString(" ", postfix = " ")

    override fun toString() = "${formatAnnotations()}${formatMutability()}${clazz.canonicalName}${formatArgs()}${formatQuestionMark()}"
}

val TypeHolder.nonNullable: TypeHolder get() = copy(isNullable = false)
val TypeHolder.nullable: TypeHolder get() = copy(isNullable = true)
val TypeHolder.mutable: TypeHolder get() = copy(mutability = Mutability.MUTABLE)
val TypeHolder.immutable: TypeHolder get() = copy(mutability = Mutability.IMMUTABLE)
val TypeHolder.erasure: TypeHolder get() = copy(arguments = emptyList())
val TypeHolder.isPrimitive: Boolean get() = clazz.isPrimitive

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
                TypeHolder(Array<Any?>::class.java, resArgs, kotlin.isMarkedNullable, mutability, kotlin.computeAnnotations())
            } else TypeHolder(java, emptyList(), kotlin.isMarkedNullable, mutability, kotlin.computeAnnotations())
        }
        is ParameterizedType -> {
            val javaArgs = java.actualTypeArguments
            val kotlinArgs = kotlin.arguments
            val resArgs = javaArgs.zip(kotlinArgs){ jArg, kArg ->
                buildTypeHolder(jArg, kArg.type)
            }
            TypeHolder(java.erasure, resArgs, kotlin.isMarkedNullable, mutability, kotlin.computeAnnotations())
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
            TypeHolder(Array<Any?>::class.java, resArgs, kotlin.isMarkedNullable, mutability, kotlin.computeAnnotations())
        }
        else -> TODO("Sorry, not supported yet: $java -> $kotlin")
    }
}

inline fun<reified T> buildTypeHolder(type: KotlinType): TypeHolder {
    val javatype = javaTypeOf<T>()
    return buildTypeHolder(javatype, type)
}

inline fun<reified T> buildTypeHolder(type: KType): TypeHolder {
    return buildTypeHolder<T>(type.kotlinType)
}

fun buildTHRaw(type: KType): TypeHolder {
    return buildTypeHolder(type.javaType, type.kotlinType)
}

inline fun<reified T, R> buildTypeHolderFromInput(noinline discriminator: (T) -> R): TypeHolder {
    return buildTypeHolder<T>(discriminator.reflect()?.parameters?.first()?.type!!)
}

inline fun<reified T> typeOf(noinline discriminator: () -> T): TypeHolder {
    return buildTypeHolder<T>(discriminator.reflect()?.returnType!!)
}

fun <T> declval(): T = error("any() should never be called directly")

fun kclassForName(name: String): KClass<*> {
    val j2k = JavaToKotlinClassMap.INSTANCE
    val jclass = j2k.mapKotlinToJava(FqNameUnsafe(name))?.asSingleFqName()?.toString() ?: name
    return Class.forName(jclass).kotlin
}

fun kclassForName(name: String, initialize: Boolean, loader: ClassLoader): KClass<*> {
    val j2k = JavaToKotlinClassMap.INSTANCE
    val jclass = j2k.mapKotlinToJava(FqNameUnsafe(name))?.asSingleFqName()?.toString() ?: name
    return Class.forName(jclass, initialize, loader).kotlin
}
