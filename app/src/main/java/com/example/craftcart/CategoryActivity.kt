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

        // Category Cards
        val categoryClickListener = View.OnClickListener {
            startActivity(Intent(this, ProductListActivity::class.java))
        }

        findViewById<View>(R.id.paintingsCategory)?.setOnClickListener(categoryClickListener)
        findViewById<View>(R.id.giftsCategory)?.setOnClickListener(categoryClickListener)
        findViewById<View>(R.id.keychainsCategory)?.setOnClickListener(categoryClickListener)
        findViewById<View>(R.id.lettersCategory)?.setOnClickListener(categoryClickListener)
        findViewById<View>(R.id.homeDecorCategory)?.setOnClickListener(categoryClickListener)
        findViewById<View>(R.id.crochetCategory)?.setOnClickListener(categoryClickListener)

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
}