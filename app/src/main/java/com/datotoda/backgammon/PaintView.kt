package com.datotoda.backgammon

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.*
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import kotlin.math.absoluteValue
import kotlin.math.min


class PaintView constructor(context: Context) : View(context) {
    private var onShowDiceListener: (() -> Unit)? = null
    private var onAITurnListener: (() -> Unit)? = null
    private var onRollDiceListener: (() -> Unit)? = null
    private var onMakeMoveListener: (() -> Unit)? = null

    private var boardPaint: Paint
    private var rocksPaint: Paint
    private var dicePaint: Paint

    private var d: Float  // rockDiameter
    private var boardWidth: Float
    private var outerBoardRect: RectF
    private var innerBoardRect: RectF
    private var middleBoardRect: RectF
    private var p1rocksMiddleBoardRect: RectF
    private var p2rocksMiddleBoardRect: RectF
    private var p1deadRocksRect: RectF
    private var p2deadRocksRect: RectF
    private var diceRollButtonRect: RectF
    private var diceRollAIButtonRect: RectF
    private var boardTriangles: ArrayList<BoardTriangle>
    var rocks: ArrayList<Rock>
    private var dfIsActive: DFIsActive
    private var dices: ArrayList<Dice>

    var userTurn: Boolean = true
    private lateinit var validActionsList: List<List<List<Int>>>
    private lateinit var validActionsIndexes: ArrayList<Int>
    lateinit var selectedAction: ArrayList<List<Int>>
    private var state: State = State.NONE

