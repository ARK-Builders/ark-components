package dev.arkbuilders.sample.canvas

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.arkbuilders.sample.R

class CanvasActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_canvas)
        val filePickerFragment = FilePickerFragment.newInstance()

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.canvas_content, filePickerFragment)
            .commit()
    }
}