package com.example.tdip

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

// MainActivity: Ang pinto ng app. Dito nagse-select ng role at naglo-log in.
class MainActivity : AppCompatActivity() {

    private var selectedRole: String = "Customer"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Sy-sync muna ang user accounts mula sa Cloud agad
        if (NetworkUtils.isInternetAvailable(this)) {
            SupabaseCloudManager.fetchUsersFromSupabase { _, _ -> }
        }

        val btnRoleCustomer = findViewById<Button>(R.id.btn_role_customer)
        val btnRoleAdmin = findViewById<Button>(R.id.btn_role_admin)

        val registerButton = findViewById<Button>(R.id.register_button)
        val continueButton = findViewById<Button>(R.id.continue_button)
        val emailInput = findViewById<EditText>(R.id.editTextTextEmailAddress)
        val passwordInput = findViewById<EditText>(R.id.editTextTextPassword)

        // Select Role Buttons
        btnRoleCustomer.setOnClickListener {
            selectedRole = "Customer"
            btnRoleCustomer.setTextColor(Color.parseColor("#B388FF"))
            btnRoleAdmin.setTextColor(Color.parseColor("#CCCCCC"))
        }

        btnRoleAdmin.setOnClickListener {
            selectedRole = "Admin"
            btnRoleAdmin.setTextColor(Color.parseColor("#B388FF"))
            btnRoleCustomer.setTextColor(Color.parseColor("#CCCCCC"))
        }

        // Navigate to Registration Screen
        registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Login Continue Button
        continueButton.setOnClickListener {
            val emailOrName = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (emailOrName.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email/username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Authenticate directly from local database first
            var user = UserManager.login(emailOrName, password, selectedRole)

            if (user != null) {
                Toast.makeText(this, "Welcome back, ${user.name}!", Toast.LENGTH_SHORT).show()
                proceedToDashboard(user)
                return@setOnClickListener
            }

            if (!NetworkUtils.isInternetAvailable(this)) {
                Toast.makeText(this, "Offline: Account not found for $selectedRole.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show()

            // Fetch latest user database from Supabase immediately, then authenticate
            SupabaseCloudManager.fetchUsersFromSupabase { _, _ ->
                user = UserManager.login(emailOrName, password, selectedRole)

                if (user != null) {
                    Toast.makeText(this, "Welcome back, ${user!!.name}!", Toast.LENGTH_SHORT).show()
                    proceedToDashboard(user!!)
                } else {
                    Toast.makeText(this, "Invalid credentials or user not found for $selectedRole!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun proceedToDashboard(user: Person) {
        if (user is ResortAdmin) {
            val intent = Intent(this, AdminScreen::class.java)
            intent.putExtra("ADMIN_NAME", user.name)
            intent.putExtra("RESORT_NAME", user.resortName)
            startActivity(intent)
            finish()
        } else if (user is Tourist) {
            val intent = Intent(this, Mainscreen::class.java)
            intent.putExtra("USER_EMAIL", user.email)
            startActivity(intent)
            finish()
        }
    }
}
