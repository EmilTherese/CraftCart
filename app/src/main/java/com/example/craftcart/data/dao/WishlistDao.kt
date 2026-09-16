package com.example.craftcart.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.craftcart.data.entity.Wishlist

@Dao
interface WishlistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(wishlist: Wishlist): Long

    @Delete
    fun delete(wishlist: Wishlist): Int

    @Query("DELETE FROM wishlist_items WHERE productId = :productId")
    fun deleteByProductId(productId: Long): Int

    @Query("SELECT * FROM wishlist_items")
    fun getAllWishlistItems(): List<Wishlist>

    @Query("SELECT * FROM wishlist_items WHERE productId = :productId LIMIT 1")
    fun getWishlistItemByProductId(productId: Long): Wishlist?
}
