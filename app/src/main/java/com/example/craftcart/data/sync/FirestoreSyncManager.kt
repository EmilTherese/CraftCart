package com.example.craftcart.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.util.Log
import com.example.craftcart.data.AppDatabase
import com.example.craftcart.data.entity.Cart
import com.example.craftcart.data.entity.Order
import com.example.craftcart.data.entity.Product
import com.example.craftcart.data.entity.User
import com.example.craftcart.data.entity.Wishlist
import com.example.craftcart.network.NetworkUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object FirestoreSyncManager {

    private const val TAG = "FirestoreSyncManager"
    private const val PREFS_QUEUE = "craftcart_sync_queue"
    private const val KEY_QUEUE = "pending_actions"

    @Volatile
    private var activeUid: String? = null

    @Volatile
    private var isNetworkCallbackRegistered = false

    private val recentCartDeletes = ConcurrentHashMap<Long, Long>()
    private val recentWishlistDeletes = ConcurrentHashMap<Long, Long>()

    private var usersListener: ListenerRegistration? = null
    private var productsListener: ListenerRegistration? = null
    private var cartListener: ListenerRegistration? = null
    private var wishlistListener: ListenerRegistration? = null
    private var ordersListener: ListenerRegistration? = null

    private val syncScope = CoroutineScope(Dispatchers.IO)

    fun getAuthenticatedUid(context: Context? = null): String {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && currentUser.uid.isNotEmpty()) {
            return currentUser.uid
        }
        if (context != null) {
            return context.getSharedPreferences("craftcart_prefs", Context.MODE_PRIVATE)
                .getString("auth_uid", "") ?: ""
        }
        return ""
    }

    @Synchronized
    fun startRealtimeSync(context: Context) {
        val appContext = context.applicationContext
        val currentUid = getAuthenticatedUid(appContext)

        // 1. Ensure network callback is registered to flush offline sync queue on reconnect
        if (!isNetworkCallbackRegistered && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            try {
                cm?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        Log.d(TAG, "[NETWORK] Connection restored. Triggering pending sync queue flush...")
                        syncPendingQueue(appContext)
                    }
                })
                isNetworkCallbackRegistered = true
                Log.d(TAG, "[NETWORK] Registered DefaultNetworkCallback for queue auto-flush.")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to register default network callback", e)
            }
        }

        // 2. Auth-aware check:
        // Do NOT mark sync as active if there is no authenticated Firebase UID.
        if (currentUid.isEmpty()) {
            Log.w(TAG, "[AUTH] No authenticated Firebase UID found. Realtime user listeners will NOT attach.")
            // If listeners were previously attached to an old session, detach them now.
            if (activeUid != null) {
                Log.d(TAG, "[AUTH] Detaching previous listeners because current UID is empty.")
                stopRealtimeSync()
            }
            return
        }

        // 3. If listeners are already attached for THIS EXACT UID, do not re-attach duplicates
        if (activeUid == currentUid) {
            Log.d(TAG, "[AUTH] Realtime sync is already active for UID=$currentUid. Checking pending queue...")
            syncPendingQueue(appContext)
            return
        }

        // 4. If listeners were attached for a different UID, detach them first
        if (activeUid != null && activeUid != currentUid) {
            Log.d(TAG, "[AUTH] UID changed from '$activeUid' to '$currentUid'. Re-binding listeners...")
            detachListenersOnly()
        }

        activeUid = currentUid
        Log.i(TAG, "[AUTH] Attaching realtime Firestore listeners for UID=$currentUid")

        val db = AppDatabase.getDatabase(appContext)
        val firestore = FirebaseFirestore.getInstance()

        // Flush any pending writes for this authenticated UID
        syncPendingQueue(appContext)

        // 1. Synchronize "users" -> User entity
        usersListener = firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "[USERS] Error in users snapshot listener: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                syncScope.launch {
                    try {
                        for (change in snapshot.documentChanges) {
                            val doc = change.document
                            if (doc.metadata.hasPendingWrites()) continue

                            val email = doc.getString("email") ?: ""
                            val name = doc.getString("name") ?: ""
                            if (email.isEmpty()) continue

                            when (change.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val existing = db.userDao().getUserByEmail(email)
                                    if (existing != null) {
                                        val updated = existing.copy(name = name)
                                        db.userDao().update(updated)
                                    } else {
                                        val user = User(name = name, email = email, password = "")
                                        db.userDao().insert(user)
                                    }
                                    Log.d(TAG, "[USERS] Synced user from Firestore to Room: email=$email, name=$name")
                                }
                                DocumentChange.Type.REMOVED -> {
                                    val existing = db.userDao().getUserByEmail(email)
                                    if (existing != null) {
                                        db.userDao().delete(existing)
                                        Log.d(TAG, "[USERS] Removed user from Room: email=$email")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "[USERS] Error processing users changes", e)
                    }
                }
            }

        // 2. Synchronize "products" -> Product entity
        productsListener = firestore.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "[PRODUCTS] Error in products snapshot listener: ${error.message}", error)
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
                                    val existing = db.productDao().getProductById(id)
                                    val imageRes = if (existing != null && existing.imageResId != 0) {
                                        existing.imageResId
                                    } else {
                                        resolveProductDrawable(id, name)
                                    }
                                    val product = Product(
                                        id = id,
                                        name = name,
                                        price = price,
                                        category = category,
                                        imageResId = imageRes
                                    )
                                    db.productDao().insert(product)
                                    Log.d(TAG, "[PRODUCTS] Synced product from Firestore to Room: id=$id, name=$name")
                                }
                                DocumentChange.Type.REMOVED -> {
                                    val existing = db.productDao().getProductById(id)
                                    if (existing != null) {
                                        db.productDao().delete(existing)
                                        Log.d(TAG, "[PRODUCTS] Removed product from Room: id=$id")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "[PRODUCTS] Error processing products changes", e)
                    }
                }
            }

        // 3. Synchronize "cart_items" -> Cart entity
        cartListener = firestore.collection("cart_items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "[CART] Error in cart_items snapshot listener: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                syncScope.launch {
                    try {
                        val sessionUid = activeUid ?: return@launch
                        for (change in snapshot.documentChanges) {
                            val doc = change.document

                            // Ignore local pending writes to prevent echo / race condition
                            if (doc.metadata.hasPendingWrites()) {
                                Log.d(TAG, "[CART] Ignoring echo of local pending write for cart document ${doc.id}")
                                continue
                            }

                            // ISOLATE LEGACY & UNMATCHED DOCUMENTS:
                            // Must have a non-null, non-empty userId that strictly equals active sessionUid
                            val docUserId = doc.getString("userId") ?: doc.getString("uid")
                            if (docUserId.isNullOrEmpty() || docUserId != sessionUid) {
                                Log.d(TAG, "[CART] Ignoring unowned or legacy cart document: ${doc.id} (docUserId=$docUserId, activeUid=$sessionUid)")
                                continue
                            }

                            val productId = doc.getLong("productId")
                                ?: doc.id.substringAfterLast("_").toLongOrNull()
                                ?: continue

                            // Tombstone check: do not resurrect an item deleted locally within 60s
                            val deleteTimestamp = recentCartDeletes[productId]
                            if (deleteTimestamp != null && (System.currentTimeMillis() - deleteTimestamp < 60_000L)) {
                                Log.d(TAG, "[CART] Ignoring stale snapshot for locally deleted cart item productId=$productId")
                                continue
                            }

                            when (change.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val quantity = (doc.getLong("quantity") ?: 1L).toInt()
                                    val existingCartItem = db.cartDao().getCartItemByProductId(productId)
                                    val id = existingCartItem?.id ?: 0L
                                    db.cartDao().deleteByProductId(productId)
                                    val cart = Cart(id = id, productId = productId, quantity = quantity)
                                    db.cartDao().insert(cart)
                                    Log.d(TAG, "[CART] Synced cart item from Firestore to Room: productId=$productId, qty=$quantity (uid=$sessionUid)")
                                }
                                DocumentChange.Type.REMOVED -> {
                                    db.cartDao().deleteByProductId(productId)
                                    recentCartDeletes.remove(productId)
                                    Log.d(TAG, "[CART] Removed cart item from Room: productId=$productId (uid=$sessionUid)")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "[CART] Error processing cart changes", e)
                    }
                }
            }

        // 4. Synchronize "wishlist_items" -> Wishlist entity
        wishlistListener = firestore.collection("wishlist_items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "[WISHLIST] Error in wishlist_items snapshot listener: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                syncScope.launch {
                    try {
                        val sessionUid = activeUid ?: return@launch
                        for (change in snapshot.documentChanges) {
                            val doc = change.document

                            if (doc.metadata.hasPendingWrites()) {
                                Log.d(TAG, "[WISHLIST] Ignoring echo of local pending write for wishlist document ${doc.id}")
                                continue
                            }

                            // ISOLATE LEGACY & UNMATCHED DOCUMENTS:
                            // Must have a non-null, non-empty userId that strictly equals active sessionUid
                            val docUserId = doc.getString("userId") ?: doc.getString("uid")
                            if (docUserId.isNullOrEmpty() || docUserId != sessionUid) {
                                Log.d(TAG, "[WISHLIST] Ignoring unowned or legacy wishlist document: ${doc.id} (docUserId=$docUserId, activeUid=$sessionUid)")
                                continue
                            }

                            val productId = doc.getLong("productId")
                                ?: doc.id.substringAfterLast("_").toLongOrNull()
                                ?: continue

                            val deleteTimestamp = recentWishlistDeletes[productId]
                            if (deleteTimestamp != null && (System.currentTimeMillis() - deleteTimestamp < 60_000L)) {
                                Log.d(TAG, "[WISHLIST] Ignoring stale snapshot for locally deleted wishlist item productId=$productId")
                                continue
                            }

                            when (change.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val existingWishlistItem = db.wishlistDao().getWishlistItemByProductId(productId)
                                    val id = existingWishlistItem?.id ?: 0L
                                    db.wishlistDao().deleteByProductId(productId)
                                    val wishlist = Wishlist(id = id, productId = productId)
                                    db.wishlistDao().insert(wishlist)
                                    Log.d(TAG, "[WISHLIST] Synced wishlist item from Firestore to Room: productId=$productId (uid=$sessionUid)")
                                }
                                DocumentChange.Type.REMOVED -> {
                                    db.wishlistDao().deleteByProductId(productId)
                                    recentWishlistDeletes.remove(productId)
                                    Log.d(TAG, "[WISHLIST] Removed wishlist item from Room: productId=$productId (uid=$sessionUid)")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "[WISHLIST] Error processing wishlist changes", e)
                    }
                }
            }

        // 5. Synchronize "orders" -> Order entity
        ordersListener = firestore.collection("orders")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "[ORDERS] Error in orders snapshot listener: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                syncScope.launch {
                    try {
                        val sessionUid = activeUid ?: return@launch
                        for (change in snapshot.documentChanges) {
                            val doc = change.document

                            if (doc.metadata.hasPendingWrites()) {
                                Log.d(TAG, "[ORDERS] Ignoring echo of local pending write for order document ${doc.id}")
                                continue
                            }

                            // ISOLATE LEGACY & UNMATCHED DOCUMENTS:
                            // Must have a non-null, non-empty userId that strictly equals active sessionUid
                            val docUserId = doc.getString("userId") ?: doc.getString("uid")
                            if (docUserId.isNullOrEmpty() || docUserId != sessionUid) {
                                Log.d(TAG, "[ORDERS] Ignoring unowned or legacy order document: ${doc.id} (docUserId=$docUserId, activeUid=$sessionUid)")
                                continue
                            }

                            val id = doc.getLong("id")
                                ?: doc.id.substringAfterLast("_").toLongOrNull()
                                ?: continue

                            when (change.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val productId = doc.getLong("productId") ?: 0L
                                    val quantity = (doc.getLong("quantity") ?: 1L).toInt()
                                    val totalPrice = doc.getDouble("totalPrice") ?: 0.0
                                    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                    val orderDate = doc.getString("orderDate") ?: currentDate
                                    val order = Order(
                                        id = id,
                                        productId = productId,
                                        quantity = quantity,
                                        totalPrice = totalPrice,
                                        orderDate = orderDate
                                    )
                                    db.orderDao().insert(order)
                                    Log.d(TAG, "[ORDERS] Synced order from Firestore to Room: orderId=$id, productId=$productId (uid=$sessionUid)")
                                }
                                DocumentChange.Type.REMOVED -> {
                                    val existing = db.orderDao().getOrderById(id)
                                    if (existing != null) {
                                        db.orderDao().delete(existing)
                                        Log.d(TAG, "[ORDERS] Removed order from Room: orderId=$id (uid=$sessionUid)")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "[ORDERS] Error processing orders changes", e)
                    }
                }
            }
    }

    private fun detachListenersOnly() {
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
        Log.d(TAG, "[LISTENERS] Detached existing Firestore listeners.")
    }

    @Synchronized
    fun stopRealtimeSync() {
        detachListenersOnly()
        val oldUid = activeUid
        activeUid = null
        Log.i(TAG, "[AUTH] Stopped Firestore -> Room realtime sync for UID=$oldUid")
    }

    // ==========================================
    // ROOM -> FIRESTORE SYNCHRONIZATION METHODS
    // ==========================================

    fun uploadCartItem(context: Context, cart: Cart, product: Product) {
        val uid = getAuthenticatedUid(context)
        if (uid.isEmpty()) {
            Log.w(TAG, "[CART] Cannot upload cart item: No authenticated UID.")
            return
        }
        val docId = "${uid}_${cart.productId}"

        recentCartDeletes.remove(cart.productId)

        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "productId" to cart.productId,
            "productName" to product.name,
            "category" to product.category,
            "price" to product.price,
            "quantity" to cart.quantity,
            "updatedAt" to System.currentTimeMillis()
        )

        val isOnline = NetworkUtils.isNetworkAvailable(context)
        if (!isOnline) {
            Log.i(TAG, "[OFFLINE] Device offline. Enqueuing cart upload for doc: $docId (uid=$uid)")
            enqueueAction(context, uid, "SET", "cart_items", docId, payload)
            return
        }

        FirebaseFirestore.getInstance()
            .collection("cart_items")
            .document(docId)
            .set(payload)
            .addOnSuccessListener {
                Log.d(TAG, "[SUCCESS] Uploaded cart item to cart_items/$docId (qty=${cart.quantity}, uid=$uid)")
                dequeueAction(context, uid, docId)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "[FAILURE] Failed to upload cart item cart_items/$docId: ${e.message}", e)
                enqueueAction(context, uid, "SET", "cart_items", docId, payload)
            }
    }

    fun deleteCartItem(context: Context, productId: Long) {
        val uid = getAuthenticatedUid(context)
        if (uid.isEmpty()) {
            Log.w(TAG, "[CART] Cannot delete cart item: No authenticated UID.")
            return
        }
        val docId = "${uid}_$productId"

        recentCartDeletes[productId] = System.currentTimeMillis()

        val isOnline = NetworkUtils.isNetworkAvailable(context)
        if (!isOnline) {
            Log.i(TAG, "[OFFLINE] Device offline. Enqueuing cart delete for doc: $docId (uid=$uid)")
            enqueueAction(context, uid, "DELETE", "cart_items", docId, emptyMap())
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("cart_items")
            .document(docId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "[SUCCESS] Deleted cart item cart_items/$docId (uid=$uid)")
                dequeueAction(context, uid, docId)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "[FAILURE] Failed to delete cart item cart_items/$docId: ${e.message}", e)
                enqueueAction(context, uid, "DELETE", "cart_items", docId, emptyMap())
            }
    }

    fun uploadWishlistItem(context: Context, wishlist: Wishlist, product: Product) {
        val uid = getAuthenticatedUid(context)
        if (uid.isEmpty()) {
            Log.w(TAG, "[WISHLIST] Cannot upload wishlist item: No authenticated UID.")
            return
        }
        val docId = "${uid}_${wishlist.productId}"

        recentWishlistDeletes.remove(wishlist.productId)

        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "productId" to wishlist.productId,
            "productName" to product.name,
            "category" to product.category,
            "price" to product.price,
            "updatedAt" to System.currentTimeMillis()
        )

        val isOnline = NetworkUtils.isNetworkAvailable(context)
        if (!isOnline) {
            Log.i(TAG, "[OFFLINE] Device offline. Enqueuing wishlist upload for doc: $docId (uid=$uid)")
            enqueueAction(context, uid, "SET", "wishlist_items", docId, payload)
            return
        }

        FirebaseFirestore.getInstance()
            .collection("wishlist_items")
            .document(docId)
            .set(payload)
            .addOnSuccessListener {
                Log.d(TAG, "[SUCCESS] Uploaded wishlist item to wishlist_items/$docId (uid=$uid)")
                dequeueAction(context, uid, docId)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "[FAILURE] Failed to upload wishlist item wishlist_items/$docId: ${e.message}", e)
                enqueueAction(context, uid, "SET", "wishlist_items", docId, payload)
            }
    }

    fun deleteWishlistItem(context: Context, productId: Long) {
        val uid = getAuthenticatedUid(context)
        if (uid.isEmpty()) {
            Log.w(TAG, "[WISHLIST] Cannot delete wishlist item: No authenticated UID.")
            return
        }
        val docId = "${uid}_$productId"

        recentWishlistDeletes[productId] = System.currentTimeMillis()

        val isOnline = NetworkUtils.isNetworkAvailable(context)
        if (!isOnline) {
            Log.i(TAG, "[OFFLINE] Device offline. Enqueuing wishlist delete for doc: $docId (uid=$uid)")
            enqueueAction(context, uid, "DELETE", "wishlist_items", docId, emptyMap())
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("wishlist_items")
            .document(docId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "[SUCCESS] Deleted wishlist item wishlist_items/$docId (uid=$uid)")
                dequeueAction(context, uid, docId)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "[FAILURE] Failed to delete wishlist item wishlist_items/$docId: ${e.message}", e)
                enqueueAction(context, uid, "DELETE", "wishlist_items", docId, emptyMap())
            }
    }

    fun uploadOrder(context: Context, order: Order, product: Product) {
        val uid = getAuthenticatedUid(context)
        if (uid.isEmpty()) {
            Log.w(TAG, "[ORDERS] Cannot upload order: No authenticated UID.")
            return
        }
        val docId = "${uid}_${order.id}"

        val payload = hashMapOf<String, Any>(
            "id" to order.id,
            "userId" to uid,
            "productId" to order.productId,
            "productName" to product.name,
            "quantity" to order.quantity,
            "totalPrice" to order.totalPrice,
            "orderDate" to order.orderDate,
            "updatedAt" to System.currentTimeMillis()
        )

        val isOnline = NetworkUtils.isNetworkAvailable(context)
        if (!isOnline) {
            Log.i(TAG, "[OFFLINE] Device offline. Enqueuing order upload for doc: $docId (uid=$uid)")
            enqueueAction(context, uid, "SET", "orders", docId, payload)
            return
        }

        FirebaseFirestore.getInstance()
            .collection("orders")
            .document(docId)
            .set(payload)
            .addOnSuccessListener {
                Log.d(TAG, "[SUCCESS] Uploaded order to orders/$docId (total=₹${order.totalPrice.toInt()}, uid=$uid)")
                dequeueAction(context, uid, docId)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "[FAILURE] Failed to upload order orders/$docId: ${e.message}", e)
                enqueueAction(context, uid, "SET", "orders", docId, payload)
            }
    }

    fun uploadUser(context: Context, user: User, uid: String) {
        if (uid.isEmpty()) {
            Log.w(TAG, "[USERS] Cannot upload user profile: Empty UID.")
            return
        }

        val payload = hashMapOf<String, Any>(
            "uid" to uid,
            "id" to user.id,
            "name" to user.name,
            "email" to user.email,
            "updatedAt" to System.currentTimeMillis()
        )

        val isOnline = NetworkUtils.isNetworkAvailable(context)
        if (!isOnline) {
            Log.i(TAG, "[OFFLINE] Device offline. Enqueuing user upload for doc: $uid")
            enqueueAction(context, uid, "SET", "users", uid, payload)
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(payload)
            .addOnSuccessListener {
                Log.d(TAG, "[SUCCESS] Uploaded user profile to users/$uid")
                dequeueAction(context, uid, uid)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "[FAILURE] Failed to upload user users/$uid: ${e.message}", e)
                enqueueAction(context, uid, "SET", "users", uid, payload)
            }
    }

    // ==========================================
    // PERSISTENT RETRY QUEUE MANAGEMENT
    // ==========================================

    @Synchronized
    private fun enqueueAction(
        context: Context,
        uid: String,
        actionType: String,
        collection: String,
        docId: String,
        payload: Map<String, Any>
    ) {
        try {
            val prefs = context.getSharedPreferences(PREFS_QUEUE, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_QUEUE, "[]") ?: "[]"
            val existingArray = JSONArray(raw)
            val newArray = JSONArray()

            // Remove any existing action for the same collection and docId to stay deduplicated
            for (i in 0 until existingArray.length()) {
                val item = existingArray.getJSONObject(i)
                if (!(item.optString("collection") == collection && item.optString("docId") == docId)) {
                    newArray.put(item)
                }
            }

            val actionObj = JSONObject().apply {
                put("uid", uid)
                put("actionType", actionType)
                put("collection", collection)
                put("docId", docId)
                put("payload", JSONObject(payload))
                put("timestamp", System.currentTimeMillis())
            }
            newArray.put(actionObj)

            prefs.edit().putString(KEY_QUEUE, newArray.toString()).apply()
            Log.i(TAG, "[QUEUE] Enqueued $actionType for $collection/$docId (uid=$uid). Total pending: ${newArray.length()}")
        } catch (e: Exception) {
            Log.e(TAG, "Error enqueuing sync action", e)
        }
    }

    @Synchronized
    private fun dequeueAction(context: Context, uid: String, docId: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS_QUEUE, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_QUEUE, "[]") ?: "[]"
            val existingArray = JSONArray(raw)
            var removed = false
            val newArray = JSONArray()
            for (i in 0 until existingArray.length()) {
                val item = existingArray.getJSONObject(i)
                val itemUid = item.optString("uid")
                val itemDocId = item.optString("docId")
                if (itemDocId == docId && (itemUid.isEmpty() || itemUid == uid)) {
                    removed = true
                } else {
                    newArray.put(item)
                }
            }
            if (removed) {
                prefs.edit().putString(KEY_QUEUE, newArray.toString()).apply()
                Log.d(TAG, "[QUEUE] Dequeued action for $docId (uid=$uid). Remaining pending: ${newArray.length()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error dequeuing sync action", e)
        }
    }

    @Synchronized
    fun syncPendingQueue(context: Context) {
        val currentUid = getAuthenticatedUid(context)
        if (currentUid.isEmpty()) {
            Log.d(TAG, "[QUEUE_SYNC] Skipping queue flush: No authenticated UID.")
            return
        }

        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.d(TAG, "[QUEUE_SYNC] Cannot flush queue: device is offline.")
            return
        }

        val prefs = context.getSharedPreferences(PREFS_QUEUE, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_QUEUE, "[]") ?: "[]"
        val array = try {
            JSONArray(raw)
        } catch (e: Exception) {
            return
        }

        if (array.length() == 0) return

        Log.i(TAG, "[QUEUE_SYNC] Evaluating ${array.length()} pending queue actions for UID=$currentUid...")
        val firestore = FirebaseFirestore.getInstance()

        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val itemUid = item.optString("uid")

            // Strict safety: Only flush queued items that match current authenticated user
            if (itemUid.isNotEmpty() && itemUid != currentUid) {
                Log.d(TAG, "[QUEUE_SYNC] Skipping queued action for different UID ($itemUid vs active $currentUid)")
                continue
            }

            val actionType = item.optString("actionType")
            val collection = item.optString("collection")
            val docId = item.optString("docId")
            val payloadJson = item.optJSONObject("payload") ?: JSONObject()

            if (actionType == "SET") {
                val map = jsonToMap(payloadJson)
                firestore.collection(collection).document(docId)
                    .set(map)
                    .addOnSuccessListener {
                        Log.d(TAG, "[QUEUE_SYNC] SUCCESS: Flushed $actionType to $collection/$docId (uid=$currentUid)")
                        dequeueAction(context, currentUid, docId)
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "[QUEUE_SYNC] RETRY FAILED for $collection/$docId: ${e.message}")
                    }
            } else if (actionType == "DELETE") {
                firestore.collection(collection).document(docId)
                    .delete()
                    .addOnSuccessListener {
                        Log.d(TAG, "[QUEUE_SYNC] SUCCESS: Flushed $actionType to $collection/$docId (uid=$currentUid)")
                        dequeueAction(context, currentUid, docId)
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "[QUEUE_SYNC] RETRY FAILED for $collection/$docId: ${e.message}")
                    }
            }
        }
    }

    private fun jsonToMap(json: JSONObject): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = json.get(key)
            if (value is JSONObject) {
                map[key] = jsonToMap(value)
            } else if (value != JSONObject.NULL) {
                map[key] = value
            }
        }
        return map
    }

    private fun resolveProductDrawable(id: Long, name: String): Int {
        val nameLower = name.lowercase().trim()
        return when {
            nameLower.contains("puppy portrait") || id == 1L -> com.example.craftcart.R.drawable.pup_painting
            nameLower.contains("puppy dreams") || id == 2L -> com.example.craftcart.R.drawable.puppy_painting
            nameLower.contains("tasty") || id == 3L -> com.example.craftcart.R.drawable.tasty_painting
            nameLower.contains("cute christmas") || id == 4L -> com.example.craftcart.R.drawable.cute_christmas
            nameLower.contains("meow christmas") || id == 5L -> com.example.craftcart.R.drawable.meow_christmas
            nameLower.contains("garden portrait") || id == 6L -> com.example.craftcart.R.drawable.pup_painting
            nameLower.contains("clay house") || id == 7L -> com.example.craftcart.R.drawable.clay_house
            nameLower.contains("miffy") || id == 8L -> com.example.craftcart.R.drawable.miffy_keychai
            nameLower.contains("flower") || nameLower.contains("planter") || id == 9L -> com.example.craftcart.R.drawable.flower_planters
            else -> com.example.craftcart.R.drawable.pup_painting
        }
    }
}
