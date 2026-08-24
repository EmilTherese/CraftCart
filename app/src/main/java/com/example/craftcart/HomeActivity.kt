package com.example.craftcart

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Header Navigation
        findViewById<TextView>(R.id.headerWishlist)?.setOnClickListener {
            val intent = Intent(this, WishlistActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)
        }

        findViewById<TextView>(R.id.headerCart)?.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        // Recommended Section & Products
        findViewById<TextView>(R.id.seeAllText)?.setOnClickListener {
            startActivity(Intent(this, ProductListActivity::class.java))
        }

        findViewById<View>(R.id.clayHouseCard)?.setOnClickListener {
            startActivity(Intent(this, ProductDetailActivity::class.java))
        }

        findViewById<View>(R.id.miffyCard)?.setOnClickListener {
            startActivity(Intent(this, ProductDetailActivity::class.java))
        }

        findViewById<View>(R.id.flowerCard)?.setOnClickListener {
            startActivity(Intent(this, ProductDetailActivity::class.java))
        }

        // Chatbot Button
        findViewById<TextView>(R.id.chatbotButton)?.setOnClickListener {
            startActivity(Intent(this, ChatbotActivity::class.java))
        }

        // Bottom Navigation
        findViewById<View>(R.id.navHome)?.setOnClickListener {
            // Stay on HomeActivity
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
            val intent = Intent(this, WishlistActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)
        }

        findViewById<View>(R.id.navProfile)?.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)
        }
    }
}