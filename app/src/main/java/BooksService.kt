import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface BooksService {

    @GET("lists/best-sellers/history.json?author")
    fun getNYTimesBestSellerDataByAuthor(@Query("api-key") apikey: String, @Query("author") author: String): Call<BooksResponse>

    @GET("lists/best-sellers/history.json?title")
    fun getNYTimesBestSellerDataByTitle(@Query("api-key") apikey: String, @Query("title") title: String): Call<BooksResponse>

    @GET("lists/best-sellers/history.json?isbn")
    fun getNYTimesBestSellerDataByIsbn(@Query("api-key") apikey: String, @Query("isbn") isbn: String): Call<BooksResponse>
}