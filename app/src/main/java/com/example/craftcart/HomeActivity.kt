package com.example.craftcart

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.craftcart.data.entity.Product
import com.example.craftcart.data.sync.FirestoreSyncManager
import com.example.craftcart.ml.ProductRecommendationEngine
import com.example.craftcart.notification.SmartNotificationHelper
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            triggerPersonalizedAlertIfAppropriate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize notification channel for Practical 10
        SmartNotificationHelper.initNotificationChannel(this)

        // Request POST_NOTIFICATIONS on Android 13+ if needed
        checkAndRequestNotificationPermission()

        // Start Firestore -> Room realtime synchronization
        FirestoreSyncManager.startRealtimeSync(applicationContext)

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

        // Default click handlers for cards
        findViewById<View>(R.id.clayHouseCard)?.setOnClickListener {
            openDetail(7L)
        }

        findViewById<View>(R.id.miffyCard)?.setOnClickListener {
            openDetail(8L)
        }

        findViewById<View>(R.id.flowerCard)?.setOnClickListener {
            openDetail(9L)
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

    override fun onResume() {
        super.onResume()
        // Refresh ML-based recommendations on return
        loadPersonalizedRecommendations()
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val currentPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            if (currentPerm != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                triggerPersonalizedAlertIfAppropriate()
            }
        } else {
            triggerPersonalizedAlertIfAppropriate()
        }
    }

    private fun loadPersonalizedRecommendations() {
        lifecycleScope.launch {
            try {
                val recommendations = ProductRecommendationEngine.getPersonalizedRecommendations(
                    context = applicationContext,
                    limit = 3
                )
                if (recommendations.isNotEmpty()) {
                    bindRecommendationCards(recommendations)
                    triggerPersonalizedAlertIfAppropriate()
                }
            } catch (e: Exception) {
                // If anything fails, UI retains default layout cards smoothly
            }
        }
    }

    private fun bindRecommendationCards(items: List<Product>) {
        if (items.isNotEmpty()) {
            val p1 = items[0]
            val card1 = findViewById<View>(R.id.clayHouseCard)
            findViewById<TextView>(R.id.clayHouseTitle)?.text = p1.name
            findViewById<TextView>(R.id.clayHousePrice)?.text = "₹" + String.format(java.util.Locale.US, "%.0f", p1.price)
            if (p1.imageResId != 0) {
                findViewById<ImageView>(R.id.clayHouseImage)?.setImageResource(p1.imageResId)
            }
            card1?.setOnClickListener { openDetail(p1.id) }
        }

        if (items.size > 1) {
            val p2 = items[1]
            val card2 = findViewById<View>(R.id.miffyCard)
            findViewById<TextView>(R.id.miffyTitle)?.text = p2.name
            findViewById<TextView>(R.id.miffyPrice)?.text = "₹" + String.format(java.util.Locale.US, "%.0f", p2.price)
            if (p2.imageResId != 0) {
                findViewById<ImageView>(R.id.miffyImage)?.setImageResource(p2.imageResId)
            }
            card2?.setOnClickListener { openDetail(p2.id) }
        }

        if (items.size > 2) {
            val p3 = items[2]
            val card3 = findViewById<View>(R.id.flowerCard)
            findViewById<TextView>(R.id.flowerPlantersTitle)?.text = p3.name
            findViewById<TextView>(R.id.flowerPlantersPrice)?.text = "₹" + String.format(java.util.Locale.US, "%.0f", p3.price)
            if (p3.imageResId != 0) {
                findViewById<ImageView>(R.id.flowerPlantersImage)?.setImageResource(p3.imageResId)
            }
            card3?.setOnClickListener { openDetail(p3.id) }
        }
    }

    private fun triggerPersonalizedAlertIfAppropriate() {
        lifecycleScope.launch {
            try {
                val recommendations = ProductRecommendationEngine.getPersonalizedRecommendations(
                    context = applicationContext,
                    limit = 1
                )
                if (recommendations.isNotEmpty()) {
                    val topPick = recommendations[0]
                    SmartNotificationHelper.sendPersonalizedRecommendationAlert(
                        context = applicationContext,
                        product = topPick,
                        reason = "Recommended based on your recent activity"
                    )
                }
            } catch (e: Exception) {
                // Suppress background notification failures safely
            }
        }
    }

    private fun openDetail(productId: Long) {
        val intent = Intent(this, ProductDetailActivity::class.java).apply {
            putExtra("PRODUCT_ID", productId)
        }
        startActivity(intent)
    }
}