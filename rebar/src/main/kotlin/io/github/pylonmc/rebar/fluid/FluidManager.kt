package io.github.pylonmc.rebar.fluid

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.block.base.RebarFluidBlock
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.event.PreRebarFluidPointConnectEvent
import io.github.pylonmc.rebar.event.PreRebarFluidPointDisconnectEvent
import io.github.pylonmc.rebar.event.RebarFluidPointConnectEvent
import io.github.pylonmc.rebar.event.RebarFluidPointDisconnectEvent
import io.github.pylonmc.rebar.fluid.FluidManager.unload
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.util.IdentityHashMap
import java.util.UUID
import java.util.function.Predicate
import kotlin.math.min

/**
 * Fluid networks are organised into 'segments' which is just a set of connected points. Each
 * segment is created at runtime - they are not persisted. When a point is loaded, we initialise
 * it with its own segment, check whether it's connected to any other points, and if so, join all
 * those connected points (and their connected points, and so on) into one segment.
 *
 * Example: Imagine we have
 * A------B------C------D
 * A is loaded first, then C, then D. This means that A and C are on their own segment to start with.
 * Then, D is loaded. D sees that C is connected and loaded, so C and D join together into one
 * segment. Then, B loads, and sees that A and C are connected, so A, B, C, and D join together into
 * one segment.
 *
 * This may seem convoluted, but it's the best (and only) way that I could find to deal with the
 * utter chaos that is chunk loading/unloading. Other models usually break when you try to modify a
 * partially unloaded fluid network.
 *
 * FUTURE OPTIMISATION POINTS:
 * - Use disjoint sets data structure to reduce overhead of connecting/disconnecting and getting
 *   connected nodes
 * - We currently use round-robin for input and output, this would be more efficient to do first-come-
 *   first-serve but would also be much less intuitive so probably not a good idea.
 * - Currently not asynchronous, I think parts of this can definitely be made asynchronous
 */
internal object FluidManager {

    private class Segment(
        val points: MutableSet<VirtualFluidPoint> = mutableSetOf(),
        var fluidPerSecond: Double = Double.MAX_VALUE,
        var predicate: Predicate<RebarFluid>? = null,
    )

    /**
     * A point is just a connection in a fluid network, like a machine's output or the end of a pipe
     */
    private val points: MutableMap<UUID, VirtualFluidPoint> = mutableMapOf()

    /**
     * A segment is a collection of connection points
     */
    private val segments: MutableMap<UUID, Segment> = mutableMapOf()

    /**
     * Each segment has a corresponding ticker
     */
    private val tickers: MutableMap<UUID, Job> = mutableMapOf()

    /**
     * Adds the point to its stored segment, creating the segment and starting a ticker for it if it does not exist
     */
    private fun addToSegment(point: VirtualFluidPoint) {
        if (point.segment !in segments) {
            segments[point.segment] = Segment()
            startTicker(point.segment)
        }
        segments[point.segment]!!.points.add(point)
    }

    /**
     * Removes the point from its segment, deleting the segment and cancelling the ticker if the segment is
     * now empty
     */
    private fun removeFromSegment(point: VirtualFluidPoint) {
        segments[point.segment]!!.points.remove(point)
        if (segments[point.segment]!!.points.isEmpty()) {
            segments.remove(point.segment)
            removeTicker(point.segment)
        }
    }

    @JvmStatic
    fun getById(uuid: UUID): VirtualFluidPoint?
        = points[uuid]

    /**
     * Call when creating a new connection point, or when one has been loaded
     */
    @JvmStatic
    fun add(point: VirtualFluidPoint) {
        check(point.id !in points) { "Duplicate connection point" }

        points[point.id] = point

        addToSegment(point)

        for (otherPointId in point.connectedPoints) {
            points[otherPointId]?.let {
                connect(point, it)
            }
        }
    }

    /**
     * Call when removing a connection point. Use [unload] for when a connection point is unloaded.
     */
    @JvmStatic
    fun remove(point: VirtualFluidPoint) {
        check(point.id in points) { "Nonexistant connection point" }

        // Clone to prevent ConcurrentModificationException; disconnect modifies point.connectedPoints
        for (otherPointId in point.connectedPoints.toSet()) {
            points[otherPointId]?.let {
                disconnect(point, it)
            }
        }

        removeFromSegment(point)

        points.remove(point.id)
    }

    /**
     * Removes a connection point from the cache, but keeps its connection information intact.
     */
    @JvmStatic
    fun unload(point: VirtualFluidPoint) {
        check(point.id in points) { "Nonexistant connection point" }

        removeFromSegment(point)

        points.remove(point.id)
    }

    /**
     * Sets the flow rate per tick for a segment. The segment will not transfer more fluid than the
     * flow rate per tick.
     *
     * Preserved across disconnects and connects (when connecting two points, one of the two points
     * being connected is selected, and its segment's flow rate is copied to the new segment).
     */
    @JvmStatic
    fun setFluidPerSecond(segment: UUID, fluidPerSecond: Double) {
        check(segment in segments) { "Segment does not exist" }
        segments[segment]!!.fluidPerSecond = fluidPerSecond
    }

