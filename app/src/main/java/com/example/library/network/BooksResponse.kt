package com.example.library.network

import com.google.gson.annotations.SerializedName

class BooksResponse {
    @SerializedName("status")
    var status: String? = null
    @SerializedName("copyright")
    var copyright: String? = null
    @SerializedName("num_results")
    var num_results: String? = null
    @SerializedName("results")
    var books = ArrayList<Book>()
}

class NYTimesBook{
    @SerializedName("title")
    var title: String? = null
    @SerializedName("description")
    var description: String? = null
    @SerializedName("contributor")
    var contributor: String? = null
    @SerializedName("author")
    var author: String? = null
    @SerializedName("publisher")
    var publisher: String? = null
    //@SerializedName("isbns")
    //var isbns = ArrayList<Isbn>()
    @SerializedName("ranks_history")
    var ranks_history = ArrayList<History>()

}

class Isbn{
    @SerializedName("isbn10")
    var isbn10: String? = null
    @SerializedName("isbn13")
    var isbn13: String? = null
}

class History{
    @SerializedName("primary_isbn10")
    var primary_isbn10: String? = null
    @SerializedName("primary_isbn13")
    var primary_isbn13: String? = null
    @SerializedName("rank")
    var rank: String? = null
    @SerializedName("list_name")
    var list_name: String? = null
    @SerializedName("display_name")
    var display_name: String? = null
    @SerializedName("published_date")
    var published_date: String? = null
    @SerializedName("bestsellers_date")
    var bestsellers_date: String? = null
    @SerializedName("weeks_on_list")
    var weeks_on_list: String? = null

}