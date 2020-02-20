package com.example.library

import android.webkit.WebView
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.example.library.database.Favorite

@BindingAdapter("imageUrl")
fun bindImage(imgView: ImageView, imgUrl: String?){
    imgUrl?.let {
        val imgUri = imgUrl.toUri().buildUpon().scheme("https").build()
        Glide.with(imgView.context)
            .load(imgUri)
            .into(imgView)
    }
}

@BindingAdapter("reviewHtml")
fun bindWebView(webView: WebView, reviewHtml: String?){
    reviewHtml?.let {
        webView.webChromeClient
        webView.loadData(reviewHtml, "text/html", "utf-8")
    }
}

@BindingAdapter("quality_image")
fun bindFavoriteImage(imgView: ImageView, item: Favorite){
    item.image.let {
        val imgUri = item.image.toUri().buildUpon().scheme("https").build()
        Glide.with(imgView.context)
            .load(imgUri)
            .into(imgView)
    }
}

@BindingAdapter("favoriteTitle")
fun TextView.setFavoriteTitle(item: Favorite){
    text = item.title
}

@BindingAdapter("favoriteAuthor")
fun TextView.setFavoriteAuthor(item: Favorite){
    text = item.author
}

@BindingAdapter("favoriteDescription")
fun TextView.setFavoriteDescription(item: Favorite){
    text = item.desc
}
