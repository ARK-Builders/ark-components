package dev.arkbuilders.canvas.presentation

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import dev.arkbuilders.canvas.R
import dev.arkbuilders.canvas.presentation.data.Preferences
import dev.arkbuilders.canvas.presentation.data.Resolution
import dev.arkbuilders.canvas.presentation.edit.EditScreen
import dev.arkbuilders.canvas.presentation.edit.EditViewModel

private const val imagePath = "image_path_param"

class ArkCanvasFragment : Fragment() {
    private var imagePathParam: String? = null

    lateinit var prefs: Preferences

    lateinit var viewModel: EditViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imagePathParam = it.getString(imagePath)
        }
        prefs = Preferences(appCtx = requireActivity().applicationContext)
        viewModel = EditViewModel(
            primaryColor = 0,
            launchedFromIntent = false,
            imagePath = null,
            imageUri = null,
            maxResolution = Resolution(350, 720),
            prefs = prefs,
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ark_canvas, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val composeView = view.findViewById<ComposeView>(R.id.compose_view)

        composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                // Set Content here
                EditScreen(
                    imagePath = null,
                    imageUri = null,
                    fragmentManager = childFragmentManager,
                    navigateBack = { /*TODO*/ },
                    launchedFromIntent = false,
                    maxResolution = Resolution(350, 720),
                    onSaveSvg = { /*TODO*/ },
                    viewModel = viewModel
                )
//                EditCanvas(viewModel = viewModel)
            }
        }
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