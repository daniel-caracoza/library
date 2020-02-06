package com.example.library

import android.webkit.WebView
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide

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