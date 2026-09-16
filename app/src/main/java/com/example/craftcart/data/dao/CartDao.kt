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
    fun insert(cart: Cart): Long

    @Update
    fun update(cart: Cart): Int

    @Delete
    fun delete(cart: Cart): Int

    @Query("SELECT * FROM cart_items")
    fun getAllCartItems(): List<Cart>

    @Query("SELECT * FROM cart_items WHERE productId = :productId LIMIT 1")
    fun getCartItemByProductId(productId: Long): Cart?

    @Query("DELETE FROM cart_items")
    fun clearCart(): Int
}