    @JvmStatic
    fun getFluidPerSecond(segment: UUID): Double {
        check(segment in segments) { "Segment does not exist" }
        return segments[segment]!!.fluidPerSecond
    }

    /**
     * Sets the fluid predicate for a segment. The segment will only transfer fluids that match the
     * predicate.
     *
     * Preserved across disconnects and connects (when connecting two points, one of the two points
     * being connected is selected, and its segment's predicate is copied to the new segment).
     */
    @JvmStatic
    fun setFluidPredicate(segment: UUID, predicate: Predicate<RebarFluid>) {
        check(segment in segments) { "Segment does not exist" }
        segments[segment]!!.predicate = predicate
    }

    @JvmStatic
    fun getFluidPredicate(segment: UUID): Predicate<RebarFluid>? {
        check(segment in segments) { "Segment does not exist" }
        return segments[segment]!!.predicate
    }

    /**
     * Connects two points - and all their connected points - into one segment. Preserves the
     * flow rate and predicate of the first point's segment.
     */
    @JvmStatic
    fun connect(point1: VirtualFluidPoint, point2: VirtualFluidPoint) {
        check(point1.segment in segments) { "Attempt to connect a nonexistant segment" }
        check(point2.segment in segments) { "Attempt to connect a nonexistant segment" }

        if (!PreRebarFluidPointConnectEvent(point1, point2).callEvent()) {
            return
        }

        val fluidPerSecond = getFluidPerSecond(point1.segment)
        val fluidPredicate = getFluidPredicate(point1.segment)

        if (point1.segment != point2.segment) {
            val newSegment = point2.segment
            for (point in getAllConnected(point1)) {
                removeFromSegment(point)
                point.segment = newSegment
                addToSegment(point)
            }
            setFluidPerSecond(newSegment, fluidPerSecond)
            if (fluidPredicate != null) {
                setFluidPredicate(newSegment, fluidPredicate)
            }
        }

        point1.connectedPoints.add(point2.id)
        point2.connectedPoints.add(point1.id)

        RebarFluidPointConnectEvent(point1, point2).callEvent()
    }

    /**
     * Disconnects two points, potentially splitting them into two segments if there is no
     * other link between them.
     */
    @JvmStatic
    fun disconnect(point1: VirtualFluidPoint, point2: VirtualFluidPoint) {
        check(point1.segment in segments) { "Attempt to disconnect a nonexistant segment" }
        check(point2.segment in segments) { "Attempt to disconnect a nonexistant segment" }
        check(point2.id in point1.connectedPoints) { "Attempt to disconnect two points that are not connected" }
        check(point1.id in point2.connectedPoints) { "Attempt to disconnect two points that are not connected" }

        if (!PreRebarFluidPointDisconnectEvent(point1, point2).callEvent()) {
            return
        }

        point1.connectedPoints.remove(point2.id)
        point2.connectedPoints.remove(point1.id)

        val connectedToPoint1 = getAllConnected(point1)
        if (point2 !in connectedToPoint1) {
            // points no longer (even indirectly) connected
            val newSegment = UUID.randomUUID()
            segments[newSegment] = Segment(
                mutableSetOf(),
                segments[point1.segment]!!.fluidPerSecond,
                segments[point1.segment]!!.predicate
            )
            startTicker(newSegment)
            for (point in connectedToPoint1) {
                removeFromSegment(point)
            }
            for (point in connectedToPoint1) {
                point.segment = newSegment
                addToSegment(point)
            }
        }

        RebarFluidPointDisconnectEvent(point1, point2).callEvent()
    }

    /**
     * Recursively gets all the points connected to another point *that are loaded*
     */
    @JvmStatic
    fun getAllConnected(point: VirtualFluidPoint): Set<VirtualFluidPoint> {
        val visitedPoints: MutableSet<VirtualFluidPoint> = mutableSetOf()
        val pointsToVisit: MutableList<VirtualFluidPoint> = mutableListOf(point)
        while (pointsToVisit.isNotEmpty()) {
            val nextPoint = pointsToVisit.removeFirst()
            visitedPoints.add(nextPoint)
            for (uuid in nextPoint.connectedPoints) {
                if (points[uuid] != null && points[uuid] !in visitedPoints) {
                    pointsToVisit.add(points[uuid]!!)
                }
            }
        }
        return visitedPoints
    }

    @JvmStatic
    fun getPoints(segment: UUID, type: FluidPointType): List<VirtualFluidPoint>
        = segments[segment]!!.points.filter { it.type == type }

    data class FluidSupplyInfo(var amount: Double, val blocks: IdentityHashMap<RebarFluidBlock, Double>)

