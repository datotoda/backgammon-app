package com.datotoda.backgammon
import kotlin.random.Random


class Agent {
    fun rollDice(): Pair<Int, Int> {
        return Pair(-Random.nextInt(1, 7), -Random.nextInt(1, 7))

    }
}