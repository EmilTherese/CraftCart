package com.example.craftcart.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wishlist_items")
data class Wishlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long
)
