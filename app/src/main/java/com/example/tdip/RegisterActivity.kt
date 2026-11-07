package com.example.tdip

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

// RegisterActivity: Dito gumagawa ng bagong account para sa Customer o Resort Admin.
class RegisterActivity : AppCompatActivity() {

    private var selectedRole: String = "Customer"

    private val nasugbuResorts = listOf(
        "Bernabeach",
        "Calayo Beach Resort",
        "Sand Bar Beach Resort",
        "Pamana Resorts",
        "Canyon Cove",
        "Pico Sands Hotel",
        "Club Punta Fuego",
        "Shoreland",
        "RSAM Beach Resort",
        "Pico de Loro"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnRoleCustomer = findViewById<Button>(R.id.reg_btn_role_customer)
        val btnRoleAdmin = findViewById<Button>(R.id.reg_btn_role_admin)
        val lblResortName = findViewById<TextView>(R.id.lbl_resort_name)
        val spinnerResort = findViewById<Spinner>(R.id.reg_resort_spinner)

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, nasugbuResorts)
        spinnerResort.adapter = spinnerAdapter

        val inputName = findViewById<EditText>(R.id.reg_name_input)
        val inputEmail = findViewById<EditText>(R.id.reg_email_input)
        val inputPassword = findViewById<EditText>(R.id.reg_password_input)

        val btnRegister = findViewById<Button>(R.id.btn_register)
        val btnBackToLogin = findViewById<Button>(R.id.btn_back_to_login)

        // Role Toggle
        btnRoleCustomer.setOnClickListener {
            selectedRole = "Customer"
            btnRoleCustomer.setTextColor(Color.parseColor("#B388FF"))
            btnRoleAdmin.setTextColor(Color.parseColor("#CCCCCC"))
            lblResortName.visibility = View.GONE
            spinnerResort.visibility = View.GONE
        }

        btnRoleAdmin.setOnClickListener {
            selectedRole = "Admin"
            btnRoleAdmin.setTextColor(Color.parseColor("#B388FF"))
            btnRoleCustomer.setTextColor(Color.parseColor("#CCCCCC"))
            lblResortName.visibility = View.VISIBLE
            spinnerResort.visibility = View.VISIBLE
        }

        btnRegister.setOnClickListener {
            val name = inputName.text.toString().trim()
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString().trim()
            val chosenResort = spinnerResort.selectedItem?.toString() ?: "Canyon Cove"

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val registeredUser: Person = if (selectedRole == "Admin") {
                UserManager.registerAdmin(name, email, password, chosenResort)
            } else {
                UserManager.registerTourist(name, email, password)
            }

            // Sync user to Supabase Cloud and display full readable message
            Toast.makeText(this, "Uploading user to Cloud...", Toast.LENGTH_SHORT).show()
            SupabaseCloudManager.uploadUserToSupabase(registeredUser) { success, msg ->
                if (success) {
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    DialogUtils.showFullErrorDialog(
                        this,
                        "Cloud Registration Response",
                        "Account registered in local memory.\n\nCloud Status:\n$msg"
                    )
                }
            }
        }

        btnBackToLogin.setOnClickListener {
            finish()
        }
    }
}
