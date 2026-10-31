package com.example.craftcart.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.craftcart.R

class ProductGalleryAdapter(
    private var images: List<Int> = emptyList()
) : RecyclerView.Adapter<ProductGalleryAdapter.GalleryViewHolder>() {

    class GalleryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.galleryImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_gallery_image, parent, false)
        return GalleryViewHolder(view)
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        val imageRes = images[position]
        holder.imageView.setImageResource(imageRes)
        val context = holder.imageView.context
        val desc = context.getString(R.string.gallery_image_item_desc, position + 1, images.size)
        holder.imageView.contentDescription = desc
    }

    override fun getItemCount(): Int = images.size

    fun submitList(newImages: List<Int>) {
        images = newImages
        notifyDataSetChanged()
    }
}
