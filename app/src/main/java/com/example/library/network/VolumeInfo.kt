package com.example.library.network

class GoogleBook(val id:String, val volumeInfo: VolumeInfo, val saleInfo: SaleInfo)

class VolumeInfo(val title:String,
                 val subtitle:String,
                 val authors:List<String>,
                 val publisher: String,
                 val publishedDate: String,
                 val description: String,
                 val pageCount: String,
                 val averageRating: String,
                 val categories: List<String>,
                 val imageLinks: ImageLinks)

class SaleInfo(
    val retailPrice: RetailPrice,
    val buyLink: String
)

class ImageLinks(
    val smallThumbnail:String
)

class RetailPrice(
    val amount: String
)

class Items(val items: List<GoogleBook>)

