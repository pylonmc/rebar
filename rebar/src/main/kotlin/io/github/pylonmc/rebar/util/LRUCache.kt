package io.github.pylonmc.rebar.util

class LRUCache<K, V>(private val initialCapacity: Int) :
    LinkedHashMap<K, V>(initialCapacity, 0.75f, true) {

    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
        return size > initialCapacity
    }
}