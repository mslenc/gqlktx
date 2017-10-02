package com.xs0.gqlktx.utils

import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.selects.select
import java.util.*
import kotlin.collections.ArrayList

suspend fun <T> awaitAll(futures: List<Deferred<T>>): List<T?> {
    val list: LinkedList<IndexedValue<Deferred<T>>> = LinkedList()
    val result = ArrayList<T?>(futures.size)
    for (el in futures.withIndex()) {
        list += el
        result.add(null)
    }

    var error: Throwable? = null

    try {
        outerLoop@
        while (list.isNotEmpty()) {
            // first we remove everything that has already finished
            val i = list.iterator()
            while (i.hasNext()) {
                val (index, future) = i.next()

                if (future.isCompleted) {
                    i.remove()

                    if (future.isCompletedExceptionally) {
                        error = future.getCompletionException()
                        break@outerLoop
                    } else {
                        result.set(index, future.getCompleted())
                    }
                }
            }

            if (list.isEmpty())
                break

            select<Unit> {
                for ((_, future) in list) {
                    future.onAwait {
                        // just go on with the loop...
                    }
                }
            }
        }
    } catch (e: Throwable) {
        error = e
    }

    if (list.isNotEmpty()) {
        error = error ?: IllegalStateException("futures remaining")
        for ((_, future) in list) {
            future.cancel()
        }
    }

    if (error != null) {
        throw error
    } else {
        return result
    }
}