    init {
        val displayMetrics = DisplayMetrics()

        (getContext() as Activity).windowManager
            .defaultDisplay
            .getMetrics(displayMetrics)

        boardPaint = Paint()
        rocksPaint = Paint()
        dicePaint = Paint()

        val l = 0
        val t = 0
        val r = displayMetrics.widthPixels
        val b = displayMetrics.heightPixels
        d = (r - l).toFloat() / 16

        boardWidth = min((r - l) - 2 * d, (b - t) - 6 * d)
        val padding = (d / 4)
        outerBoardRect = RectF(
            l + (r - boardWidth) / 2,
            t + (b - boardWidth) / 2,
            r - (r - boardWidth) / 2,
            b - (b - boardWidth) / 2
        )
        innerBoardRect = RectF(
            outerBoardRect.left + padding,
            outerBoardRect.top + padding,
            outerBoardRect.right - padding,
            outerBoardRect.bottom - padding
        )
        middleBoardRect = RectF(
            innerBoardRect.left + (d * 6),
            outerBoardRect.top,
            innerBoardRect.right - (d * 6),
            outerBoardRect.bottom
        )
        val temp = (d * 4.5f)
        p1rocksMiddleBoardRect = RectF(
            middleBoardRect.left + padding,
            innerBoardRect.bottom - temp,
            middleBoardRect.right - padding,
            innerBoardRect.bottom
        )
        p2rocksMiddleBoardRect = RectF(
            middleBoardRect.left + padding,
            innerBoardRect.top,
            middleBoardRect.right - padding,
            innerBoardRect.top + temp
        )
        p1deadRocksRect = RectF(
            middleBoardRect.centerX() - d / 2f,
            middleBoardRect.centerY() - d * 1.5f,
            middleBoardRect.centerX() + d / 2f,
            middleBoardRect.centerY() - d * 0.5f
        )
        p2deadRocksRect = RectF(
            middleBoardRect.centerX() - d / 2f,
            middleBoardRect.centerY() + d * 0.5f,
            middleBoardRect.centerX() + d / 2f,
            middleBoardRect.centerY() + d * 1.5f
        )
        diceRollButtonRect = RectF(
            middleBoardRect.right + d,
            middleBoardRect.centerY() - d / 1.5f,
            innerBoardRect.right - d,
            middleBoardRect.centerY() + d / 1.5f
        )
        diceRollAIButtonRect = RectF(
            innerBoardRect.left + d,
            middleBoardRect.centerY() - d / 1.5f,
            middleBoardRect.left - d,
            middleBoardRect.centerY() + d / 1.5f
        )


        boardTriangles  = ArrayList(24)
        for (i in 0 until 24) {
            boardTriangles.add(BoardTriangle(
                x = if (i < 12) innerBoardRect.right - i * d - (if (i % 12 < 6) d else d * 2.5f) else boardTriangles[11 - i % 12].x,
                y = if (i >= 12) innerBoardRect.top else innerBoardRect.bottom,
                width = d,
                height = d * 5.25f,
                inverted = i >= 12,
                color = resources.getColor(if (i % 2 == 0 ) R.color.triangle_color_1 else R.color.triangle_color_2),
                active = false
            ))
        }

        rocks  = ArrayList(30)
        fillRocksFromArray(arrayListOf(
            -2, 0, 0, 0, 0, 5,
            0, 3, 0, 0, 0, -5,
            5, 0, 0, 0, -3, 0,
            -5, 0, 0, 0, 0, 2,
            0, 0, 0, 0  // dead white, dead black, off white, off black
        ))
        dfIsActive = DFIsActive()
        dices = ArrayList(4)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawBoard(canvas)
        drawRocks(canvas)
        drawDeadRock(false, dfIsActive.p1DeadIsActive, canvas)
        drawDeadRock(true, dfIsActive.p2DeadIsActive, canvas)
        drawFinishedRock(false, dfIsActive.p1FinishedIsActive, canvas)
        drawFinishedRock(true, dfIsActive.p2FinishedIsActive, canvas)

        drawDices(userTurn, canvas)
        if (!userTurn){
            drawRollDiceButton(canvas)
        } else if (dices.none { !it.done }){
            drawRollDiceAIButton(canvas)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (!userTurn && isClicked(event, diceRollButtonRect)) {
                userTurn = true
                onRollDiceListener?.invoke()
            } else if (userTurn && isClicked(event, diceRollAIButtonRect)) {
                userTurn = false
                onAITurnListener?.invoke()
            }

            else if (state == State.POSSIBLE_MOVES && isClicked(event, p1deadRocksRect)) {
                dfIsActive.reset()
                dfIsActive.p1DeadIsActive = true
                validActionsList
                    .filterIndexed { _index, _ ->
                        validActionsIndexes.contains(
                            _index
                        )
                    }
                    .filter { it[selectedAction.count()].first() == 24 }
                    .map { it[selectedAction.count()].last() }
                    .distinct()
                    .forEach { boardTriangles[it].active = true }
                state = State.SELECTED_ROCK
            } else if (state == State.SELECTED_ROCK && dfIsActive.p1FinishedIsActive && isClicked(event, p1rocksMiddleBoardRect)) {
                selectedAction.add(listOf(rocks.first { it.active }.triangleIndex, 26))
                makeMove(selectedAction.last())
                clearActiveRocks()
                clearActiveTriangles()
                validActionsIndexes = validActionsList.indices.filter { validActionsIndexes.contains(it) && validActionsList[it][selectedAction.lastIndex] == selectedAction.last() } as ArrayList<Int>

                if (validActionsIndexes.count() == 1 && selectedAction.count() == validActionsList[validActionsIndexes.first()].count()) {
                    selectedAction = validActionsList[validActionsIndexes.first()] as ArrayList<List<Int>>
                    onMakeMoveListener?.invoke()
                    dices.forEach { it.done = true }
                } else {
                    validActionsList
                        .filterIndexed { _index, _ ->
                            validActionsIndexes.contains(
                                _index
                            )
                        }
                        .map { it[selectedAction.count()].first() }
                        .distinct()
                        .forEach { _index ->
                            setActiveRock(_index)
                        }
                }
                state = State.POSSIBLE_MOVES

            } else {
                boardTriangles.forEachIndexed { index, triangle ->
                    if (isClicked(event, triangle.rectF)) {
                        // select rock
                        if (state == State.POSSIBLE_MOVES && getRock(index)?.active == true) {
                            clearActiveRocks()
                            setActiveRock(index)
                            validActionsList
                                .filterIndexed { _index, _ ->
                                    validActionsIndexes.contains(
                                        _index
                                    )
                                }
                                .filter { it[selectedAction.count()].first() == index }
                                .map { it[selectedAction.count()].last() }
                                .distinct()
                                .forEach { if (it == 26) dfIsActive.p1FinishedIsActive = true else boardTriangles[it].active = true }
                            state = State.SELECTED_ROCK
                        }
                        // make move
                        else if (state == State.SELECTED_ROCK && boardTriangles[index].active) {
                            selectedAction.add(listOf(rocks.first { it.active }.triangleIndex, index))
                            makeMove(selectedAction.last())
                            clearActiveRocks()
                            clearActiveTriangles()
                            validActionsIndexes =
                                validActionsList.indices.filter { validActionsIndexes.contains(it) && validActionsList[it][selectedAction.lastIndex] == selectedAction.last() } as ArrayList<Int>

                            if (validActionsIndexes.count() == 1 && selectedAction.count() == validActionsList[validActionsIndexes.first()].count()) {
                                selectedAction = validActionsList[validActionsIndexes.first()] as ArrayList<List<Int>>
                                onMakeMoveListener?.invoke()
                                dices.forEach { it.done = true }
                            } else {
                                validActionsList
                                    .filterIndexed { _index, _ ->
                                        validActionsIndexes.contains(
                                            _index
                                        )
                                    }
                                    .map { it[selectedAction.count()].first() }
                                    .distinct()
                                    .forEach { _index ->
                                        setActiveRock(_index)
                                    }
                            }
                            state = State.POSSIBLE_MOVES
                        }
                    }
                }
            }

            invalidate()
        }

        return super.onTouchEvent(event)
    }

