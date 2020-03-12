package com.example.myapplication

import Book
import DataSource
import TopSpacingItemDecoration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    var searchOption = "isbn"
    private lateinit var bookAdapter: BestSellerBookListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initRecyclerView()

    }



    private fun initRecyclerView(){

        recycler_view.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            val topSpacingDecorator = TopSpacingItemDecoration(30)
            addItemDecoration(topSpacingDecorator)
            bookAdapter = BestSellerBookListAdapter()
            adapter = bookAdapter
        }
    }

    fun sendBookList(bookList: ArrayList<Book>) {

        bookAdapter.submitList(bookList)
        bookAdapter.notifyDataSetChanged()

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater

        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId){
            R.id.search_options -> {

                true
            }
            R.id.option_isbn ->{

                searchOption = "isbn"
                DataSource(this).getCurrentData(searchOption)

                true
            }R.id.option_author->{
                searchOption = "author"
                DataSource(this).getCurrentData(searchOption)
                true
            }R.id.option_title->{
                searchOption = "title"
                DataSource(this).getCurrentData(searchOption)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
