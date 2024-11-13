package dev.arkbuilders.sample.canvas

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.arkbuilders.canvas.presentation.ArkCanvasFragment
import dev.arkbuilders.sample.R

class CanvasActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_canvas)
        val canvasFragment = ArkCanvasFragment.newInstance(
            param1 = "imagePath"
        )

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.canvas_content, canvasFragment)
            .commit()
    }
}