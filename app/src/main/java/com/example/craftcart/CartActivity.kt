package com.example.craftcart

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.craftcart.data.AppDatabase
import com.example.craftcart.data.entity.Cart
import com.example.craftcart.data.entity.Order
import com.example.craftcart.data.entity.Product
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CartActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout
    private lateinit var subtotalTextView: TextView
    private lateinit var totalTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        container = findViewById(R.id.cartItemsContainer)
        subtotalTextView = findViewById(R.id.subtotalText)
        totalTextView = findViewById(R.id.totalText)

        findViewById<TextView>(R.id.backButton)?.setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.checkoutButton)?.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(applicationContext)
                val cartList = db.cartDao().getAllCartItems()
                for (item in cartList) {
                    val product = db.productDao().getProductById(item.productId) ?: getFallbackProduct(item.productId)
                    val order = Order(
                        productId = item.productId,
                        quantity = item.quantity,
                        totalPrice = product.price * item.quantity,
                        orderDate = "2026-09-16"
                    )
                    val insertedOrderId = db.orderDao().insert(order)

                    // Sync order to Firestore "orders"
                    try {
                        val orderData = hashMapOf(
                            "id" to insertedOrderId,
                            "productId" to item.productId,
                            "productName" to product.name,
                            "quantity" to item.quantity,
                            "totalPrice" to (product.price * item.quantity),
                            "orderDate" to "2026-09-16"
                        )
                        FirebaseFirestore.getInstance()
                            .collection("orders")
                            .document(insertedOrderId.toString())
                            .set(orderData)

                        // Delete from Firestore "cart_items"
                        FirebaseFirestore.getInstance()
                            .collection("cart_items")
                            .document(item.productId.toString())
                            .delete()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    db.cartDao().delete(item)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CartActivity, "Order Placed Successfully!", Toast.LENGTH_SHORT).show()
                    loadCartFromRoom()
                }
            }
        }

        loadCartFromRoom()
    }

    override fun onResume() {
        super.onResume()
        loadCartFromRoom()
    }

    private fun loadCartFromRoom() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            val cartList = db.cartDao().getAllCartItems()

            val itemsWithProducts = cartList.map { cart ->
                val product = db.productDao().getProductById(cart.productId) ?: getFallbackProduct(cart.productId)
                Pair(cart, product)
            }

            withContext(Dispatchers.Main) {
                renderCartUI(itemsWithProducts)
            }
        }
    }

    private fun renderCartUI(items: List<Pair<Cart, Product>>) {
        container.removeAllViews()

        if (items.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "Your Cart is empty"
                textSize = 16f
                setTextColor(Color.parseColor("#888888"))
                gravity = Gravity.CENTER
                setPadding(0, 60, 0, 60)
            }
            container.addView(emptyText)
            subtotalTextView.text = "₹0"
            totalTextView.text = "₹0"
            return
        }

        var subtotal = 0.0

        for ((cartItem, product) in items) {
            val itemPrice = product.price * cartItem.quantity
            subtotal += itemPrice

            val itemLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(94)
                ).apply {
                    topMargin = dpToPx(6)
                }
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(Color.WHITE)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(7), dpToPx(7), dpToPx(7), dpToPx(7))
            }

            // Image
            val imageView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(78), dpToPx(80))
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageResource(getDrawableResId(product))
            }
            itemLayout.addView(imageView)

            // Info Column
            val infoLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, dpToPx(80), 1f).apply {
                    marginStart = dpToPx(10)
                }
                orientation = LinearLayout.VERTICAL
                setPadding(0, dpToPx(4), 0, 0)
            }

            val titleText = TextView(this).apply {
                text = product.name
                textSize = 14f
                setTextColor(Color.parseColor("#2E1B18"))
                setTypeface(null, Typeface.BOLD)
            }
            val categoryText = TextView(this).apply {
                text = product.category
                textSize = 11f
                setTextColor(Color.parseColor("#888888"))
            }
            val priceText = TextView(this).apply {
                text = "₹${product.price.toInt()}"
                textSize = 14f
                setTextColor(Color.parseColor("#B56576"))
                setTypeface(null, Typeface.BOLD)
            }

            infoLayout.addView(titleText)
            infoLayout.addView(categoryText)
            infoLayout.addView(priceText)
            itemLayout.addView(infoLayout)

            // Quantity Control
            val qtyLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(72), dpToPx(32))
                setBackgroundColor(Color.parseColor("#FDF8F4"))
                gravity = Gravity.CENTER
                orientation = LinearLayout.HORIZONTAL
            }

            val minusBtn = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(22), LinearLayout.LayoutParams.MATCH_PARENT)
                text = "−"
                textSize = 14f
                setTextColor(Color.parseColor("#555555"))
                gravity = Gravity.CENTER
                setOnClickListener { updateQuantity(cartItem, cartItem.quantity - 1) }
            }

            val qtyText = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(28), LinearLayout.LayoutParams.MATCH_PARENT)
                text = "${cartItem.quantity}"
                textSize = 13f
                setTextColor(Color.parseColor("#2E1B18"))
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
            }

            val plusBtn = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(22), LinearLayout.LayoutParams.MATCH_PARENT)
                text = "+"
                textSize = 14f
                setTextColor(Color.parseColor("#555555"))
                gravity = Gravity.CENTER
                setOnClickListener { updateQuantity(cartItem, cartItem.quantity + 1) }
            }

            qtyLayout.addView(minusBtn)
            qtyLayout.addView(qtyText)
            qtyLayout.addView(plusBtn)
            itemLayout.addView(qtyLayout)

            // Delete Btn
            val deleteBtn = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(30), dpToPx(40))
                text = "⌫"
                textSize = 16f
                setTextColor(Color.parseColor("#777777"))
                gravity = Gravity.CENTER
                setOnClickListener { deleteCartItem(cartItem) }
            }
            itemLayout.addView(deleteBtn)

            container.addView(itemLayout)
        }

        subtotalTextView.text = "₹${subtotal.toInt()}"
        val grandTotal = if (subtotal > 0) subtotal + 40 else 0.0
        totalTextView.text = "₹${grandTotal.toInt()}"
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

    private fun updateQuantity(cartItem: Cart, newQty: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            if (newQty <= 0) {
                db.cartDao().delete(cartItem)
                try {
                    FirebaseFirestore.getInstance()
                        .collection("cart_items")
                        .document(cartItem.productId.toString())
                        .delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                val updated = cartItem.copy(quantity = newQty)
                db.cartDao().update(updated)
                try {
                    val product = db.productDao().getProductById(cartItem.productId) ?: getFallbackProduct(cartItem.productId)
                    val cartData = hashMapOf(
                        "productId" to cartItem.productId,
                        "productName" to product.name,
                        "price" to product.price,
                        "quantity" to newQty
                    )
                    FirebaseFirestore.getInstance()
                        .collection("cart_items")
                        .document(cartItem.productId.toString())
                        .set(cartData)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            loadCartFromRoom()
        }
    }

    private fun deleteCartItem(cartItem: Cart) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            db.cartDao().delete(cartItem)
            try {
                FirebaseFirestore.getInstance()
                    .collection("cart_items")
                    .document(cartItem.productId.toString())
                    .delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            loadCartFromRoom()
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
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
}