    @JvmStatic
    fun getSuppliedFluids(segment: UUID): Map<RebarFluid, FluidSupplyInfo> {
        val suppliedFluids: MutableMap<RebarFluid, FluidSupplyInfo> = mutableMapOf()
        for (point in getPoints(segment, FluidPointType.OUTPUT)) {
            try {
                if (!point.position.chunk.isLoaded) {
                    continue
                }
                val block = BlockStorage.getAs<RebarFluidBlock>(point.position)
                    ?: continue
                for ((fluid, amount) in block.getSuppliedFluids()) {
                    if (amount < 1.0e-6) {
                        // prevent floating point issues supplying tiny amounts of liquid
                        continue
                    }
                    val pair = suppliedFluids.getOrPut(fluid) {
                        FluidSupplyInfo(0.0, IdentityHashMap())
                    }
                    pair.amount += amount
                    pair.blocks[block] = amount
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                continue
            }
        }
        return suppliedFluids
    }

    /**
     * Find how much of the fluid each input point on the block is requesting
     * Ignore input points requesting zero or effectively zero of the fluid
     */
    fun getRequestedFluids(
        segment: UUID, fluid: RebarFluid
    ): Pair<MutableMap<RebarFluidBlock, Double>, Double> {
        val requesters: MutableMap<RebarFluidBlock, Double> = mutableMapOf()
        var totalRequested = 0.0
        for (point in getPoints(segment, FluidPointType.INPUT)) {
            try {
                if (!point.position.chunk.isLoaded) {
                    continue
                }
                val block = BlockStorage.getAs<RebarFluidBlock>(point.position)
                    ?: continue
                val fluidAmountRequested = block.fluidAmountRequested(fluid)
                if (fluidAmountRequested < 1.0e-9) {
                    continue
                }
                requesters[block] = fluidAmountRequested
                totalRequested += fluidAmountRequested
            } catch (t: Throwable) {
                t.printStackTrace()
                continue
            }
        }
        return Pair(requesters, totalRequested)
    }

    private fun tick(segment: UUID) {
        val suppliedFluids = getSuppliedFluids(segment)

        for ((fluid, info) in suppliedFluids) {

            // Check if the segment is capable of passing the fluid
            val predicate = segments[segment]!!.predicate
            if (predicate != null && !predicate.test(fluid)) {
                continue
            }

            var (requesters, totalRequested) = getRequestedFluids(segment, fluid)

            // Continue if no machine is requesting the fluid
            if (requesters.isEmpty()) {
                continue
            }

            // Use round-robin to compute how much fluid to take from each supplier
            // Done in-place using the info's 'blocks' map to reduce memory operations
            totalRequested = min(totalRequested, segments[segment]!!.fluidPerSecond * RebarConfig.FLUID_TICK_INTERVAL / 20.0)
            val suppliers = info.blocks
            var remainingFluidNeeded = totalRequested
            var totalFluidSupplied = 0.0;
            var changed = true
            // First phase: Repeatedly find all suppliers that we'd try to take more fluid
            // from than possible, were we to take fluid evenly, and take all their fluid
            // and remove them from the list.
            while (changed) {
                changed = false
                val iterator = suppliers.iterator()
                while (iterator.hasNext()) {
                    val (block, amountSupplied) = iterator.next()
                    if (amountSupplied > remainingFluidNeeded / suppliers.size) {
                        continue
                    }

                    remainingFluidNeeded -= amountSupplied
                    block.onFluidRemoved(fluid, amountSupplied)
                    totalFluidSupplied += amountSupplied;
                    iterator.remove()
                    changed = true
                }
            }
            // Second phase: All remaining suppliers supply more than we need from them
            // (assuming we take fluid evenly), so take the same amount of fluid from
            // each one
            for ((block, _) in suppliers) {
                block.onFluidRemoved(fluid, remainingFluidNeeded / suppliers.size)
                totalFluidSupplied += remainingFluidNeeded / suppliers.size
            }

            // Now do the same thing for requesters
            changed = true
            // First phase: Repeatedly find all requesters that we have more than enough
            // fluid to saturate them, add as much fluid as possible, and remove them
            // from the list.
            while (changed) {
                changed = false
                val iterator = requesters.iterator()
                while (iterator.hasNext()) {
                    val (block, amountRequested) = iterator.next()
                    if (amountRequested > totalFluidSupplied / requesters.size) {
                        continue
                    }

                    totalFluidSupplied -= amountRequested
                    block.onFluidAdded(fluid, amountRequested)
                    iterator.remove()
                    changed = true
                }
            }
            // Second phase: All remaining requesters want more than we can supply
            // (assuming we distribute fluid evenly), so give the same amount of fluid to
            // each one
            for ((block, _) in requesters) {
                block.onFluidAdded(fluid, totalFluidSupplied / requesters.size)
            }

            // Break to only allow one type of fluid to be distributed per tick
            break
        }
    }

    private fun startTicker(segment: UUID) {
        check(segment !in tickers) { "Ticker already active" }

        tickers[segment] = Rebar.launch {
            var lastTickNanos = System.nanoTime()
            while (true) {
                delay(RebarConfig.FLUID_TICK_INTERVAL.ticks)
                val dt = (System.nanoTime() - lastTickNanos) / 1.0e9
                lastTickNanos = System.nanoTime()
                tick(segment)
            }
        }
    }

    private fun removeTicker(segment: UUID) {
        check(segment in tickers) { "Ticker does not exist" }

        tickers[segment]!!.cancel()
        tickers.remove(segment)
    }
}