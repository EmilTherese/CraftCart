package com.example.craftcart

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.craftcart.data.AppDatabase
import com.example.craftcart.data.entity.Order
import com.example.craftcart.data.entity.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OrdersActivity : AppCompatActivity() {

    private lateinit var ordersContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_orders)

        ordersContainer = findViewById(R.id.ordersContainer)

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
            // Stay on OrdersActivity
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

        observeOrdersFromRoom()
    }

    private fun observeOrdersFromRoom() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val db = AppDatabase.getDatabase(applicationContext)
                db.orderDao().getOrdersFlow().collect { rawOrders ->
                    val orders = rawOrders.reversed()
                    val ordersWithProducts = withContext(Dispatchers.IO) {
                        orders.map { order ->
                            val product = db.productDao().getProductById(order.productId) ?: getFallbackProduct(order.productId)
                            Pair(order, product)
                        }
                    }
                    renderOrdersUI(ordersWithProducts)
                }
            }
        }
    }

    private fun renderOrdersUI(items: List<Pair<Order, Product>>) {
        ordersContainer.removeAllViews()

        if (items.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "No orders yet"
                textSize = 16f
                setTextColor(Color.parseColor("#888888"))
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(60), 0, dpToPx(60))
            }
            ordersContainer.addView(emptyText)
            return
        }

        for ((order, product) in items) {
            val orderCard = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dpToPx(8)
                }
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.WHITE)
                setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            }

            // Header row: Order ID & Date on left, Status on right
            val headerLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val idDateLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                orientation = LinearLayout.VERTICAL
            }

            val orderIdText = TextView(this).apply {
                text = "Order ID: #CC${order.id}"
                textSize = 13f
                setTextColor(Color.parseColor("#2E1B18"))
                setTypeface(null, Typeface.BOLD)
            }

            val dateText = TextView(this).apply {
                text = "Placed on ${order.orderDate}"
                textSize = 10f
                setTextColor(Color.parseColor("#777777"))
                setPadding(0, dpToPx(3), 0, 0)
            }

            idDateLayout.addView(orderIdText)
            idDateLayout.addView(dateText)

            val statusText = TextView(this).apply {
                text = "Confirmed"
                textSize = 11f
                setTextColor(Color.parseColor("#57935B"))
                setTypeface(null, Typeface.BOLD)
            }

            headerLayout.addView(idDateLayout)
            headerLayout.addView(statusText)
            orderCard.addView(headerLayout)

            // Body row: Product Image + Product Name/Qty + Total Price
            val bodyLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dpToPx(10)
                }
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val imageView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(52), dpToPx(52))
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageResource(getDrawableResId(product))
            }

            val infoLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dpToPx(10)
                }
                orientation = LinearLayout.VERTICAL
            }

            val productNameText = TextView(this).apply {
                text = product.name
                textSize = 13f
                setTextColor(Color.parseColor("#2E1B18"))
                setTypeface(null, Typeface.BOLD)
            }

            val categoryText = TextView(this).apply {
                text = product.category
                textSize = 11f
                setTextColor(Color.parseColor("#777777"))
            }

            val qtyText = TextView(this).apply {
                text = "Qty: ${order.quantity}"
                textSize = 11f
                setTextColor(Color.parseColor("#555555"))
                setPadding(0, dpToPx(2), 0, 0)
            }

            infoLayout.addView(productNameText)
            infoLayout.addView(categoryText)
            infoLayout.addView(qtyText)

            val priceLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.END
            }

            val totalPriceText = TextView(this).apply {
                text = "Total: ₹${order.totalPrice.toInt()}"
                textSize = 12f
                setTextColor(Color.parseColor("#2E1B18"))
                setTypeface(null, Typeface.BOLD)
            }

            val detailsText = TextView(this).apply {
                text = "View Details  ›"
                textSize = 10f
                setTextColor(Color.parseColor("#B56576"))
                setTypeface(null, Typeface.BOLD)
                setPadding(0, dpToPx(6), 0, 0)
            }

            priceLayout.addView(totalPriceText)
            priceLayout.addView(detailsText)

            bodyLayout.addView(imageView)
            bodyLayout.addView(infoLayout)
            bodyLayout.addView(priceLayout)
            orderCard.addView(bodyLayout)

            // Click opens Product Details
            orderCard.setOnClickListener {
                val intent = Intent(this, ProductDetailActivity::class.java).apply {
                    putExtra("PRODUCT_ID", product.id)
                }
                startActivity(intent)
            }

            ordersContainer.addView(orderCard)
        }
    }

    private fun getDrawableResId(product: Product): Int {
        val nameLower = product.name.lowercase().trim()
        return when {
            nameLower.contains("puppy portrait") || product.id == 1L -> R.drawable.pup_painting
            nameLower.contains("puppy dreams") || product.id == 2L -> R.drawable.puppy_painting
            nameLower.contains("tasty") || product.id == 3L -> R.drawable.tasty_painting
            nameLower.contains("cute christmas") || product.id == 4L -> R.drawable.cute_christmas
            nameLower.contains("meow christmas") || product.id == 5L -> R.drawable.meow_christmas
            nameLower.contains("garden portrait") || product.id == 6L -> R.drawable.pup_painting
            nameLower.contains("clay house") || product.id == 7L -> R.drawable.clay_house
            nameLower.contains("miffy") || product.id == 8L -> R.drawable.miffy_keychai
            nameLower.contains("flower") || nameLower.contains("planter") || product.id == 9L -> R.drawable.flower_planters
            else -> R.drawable.pup_painting
        }
    }

    private fun getFallbackProduct(id: Long): Product {
        return when (id) {
            1L -> Product(id = 1, name = "Puppy Portrait", price = 499.0, category = "Canvas Painting", imageResId = R.drawable.pup_painting)
            2L -> Product(id = 2, name = "Puppy Dreams", price = 499.0, category = "Canvas Painting", imageResId = R.drawable.puppy_painting)
            3L -> Product(id = 3, name = "Tasty Little Things", price = 449.0, category = "Canvas Painting", imageResId = R.drawable.tasty_painting)
            4L -> Product(id = 4, name = "Cute Christmas", price = 399.0, category = "Seasonal Art", imageResId = R.drawable.cute_christmas)
            5L -> Product(id = 5, name = "Meow Christmas", price = 449.0, category = "Seasonal Art", imageResId = R.drawable.meow_christmas)
            6L -> Product(id = 6, name = "Garden Portrait", price = 499.0, category = "Handmade Art", imageResId = R.drawable.pup_painting)
            7L -> Product(id = 7, name = "Clay House", price = 599.0, category = "Home Decor", imageResId = R.drawable.clay_house)
            8L -> Product(id = 8, name = "Miffy Keychain", price = 349.0, category = "Keychains", imageResId = R.drawable.miffy_keychai)
            9L -> Product(id = 9, name = "Flower Planters", price = 449.0, category = "Planters", imageResId = R.drawable.flower_planters)
            else -> Product(id = id, name = "Craft Product", price = 499.0, category = "Handmade", imageResId = R.drawable.pup_painting)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}