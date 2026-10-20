package io.github.pylonmc.rebar.util

import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference

class WeakIdentityHashMap<K : Any, V> : MutableMap<K, V> {

    private val queue = ReferenceQueue<K>()
    private val delegateMap = mutableMapOf<IdentityWeakReference<K>, V>()

    override val size: Int
        get() {
            removeStaleElements()
            return delegateMap.size
        }

    private var modifications = 0

    override val keys: MutableSet<K>
        get() {
            removeStaleElements()
            return object : AbstractMutableSet<K>() {

                override val size: Int
                    get() = this@WeakIdentityHashMap.size

                override fun add(element: K): Boolean {
                    throw UnsupportedOperationException()
                }

                override fun remove(element: K): Boolean {
                    return if (element in this@WeakIdentityHashMap) {
                        this@WeakIdentityHashMap.remove(element)
                        true
                    } else {
                        false
                    }
                }

                override fun contains(element: K): Boolean {
                    return element in this@WeakIdentityHashMap
                }

                override fun clear() {
                    this@WeakIdentityHashMap.clear()
                }

                override fun iterator() = object : HashIterator<K>() {
                    override fun next() = nextEntry().key
                }
            }
        }

    override val values: MutableCollection<V>
        get() {
            removeStaleElements()
            return object : AbstractMutableCollection<V>() {

                override val size: Int
                    get() = this@WeakIdentityHashMap.size

                override fun add(element: V): Boolean {
                    throw UnsupportedOperationException()
                }

                override fun contains(element: V): Boolean {
                    return containsValue(element)
                }

                override fun clear() {
                    this@WeakIdentityHashMap.clear()
                }

                override fun iterator() = object : HashIterator<V>() {
                    override fun next() = nextEntry().value
                }
            }
        }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() {
            removeStaleElements()
            return object : AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {

                override val size: Int
                    get() = this@WeakIdentityHashMap.size

                override fun add(element: MutableMap.MutableEntry<K, V>): Boolean {
                    throw UnsupportedOperationException()
                }

                override fun remove(element: MutableMap.MutableEntry<K, V>): Boolean {
                    return if (element in this) {
                        this@WeakIdentityHashMap.remove(element.key)
                        true
                    } else {
                        false
                    }
                }

                override fun contains(element: MutableMap.MutableEntry<K, V>): Boolean {
                    return containsKey(element.key) && get(element.key) == element.value
                }

                override fun clear() {
                    this@WeakIdentityHashMap.clear()
                }

                override fun iterator() = object : HashIterator<MutableMap.MutableEntry<K, V>>() {
                    override fun next() = nextEntry()
                }
            }
        }

    override fun put(key: K, value: V): V? {
        removeStaleElements()
        val key = IdentityWeakReference(key, queue)
        if (key !in delegateMap) {
            modifications++
        }
        return delegateMap.put(key, value)
    }

    override fun remove(key: K): V? {
        removeStaleElements()
        val key = IdentityWeakReference(key)
        if (key in delegateMap) {
            modifications++
        }
        return delegateMap.remove(key)
    }

    override fun putAll(from: Map<out K, V>) {
        for ((key, value) in from) {
            put(key, value)
        }
    }

    override fun clear() {
        removeStaleElements()
        modifications++
        delegateMap.clear()
    }

    override fun isEmpty(): Boolean {
        removeStaleElements()
        return delegateMap.isEmpty()
    }

    override fun containsKey(key: K): Boolean {
        removeStaleElements()
        return delegateMap.containsKey(IdentityWeakReference(key))
    }

    override fun containsValue(value: V): Boolean {
        removeStaleElements()
        return delegateMap.containsValue(value)
    }

    override fun get(key: K): V? {
        removeStaleElements()
        return delegateMap[IdentityWeakReference(key)]
    }

    private fun removeStaleElements() {
        var ref = queue.poll()
        while (ref != null) {
            if (ref in delegateMap) {
                delegateMap.remove(ref)
                modifications++
            }
            ref = queue.poll()
        }
    }

    override fun equals(other: Any?) = other === this || (other is WeakIdentityHashMap<*, *> && delegateMap == other.delegateMap)

    override fun hashCode() = delegateMap.hashCode()

    private class IdentityWeakReference<T>(referent: T, queue: ReferenceQueue<T>? = null) : WeakReference<T>(referent, queue) {
        private val hashCode = System.identityHashCode(referent)
        override fun hashCode() = hashCode
        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is IdentityWeakReference<*>) return false
            val referent = this.get() ?: return false
            return referent === other.get()
        }
    }

    private class Entry<K : Any, V>(val map: WeakIdentityHashMap<K, V>, override val key: K, override var value: V) : MutableMap.MutableEntry<K, V> {
        override fun setValue(newValue: V): V {
            val oldValue = value
            value = newValue
            map[key] = newValue
            return oldValue
        }

        override fun equals(other: Any?) = other is Entry<*, *> && map === other.map && key == other.key && value == other.value

        override fun hashCode() = key.hashCode() xor value.hashCode()

        override fun toString() = "$key=$value"
    }

    private abstract inner class HashIterator<E> : MutableIterator<E> {

        val entryIt: MutableIterator<Entry<K, V>>
        lateinit var lastEntry: Entry<K, V>

        init {
            removeStaleElements()
            entryIt = delegateMap.entries.mapNotNullTo(mutableListOf()) {
                val key = it.key.get() ?: return@mapNotNullTo null
                Entry(this@WeakIdentityHashMap, key, it.value)
            }.iterator()
        }

        var expectedModifications = modifications

        override fun hasNext() = entryIt.hasNext()

        fun nextEntry(): Entry<K, V> {
            if (modifications != expectedModifications) {
                throw ConcurrentModificationException()
            }
            lastEntry = entryIt.next()
            @Suppress("UNCHECKED_CAST")
            lastEntry.value = get(lastEntry.key) as V
            return lastEntry
        }

        override fun remove() {
            if (modifications != expectedModifications) {
                throw ConcurrentModificationException()
            }
            entryIt.remove()
            remove(lastEntry.key)
            expectedModifications = modifications
        }
    }
}