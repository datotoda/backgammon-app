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
import kotlin.random.Random


class PaintView constructor(context: Context) : View(context) {
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
    private var boardTriangles: ArrayList<BoardTriangle>
    private var rocks: ArrayList<Rock>
    private var df_is_active: DFIsActive
    private var dices: ArrayList<Dice>

    private var p1_turn: Boolean = false

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
//        fillRocksFromArray(arrayListOf(
//            -2, 0, 0, 0, 0, 5,
//            0, 3, 0, 0, 0, -5,
//            5, 0, 0, 0, -3, 0,
//            -5, 0, 0, 0, 0, 2
//        ))
        fillRocksFromArray(arrayListOf(
            0, 0, 0, 0, 0, 6,
            0, 2, 0, 0, 0, -4,
            7, 0, 0, 0, -2, 0,
            -9, 0, 0, 0, 0, 0
        ))
        df_is_active = DFIsActive()
        dices = ArrayList(4)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawBoard(canvas)
        drawRocks(canvas)
        drawDeadRock(false, df_is_active.p1DeadIsActive, canvas)
        drawDeadRock(true, df_is_active.p2DeadIsActive, canvas)
        drawFinishedRock(false, df_is_active.p1FinishedIsActive, canvas)
        drawFinishedRock(true, df_is_active.p2FinishedIsActive, canvas)
        drawDices(p1_turn, canvas)
        if (!p1_turn){
            drawRollDiceButton(canvas)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {

        if (event.action == MotionEvent.ACTION_DOWN) {
            println("" + event.x + " " + event.y)
//            boardTriangles[7].active = ! boardTriangles[7].active
//            boardTriangles[8].active = ! boardTriangles[8].active
//            boardTriangles[14].active = ! boardTriangles[14].active
//            df_is_active.p1FinishedIsActive = ! df_is_active.p1FinishedIsActive
//            df_is_active.p2DeadIsActive = ! df_is_active.p2DeadIsActive

            rocks[7].dead = ! rocks[7].dead
            rocks[2].dead = ! rocks[2].dead
            rocks[3].dead = ! rocks[3].dead
//            rocks[27].dead = ! rocks[27].dead
//            rocks[25].dead = ! rocks[25].dead
//            rocks[26].finished = ! rocks[26].finished
//            rocks[0].finished = ! rocks[0].finished

//            rocks[4].active = ! rocks[4].active
//            rocks[22].active = ! rocks[22].active
//            rocks[5].active = ! rocks[5].active
//            rocks[24].active = ! rocks[24].active

            if (!p1_turn && isClicked(event, diceRollButtonRect)) {
                rollDices()
                p1_turn = true
            }
            else if (isClicked(event, diceRollButtonRect)) {
                rollDices()
                p1_turn = false
            }

            if (isClicked(event, p1deadRocksRect)) {
                df_is_active.reset()
                df_is_active.p1DeadIsActive = true
            } else if (isClicked(event, p2deadRocksRect)) {
                df_is_active.reset()
                df_is_active.p2DeadIsActive = true
            } else if (isClicked(event, p1rocksMiddleBoardRect)) {
                df_is_active.reset()
                df_is_active.p1FinishedIsActive = true
            } else if (isClicked(event, p2rocksMiddleBoardRect)) {
                df_is_active.reset()
                df_is_active.p2FinishedIsActive = true
            }

            boardTriangles.forEachIndexed { index, triangle ->
                if (isClicked(event, triangle.rectF)) {
                    clearActiveRocks()
                    setActiveRock(index, true)
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
        var r = d / 2f
        val cx = x + r
        val cy = y - r
        rocksPaint.color = resources.getColor(if (is_black) R.color.black_rock else R.color.white_rock)

        if (is_active) {
            val color = rocksPaint.color
            rocksPaint.color = resources.getColor(R.color.active_rock)
//            canvas.drawCircle(cx, cy, r, rocksPaint)
//            r *= 0.8f
//            rocksPaint.color = color
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

    private fun fillRocksFromArray(rocksArrayList: ArrayList<Int>) {
        rocks.clear()
        rocksArrayList.forEachIndexed { index, i ->
            if (i != 0) {
                val text = if (i.absoluteValue > 5) "+${i.absoluteValue - 5}" else ""
                val is_black = i < 0
                val inverted = index > 11
                val diffY = if (inverted) d else -d
                val triangleX = boardTriangles[index].x
                var triangleY = boardTriangles[index].y + if (inverted) d else 0f

                for (j in 1..min(5, i.absoluteValue)) {
                    rocks.add(Rock(
                        x = triangleX,
                        y = triangleY,
                        triangleIndex = index,
                        is_black = is_black,
                        text = if (j == 5) text else "",
                        active = false,
                        dead = false,
                        finished = false
                    ))
                    triangleY += diffY
                }
            }
        }
    }

    private fun setActiveRock(triangleIndex: Int, active: Boolean) {
        rocks.filter { rock -> rock.triangleIndex == triangleIndex }.lastOrNull()?.active = active
    }

    private fun clearActiveRocks() = rocks.forEach { it.active = false }
    private fun clearActiveTriangles() = boardTriangles.forEach { it.active = false }

    private fun rollDices() {
        dices.clear()
        val d1 = (Random.nextInt() % 6).absoluteValue + 1
        val d2 = (Random.nextInt() % 6).absoluteValue + 1

        dices.add(Dice(d1, done = Random.nextBoolean()))
        dices.add(Dice(d2, done = Random.nextBoolean()))
        if (d1 == d2) {
            dices.add(Dice(d1, done = Random.nextBoolean()))
            dices.add(Dice(d2, done = Random.nextBoolean()))
        }
    }

    private fun isClicked(event: MotionEvent, rectF: RectF): Boolean =
        rectF.left <= event.x
                && event.x <= rectF.right
                && rectF.top <= event.y
                && event.y <= rectF.bottom

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
