package com.example.library

import com.example.library.database.AppDatabase
import com.example.library.database.Favorite
import com.example.library.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repo class to mediate data access to Model(SQLite database) and webservices.
 */
class ApplicationRepository(private val database: AppDatabase) {

    private val googleApiKey = BuildConfig.googleKey

    private val goodreadsApiKey = BuildConfig.goodreadsKey

    /*
    @return a GoodsreadsResponse which contains a list of works(books) author name & id.
    @param the extracted text from the camera view.
    */
    suspend fun goodReadsSearchRequest(keyword: String): GoodreadsResponse {
        return withContext(Dispatchers.IO) {
            val parameters = mapOf("key" to goodreadsApiKey, "q" to keyword)
            GoodreadsApiService.GoodreadsApi.retrofitService.getSearchProperties(parameters)
        }
    }

    /*
    @return an authors id in a GoodreadsResponse
    @param map of author name and api key
     */
    suspend fun goodReadsGetAuthorId(authorName:String):GoodreadsResponse {
        return withContext(Dispatchers.IO){
            val parameters = mapOf("key" to goodreadsApiKey)
            GoodreadsApiService.GoodreadsApi.retrofitService.getAuthorId(authorName, parameters)
        }
    }

    
    /*
    @return a GoodsreadsResponse which contains a list of books by author
    @param the author id
     */
    suspend fun goodReadsAuthorBooksRequest(authorId: String): GoodreadsResponse {
        return withContext(Dispatchers.IO){
            val parameters = mapOf("key" to goodreadsApiKey)
            GoodreadsApiService.GoodreadsApi.retrofitService.getAuthorsBooks(authorId, parameters)
        }
    }
    /*@return a GoodreadsResponse which contains a list of genres the book belongs to and a review widget in raw html as an iframe, you
     *you need to display on a webview.
     */
    suspend fun goodReadsReviewsAndGenresRequest(id: Int): GoodreadsResponse {
        return withContext(Dispatchers.IO){
            val parameters = mapOf("key" to goodreadsApiKey)
            GoodreadsApiService.GoodreadsApi.retrofitService.getReviewsAndGenres(id, parameters)
        }
    }

    suspend fun goodReadsReviewsAndGenresRequestByISBN(isbn: String): GoodreadsResponse {
        return withContext(Dispatchers.IO) {
            val parameters = mapOf("key" to goodreadsApiKey)
            GoodreadsApiService.GoodreadsApi.retrofitService.getReviewsAndGenresByISBN(isbn, parameters)
        }
    }
    /*
     *  Retrieve the book id by performing search request and return a list of books
     */
    suspend fun googleBooksSearchRequest(keyword: String): Items {
        return withContext(Dispatchers.IO) {
            val parameters = mapOf("q" to keyword, "maxResults" to "5",  "key" to googleApiKey)
            GoogleBooksApiService.GoogleBooksApi.retrofitService.performSearch(parameters)
        }
    }
    /*
     *  Get a specific volume from google books api using volumeid retrieved from initial search request
     */
    suspend fun googleBooksGetVolume(keyword: String): Items {
        return withContext(Dispatchers.IO){
            val parameters = mapOf("q" to keyword, "maxResults" to "1", "key" to googleApiKey)
            GoogleBooksApiService.GoogleBooksApi.retrofitService.performBookSearch(parameters)
        }
    }

    /** Interactions with the Model(SQLite database **/

    /*
     *  function to add favorite to Model
     */
    suspend fun addFavoriteAsync(favorite: Favorite){
        withContext(Dispatchers.IO){
            database.favoriteDao.insertAll(favorite)
        }
    }

}