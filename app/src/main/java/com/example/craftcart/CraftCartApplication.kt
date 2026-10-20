package com.example.craftcart

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

class CraftCartApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Firebase
        FirebaseApp.initializeApp(this)

        // 2. Initialize Firebase App Check with Debug provider on development/debug builds
        try {
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d("CraftCartAppCheck", "DebugAppCheckProviderFactory successfully installed.")

            // Log App Check token state when retrieved
            firebaseAppCheck.getAppCheckToken(false).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result?.token
                    Log.d("CraftCartAppCheck", "App Check token obtained: ${token?.take(10)}... (valid)")
                } else {
                    Log.w("CraftCartAppCheck", "App Check token retrieval notice: ${task.exception?.message}. Remember to register your Logcat debug secret in Firebase Console -> App Check -> Manage debug tokens.")
                }
            }
        } catch (e: Exception) {
            Log.e("CraftCartAppCheck", "Failed to initialize Firebase App Check: ${e.message}", e)
        }
    }
}
