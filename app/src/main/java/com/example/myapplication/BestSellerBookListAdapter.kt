package com.example.myapplication

import Book
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.data_row.view.*

class BestSellerBookListAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<Book> = ArrayList()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return BookViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.data_row, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder) {

            is BookViewHolder -> {
                holder.bind(items.get(position))
            }

        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun submitList(bookList: List<Book>){
        items = bookList
    }

    class BookViewHolder
    constructor(
        itemView: View
    ): RecyclerView.ViewHolder(itemView){

        val bookTitle = itemView.book_title
        val bookAuthor = itemView.book_author
        val bookDescription = itemView.book_description
        val bookContributor = itemView.book_contributor
        val bookPublisher = itemView.book_publisher
        val bookHighestRank = itemView.book_ranks

        fun bind(book: Book){

            var rankList: MutableList<Int> = ArrayList()
            for (history in book.ranks_history) {
                val intRank = history.rank!!.toIntOrNull()
                rankList.add(intRank!!)
            }
            val minRank = rankList.min()
            val highestRank = minRank.toString()

            if(minRank == null) {
                bookHighestRank.setText("**NY Times BestSeller")

            }else {
                bookHighestRank.setText("NY Times BestSeller Rank: " + highestRank)
            }
            if(book.title == "null" || book.author == "null" || book.description == "null" ||
                    book.contributor == "null" || book.publisher == "null"){
                bookTitle.setText("Title: N/A ")

                bookAuthor.setText("Author:  N/A " )
                bookDescription.setText("Description:  N/A " )
                bookContributor.setText("Contributor:  N/A")
                bookPublisher.setText("Publisher:  N/A" )
            }else {
                bookTitle.setText("Title:  " + book.title)

                bookAuthor.setText("Author:  " + book.author)
                bookDescription.setText("Description:\n " + book.description)
                bookContributor.setText("Contributor:  " + book.contributor)
                bookPublisher.setText("Publisher:  " + book.publisher)
            }

        }

    }
}

