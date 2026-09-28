package com.example.craftcart.data.sync

import android.content.Context
import android.util.Log
import com.example.craftcart.data.AppDatabase
import com.example.craftcart.data.entity.Cart
import com.example.craftcart.data.entity.Order
import com.example.craftcart.data.entity.Product
import com.example.craftcart.data.entity.User
import com.example.craftcart.data.entity.Wishlist
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FirestoreSyncManager {

    private const val TAG = "FirestoreSyncManager"

    @Volatile
    private var isSyncing = false

    private var usersListener: ListenerRegistration? = null
    private var productsListener: ListenerRegistration? = null
    private var cartListener: ListenerRegistration? = null
    private var wishlistListener: ListenerRegistration? = null
    private var ordersListener: ListenerRegistration? = null

    private val syncScope = CoroutineScope(Dispatchers.IO)

    @Synchronized
    fun startRealtimeSync(context: Context) {
        if (isSyncing) {
            Log.d(TAG, "Realtime sync is already active.")
            return
        }

        val appContext = context.applicationContext
        val db = AppDatabase.getDatabase(appContext)
        val firestore = FirebaseFirestore.getInstance()

        isSyncing = true
        Log.d(TAG, "Starting Firestore -> Room realtime sync...")

        // 1. Synchronize "users" -> User entity
        usersListener = firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error in users snapshot listener", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                syncScope.launch {
                    try {
                        for (change in snapshot.documentChanges) {
                            val doc = change.document
                            val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: continue
                            when (change.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val name = doc.getString("name") ?: ""
                                    val email = doc.getString("email") ?: ""
                                    val user = User(id = id, name = name, email = email, password = "")
                                    db.userDao().insert(user)
                                }
                                DocumentChange.Type.REMOVED -> {
                                    val existing = db.userDao().getUserById(id)
                                    if (existing != null) {
                                        db.userDao().delete(existing)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing users changes", e)
                    }
                }
            }

        // 2. Synchronize "products" -> Product entity
        productsListener = firestore.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error in products snapshot listener", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                syncScope.launch {
                    try {
                        for (change in snapshot.documentChanges) {
                            val doc = change.document
                            val id = doc.getLong("productId") ?: doc.getLong("id") ?: doc.id.toLongOrNull() ?: continue
                            when (change.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val name = doc.getString("productName") ?: doc.getString("name") ?: ""
                                    val price = doc.getDouble("price") ?: 0.0
                                    val category = doc.getString("category") ?: ""
                                    val product = Product(id = id, name = name, price = price, category = category)
                                    db.productDao().insert(product)
                                }
                                DocumentChange.Type.REMOVED -> {
                                    val existing = db.productDao().getProductById(id)
                                    if (existing != null) {
                                        db.productDao().delete(existing)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing products changes", e)
                    }
                }
            }

        // 3. Synchronize "cart_items" -> Cart entity
        cartListener = firestore.collection("cart_items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error in cart_items snapshot listener", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                syncScope.launch {
                    try {
                        for (change in snapshot.documentChanges) {
                            val doc = change.document
                            val productId = doc.getLong("productId") ?: doc.id.toLongOrNull() ?: continue
                            when (change.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val quantity = (doc.getLong("quantity") ?: 1L).toInt()
                                    val existingCartItem = db.cartDao().getCartItemByProductId(productId)
                                    val id = existingCartItem?.id ?: doc.getLong("id") ?: productId
                                    val cart = Cart(id = id, productId = productId, quantity = quantity)
                                    db.cartDao().insert(cart)
                                }
                                DocumentChange.Type.REMOVED -> {
                                    val existing = db.cartDao().getCartItemByProductId(productId)
                                    if (existing != null) {
                                        db.cartDao().delete(existing)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing cart changes", e)
                    }
                }
            }

        // 4. Synchronize "wishlist_items" -> Wishlist entity
        wishlistListener = firestore.collection("wishlist_items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error in wishlist_items snapshot listener", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                syncScope.launch {
                    try {
                        for (change in snapshot.documentChanges) {
                            val doc = change.document
                            val productId = doc.getLong("productId") ?: doc.id.toLongOrNull() ?: continue
                            when (change.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val existingWishlistItem = db.wishlistDao().getWishlistItemByProductId(productId)
                                    val id = existingWishlistItem?.id ?: doc.getLong("id") ?: productId
                                    val wishlist = Wishlist(id = id, productId = productId)
                                    db.wishlistDao().insert(wishlist)
                                }
                                DocumentChange.Type.REMOVED -> {
                                    val existing = db.wishlistDao().getWishlistItemByProductId(productId)
                                    if (existing != null) {
                                        db.wishlistDao().delete(existing)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing wishlist changes", e)
                    }
                }
            }

        // 5. Synchronize "orders" -> Order entity
        ordersListener = firestore.collection("orders")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error in orders snapshot listener", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                syncScope.launch {
                    try {
                        for (change in snapshot.documentChanges) {
                            val doc = change.document
                            val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: continue
                            when (change.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val productId = doc.getLong("productId") ?: 0L
                                    val quantity = (doc.getLong("quantity") ?: 1L).toInt()
                                    val totalPrice = doc.getDouble("totalPrice") ?: 0.0
                                    val orderDate = doc.getString("orderDate") ?: "2026-09-16"
                                    val order = Order(
                                        id = id,
                                        productId = productId,
                                        quantity = quantity,
                                        totalPrice = totalPrice,
                                        orderDate = orderDate
                                    )
                                    db.orderDao().insert(order)
                                }
                                DocumentChange.Type.REMOVED -> {
                                    val existing = db.orderDao().getOrderById(id)
                                    if (existing != null) {
                                        db.orderDao().delete(existing)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing orders changes", e)
                    }
                }
            }
    }

    @Synchronized
    fun stopRealtimeSync() {
        usersListener?.remove()
        usersListener = null

        productsListener?.remove()
        productsListener = null

        cartListener?.remove()
        cartListener = null

        wishlistListener?.remove()
        wishlistListener = null

        ordersListener?.remove()
        ordersListener = null

        isSyncing = false
        Log.d(TAG, "Stopped Firestore -> Room realtime sync.")
    }
}
