package com.example.craftcart

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.craftcart.data.AppDatabase
import com.example.craftcart.data.entity.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductListActivity : AppCompatActivity() {

    private var currentCategoryName: String = "Paintings"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_list)

        currentCategoryName = intent.getStringExtra("CATEGORY_NAME") ?: "Paintings"
        findViewById<TextView>(R.id.pageTitle)?.text = currentCategoryName

        // Top Bar
        findViewById<TextView>(R.id.backButton)?.setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.cartButton)?.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
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

        loadProductsForCategory(currentCategoryName)
    }

    override fun onResume() {
        super.onResume()
        loadProductsForCategory(currentCategoryName)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentCategoryName = intent.getStringExtra("CATEGORY_NAME") ?: "Paintings"
        findViewById<TextView>(R.id.pageTitle)?.text = currentCategoryName
        loadProductsForCategory(currentCategoryName)
    }

    private fun loadProductsForCategory(categoryName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            var allProducts = db.productDao().getAllProducts()
            if (allProducts.isEmpty()) {
                allProducts = getFallbackProducts()
            }

            val filtered = allProducts.filter { matchesCategory(it, categoryName) }

            withContext(Dispatchers.Main) {
                bindProductList(filtered)
            }
        }
    }

    private fun bindProductList(products: List<Product>) {
        val emptyStateText = findViewById<TextView>(R.id.emptyStateText)
        if (products.isEmpty()) {
            emptyStateText?.visibility = View.VISIBLE
        } else {
            emptyStateText?.visibility = View.GONE
        }

        val cardIds = listOf(R.id.productCard1, R.id.productCard2, R.id.productCard3, R.id.productCard4, R.id.productCard5, R.id.productCard6)
        val nameIds = listOf(R.id.productName1, R.id.productName2, R.id.productName3, R.id.productName4, R.id.productName5, R.id.productName6)
        val catIds = listOf(R.id.productCategory1, R.id.productCategory2, R.id.productCategory3, R.id.productCategory4, R.id.productCategory5, R.id.productCategory6)
        val priceIds = listOf(R.id.productPrice1, R.id.productPrice2, R.id.productPrice3, R.id.productPrice4, R.id.productPrice5, R.id.productPrice6)
        val imageIds = listOf(R.id.productImage1, R.id.productImage2, R.id.productImage3, R.id.productImage4, R.id.productImage5, R.id.productImage6)

        for (i in 0 until 6) {
            val cardView = findViewById<View>(cardIds[i]) ?: continue
            if (i < products.size) {
                val item = products[i]
                cardView.visibility = View.VISIBLE
                findViewById<TextView>(nameIds[i])?.text = item.name
                findViewById<TextView>(catIds[i])?.text = item.category
                findViewById<TextView>(priceIds[i])?.text = "₹${item.price.toInt()}"

                val imageRes = getDrawableResId(item)
                findViewById<ImageView>(imageIds[i])?.setImageResource(imageRes)

                cardView.setOnClickListener { openDetail(item.id) }
            } else {
                cardView.visibility = View.GONE
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

    private fun matchesCategory(product: Product, category: String): Boolean {
        val pCat = product.category.lowercase()
        val selCat = category.lowercase()

        return when {
            selCat.contains("paint") -> pCat.contains("paint") || pCat.contains("canvas")
            selCat.contains("keychain") -> pCat.contains("keychain")
            selCat.contains("home") || selCat.contains("decor") -> pCat.contains("decor") || pCat.contains("home")
            selCat.contains("crochet") || selCat.contains("knit") -> pCat.contains("crochet") || pCat.contains("planter")
            selCat.contains("gift") || selCat.contains("handmade") -> (pCat.contains("handmade") || pCat.contains("seasonal") || pCat.contains("gift")) && !pCat.contains("canvas")
            selCat.contains("letter") -> pCat.contains("letter")
            else -> pCat.contains(selCat) || selCat.contains(pCat)
        }
    }

    private fun getFallbackProducts(): List<Product> {
        return listOf(
            Product(id = 1, name = "Puppy Portrait", price = 499.0, category = "Canvas Painting", imageResId = R.drawable.pup_painting),
            Product(id = 2, name = "Puppy Dreams", price = 499.0, category = "Canvas Painting", imageResId = R.drawable.puppy_painting),
            Product(id = 3, name = "Tasty Little Things", price = 449.0, category = "Canvas Painting", imageResId = R.drawable.tasty_painting),
            Product(id = 4, name = "Cute Christmas", price = 399.0, category = "Seasonal Art", imageResId = R.drawable.cute_christmas),
            Product(id = 5, name = "Meow Christmas", price = 449.0, category = "Seasonal Art", imageResId = R.drawable.meow_christmas),
            Product(id = 6, name = "Garden Portrait", price = 499.0, category = "Handmade Art", imageResId = R.drawable.pup_painting),
            Product(id = 7, name = "Clay House", price = 599.0, category = "Home Decor", imageResId = R.drawable.clay_house),
            Product(id = 8, name = "Miffy Keychain", price = 349.0, category = "Keychains", imageResId = R.drawable.miffy_keychai),
            Product(id = 9, name = "Flower Planters", price = 449.0, category = "Planters", imageResId = R.drawable.flower_planters)
        )
    }

    private fun openDetail(productId: Long) {
        val intent = Intent(this, ProductDetailActivity::class.java).apply {
            putExtra("PRODUCT_ID", productId)
        }
        startActivity(intent)
    }
}