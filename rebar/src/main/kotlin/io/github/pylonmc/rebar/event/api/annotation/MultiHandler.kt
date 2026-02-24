package io.github.pylonmc.rebar.event.api.annotation

import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.item.base.RebarInteractor
import io.github.pylonmc.rebar.util.isSubclassOf
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * A variant of [EventHandler] that listens to all priorities specified
 * Must be used with [MultiListener] registered using [MultiListener.register] unless it's a
 * Rebar block/item/entity interface such as [RebarInteractor], those are handled internally using [MultiHandler.handleEvent]
 *
 * All methods annotated with this should be formatted as `fun methodName(event: Event, priority: EventPriority)`
 */
annotation class MultiHandler(
    val priorities: Array<EventPriority> = [EventPriority.NORMAL],
    val ignoreCancelled: Boolean = false
) {
    companion object {
        private val DEFAULT = MultiHandler()
        private val HANDLERS = mutableMapOf<Class<*>, MutableMap<EventInfo, (Any, Any, EventPriority) -> Unit>>()

        @JvmStatic
        fun <E : Event> handleEvent(
            handler: Any,
            handlerMethod: String,
            event: E,
            priority: EventPriority
        ) {
            val directClass = handler::class.java
            val handlerMap = HANDLERS.computeIfAbsent(directClass) { _ -> mutableMapOf() }
            val eventClass = event::class.java
            val info = EventInfo(handlerMethod, eventClass)
            val function = handlerMap.getOrPut(info) {
                val method = findMethod(directClass, info)
                if (!method.trySetAccessible()) {
                    throw IllegalStateException("Could not access method ${method.name} in class ${directClass.name}")
                }

                try {
                    val lookup = MethodHandles.privateLookupIn(directClass, MethodHandles.lookup())
                    val methodHandle = lookup.unreflect(method)

                    val annotation = method.getAnnotation(MultiHandler::class.java) ?: DEFAULT
                    val priorities = annotation.priorities.toSet()
                    val ignoreCancelled = annotation.ignoreCancelled

                    { instance, evt, priority ->
                        if ((evt !is Cancellable || !evt.isCancelled || !ignoreCancelled) && priorities.contains(
                                priority
                            )
                        ) {
                            methodHandle.invoke(instance, evt, priority)
                        }
                    }
                } catch (e: IllegalAccessException) {
                    throw IllegalStateException(
                        "Could not access method ${method.name} in class ${directClass.name}",
                        e
                    )
                }
            }
            function(handler, event, priority)
        }

        private fun findMethod(clazz: Class<*>, info: EventInfo): Method {
            return clazz.allMethods.firstOrNull {
                it.parameters.size == 2
                        && info.eventClass.isSubclassOf(it.parameters[0].type)
                        && it.parameters[1].type == EventPriority::class.java
                        && it.name == info.handlerMethod
                        && !Modifier.isAbstract(it.modifiers)
            } ?: throw NoSuchMethodException("Could not find method ${info.handlerMethod} in class ${clazz.name} with parameters (${info.eventClass.name}, EventPriority)")
        }

        private data class EventInfo(
            val handlerMethod: String,
            val eventClass: Class<*>
        )
    }
}

private val Class<*>.allMethods: Set<Method>
    get() = declaredMethods.toSet() + interfaces.flatMap { it.allMethods } + superclass?.allMethods.orEmpty()
