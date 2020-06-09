package com.example.library.home

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment
import com.example.library.R
import com.example.library.home.ARView.Companion.barcode
import kotlinx.android.synthetic.main.dialog_box.*

class DialogBox(text1: String, text2: String) : DialogFragment() {
    var text1 = text1
    var text2 = text2
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val viewInflate = inflater.inflate(R.layout.dialog_box, null)
            val mu = viewInflate.findViewById<Button>(R.id.switch_button)
            mu.setOnClickListener{
                val t = dialog?.author?.text
                dialog?.author?.setText(dialog?.title?.text)
                dialog?.title?.setText(t)
            }
            builder.setView(viewInflate)
            builder.setMessage("Is this correct?")
                .setPositiveButton(
                    "Ok",
                    DialogInterface.OnClickListener { dialog, id ->
                        // FIRE ZE MISSILES!
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
        if(barcode){
            dialog?.titleText?.text = "Barcode"
            dialog?.author?.visibility = View.INVISIBLE
            dialog?.authorText?.visibility = View.INVISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ARView.returnText1 = dialog?.title.toString()
        ARView.returnText2 = dialog?.author.toString()
    }
}