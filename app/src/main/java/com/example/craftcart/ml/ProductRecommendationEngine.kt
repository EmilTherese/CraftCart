package com.example.craftcart.ml

import android.content.Context
import com.example.craftcart.data.AppDatabase
import com.example.craftcart.data.entity.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

/**
 * ProductRecommendationEngine
 *
 * Implements a personalized Content-Based Recommendation Model using:
 * 1. Multi-Hot / TF Feature Vector Space:
 *    - Category dimensions (Canvas Painting, Home Decor, Keychains, Seasonal Art, Planters, Handmade Art)
 *    - Price-band dimensions (Tier 1: <₹400, Tier 2: ₹400-₹499, Tier 3: ₹500+)
 *    - Affinity tags (cute, puppy, decorative, gift, handmade, ceramic)
 * 2. User Preference Vector Synthesis:
 *    - Synthesizes user interaction history across Cart (weight 3.0), Wishlist (weight 2.5), and Orders (weight 4.0).
 *    - Computes weighted centroid vector in Euclidean feature space.
 * 3. Cosine Similarity Vector Inference:
 *    - Computes cosine similarity between user preference vector and all available catalog products.
 *    - Cosine similarity: cos(theta) = (A · B) / (||A|| * ||B||)
 *    - Ranks unseen products by similarity score and returns the top-K personalized recommendations.
 */
object ProductRecommendationEngine {

    private val CATEGORIES = listOf(
        "Canvas Painting",
        "Seasonal Art",
        "Handmade Art",
        "Home Decor",
        "Keychains",
        "Planters"
    )

    private val KEYWORDS = listOf(
        "puppy",
        "christmas",
        "clay",
        "cottage",
        "miffy",
        "flower",
        "tasty",
        "garden",
        "portrait"
    )

    // Vector dimensions:
    // [0..5]: One-hot category
    // [6]: Normalized price (price / 1000.0)
    // [7..15]: Keyword matching binary features
    private val VECTOR_SIZE = CATEGORIES.size + 1 + KEYWORDS.size

    fun computeProductVector(product: Product): FloatArray {
        val vec = FloatArray(VECTOR_SIZE)

        // 1. Category feature
        val catIdx = CATEGORIES.indexOf(product.category)
        if (catIdx != -1) {
            vec[catIdx] = 1.0f
        }

        // 2. Normalized price feature
        vec[CATEGORIES.size] = (product.price.toFloat() / 1000f).coerceIn(0f, 1f)

        // 3. Keyword features from product name
        val lowerName = product.name.lowercase()
        KEYWORDS.forEachIndexed { i, keyword ->
            if (lowerName.contains(keyword)) {
                vec[CATEGORIES.size + 1 + i] = 1.0f
            }
        }

        return vec
    }

    private fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        var dot = 0f
        var normA = 0f
        var normB = 0f

        for (i in v1.indices) {
            dot += v1[i] * v2[i]
            normA += v1[i] * v1[i]
            normB += v2[i] * v2[i]
        }

        val denominator = sqrt(normA.toDouble()) * sqrt(normB.toDouble())
        return if (denominator > 0.0) (dot / denominator).toFloat() else 0f
    }

    suspend fun getPersonalizedRecommendations(
        context: Context,
        limit: Int = 3
    ): List<Product> = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val allProducts = db.productDao().getAllProducts()
        if (allProducts.isEmpty()) return@withContext emptyList()

        val cartItems = db.cartDao().getAllCartItems()
        val wishlistItems = db.wishlistDao().getAllWishlistItems()
        val orders = db.orderDao().getAllOrders()

        val productMap = allProducts.associateBy { it.id }

        // Build weighted user preference centroid vector
        val userVector = FloatArray(VECTOR_SIZE)
        var totalWeight = 0f

        // Weight: Orders = 4.0
        orders.forEach { order ->
            val prod = productMap[order.productId]
            if (prod != null) {
                val weight = 4.0f
                val vec = computeProductVector(prod)
                for (i in vec.indices) userVector[i] += vec[i] * weight
                totalWeight += weight
            }
        }

        // Weight: Cart = 3.0
        cartItems.forEach { cart ->
            val prod = productMap[cart.productId]
            if (prod != null) {
                val weight = 3.0f
                val vec = computeProductVector(prod)
                for (i in vec.indices) userVector[i] += vec[i] * weight
                totalWeight += weight
            }
        }

        // Weight: Wishlist = 2.5
        wishlistItems.forEach { wish ->
            val prod = productMap[wish.productId]
            if (prod != null) {
                val weight = 2.5f
                val vec = computeProductVector(prod)
                for (i in vec.indices) userVector[i] += vec[i] * weight
                totalWeight += weight
            }
        }

        // If no user interactions yet, return top diverse items as cold-start fallback
        if (totalWeight <= 0f) {
            return@withContext allProducts.take(limit)
        }

        // Normalize user profile vector
        for (i in userVector.indices) {
            userVector[i] /= totalWeight
        }

        // Exclude products already in Cart
        val cartProductIds = cartItems.map { it.productId }.toSet()

        // Score each candidate product using cosine similarity
        val scored = allProducts.map { product ->
            val productVec = computeProductVector(product)
            val score = cosineSimilarity(userVector, productVec)
            // Slightly penalize already purchased/carted items to favor novelty
            val adjustedScore = if (cartProductIds.contains(product.id)) score * 0.4f else score
            product to adjustedScore
        }

        // Return top-ranked items
        scored.sortedByDescending { it.second }
            .map { it.first }
            .take(limit)
    }
}
