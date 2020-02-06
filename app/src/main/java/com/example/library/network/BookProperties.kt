package com.example.library.network

import com.tickaroo.tikxml.annotation.*

@Xml
class GoodreadsResponse {

    @Element(
        typesByElement = arrayOf<ElementNameMatcher>(
            ElementNameMatcher(type= Author::class),
            ElementNameMatcher(type= Search::class)
        )
    ) lateinit var author: Author
}

@Xml
class Search {
    @Element
    lateinit var results: Results
}

@Xml
class Results {
    @Element
    lateinit var works: List<Work>
}
@Xml
data class Work (
    @PropertyElement(name = "id")
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

    @Element
    val author: Author
)

@Xml
data class Author(
    @PropertyElement
    val id: Int,

    @PropertyElement
    val name: String
)