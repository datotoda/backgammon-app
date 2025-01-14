package com.datotoda.backgammon
import kotlin.random.Random


class Agent {
    private val randomGenerator = Random(System.currentTimeMillis())

    fun rollDice(user_turn: Boolean): Pair<Int, Int> {
        val result = Pair(randomGenerator.nextInt(1, 7), randomGenerator.nextInt(1, 7))
        return if (!user_turn) result else Pair(-result.first, -result.second)
    }
}