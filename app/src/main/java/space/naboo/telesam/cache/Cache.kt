package space.naboo.telesam.cache

interface Cache<in K: Any, V: Any> {
    fun get(key: K): V?
    fun put(key: K, value: V): Boolean
    fun remove(key: K)
    fun clear()
}
