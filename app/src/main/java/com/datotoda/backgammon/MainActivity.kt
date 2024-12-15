package com.datotoda.backgammon

import android.os.Bundle
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity


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
    }
}
