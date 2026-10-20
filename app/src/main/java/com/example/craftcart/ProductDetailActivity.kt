package com.example.craftcart

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.viewpager2.widget.ViewPager2
import com.example.craftcart.data.AppDatabase
import com.example.craftcart.data.MediaCatalog
import com.example.craftcart.data.entity.Cart
import com.example.craftcart.data.entity.Order
import com.example.craftcart.data.entity.Product
import com.example.craftcart.data.entity.Wishlist
import com.example.craftcart.data.sync.FirestoreSyncManager
import com.example.craftcart.network.NetworkUtils
import com.example.craftcart.ui.ProductGalleryAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductDetailActivity : AppCompatActivity() {

    private var currentPrice: Double = 499.0
    private var currentProductId: Long = 1L
    private lateinit var galleryAdapter: ProductGalleryAdapter
    private var tabLayoutMediator: TabLayoutMediator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        val galleryViewPager = findViewById<ViewPager2>(R.id.productGalleryViewPager)
        val galleryIndicator = findViewById<TabLayout>(R.id.galleryIndicator)
        galleryAdapter = ProductGalleryAdapter()
        galleryViewPager.adapter = galleryAdapter

        tabLayoutMediator?.detach()
        tabLayoutMediator = TabLayoutMediator(galleryIndicator, galleryViewPager) { _, _ -> }
        tabLayoutMediator?.attach()

        currentProductId = intent.getLongExtra("PRODUCT_ID", 1L)

        // Load dynamic product details from Room Database
        loadProductDetails(currentProductId)

        // Top Bar
        findViewById<TextView>(R.id.backButton)?.setOnClickListener {
            finish()
        }

        // Top-right heart icon -> Navigates to Wishlist Screen
        findViewById<TextView>(R.id.wishlistButton)?.setOnClickListener {
            val intent = Intent(this, WishlistActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)
        }

        findViewById<TextView>(R.id.cartButton)?.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        // Product Image Heart Icon -> Add to Wishlist in Room Database
        findViewById<TextView>(R.id.imageWishlistButton)?.setOnClickListener {
            addToWishlist(currentProductId)
        }

        // Action Buttons
        findViewById<Button>(R.id.addToCartButton)?.setOnClickListener {
            addToCart(currentProductId)
        }

        findViewById<Button>(R.id.buyNowButton)?.setOnClickListener {
            placeOrder(currentProductId, currentPrice)
        }
    }

    override fun onDestroy() {
        tabLayoutMediator?.detach()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentProductId = intent.getLongExtra("PRODUCT_ID", 1L)
        loadProductDetails(currentProductId)
    }

    private fun loadProductDetails(productId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            var product = db.productDao().getProductById(productId)
            if (product == null) {
                product = getFallbackProduct(productId)
            }

            withContext(Dispatchers.Main) {
                product?.let { item ->
                    currentPrice = item.price
                    findViewById<TextView>(R.id.productName)?.text = item.name
                    findViewById<TextView>(R.id.productCategory)?.text = item.category
                    findViewById<TextView>(R.id.productPrice)?.text = "₹${item.price.toInt()}"

                    val fallbackImageRes = getDrawableResId(item)
                    val images = MediaCatalog.imagesFor(item.id, fallbackImageRes)
                    galleryAdapter.submitList(images)
                }
            }
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

    private fun addToCart(productId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            val existingItem = db.cartDao().getCartItemByProductId(productId)
            val finalQuantity: Int
            val cartId: Long
            if (existingItem != null) {
                val updated = existingItem.copy(quantity = existingItem.quantity + 1)
                db.cartDao().deleteByProductId(productId)
                cartId = db.cartDao().insert(updated)
                finalQuantity = updated.quantity
            } else {
                db.cartDao().deleteByProductId(productId)
                val cartItem = Cart(productId = productId, quantity = 1)
                cartId = db.cartDao().insert(cartItem)
                finalQuantity = 1
            }

            var product = db.productDao().getProductById(productId)
            if (product == null) {
                product = getFallbackProduct(productId)
            }
            FirestoreSyncManager.uploadCartItem(
                applicationContext,
                Cart(id = cartId, productId = productId, quantity = finalQuantity),
                product
            )

            withContext(Dispatchers.Main) {
                val isOnline = NetworkUtils.isNetworkAvailable(applicationContext)
                val msg = if (isOnline) "Added to Cart!" else "Added to Cart! (Offline Mode - Queued for Sync)"
                Toast.makeText(this@ProductDetailActivity, msg, Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@ProductDetailActivity, CartActivity::class.java))
            }
        }
    }

    private fun addToWishlist(productId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            val existing = db.wishlistDao().getWishlistItemByProductId(productId)
            if (existing != null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProductDetailActivity, "Already in Wishlist", Toast.LENGTH_SHORT).show()
                }
            } else {
                db.wishlistDao().deleteByProductId(productId)
                val wishlistId = db.wishlistDao().insert(Wishlist(productId = productId))

                var product = db.productDao().getProductById(productId)
                if (product == null) {
                    product = getFallbackProduct(productId)
                }
                FirestoreSyncManager.uploadWishlistItem(
                    applicationContext,
                    Wishlist(id = wishlistId, productId = productId),
                    product
                )

                withContext(Dispatchers.Main) {
                    val isOnline = NetworkUtils.isNetworkAvailable(applicationContext)
                    val msg = if (isOnline) "Added to Wishlist" else "Added to Wishlist (Offline Mode - Queued for Sync)"
                    Toast.makeText(this@ProductDetailActivity, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun placeOrder(productId: Long, price: Double) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val order = Order(
                productId = productId,
                quantity = 1,
                totalPrice = price,
                orderDate = currentDate
            )
            val insertedOrderId = db.orderDao().insert(order)

            var product = db.productDao().getProductById(productId)
            if (product == null) {
                product = getFallbackProduct(productId)
            }
            FirestoreSyncManager.uploadOrder(
                applicationContext,
                order.copy(id = insertedOrderId),
                product
            )

            withContext(Dispatchers.Main) {
                val isOnline = NetworkUtils.isNetworkAvailable(applicationContext)
                val msg = if (isOnline) "Order Placed!" else "Order Placed! (Offline Mode - Queued for Sync)"
                Toast.makeText(this@ProductDetailActivity, msg, Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@ProductDetailActivity, OrdersActivity::class.java))
            }
        }
    }
}