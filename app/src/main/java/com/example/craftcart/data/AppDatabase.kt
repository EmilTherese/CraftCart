package com.example.craftcart.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.craftcart.data.dao.CartDao
import com.example.craftcart.data.dao.OrderDao
import com.example.craftcart.data.dao.ProductDao
import com.example.craftcart.data.dao.UserDao
import com.example.craftcart.data.dao.WishlistDao
import com.example.craftcart.data.entity.Cart
import com.example.craftcart.data.entity.Order
import com.example.craftcart.data.entity.Product
import com.example.craftcart.data.entity.User
import com.example.craftcart.data.entity.Wishlist

@Database(
    entities = [
        User::class,
        Product::class,
        Cart::class,
        Wishlist::class,
        Order::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun orderDao(): OrderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "craftcart_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