    private fun drawTriangle(x: Float, y: Float, width: Float, height: Float, inverted: Boolean, color: Int, active: Boolean, paint: Paint, canvas: Canvas) {
        val p1 = PointF(x, y)
        val pointX = x + width / 2
        val pointY = if (inverted) y + height else y - height
        val p2 = PointF(pointX, pointY)
        val p3 = PointF(x + width, y)
        val path = Path()
        paint.color = color
        path.fillType = Path.FillType.EVEN_ODD
        path.moveTo(p1.x, p1.y)
        path.lineTo(p2.x, p2.y)
        path.lineTo(p3.x, p3.y)
        path.close()
        canvas.drawPath(path, paint)
        if (active) {
            paint.color = resources.getColor(R.color.active_triangle)
            canvas.drawPath(path, paint)
        }
    }

    private fun drawTriangle(boardTriangle: BoardTriangle, paint: Paint, canvas: Canvas) = drawTriangle(
        boardTriangle.x,
        boardTriangle.y,
        boardTriangle.width,
        boardTriangle.height,
        boardTriangle.inverted,
        boardTriangle.color,
        boardTriangle.active,
        paint,
        canvas
    )

    private fun drawRock(x: Float, y: Float, text: String, is_black: Boolean, is_active: Boolean, canvas: Canvas) {
        val r = d / 2f
        val cx = x + r
        val cy = y - r
        rocksPaint.color = resources.getColor(if (is_black) R.color.black_rock else R.color.white_rock)

        if (is_active) {
            rocksPaint.color = resources.getColor(R.color.active_rock)

        }
        canvas.drawCircle(cx, cy, r, rocksPaint)

        if (text != ""){
            rocksPaint.color = resources.getColor(if (is_black) R.color.black_rock_text else R.color.white_rock_text)
            rocksPaint.textSize = r
            rocksPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(text, cx, cy - (rocksPaint.descent() + rocksPaint.ascent()) / 2, rocksPaint)
        }
    }

    private fun drawRock(rock: Rock, canvas: Canvas) {
        if (!rock.dead && !rock.finished) {
            drawRock(
                x = rock.x,
                y = rock.y,
                text = rock.text,
                is_black = rock.is_black,
                is_active = rock.active,
                canvas = canvas
            )
        }
    }

