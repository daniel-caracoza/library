package com.example.library.network

import com.tickaroo.tikxml.annotation.*

@Xml
class GoodreadsResponse {
    @Element
    lateinit var search: Search

    @Element
    lateinit var author: Author

    @Element
    lateinit var book: Book

}
@Xml
class Search {
    @Path("results")
    @Element
    lateinit var works: List<Work>
}

@Xml
data class Work (
    @PropertyElement
    val id : Int,

    @PropertyElement
    val ratings_count: Int,

    @PropertyElement
    val average_rating: String,

    @Element
    val best_book: BestBook
    )

@Xml(name = "best_book")
data class BestBook(
    @PropertyElement
    val id: Int,

    @PropertyElement
    val title: String,

    @Path("author")
    @PropertyElement(name = "id")
    val authorId: String,

    @Path("author")
    @PropertyElement(name = "name")
    val name: String
)

@Xml
class Author {

    @Attribute
    lateinit var id: String

    @PropertyElement
    lateinit var name: String

    @PropertyElement
    lateinit var image_url: String

    @PropertyElement
    lateinit var about: String

    @Path("books")
    @Element
    lateinit var books: List<Book>
    
}
@Xml
class Book {
    @PropertyElement var id: Int = 0

    @PropertyElement lateinit var title: String

    @PropertyElement lateinit var average_rating: String

    @PropertyElement lateinit var isbn: String

    @PropertyElement lateinit var isbn13: String

    @PropertyElement lateinit var reviews_widget: String

    @PropertyElement lateinit var description:String

    @PropertyElement lateinit var small_image_url:String

    @Path("popular_shelves")
    @Element
    lateinit var shelves: List<Shelf>
}

@Xml
class Shelf {
    @Attribute(name = "name")
    lateinit var name: String
}
