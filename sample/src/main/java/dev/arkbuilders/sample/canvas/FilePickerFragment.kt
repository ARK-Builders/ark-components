package dev.arkbuilders.sample.canvas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import dev.arkbuilders.canvas.presentation.ArkCanvasFragment
import dev.arkbuilders.canvas.presentation.picker.PickerScreen
import dev.arkbuilders.sample.R
import java.nio.file.Path

class FilePickerFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_file_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val composeView = view.findViewById<ComposeView>(dev.arkbuilders.canvas.R.id.compose_view)

        composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                // Set Content here
                PickerScreen(fragmentManager = requireActivity().supportFragmentManager,
                    onNavigateToEdit = { path, resolution ->
                        onNavigateToEdit(path, requireActivity().supportFragmentManager)
                    })
            }
        }
    }

    private fun onNavigateToEdit(path: Path?, fragmentManager: FragmentManager) {
        val canvasFragment = ArkCanvasFragment.newInstance(
            param1 = path?.toString() ?: ""
        )

        fragmentManager
            .beginTransaction()
            .add(R.id.canvas_content, canvasFragment)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        @JvmStatic
        fun newInstance() = FilePickerFragment()
    }
}