    private fun drawDeadRock(is_black: Boolean, is_active: Boolean, canvas: Canvas) {
        val deadRocks = rocks.count { it.is_black == is_black && it.dead }
        if (deadRocks > 0) {
            val deadRocksRect = if (is_black) p2deadRocksRect else p1deadRocksRect
            drawRock(
                x = deadRocksRect.left,
                y = deadRocksRect.bottom,
                text = deadRocks.toString(),
                is_black = is_black,
                is_active = is_active,
                canvas = canvas
            )
        }
    }

    private fun drawFinishedRock(is_black: Boolean, is_active: Boolean, canvas: Canvas) {
        val rocksMiddleBoardRect = if (is_black) p2rocksMiddleBoardRect else p1rocksMiddleBoardRect
        val boardColor = resources.getColor(if (is_active) R.color.active_rock else R.color.inner_board)
        val rockColor = resources.getColor(if (is_black) R.color.black_rock else R.color.white_rock)
        val rockSize = rocksMiddleBoardRect.height() / 16f
        val offset = (rocksMiddleBoardRect.height() - rockSize * 15f) / 16f

        boardPaint.color = boardColor
        canvas.drawRect(rocksMiddleBoardRect, boardPaint)
        rocksPaint.color = rockColor
        var x = if (is_black) rocksMiddleBoardRect.bottom - offset else rocksMiddleBoardRect.top + offset + rockSize

        for (i in 0 until  min(rocks.count { it.is_black == is_black && it.finished }, 15)) {
            canvas.drawRect(
                rocksMiddleBoardRect.left + 1,
                x - rockSize,
                rocksMiddleBoardRect.right - 1,
                x,
                rocksPaint
            )
            if(is_black) x -= (rockSize + offset) else x += (rockSize + offset)
        }
    }

    private fun drawDices(for_p1: Boolean , canvas: Canvas) {
        val diceWidth = d
        val diceDotR = diceWidth / 8
        val diceDotGap = (diceWidth - (7 * diceDotR)) / 4
        val diceR2R = (diceDotR * 2) + diceDotGap
        val diceCurve = diceWidth / 4
        val dicesWidth = (innerBoardRect.width() - middleBoardRect.width()) / 2 - diceWidth
        val dicesGap = (dicesWidth - (4 * diceWidth)) / 3
        val diceY = (innerBoardRect.bottom + innerBoardRect.top - diceWidth) / 2
        var diceX = (diceWidth / 2) + (if (for_p1) middleBoardRect.right else innerBoardRect.left)

        if (dices.count() == 2) {
            diceX += diceWidth + dicesGap
        }

        dices.forEach { dice: Dice ->
            dicePaint.color = resources.getColor(if (dice.done) R.color.dice_done else R.color.dice)
            val diceRect = RectF(diceX, diceY, diceX + diceWidth, diceY + diceWidth)
            canvas.drawRoundRect(diceRect, diceCurve, diceCurve, dicePaint)
            dicePaint.color = resources.getColor(if (dice.done) R.color.dice_dot_done else R.color.dice_dot)

            if (dice.value % 2 == 1) {
                canvas.drawCircle(diceRect.centerX(), diceRect.centerY(), diceDotR, dicePaint)
            }
            if (dice.value >= 2) {
                canvas.drawCircle(diceRect.centerX() + diceR2R, diceRect.centerY() - diceR2R, diceDotR, dicePaint)
                canvas.drawCircle(diceRect.centerX() - diceR2R, diceRect.centerY() + diceR2R, diceDotR, dicePaint)
            }
            if (dice.value >= 4) {
                canvas.drawCircle(diceRect.centerX() + diceR2R, diceRect.centerY() + diceR2R, diceDotR, dicePaint)
                canvas.drawCircle(diceRect.centerX() - diceR2R, diceRect.centerY() - diceR2R, diceDotR, dicePaint)
            }
            if (dice.value == 6) {
                canvas.drawCircle(diceRect.centerX() + diceR2R, diceRect.centerY(), diceDotR, dicePaint)
                canvas.drawCircle(diceRect.centerX() - diceR2R, diceRect.centerY(), diceDotR, dicePaint)
            }
            diceX += diceWidth + dicesGap
        }

    }

