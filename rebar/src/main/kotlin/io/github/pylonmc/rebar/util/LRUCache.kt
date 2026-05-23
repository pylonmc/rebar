package io.github.pylonmc.rebar.util

/**
 * A map where if capacity is reached, the least recently used element is removed
 */
class LRUCache<K, V>(val capacity: Int) :
    LinkedHashMap<K, V>(16, 0.75f, true) {

    /**
     * Called on the oldest element when a new element is
     * added, it will be deleted if this returns true
     * @see java.util.LinkedHashMap.removeEldestEntry
     */
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
        return size > capacity
    }
}