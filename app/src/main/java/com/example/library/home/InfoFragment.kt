package com.example.library.home

import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.library.ApplicationRepository
import com.example.library.R
import com.example.library.database.AppDatabase
import com.example.library.databinding.InfoFragmentBinding
import com.example.library.network.GoodreadsResponse

class InfoFragment : Fragment() {
    private lateinit var infoViewModel: InfoViewModel
    private lateinit var binding: InfoFragmentBinding
    private lateinit var listView: ListView
    private lateinit var gr: GoodreadsResponse
    private lateinit var main: MainActivity
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.info_fragment, container, false)
        val application = requireNotNull(this.activity).application
        val dataSource = AppDatabase.getInstance(application)
        val repository = ApplicationRepository(dataSource)
        main = activity as MainActivity
        val googleBook = main._googleBook
        gr = main._goodreadsResponse
        val viewModelFactory = InfoViewModelFactory(googleBook, gr, repository)
        infoViewModel = ViewModelProviders.of(this,
            viewModelFactory).get(InfoViewModel::class.java)
        binding.infoViewModel = infoViewModel
        binding.lifecycleOwner = this

        infoViewModel.favoritePressed.observe(viewLifecycleOwner, Observer {
            it.let {
                Toast.makeText(activity, "Favorite button pressed", Toast.LENGTH_LONG).show()
            }
        })
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val list = mutableListOf<String>()
        gr.author.books.forEach {
            list.add(it.title)
        }

        val arrayAdapter = ArrayAdapter(main, android.R.layout.simple_list_item_1, list)
        listView = main.findViewById<ListView>(R.id.author_books)
        listView.apply { adapter = arrayAdapter }

    }
}
