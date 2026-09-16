package com.example.craftcart

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.craftcart.data.AppDatabase
import com.example.craftcart.data.entity.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val nameEditText = findViewById<EditText>(R.id.nameEditText)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)

        val createAccountButton = findViewById<Button>(R.id.createAccountButton)
        val loginText = findViewById<TextView>(R.id.loginText)

        createAccountButton?.setOnClickListener {
            val name = nameEditText?.text?.toString()?.trim() ?: ""
            val email = emailEditText?.text?.toString()?.trim() ?: ""
            val password = passwordEditText?.text?.toString()?.trim() ?: ""

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(applicationContext)
                val user = User(name = name, email = email, password = password)
                val insertedId = db.userDao().insert(user)

                // Sync user to Firestore "users" collection (without password)
                try {
                    val userData = hashMapOf(
                        "id" to insertedId,
                        "name" to name,
                        "email" to email
                    )
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(insertedId.toString())
                        .set(userData)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SignupActivity, "Account created!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@SignupActivity, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }

        loginText?.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
            finish()
        }
    }
}