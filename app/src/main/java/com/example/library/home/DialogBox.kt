package com.example.library.home

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.library.R
import kotlinx.android.synthetic.main.dialog_box.*
import com.example.library.home.MainActivity.Companion.barcode
import android.view.View
import android.widget.Button


class DialogBox(text1: String, text2: String) : DialogFragment() {
    var text1 = text1
    var text2 = text2

    internal lateinit var listener: NoticeDialogListener

    interface NoticeDialogListener {
        fun onDialogPositiveClick(dialog: DialogFragment)
    }

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
                        returnText()
                        listener.onDialogPositiveClick(this)
                    })
                .setNeutralButton("Cancel", DialogInterface.OnClickListener{ dialog, id ->
                })
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun returnText() {
        MainActivity.returnText1 = dialog?.title?.text.toString()
        MainActivity.returnText2 = dialog?.author?.text.toString()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = context as NoticeDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException((context.toString() +
                    " must implement NoticeDialogListener"))
        }
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
}