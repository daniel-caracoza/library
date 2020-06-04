package com.example.library.home

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment
import com.example.library.R
import kotlinx.android.synthetic.main.dialog_box.*

class DialogBox(text1: String, text2: String) : DialogFragment() {
    var text1 = text1
    var text2 = text2
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            builder.setView(inflater.inflate(R.layout.dialog_box, null))
            builder.setMessage("Is this correct?")
                .setPositiveButton(
                    "Ok",
                    DialogInterface.OnClickListener { dialog, id ->
                        // FIRE ZE MISSILES!
                    })
                .setNeutralButton("Switch, but OK", DialogInterface.OnClickListener{
                    dialog, id ->

                })
                .setNegativeButton("Cancel",
                    DialogInterface.OnClickListener { dialog, id ->
                        // User cancelled the dialog
                    })
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onResume() {
        super.onResume()
        dialog?.title?.setText(text1)
        dialog?.author?.setText(text2)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ARView.returnText1 = dialog?.title.toString()
        ARView.returnText2 = dialog?.author.toString()
    }
}