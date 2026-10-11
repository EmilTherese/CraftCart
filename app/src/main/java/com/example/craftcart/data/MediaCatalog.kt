package com.example.craftcart.data

import android.content.Context
import com.example.craftcart.R

object MediaCatalog {

    /**
     * Returns a list of image drawable resource IDs for the specified product ID.
     * Always includes at least the fallback/primary image resource.
     */
    fun imagesFor(productId: Long, fallbackImageResId: Int): List<Int> {
        val gallery = when (productId) {
            1L -> listOf(
                fallbackImageResId,
                R.drawable.puppy_painting,
                R.drawable.cute_christmas
            )
            2L -> listOf(
                fallbackImageResId,
                R.drawable.pup_painting,
                R.drawable.tasty_painting
            )
            3L -> listOf(
                fallbackImageResId,
                R.drawable.cute_christmas,
                R.drawable.clay_house
            )
            4L -> listOf(
                fallbackImageResId,
                R.drawable.meow_christmas,
                R.drawable.pup_painting
            )
            5L -> listOf(
                fallbackImageResId,
                R.drawable.cute_christmas,
                R.drawable.puppy_painting
            )
            6L -> listOf(
                fallbackImageResId,
                R.drawable.flower_planters,
                R.drawable.clay_house
            )
            7L -> listOf(
                fallbackImageResId,
                R.drawable.flower_planters,
                R.drawable.miffy_keychai
            )
            8L -> listOf(
                fallbackImageResId,
                R.drawable.clay_house,
                R.drawable.flower_planters
            )
            9L -> listOf(
                fallbackImageResId,
                R.drawable.clay_house,
                R.drawable.pup_painting
            )
            else -> listOf(fallbackImageResId)
        }

        // Ensure fallbackImageResId is present, distinct, and first
        val distinctImages = gallery.distinct().filter { it != 0 }
        return if (distinctImages.isEmpty()) {
            if (fallbackImageResId != 0) listOf(fallbackImageResId) else listOf(R.drawable.pup_painting)
        } else {
            distinctImages
        }
    }

    /**
     * Looks up res/raw/product_<id> via resources.getIdentifier.
     * Returns the resource ID or null if not found.
     */
    fun videoFor(context: Context, productId: Long): Int? {
        val resName = "product_$productId"
        val resId = context.resources.getIdentifier(resName, "raw", context.packageName)
        return if (resId != 0) resId else null
    }
}
