package dev.arkbuilders.sample.canvas


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dev.arkbuilders.sample.R

private const val imagePath = "image_path_param"

class CanvasFragment : Fragment() {
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
        return inflater.inflate(R.layout.fragment_canvas, container, false)
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String) =
            CanvasFragment().apply {
                arguments = Bundle().apply {
                    putString(imagePath, param1)
                }
            }
    }
}