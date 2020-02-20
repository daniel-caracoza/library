package com.example.library.home


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import com.example.library.R
import com.example.library.database.AppDatabase
import com.example.library.database.User
import com.example.library.databinding.FragmentBookInformationBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

/**
 * A simple [Fragment] subclass.
 */
class BookInformation : Fragment() {

    private val args: BookInformationArgs by navArgs()

    private var userId: Int = 0

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
        val application = requireNotNull(this.activity).application

        val dataSource = AppDatabase.getInstance(application).favoriteDao

        val viewModelFactory = BookViewModelFactory(userId, dataSource, application, args.bookTitle)

        bookInfoViewModel = ViewModelProviders.of(this, viewModelFactory).get(
            BookInformationViewModel::class.java)

        binding.bookInfoViewModel = bookInfoViewModel

        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        retrieveSignIn()
    }

    private fun retrieveSignIn(){

        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(activity)
        val user: User? = activity!!.intent.getParcelableExtra("user")

        when(account == null){
            true -> userId = user!!.uid
            false -> {
                val gid = account.id.toString()
                val truncate: String  = gid.substring(0, gid.length - 12)
                userId = truncate.toInt()
            }
        }
    }
}
