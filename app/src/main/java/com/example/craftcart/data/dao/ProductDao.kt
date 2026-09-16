package com.example.craftcart.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.craftcart.data.entity.Product

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(products: List<Product>): List<Long>

    @Update
    fun update(product: Product): Int

    @Delete
    fun delete(product: Product): Int

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    fun getProductById(id: Long): Product?

    @Query("SELECT * FROM products")
    fun getAllProducts(): List<Product>

    @Query("SELECT * FROM products WHERE category = :category")
    fun getProductsByCategory(category: String): List<Product>
}
