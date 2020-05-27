package com.example.library.home

import android.content.Context
import android.view.MotionEvent
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.library.R
import com.example.library.network.GoogleBook
import com.example.library.network.Items
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.rendering.ViewSizer
import java.lang.IllegalStateException
import java.util.function.Consumer

class AuthorNode(
    val context: Context,
    val googleBook: GoogleBook,
    val authorBooks: Items): Node(), Node.OnTapListener {

    private var infoCard: Node? = null;

    private val INFO_CARD_Y_POS_COEFF = 0.1f

    init {
        setOnTapListener(this)
    }

    override fun onActivate() {
        if(scene == null){
            throw IllegalStateException("Scene is null")
        }
        if(infoCard == null){
            infoCard = Node()
            infoCard!!.setParent(this)
            infoCard!!.isEnabled = false
            val position = Vector3(0.0f, INFO_CARD_Y_POS_COEFF, 0.0f)
            infoCard!!.localPosition = position
            ViewRenderable.builder()
                .setView(context, R.layout.author_info_card)
                .build()
                .thenAccept(
                    Consumer { renderable: ViewRenderable ->
                        renderable.isShadowCaster = false
                        renderable.isShadowReceiver = false
                        renderable.sizer = ViewSizer { Vector3(0.5f, 0.5f, 0.5f) }
                        infoCard!!.renderable = renderable
                        val view = renderable.view as ConstraintLayout
                        view.findViewById<TextView>(R.id.author_name).apply {
                            text = googleBook.volumeInfo.authors[0]
                        }
                        view.findViewById<ImageView>(R.id.author_image).apply {
                            setImageResource(R.drawable.ic_account2)
                        }
                        val list = mutableListOf<String>()
                        authorBooks.items.forEach {
                            list.add(it.volumeInfo.title)
                        }
                        val arrayAdapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, list)
                        view.findViewById<ListView>(R.id.list_books).apply {
                            adapter = arrayAdapter
                        }

                    }
                )
                .exceptionally { throwable: Throwable? ->
                    throw AssertionError(
                        "Could not load plane card view.",
                        throwable
                    )
                }
        }
        super.onActivate()
    }


    override fun onTap(p0: HitTestResult?, p1: MotionEvent?) {
        infoCard!!.isEnabled = !(infoCard!!.isEnabled)
    }
}

