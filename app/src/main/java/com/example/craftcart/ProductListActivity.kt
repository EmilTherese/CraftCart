package com.example.craftcart

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ProductListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_list)

        // Top Bar
        findViewById<TextView>(R.id.backButton)?.setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.cartButton)?.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        // Product Cards
        val productClickListener = View.OnClickListener {
            startActivity(Intent(this, ProductDetailActivity::class.java))
        }

        findViewById<View>(R.id.productCard1)?.setOnClickListener(productClickListener)
        findViewById<View>(R.id.productCard2)?.setOnClickListener(productClickListener)
        findViewById<View>(R.id.productCard3)?.setOnClickListener(productClickListener)
        findViewById<View>(R.id.productCard4)?.setOnClickListener(productClickListener)
        findViewById<View>(R.id.productCard5)?.setOnClickListener(productClickListener)
        findViewById<View>(R.id.productCard6)?.setOnClickListener(productClickListener)

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