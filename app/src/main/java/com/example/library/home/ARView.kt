package com.example.library.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.library.R
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.rendering.ViewRenderable

class ARView : AppCompatActivity() {

    private lateinit var sceneView : ArSceneView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_a_r_scene_view)
        sceneView = findViewById(R.id.ar_scene_view)
        var box = ViewRenderable.builder().setView(this, R.layout.boxlayout).build()
    }
}
