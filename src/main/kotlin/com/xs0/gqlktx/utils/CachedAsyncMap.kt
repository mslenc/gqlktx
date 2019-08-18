package com.xs0.gqlktx.utils

class CachedAsyncMap<KEY, VAL: Any>(private val compute: suspend (KEY)->VAL) {
    private val entries = HashMap<KEY, CachedAsync<VAL>>()

    suspend fun get(key: KEY): VAL {
        entries[key]?.let { return it.get() }

        return CachedAsync { compute(key) }.also { entries[key] = it }.get()
    }
}