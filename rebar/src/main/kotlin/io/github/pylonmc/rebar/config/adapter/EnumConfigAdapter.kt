package io.github.pylonmc.rebar.config.adapter

class EnumConfigAdapter<E : Enum<E>>(private val enumClass: Class<E>) : ConfigAdapter<E> {

    override val type = enumClass

    private val nameMap = enumClass.enumConstants.associateBy { it.name.lowercase() }

    override fun convert(key: String?, value: Any): E {
        val name = ConfigAdapter.STRING.convert(key, value)
        return nameMap[name.lowercase()]
            ?: throw IllegalArgumentException("Cannot convert $value to enum ${enumClass.simpleName}")
    }

    companion object {
        @JvmStatic
        fun <E : Enum<E>> from(enumClass: Class<E>): ConfigAdapter<E> {
            return EnumConfigAdapter(enumClass)
        }

        @JvmSynthetic
        inline fun <reified E : Enum<E>> from(): ConfigAdapter<E> {
            return from(E::class.java)
        }
    }
}