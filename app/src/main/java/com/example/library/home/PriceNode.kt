package com.example.library.home

import android.content.Context
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.TextView
import com.example.library.R
import com.example.library.network.GoogleBook
import com.example.library.network.Items
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ViewRenderable
import java.lang.IllegalStateException
import java.util.function.Consumer

class PriceNode(
    val context: Context,
    val googleBook: GoogleBook
): Node(), Node.OnTapListener {

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
                .setView(context, R.layout.coin_info_card)
                .build()
                .thenAccept(
                    Consumer { renderable: ViewRenderable ->
                        renderable.isShadowCaster = false
                        renderable.isShadowReceiver = false
                        infoCard!!.renderable = renderable
                        val view = renderable.view as LinearLayout
                        val price = view.findViewById<TextView>(R.id.price) as TextView
                        price.text = googleBook.saleInfo.retailPrice.amount
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

