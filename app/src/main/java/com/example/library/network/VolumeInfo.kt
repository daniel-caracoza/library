package com.example.library.network

class Book(val volumeInfo: VolumeInfo)

class VolumeInfo(val title:String,
                 val subtitle:String,
                 val authors:List<String>,
                 val publisher: String,
                 val description: String,
                 val imageLinks: ImageLinks
)

class ImageLinks(val smallThumbnail: String)

class Items(val items: List<BookID>)

class BookID(val id: String)