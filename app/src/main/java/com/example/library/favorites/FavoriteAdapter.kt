package com.example.library.favorites

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.library.database.Favorite
import com.example.library.databinding.ListItemFavoriteBinding

class FavoriteAdapter: ListAdapter<Favorite,FavoriteAdapter.ViewHolder>(FavoriteDiffCallback()) {


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    class ViewHolder private constructor(val binding: ListItemFavoriteBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Favorite) {
            binding.favorite = item
            binding.executePendingBindings()
        }

        companion object {
             fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemFavoriteBinding.inflate(layoutInflater, parent, false)

                return ViewHolder(binding)
            }
        }
    }
}

class FavoriteDiffCallback : DiffUtil.ItemCallback<Favorite>(){
    override fun areItemsTheSame(oldItem: Favorite, newItem: Favorite): Boolean {
        return oldItem.bookid == newItem.bookid
    }

    override fun areContentsTheSame(oldItem: Favorite, newItem: Favorite): Boolean {
        return oldItem == newItem
    }

}
