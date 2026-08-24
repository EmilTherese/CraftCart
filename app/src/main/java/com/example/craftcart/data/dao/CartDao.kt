package com.example.craftcart.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.craftcart.data.entity.Cart

@Dao
interface CartDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cart: Cart): Long

    @Update
    suspend fun update(cart: Cart)

    @Delete
    suspend fun delete(cart: Cart)

    @Query("SELECT * FROM cart_items")
    suspend fun getAllCartItems(): List<Cart>

    @Query("SELECT * FROM cart_items WHERE productId = :productId LIMIT 1")
    suspend fun getCartItemByProductId(productId: Long): Cart?

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()
}
