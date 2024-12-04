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
import dev.arkbuilders.canvas.presentation.drawing.EditManager
import dev.arkbuilders.canvas.presentation.edit.EditScreen
import dev.arkbuilders.canvas.presentation.edit.EditViewModel
import dev.arkbuilders.canvas.presentation.resourceloader.BitmapResourceManager
import dev.arkbuilders.canvas.presentation.resourceloader.CanvasResourceManager
import dev.arkbuilders.canvas.presentation.resourceloader.SvgResourceManager
import java.nio.file.Path
import kotlin.io.path.Path

private const val imagePath = "image_path_param"

class ArkCanvasFragment : Fragment() {
    private lateinit var imagePathParam: String

    private lateinit var prefs: Preferences

    private lateinit var viewModel: EditViewModel
    private lateinit var bitmapResourceManager: CanvasResourceManager
    private lateinit var svgResourceManager: CanvasResourceManager

    lateinit var editManager: EditManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imagePathParam = it.getString(imagePath) ?: ""
        }
        val context = requireActivity().applicationContext
        prefs = Preferences(appCtx = context)
        editManager = EditManager()
        bitmapResourceManager = BitmapResourceManager(context = context, editManager = editManager)
        svgResourceManager = SvgResourceManager(editManager = editManager)
        viewModel = EditViewModel(
            primaryColor = 0xFF101828,
            launchedFromIntent = false,
            imagePath = pathFromString(),
            imageUri = null,
            maxResolution = Resolution(350, 720),
            prefs = prefs,
            editManager = editManager,
            bitMapResourceManager = bitmapResourceManager,
            svgResourceManager = svgResourceManager,
        )
    }

    private fun pathFromString(): Path?{
        return if (imagePathParam.isEmpty()) {
            null
        } else {
            Path(imagePathParam)
        }
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
                    fragmentManager = requireActivity().supportFragmentManager,
                    navigateBack = { requireActivity().supportFragmentManager.popBackStackImmediate() },
                    launchedFromIntent = false,
                    maxResolution = Resolution(350, 720),
                    onSaveSvg = { /*TODO*/ },
                    viewModel = viewModel
                )
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