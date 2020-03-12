
import android.widget.TextView
import com.example.myapplication.MainActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DataSource(val mainActivity : MainActivity) {


    private var booksData: TextView? = null
    //internal
      fun getCurrentData(searchOption: String) {
        val call: Call<BooksResponse>
        val retrofit = Retrofit.Builder()
            .baseUrl(BaseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = retrofit.create(BooksService::class.java)
        //val call = service.getNYTimesBestSellerData(apikey, bTitle, author)
        if(searchOption == "author"){
            call = service.getNYTimesBestSellerDataByAuthor(apikey, author)
            getCall(call)
        }else if(searchOption == "title"){
            call = service.getNYTimesBestSellerDataByTitle(apikey, bTitle)
            getCall(call)
        }else if(searchOption == "isbn"){
            call = service.getNYTimesBestSellerDataByIsbn(apikey, isbn)
            getCall(call)
        }

        // val call = service.getNYTimesBestSellerDataByAuthor(apikey, author)
        //, isbns, author


    }

    fun getCall(call:Call<BooksResponse>){
        call.enqueue(object : Callback<BooksResponse> {
            override fun onResponse(call: Call<BooksResponse>, response: Response<BooksResponse>){
                if (response.code() == 200) {
                    val booksResponse = response.body()!!
                    mainActivity.sendBookList(booksResponse.books)

                }
            }

            override fun onFailure(call: Call<BooksResponse>, t: Throwable) {
                booksData!!.text = t.message
            }


         })

    }

    companion object {
        var BaseUrl = "https://api.nytimes.com/svc/books/v3/"
        var apikey = "AUwP0kMAThjkx1hIPdvwnDhx5FAY5tyA"
        var bTitle = "SIMPLE GENIUS"
        //Ready Player One
        var isbn = "9781538734032"
        var author = "David Baldacci"

    }
}


