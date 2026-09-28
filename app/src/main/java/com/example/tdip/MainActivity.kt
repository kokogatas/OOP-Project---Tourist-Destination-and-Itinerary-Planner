package com.example.tdip

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        /*
        val clickThis = findViewById<Button>(R.id.clickThis)

        clickThis.setOnClickListener {
            Toast.makeText(this, "Hi there!", Toast.LENGTH_SHORT).show()
        }
        */

        val registerButton = findViewById<Button>(R.id.register_button)
        val continueButton = findViewById<Button>(R.id.continue_button)

        registerButton.setOnClickListener {
            Toast.makeText(this, "Register button is clicked", Toast.LENGTH_SHORT).show()
        }

        continueButton.setOnClickListener {
            Toast.makeText(this, "Continue button is clicked", Toast.LENGTH_SHORT).show()
        }
    }
}