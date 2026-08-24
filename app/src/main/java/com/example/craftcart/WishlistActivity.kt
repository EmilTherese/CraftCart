package com.example.craftcart

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WishlistActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wishlist)

        // Top Bar
        findViewById<TextView>(R.id.backButton)?.setOnClickListener {
            finish()
        }

        // Bottom Navigation
        findViewById<View>(R.id.navHome)?.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)
        }

        findViewById<View>(R.id.navCategories)?.setOnClickListener {
            val intent = Intent(this, CategoryActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)
        }

        findViewById<View>(R.id.navOrders)?.setOnClickListener {
            val intent = Intent(this, OrdersActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)
        }

        findViewById<View>(R.id.navWishlist)?.setOnClickListener {
            // Stay on WishlistActivity
        }

        findViewById<View>(R.id.navProfile)?.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)
        }
    }
}