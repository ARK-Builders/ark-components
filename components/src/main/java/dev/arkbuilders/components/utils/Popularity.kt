package dev.arkbuilders.components.utils

object Popularity {
    fun <T> calculate(elements: List<T>): Map<T, Int> {
        val result = mutableMapOf<T, Int>()

        elements.forEach { result[it] = (result[it] ?: 0) + 1 }

        return result.toMap()
    }
}