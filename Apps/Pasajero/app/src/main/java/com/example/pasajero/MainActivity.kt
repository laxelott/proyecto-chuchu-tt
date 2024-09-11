package com.example.pasajero

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    //Variable for the Linear Layout
    private lateinit var container: LinearLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //Code
        //Getting the layout_transports
        container = findViewById(R.id.layout_transports)

        //Adding images to layout
        val image = ImageButton(this)
        image.setBackgroundResource(R.drawable.trolebus)
        //image.layoutParams = LinearLayout.LayoutParams(90, 90)
        container.addView(image)

        //When click on the image
        image.setOnClickListener {
            val intent = Intent (this, SelectedTransportActivity::class.java)
            startActivity(intent)
        }

    }
}