    private fun drawBoard(canvas: Canvas) {
        boardPaint.color = resources.getColor(R.color.outer_board)
        canvas.drawRect(outerBoardRect, boardPaint)

        boardPaint.color = resources.getColor(R.color.inner_board)
        canvas.drawRect(innerBoardRect, boardPaint)

        boardPaint.color = resources.getColor(R.color.outer_board)
        canvas.drawRect(middleBoardRect, boardPaint)

        boardPaint.color = resources.getColor(R.color.inner_board)
        canvas.drawRect(p1rocksMiddleBoardRect, boardPaint)
        canvas.drawRect(p2rocksMiddleBoardRect, boardPaint)

        boardTriangles.forEach { drawTriangle(it, boardPaint, canvas) }
    }

    private fun drawRocks(canvas: Canvas) {
        rocks.forEach { drawRock(it, canvas) }
    }

    private fun drawRollDiceButton(canvas: Canvas) {
        dicePaint.color = resources.getColor(R.color.dice_roll_button)
        canvas.drawRoundRect(
            diceRollButtonRect,
            d / 3,
            d / 3,
            dicePaint
        )
        dicePaint.color = resources.getColor(R.color.dice_roll_button_text)
        dicePaint.textSize = d / 1.5f
        dicePaint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "Roll Dice",
            diceRollButtonRect.centerX(),
            diceRollButtonRect.centerY() - (dicePaint.descent() + dicePaint.ascent()) / 2,
            dicePaint
        )
    }
    private fun drawRollDiceAIButton(canvas: Canvas) {
        dicePaint.color = resources.getColor(R.color.dice_roll_button)
        canvas.drawRoundRect(
            diceRollAIButtonRect,
            d / 3,
            d / 3,
            dicePaint
        )
        dicePaint.color = resources.getColor(R.color.dice_roll_button_text)
        dicePaint.textSize = d / 1.5f
        dicePaint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "AI Turn",
            diceRollAIButtonRect.centerX(),
            diceRollAIButtonRect.centerY() - (dicePaint.descent() + dicePaint.ascent()) / 2,
            dicePaint
        )
    }

    private fun fillRocksFromArray(rocksArrayList: ArrayList<Int>) {
        rocks.clear()
        rocksArrayList.forEachIndexed { index, i ->
            if (i != 0 && index < 24) {
                val text = if (i.absoluteValue > 5) "+${i.absoluteValue - 5}" else ""
                val isBlack = i < 0
                val inverted = index > 11
                val diffY = if (inverted) d else -d
                val triangleX = boardTriangles[index].x
                var triangleY = boardTriangles[index].y + if (inverted) d else 0f

                for (j in 1..i.absoluteValue) {
                    rocks.add(Rock(
                        x = triangleX,
                        y = triangleY,
                        triangleIndex = index,
                        is_black = isBlack,
                        text = if (j >= 5) text else "",
                        active = false,
                        dead = false,
                        finished = false
                    ))
                    if (j < 5) triangleY += diffY
                }
            } else if (i != 0) {
                for (j in 1.. i.absoluteValue) {
                    rocks.add(Rock(
                        x = 0f,
                        y = 0f,
                        triangleIndex = index,
                        is_black = index % 2 == 1,  // 25, 27
                        text = "",
                        active = false,
                        dead = index == 24 || index == 25,
                        finished = index == 26 || index == 27
                    ))
                }
            }
        }
    }

    private fun getRocksArray(): ArrayList<Int> {
        val tempRocks: ArrayList<Int> = ArrayList(List(28) { 0 })
        rocks.forEach { tempRocks[it.triangleIndex] += if(it.is_black) -1 else 1 }
        return tempRocks
    }

    private fun setActiveRock(triangleIndex: Int) {
        rocks.lastOrNull { rock -> rock.triangleIndex == triangleIndex }?.active = true
    }

    private fun getRock(triangleIndex: Int) = rocks.lastOrNull { rock -> rock.triangleIndex == triangleIndex }


    private fun clearActiveRocks() = rocks.forEach { it.active = false }.also { dfIsActive.reset() }
    private fun clearActiveTriangles() = boardTriangles.forEach { it.active = false }.also { dfIsActive.reset() }

    fun rollDices(d1: Int, d2: Int) {
        dices.clear()
        val td1 = d1.absoluteValue
        val td2 = d2.absoluteValue

        dices.add(Dice(td1))
        dices.add(Dice(td2))
        if (td1 == td2) {
            dices.add(Dice(td1))
            dices.add(Dice(td2))
        }
    }

    fun showPossibleMoves(_validActionsList: List<List<List<Int>>>) {
        validActionsList = _validActionsList
        validActionsIndexes = ArrayList((validActionsList.indices).toList())
        selectedAction = ArrayList(4)
        clearActiveRocks()
        validActionsList.map { it.first().first() }.distinct().forEach { index ->
            setActiveRock(index)
        }
        state = State.POSSIBLE_MOVES

        if (validActionsIndexes.isEmpty()) {
            onMakeMoveListener?.invoke()
            dices.forEach { it.done = true }
        }
    }

    private fun makeMove(move: List<Int>) {
        val tempRock = getRock(move.first())

        if (tempRock != null) {
            val tempRocks = getRocksArray()

            if (tempRock.is_black && tempRocks[move.last()] == 1) {
                tempRocks[24] += 1
                tempRocks[move.last()] = 0
            }
            else if (!tempRock.is_black && tempRocks[move.last()] == -1) {
                tempRocks[25] -= 1
                tempRocks[move.last()] = 0
            }
            if (tempRock.is_black) {
                tempRocks[move.first()] += 1
                tempRocks[move.last()] -= 1
            }
            else {
                tempRocks[move.first()] -= 1
                tempRocks[move.last()] += 1
            }

            fillRocksFromArray(tempRocks)
        }

    }

    fun makeMoves(moves: List<List<Int>>) = moves.forEach{ move -> makeMove(move)}

    private fun isClicked(event: MotionEvent, rectF: RectF): Boolean =
        rectF.left <= event.x
                && event.x <= rectF.right
                && rectF.top <= event.y
                && event.y <= rectF.bottom

    fun setShowDice(printListener: (() -> Unit)?) {
        this.onShowDiceListener = printListener
    }

    fun setAITurnListener(printListener: (() -> Unit)?) {
        this.onAITurnListener = printListener
    }

    fun setRollDice(printListener: (() -> Unit)?) {
        this.onRollDiceListener = printListener
    }

    fun setMakeMoveListener(printListener: (() -> Unit)?) {
        this.onMakeMoveListener = printListener
    }

}

data class BoardTriangle(
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    var inverted: Boolean,
    var color: Int,
    var active: Boolean = false
) {
    val rectF = RectF(x, if (inverted) y else y - height, x + width, if (inverted) y + height else y)
}

data class Rock(
    var x: Float,
    var y: Float,
    var triangleIndex: Int,
    var is_black: Boolean,
    var text: String = "",
    var active: Boolean = false,
    var dead: Boolean = false,
    var finished: Boolean = false
)

data class DFIsActive(  // Dead and Finished Activities
    var p1DeadIsActive: Boolean = false,
    var p2DeadIsActive: Boolean = false,
    var p1FinishedIsActive: Boolean = false,
    var p2FinishedIsActive: Boolean = false,
) {
    fun reset() {
        p1DeadIsActive = false
        p2DeadIsActive = false
        p1FinishedIsActive = false
        p2FinishedIsActive = false
    }
}

data class Dice(
    var value: Int,
    var done: Boolean = false
)

enum class State(val state: Int) {
    NONE(0),
    POSSIBLE_MOVES(1),
    SELECTED_ROCK(2);
}