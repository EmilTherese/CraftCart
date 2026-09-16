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
import com.example.craftcart.data.entity.Product
import com.example.craftcart.data.entity.Wishlist
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WishlistActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wishlist)

        container = findViewById(R.id.wishlistItemsContainer)

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

        loadWishlistFromRoom()
    }

    override fun onResume() {
        super.onResume()
        loadWishlistFromRoom()
    }

    private fun loadWishlistFromRoom() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            val wishlistList = db.wishlistDao().getAllWishlistItems()

            val itemsWithProducts = wishlistList.map { item ->
                val product = db.productDao().getProductById(item.productId) ?: getFallbackProduct(item.productId)
                Pair(item, product)
            }

            withContext(Dispatchers.Main) {
                renderWishlistUI(itemsWithProducts)
            }
        }
    }

    private fun renderWishlistUI(items: List<Pair<Wishlist, Product>>) {
        container.removeAllViews()

        if (items.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "Your Wishlist is empty"
                textSize = 16f
                setTextColor(Color.parseColor("#888888"))
                gravity = Gravity.CENTER
                setPadding(0, 60, 0, 60)
            }
            container.addView(emptyText)
            return
        }

        for ((wishlistItem, product) in items) {
            val itemLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(112)
                ).apply {
                    topMargin = dpToPx(6)
                }
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(Color.WHITE)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10))
            }

            // Image
            val imageView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(78), dpToPx(92))
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageResource(getDrawableResId(product))
            }
            itemLayout.addView(imageView)

            // Info Column
            val infoLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, dpToPx(92), 1f).apply {
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
                setTextColor(Color.parseColor("#2E1B18"))
                setTypeface(null, Typeface.BOLD)
            }

            val moveBtn = Button(this).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(120), dpToPx(32)).apply {
                    gravity = Gravity.END
                    topMargin = dpToPx(-2)
                }
                text = "Move to Cart"
                textSize = 10f
                setTextColor(Color.parseColor("#B56576"))
                setBackgroundColor(Color.WHITE)
                setOnClickListener { moveToCart(wishlistItem, product) }
            }

            infoLayout.addView(titleText)
            infoLayout.addView(categoryText)
            infoLayout.addView(priceText)
            infoLayout.addView(moveBtn)
            itemLayout.addView(infoLayout)

            // Delete Btn
            val deleteBtn = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(28), dpToPx(40))
                text = "⌫"
                textSize = 17f
                setTextColor(Color.parseColor("#555555"))
                gravity = Gravity.CENTER
                setOnClickListener { deleteWishlistItem(wishlistItem) }
            }
            itemLayout.addView(deleteBtn)

            container.addView(itemLayout)
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

    private fun moveToCart(wishlistItem: Wishlist, product: Product) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            val existingItem = db.cartDao().getCartItemByProductId(product.id)
            val finalQuantity: Int
            if (existingItem != null) {
                val updated = existingItem.copy(quantity = existingItem.quantity + 1)
                db.cartDao().update(updated)
                finalQuantity = updated.quantity
            } else {
                db.cartDao().insert(Cart(productId = product.id, quantity = 1))
                finalQuantity = 1
            }
            db.wishlistDao().delete(wishlistItem)

            // Firestore sync
            try {
                val firestore = FirebaseFirestore.getInstance()

                val cartData = hashMapOf(
                    "productId" to product.id,
                    "productName" to product.name,
                    "price" to product.price,
                    "quantity" to finalQuantity
                )
                firestore.collection("cart_items")
                    .document(product.id.toString())
                    .set(cartData)

                firestore.collection("wishlist_items")
                    .document(wishlistItem.productId.toString())
                    .delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@WishlistActivity, "Moved to Cart!", Toast.LENGTH_SHORT).show()
                loadWishlistFromRoom()
            }
        }
    }

    private fun deleteWishlistItem(wishlistItem: Wishlist) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            db.wishlistDao().delete(wishlistItem)

            // Firestore sync
            try {
                FirebaseFirestore.getInstance()
                    .collection("wishlist_items")
                    .document(wishlistItem.productId.toString())
                    .delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            loadWishlistFromRoom()
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