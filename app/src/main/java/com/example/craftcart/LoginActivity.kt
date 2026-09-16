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
import com.example.craftcart.data.entity.Product
import com.example.craftcart.data.entity.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)

        val loginButton = findViewById<Button>(R.id.loginButton)
        val signupText = findViewById<TextView>(R.id.signupText)

        // Seed initial Room data on launch
        seedInitialDatabaseData()

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

            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(applicationContext)
                var user = db.userDao().getUserByEmail(email)
                if (user == null) {
                    user = User(name = email.substringBefore("@"), email = email, password = password)
                    val insertedId = db.userDao().insert(user)
                    user = user.copy(id = insertedId)
                }

                // Sync user to Firestore "users" collection (without password)
                try {
                    val userData = hashMapOf(
                        "id" to user.id,
                        "name" to user.name,
                        "email" to user.email
                    )
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(user.id.toString())
                        .set(userData)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Login successful!",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(this@LoginActivity, HomeActivity::class.java)
                    startActivity(intent)
                }
            }
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

    private fun seedInitialDatabaseData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            var existingProducts = db.productDao().getAllProducts()
            if (existingProducts.isEmpty()) {
                val sampleProducts = listOf(
                    Product(id = 1, name = "Puppy Portrait", price = 499.0, category = "Canvas Painting", imageResId = R.drawable.pup_painting),
                    Product(id = 2, name = "Puppy Dreams", price = 499.0, category = "Canvas Painting", imageResId = R.drawable.puppy_painting),
                    Product(id = 3, name = "Tasty Little Things", price = 449.0, category = "Canvas Painting", imageResId = R.drawable.tasty_painting),
                    Product(id = 4, name = "Cute Christmas", price = 399.0, category = "Seasonal Art", imageResId = R.drawable.cute_christmas),
                    Product(id = 5, name = "Meow Christmas", price = 449.0, category = "Seasonal Art", imageResId = R.drawable.meow_christmas),
                    Product(id = 6, name = "Garden Portrait", price = 499.0, category = "Handmade Art", imageResId = R.drawable.pup_painting),
                    Product(id = 7, name = "Clay House", price = 599.0, category = "Home Decor", imageResId = R.drawable.clay_house),
                    Product(id = 8, name = "Miffy Keychain", price = 349.0, category = "Keychains", imageResId = R.drawable.miffy_keychai),
                    Product(id = 9, name = "Flower Planters", price = 449.0, category = "Planters", imageResId = R.drawable.flower_planters)
                )
                db.productDao().insertAll(sampleProducts)
                existingProducts = db.productDao().getAllProducts()
            }

            // Sync products to Firestore "products" collection
            try {
                val firestore = FirebaseFirestore.getInstance()
                for (product in existingProducts) {
                    val productData = hashMapOf(
                        "productId" to product.id,
                        "productName" to product.name,
                        "category" to product.category,
                        "price" to product.price,
                        "imageName" to getProductImageName(product.id)
                    )
                    firestore.collection("products")
                        .document(product.id.toString())
                        .set(productData)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getProductImageName(id: Long): String {
        return when (id) {
            1L -> "pup_painting"
            2L -> "puppy_painting"
            3L -> "tasty_painting"
            4L -> "cute_christmas"
            5L -> "meow_christmas"
            6L -> "pup_painting"
            7L -> "clay_house"
            8L -> "miffy_keychai"
            9L -> "flower_planters"
            else -> "pup_painting"
        }
    }
}