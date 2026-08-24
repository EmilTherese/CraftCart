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
    suspend fun insert(wishlist: Wishlist): Long

    @Delete
    suspend fun delete(wishlist: Wishlist)

    @Query("DELETE FROM wishlist_items WHERE productId = :productId")
    suspend fun deleteByProductId(productId: Long)

    @Query("SELECT * FROM wishlist_items")
    suspend fun getAllWishlistItems(): List<Wishlist>

    @Query("SELECT * FROM wishlist_items WHERE productId = :productId LIMIT 1")
    suspend fun getWishlistItemByProductId(productId: Long): Wishlist?
}
