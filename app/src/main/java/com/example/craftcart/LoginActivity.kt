package com.example.craftcart

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)

        val loginButton = findViewById<Button>(R.id.loginButton)
        val signupText = findViewById<TextView>(R.id.signupText)

        // LOGIN BUTTON
        loginButton.setOnClickListener {

            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // Check empty fields
            if (email.isEmpty()) {
                emailEditText.error = "Please enter your email or username"
                emailEditText.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                passwordEditText.error = "Please enter your password"
                passwordEditText.requestFocus()
                return@setOnClickListener
            }

            // If both fields are filled, open Home
            Toast.makeText(
                this,
                "Login successful!",
                Toast.LENGTH_SHORT
            ).show()

            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

        // SIGN UP
        signupText.setOnClickListener {

            val intent = Intent(
                this,
                SignupActivity::class.java
            )

            startActivity(intent)
        }
    }
}