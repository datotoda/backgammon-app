package com.datotoda.backgammon

import android.app.AlertDialog
import android.os.Bundle
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.datotoda.backgammon.ml.FirstTfModel2
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder


class MainActivity : AppCompatActivity() {
    // creating a variable for our relative layout
    private var relativeLayout: RelativeLayout? = null

    private lateinit var paintView: PaintView
    private lateinit var agent: Agent
    private lateinit var dice: Pair<Int, Int>
    private lateinit var python: Python
    private lateinit var pyGameModule: PyObject
    private lateinit var gameInstance: PyObject
    private var userTurn: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // initializing our view.
        relativeLayout = findViewById(R.id.idRLView)

        // calling our paint view class and adding
        // its view to our relative layout.
        paintView = PaintView(this)


        // Set the listeners for different print functions
        paintView.setShowDice {
            showDice()
        }
        paintView.setAITurnListener {
            makeAITurn()
        }
        paintView.setRollDice {
            rollDice()
        }

        paintView.setMakeMoveListener {
            doMakeMove()
        }
        relativeLayout?.addView(paintView)

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(applicationContext))
        }

        agent = Agent()

        // Initialize Python
        python = Python.getInstance()

        pyGameModule = python.getModule("game") // "game.py" corresponds to "game"

        // Create an instance of the Game class
        gameInstance = pyGameModule.callAttr("Game")

        userTurn = gameInstance["start_color"]!!.toInt() == 0
        paintView.userTurn = userTurn
        dice = Pair(gameInstance["first_roll"]!!.asList().first().toInt(), gameInstance["first_roll"]!!.asList().last().toInt())

        showDice()
        if (userTurn){
            showPossibleMoves()
        } else {
            makeAITurn()
        }

    }

    private fun convertToListOfLists(pyObject: PyObject): List<List<Float>> {
        return pyObject.asList().map { innerPyObject ->
            innerPyObject.asList().map { it.toFloat() }
        }
    }

    private fun convertToListOfListsOfLists(pyObject: PyObject): List<List<List<Int>>> {
        return pyObject.asList().map { innerPyObject ->
            innerPyObject.asList().map { secondInnerPyObject ->
                secondInnerPyObject.asList().map {pos ->
                    if (pos.equals("bar")) (24 + if (userTurn) 0 else 1)
                    else if (pos.equals(-1)) 26
                    else if (pos.equals(24)) 27
                    else pos.toInt()
                }
            }
        }
    }

    private fun <T> convertToPythonList(kotlinList: List<T>): PyObject {
        val py = Python.getInstance()
        val pyList = py.builtins.callAttr("list")
        kotlinList.forEach { item ->
            when (item) {
                is List<*> -> pyList.callAttr("append", convertToPythonList(item as List<*>))
                else -> pyList.callAttr("append", item)
            }
        }
        return pyList
    }

    private fun showDice() {
        paintView.rollDices(dice.first, dice.second)
        paintView.userTurn = userTurn
    }

    private fun showPossibleMoves() {
        val validActions: PyObject = gameInstance.callAttr(
            "get_valid_actions",
            dice.first,  // die_1
            dice.second   // die_2
        )
        val validActionsList: List<List<List<Int>>> = convertToListOfListsOfLists(validActions)
        paintView.showPossibleMoves(validActionsList)
    }

    private fun makeAITurn() {
        userTurn = false
        dice = agent.rollDice(false)
        showDice()

        val validActions: PyObject = gameInstance.callAttr(
            "get_valid_actions",
            dice.first,  // die_1
            dice.second   // die_2
        )
        val actionOutcomeStates = gameInstance.callAttr(
            "get_action_outcome_states",
            validActions
        )

        val actionOutcomeStatesList: List<List<Float>> = convertToListOfLists(actionOutcomeStates)

        val model = FirstTfModel2.newInstance(applicationContext)
        val outputsList = mutableListOf<Float>()
        actionOutcomeStatesList.forEach { observation ->
            val byteBuffer = ByteBuffer.allocateDirect(4 * 198).order(ByteOrder.nativeOrder())
            observation.forEach { byteBuffer.putFloat(it) }
            byteBuffer.rewind()
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 198), DataType.FLOAT32)
            inputFeature0.loadBuffer(byteBuffer)

            // Runs model inference and gets result.
            val outputs = model.process(inputFeature0)
            outputs.outputFeature0AsTensorBuffer

            val outputArray = outputs.outputFeature0AsTensorBuffer.floatArray
            outputsList.add(outputArray[0])
        }
        val argmax = outputsList.withIndex().maxByOrNull { it.value }?.index
        val validActionsList: List<List<List<Int>>> = convertToListOfListsOfLists(validActions)
        if (validActionsList.isNotEmpty()) {
            paintView.makeMoves(validActionsList[argmax!!])
        }

//        // Releases model resources if no longer used.
        model.close()
        if( argmax != null){
            val pythonValidActions = convertToPythonList(validActionsList[argmax])
            gameInstance.callAttr(
                "make_step",
                pythonValidActions
            )
        } else {
            gameInstance.callAttr(
                "make_step",
                convertToPythonList(listOf<Int>())
            )
        }

        if (paintView.rocks.count { it.finished && it.is_black } == 15) {
            showWinner("AI is Winner")
        }
    }

    private fun rollDice() {
        userTurn = true
        paintView.userTurn = true
        dice = agent.rollDice(true)
        showDice()
        showPossibleMoves()
    }

    private fun doMakeMove() {
        println("doMakeMove execute")
        val pythonValidActions = convertToPythonList(paintView.selectedAction)
        gameInstance.callAttr(
            "make_step",
            pythonValidActions
        )

        if (paintView.rocks.count { it.finished && !it.is_black } == 15) {
            showWinner("You are Winner")
        }

    }
    private fun showWinner(text: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder
            .setMessage(text)
            .setTitle("Game Over")
            .setPositiveButton("Close App") { _, _ ->
                finish()
            }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

}
