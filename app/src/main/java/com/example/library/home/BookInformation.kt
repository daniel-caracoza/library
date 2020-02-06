package com.example.library.home


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import com.example.library.home.BookInformationArgs
import com.example.library.R
import com.example.library.databinding.FragmentBookInformationBinding

/**
 * A simple [Fragment] subclass.
 */
class BookInformation : Fragment() {

    private val args: BookInformationArgs by navArgs()

    private lateinit var binding: FragmentBookInformationBinding

    private lateinit var bookInfoViewModel: BookInformationViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_book_information,
            container,
            false
        )
        val viewModelFactory =
            BookViewModelFactory(args.bookTitle)
        bookInfoViewModel = ViewModelProviders.of(this, viewModelFactory).get(
            BookInformationViewModel::class.java)
        binding.bookInfoViewModel = bookInfoViewModel
        binding.lifecycleOwner = this
        return binding.root
    }
}
