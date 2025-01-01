package io.github.alelk.pws.backup.model

import io.github.alelk.pws.backup.model.serialization.NumbersSerializer
import kotlinx.serialization.Serializable

@Serializable(with = NumbersSerializer::class)
internal sealed interface Numbers {
  fun get(): List<Int>

  companion object {
    fun parse(string: String): Numbers =
      string.trim().let { n ->
        if (n.all { it.isDigit() }) Single.parse(n)
        else if (n.contains('-')) Range.parse(n)
        else throw IllegalArgumentException("invalid tag song numbers: $n")
      }

    operator fun invoke(values: Collection<Int>): List<Numbers> {
      val allNumbers = values.toList().sorted()
      val ranges = allNumbers.fold(emptyList<IntRange>()) { result, number ->
        val prev = result.lastOrNull()
        when {
          prev == null -> listOf(number..number)
          prev.last == number -> result
          prev.last == number - 1 -> result.dropLast(1) + listOf(prev.first..number)
          else -> result + listOf(number..number)
        }
      }
      return ranges.map {
        if (it.count() == 1) Single(it.first)
        else Range(it.first, it.last)
      }
    }
  }

  data class Single(val number: Int) : Numbers {
    override fun get(): List<Int> = listOf(number)
    override fun toString(): String = number.toString()

    init {
      require(number > 0) { "number must be positive, was $number" }
    }

    companion object {
      fun parse(string: String): Single = Single(string.toInt())
    }
  }

  data class Range(val from: Int, val to: Int) : Numbers {
    override fun get(): List<Int> = (from..to).toList()
    override fun toString(): String = "$from-$to"

    init {
      require(from > 0) { "number range must be positive, was $from-$to" }
      require(to > 0) { "number range must be positive, was $from-$to" }
      require(to >= from) { "invalid number range: $from-$to" }
    }

    companion object {
      fun parse(string: String): Range =
        string.split('-').let {
          require(it.size == 2) { "invalid number range: $string" }
          val from = checkNotNull(it[0].trim().toIntOrNull()) { "invalid number range $string: not integer: '${it[0]}'" }
          val to = checkNotNull(it[1].trim().toIntOrNull()) { "invalid number range $string: not integer: '${it[1]}'" }
          Range(from, to)
        }
    }
  }
}