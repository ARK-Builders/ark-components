package dev.arkbuilders.canvas.presentation

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dev.arkbuilders.canvas.R

private const val imagePath = "image_path_param"

class ArkCanvasFragment : Fragment() {
    private var imagePathParam: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imagePathParam = it.getString(imagePath)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ark_canvas, container, false)
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String) =
            ArkCanvasFragment().apply {
                arguments = Bundle().apply {
                    putString(imagePath, param1)
                }
            }
    }
}