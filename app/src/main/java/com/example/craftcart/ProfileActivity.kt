package com.example.craftcart

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

import com.example.craftcart.data.sync.FirestoreSyncManager
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {

    private lateinit var profileImageView: ImageView

    private val pickMediaLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if persistable permission is not supported for this Uri
            }
            profileImageView.setImageURI(uri)
            getSharedPreferences("craftcart_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("profile_image_uri", uri.toString())
                .apply()
            Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        profileImageView = findViewById(R.id.profileImageView)

        // Restore saved profile picture URI if present
        val savedUriString = getSharedPreferences("craftcart_prefs", Context.MODE_PRIVATE)
            .getString("profile_image_uri", null)
        if (savedUriString != null) {
            try {
                profileImageView.setImageURI(Uri.parse(savedUriString))
            } catch (e: Exception) {
                // Fallback to default
            }
        }

        val editProfileClick = View.OnClickListener {
            pickMediaLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        profileImageView.setOnClickListener(editProfileClick)
        findViewById<Button>(R.id.btnEditProfile)?.setOnClickListener(editProfileClick)

        // Profile Option Rows
        findViewById<View>(R.id.ordersRow)?.setOnClickListener {
            val intent = Intent(this, OrdersActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)
        }

        findViewById<View>(R.id.wishlistRow)?.setOnClickListener {
            val intent = Intent(this, WishlistActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)
        }

        findViewById<View>(R.id.campusNavRow)?.setOnClickListener {
            val intent = Intent(this, CampusMapActivity::class.java)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.logoutButton)?.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            FirestoreSyncManager.stopRealtimeSync()
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
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
            val intent = Intent(this, WishlistActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)
        }

        findViewById<View>(R.id.navProfile)?.setOnClickListener {
            // Stay on ProfileActivity
        }
    }
}