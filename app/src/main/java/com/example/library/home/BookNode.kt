package com.example.library.home

import android.content.Context
import android.view.MotionEvent
import android.widget.ScrollView
import android.widget.TextView
import com.example.library.R
import com.example.library.network.GoogleBook
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ViewRenderable
import java.util.function.Consumer

class BookNode(
    val context: Context,
    var googleBook: GoogleBook) : Node(), Node.OnTapListener {

    init {
        setOnTapListener(this)
    }
    val averageRating = "5.0"

    private var infoCard: Node? = null;

    private val INFO_CARD_Y_POS_COEFF = 0.1f

    override fun onActivate() {
        if(scene == null) {
            throw IllegalStateException("Scene is null!")
        }
        if(infoCard == null){
            infoCard = Node()
            infoCard!!.setParent(this)
            infoCard!!.isEnabled = false
            val position = Vector3(0.0f, INFO_CARD_Y_POS_COEFF, 0.0f)
            infoCard!!.localPosition = position
            ViewRenderable.builder()
                .setView(context, R.layout.book_info_card)
                .build()
                .thenAccept(
                    Consumer { renderable: ViewRenderable ->
                        renderable.isShadowCaster = false
                        renderable.isShadowReceiver = false
                        infoCard!!.renderable = renderable
                        val scrollView = renderable.view as ScrollView
                        val averageRating = scrollView.findViewById<TextView>(R.id.average_rating)
                        averageRating.text = googleBook.volumeInfo.averageRating

                        val pageCount = scrollView.findViewById<TextView>(R.id.page_count)
                        pageCount.text = googleBook.volumeInfo.pageCount

                        val publisher = scrollView.findViewById<TextView>(R.id.publisher)
                        publisher.text = googleBook.volumeInfo.publisher

                        val description = scrollView.findViewById<TextView>(R.id.description)
                        description.text = googleBook.volumeInfo.description
                    }
                )
                .exceptionally { throwable: Throwable? ->
                    throw AssertionError(
                        "Could not load plane card view.",
                        throwable
                    )
                }
        }
    }

    override fun onTap(p0: HitTestResult?, p1: MotionEvent?) {
        infoCard!!.isEnabled = !(infoCard!!.isEnabled)
    }
}