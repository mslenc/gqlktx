package com.xs0.gqlktx.types.kotlin.lists

import com.xs0.gqlktx.exec.InputVarParser
import com.xs0.gqlktx.types.gql.GListType
import com.xs0.gqlktx.types.kotlin.GJavaListLikeType
import com.xs0.gqlktx.types.kotlin.GJavaType
import io.vertx.core.json.JsonArray
import kotlin.reflect.KType

import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

class GJavaCollectionType<CTX>(listClass: KType, elementType: GJavaType<CTX>, gqlListType: GListType) : GJavaListLikeType<CTX>(listClass, gqlListType, elementType) {
    private val factoryClass: KClass<*>

    init {
        this.factoryClass = findConcreteClass(listClass)

        try {
            if (this.factoryClass.java.newInstance() !is Collection<*>)
                throw IllegalStateException("Not a Collection")
        } catch (e: Exception) {
            throw IllegalArgumentException("Can't construct a $factoryClass instance", e)
        }
    }

    private fun findConcreteClass(listClass: KType): KClass<*> {
        var klass: KClass<*>?
        if (listClass.classifier is KClass<*>) {
            klass = listClass.classifier as KClass<*>
        } else {
            klass = null
        }

        if (klass != null && !Collection::class.isSuperclassOf(klass))
            klass = null

        if (klass == null)
            throw IllegalArgumentException("Can't resolve $listClass to a collection type")

        return if (klass.isAbstract) {
            if (List::class.isSuperclassOf(klass)) {
                ArrayList::class
            } else if (Set::class.isSuperclassOf(klass)) {
                LinkedHashSet::class
            } else if (Queue::class.isSuperclassOf(klass)) {
                LinkedBlockingQueue::class
            } else {
                ArrayList::class
            }
        } else {
            klass
        }
    }

    override fun createList(size: Int): MutableCollection<Any?> {
        try {
            return factoryClass.java.newInstance() as MutableCollection<Any?>
        } catch (e: Exception) {
            throw Error(e) // this is most unusual- we verify the thing in constructor
        }
    }

    override fun getListSize(list: Any): Int {
        return (list as Collection<*>).size
    }

    override fun getIterator(list: Any): Iterator<*> {
        return (list as Collection<*>).iterator()
    }

    override fun appendListElement(list: Any, index: Int, value: Any) {
        (list as MutableCollection<Any?>).add(value)
    }

    @Throws(Exception::class)
    override fun transformFromJson(array: JsonArray, inputVarParser: InputVarParser<CTX>): Collection<Any?> {
        val res = createList(array.size())

        for (i in 0 until array.size()) {
            val el = array.getValue(i)
            if (el == null) {
                if (elementType.isNullAllowed()) {
                    res.add(null)
                } else {
                    throw IllegalArgumentException("null encountered in array of supposedly non-null values")
                }
            } else {
                res.add(elementType.getFromJson(el, inputVarParser))
            }
        }

        return res
    }
}
