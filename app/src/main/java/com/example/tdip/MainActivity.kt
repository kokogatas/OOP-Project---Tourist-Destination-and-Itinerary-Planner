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

        // Silent pre-fetch from GitHub Cloud on launch
        if (NetworkUtils.isInternetAvailable(this)) {
            GitHubCloudManager.fetchUsersFromGitHub { _, _ -> }
        }

        val btnRoleCustomer = findViewById<Button>(R.id.btn_role_customer)
        val btnRoleAdmin = findViewById<Button>(R.id.btn_role_admin)

        val registerButton = findViewById<Button>(R.id.register_button)
        val continueButton = findViewById<Button>(R.id.continue_button)
        val emailInput = findViewById<EditText>(R.id.editTextTextEmailAddress)
        val passwordInput = findViewById<EditText>(R.id.editTextTextPassword)

        // Role Selector Buttons
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

        // Registration Button
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

            // Step 1: Show "Connecting to Cloud..." message immediately
            Toast.makeText(this, "Connecting to Cloud...", Toast.LENGTH_SHORT).show()

            // Step 2: Attempt connection to GitHub Cloud API
            GitHubCloudManager.fetchUsersFromGitHub { success, msg ->
                if (!success) {
                    // Step 3: If no reply or no internet connection, show error message
                    Toast.makeText(this, "No internet connection or cloud server did not reply. Please check your network.", Toast.LENGTH_LONG).show()
                } else {
                    // Step 4: If reply received from Cloud, proceed to verify credentials
                    val user = UserManager.login(emailOrName, password, selectedRole)

                    if (user != null) {
                        Toast.makeText(this, "Welcome back, ${user.name}!", Toast.LENGTH_SHORT).show()

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
                    } else {
                        Toast.makeText(this, "Invalid credentials or user not found for $selectedRole!", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
