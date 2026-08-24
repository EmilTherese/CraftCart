package com.example.craftcart

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ProductDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        // Top Bar
        findViewById<TextView>(R.id.backButton)?.setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.wishlistButton)?.setOnClickListener {
            val intent = Intent(this, WishlistActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)
        }

        findViewById<TextView>(R.id.cartButton)?.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        // Action Buttons
        findViewById<Button>(R.id.addToCartButton)?.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        findViewById<Button>(R.id.buyNowButton)?.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }
    }
}