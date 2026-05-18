package io.github.pylonmc.rebar.electricity

@JvmRecord
data class VoltageRange(val min: Double, val max: Double) {

    init {
        require(min <= max) { "min must be less than or equal to max" }
    }

    operator fun contains(voltage: Double) = voltage in min..max
}
