package com.datotoda.backgammon

import android.os.Bundle
import android.util.Log
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.toAndroidPair
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.datotoda.backgammon.ml.FirstTfModel
import com.datotoda.backgammon.ml.FirstTfModel2
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.reflect.typeOf


class MainActivity : AppCompatActivity() {
    // creating a variable for our relative layout
    private var relativeLayout: RelativeLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // initializing our view.
        relativeLayout = findViewById(R.id.idRLView)

        // calling our paint view class and adding
        // its view to our relative layout.
        val paintView = PaintView(this)
        relativeLayout?.addView(paintView)

        if (! Python.isStarted()) {
            Python.start(AndroidPlatform(applicationContext));
        }

        val agent: Agent = Agent()
        var dice = agent.rollDice()

//        callPythonGetActions()
        // Initialize Python
        val python = Python.getInstance()

        val pyGameModule = python.getModule("game") // "game.py" corresponds to "game"

        // Create an instance of the Game class
        val gameInstance: PyObject = pyGameModule.callAttr("Game")

        // Call the `get_valid_actions` method
        val validActions: PyObject = gameInstance.callAttr(
            "get_valid_actions",
            dice.first,  // die_1
            dice.second   // die_2
        )
        Log.d("yle", validActions.toString())
        val actionOutcomeStates = gameInstance.callAttr(
            "get_action_outcome_states",
            validActions
        )

        val actionOutcomeStatesList: List<List<Float>> = convertToListOfLists(actionOutcomeStates)
        Log.d("Python", "Nested list: $actionOutcomeStatesList")

//
        val model = FirstTfModel2.newInstance(applicationContext)
        var outputsList = mutableListOf<Float>()
        actionOutcomeStatesList.forEach { observation ->
            val inputData = observation
            val byteBuffer = ByteBuffer.allocateDirect(4 * 198).order(ByteOrder.nativeOrder())
            inputData.forEach { byteBuffer.putFloat(it) }
            byteBuffer.rewind()
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 198), DataType.FLOAT32)
            inputFeature0.loadBuffer(byteBuffer)

            // Runs model inference and gets result.
            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer

            println("Raw Output: ${outputFeature0.floatArray.joinToString(", ")}")
            val outputArray = outputs.outputFeature0AsTensorBuffer.floatArray
            outputsList.add(outputArray[0])
        }
        val argmax = outputsList.withIndex().maxByOrNull { it.value }?.index
        val validActionsList: List<List<List<Int>>> = convertToListOfListsOfLists(validActions)
        println(validActionsList[argmax!!])

//        // Releases model resources if no longer used.
        model.close()
        if( argmax != null){
            val pythonValidActions = convertToPythonList(validActionsList[argmax!!])
            val makeMove: PyObject = gameInstance.callAttr(
                "make_step",
                pythonValidActions
            )
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
                    pos.toInt()}
            }
        }
    }
    fun <T> convertToPythonList(kotlinList: List<T>): PyObject {
        val py = Python.getInstance()
        val pyList = py.getBuiltins().callAttr("list")
        kotlinList.forEach { item ->
            when (item) {
                is List<*> -> pyList.callAttr("append", convertToPythonList(item as List<Any>))
                else -> pyList.callAttr("append", item)
            }
        }
        return pyList
    }

}
