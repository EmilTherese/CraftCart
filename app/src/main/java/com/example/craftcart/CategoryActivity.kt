package com.example.craftcart

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CategoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        // Top Bar
        findViewById<TextView>(R.id.backButton)?.setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.cartButton)?.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        // Category Cards -> Pass CATEGORY_NAME extra
        findViewById<View>(R.id.paintingsCategory)?.setOnClickListener { openCategory("Paintings") }
        findViewById<View>(R.id.giftsCategory)?.setOnClickListener { openCategory("Handmade Gifts") }
        findViewById<View>(R.id.keychainsCategory)?.setOnClickListener { openCategory("Keychains") }
        findViewById<View>(R.id.lettersCategory)?.setOnClickListener { openCategory("Handwritten Letters") }
        findViewById<View>(R.id.homeDecorCategory)?.setOnClickListener { openCategory("Home Decor") }
        findViewById<View>(R.id.crochetCategory)?.setOnClickListener { openCategory("Crochet & Knit") }

        // Bottom Navigation
        findViewById<View>(R.id.homeNav)?.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)
        }

        findViewById<View>(R.id.categoriesNav)?.setOnClickListener {
            // Stay on CategoryActivity
        }

        findViewById<View>(R.id.ordersNav)?.setOnClickListener {
            val intent = Intent(this, OrdersActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)
        }

        findViewById<View>(R.id.wishlistNav)?.setOnClickListener {
            val intent = Intent(this, WishlistActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)
        }

        findViewById<View>(R.id.profileNav)?.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)
        }
    }

    private fun openCategory(categoryName: String) {
        val intent = Intent(this, ProductListActivity::class.java).apply {
            putExtra("CATEGORY_NAME", categoryName)
        }
        startActivity(intent)
    }
}