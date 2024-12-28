package dev.arkbuilders.components.filepicker

import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import dev.arkbuilders.arklib.data.folders.FoldersRepo
import dev.arkbuilders.arklib.utils.INTERNAL_STORAGE
import dev.arkbuilders.components.utils.hasNestedRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.file.Path

class ArkRootPickerFragment : ArkFilePickerFragment() {
    private var rootNotFavorite = false

    override fun onFolderChanged(currentFolder: Path) {
        lifecycleScope.launch {
            val folders = FoldersRepo.instance.provideFolders()
            val roots = folders.keys

            if (currentFolder == INTERNAL_STORAGE || currentFolder.hasNestedRoot(roots)) {
                rootNotFavorite = true
                binding.btnPick.text = getString(R.string.ark_file_picker_root)
                binding.btnPick.isEnabled = false
                return@launch
            }

            val root = roots.find { root -> currentFolder.startsWith(root) }
            val favorites = folders[root]?.flatten()
            root?.let {
                if (root == currentFolder) {
                    rootNotFavorite = true
                    binding.btnPick.text = getString(R.string.ark_file_picker_root)
                    binding.btnPick.isEnabled = false
                } else {
                    val foundAsFavorite = favorites?.any { currentFolder.endsWith(it) } ?: false
                    rootNotFavorite = false
                    binding.btnPick.text = getString(R.string.ark_file_picker_favorite)
                    binding.btnPick.isEnabled = !foundAsFavorite
                }
            } ?: let {
                rootNotFavorite = true
                binding.btnPick.text = getString(R.string.ark_file_picker_root)
                binding.btnPick.isEnabled = true
            }
        }
    }

    override fun onPick(pickedPath: Path) {
        lifecycleScope.launch(Dispatchers.IO) {
            addRootOrFavorite(pickedPath)
            setFragmentResult(
                ROOT_PICKED_REQUEST_KEY,
                bundleOf().apply {
                    putString(PICKED_PATH_BUNDLE_KEY, pickedPath.toString())
                    putBoolean(ROOT_NOT_FAV_BUNDLE_KEY, rootNotFavorite)
                }
            )
        }
    }

    private suspend fun addRootOrFavorite(pickedPath: Path) {
        val folders = FoldersRepo.instance.provideFolders()
        if (rootNotFavorite) {
            FoldersRepo.instance.addRoot(pickedPath)
        } else {
            val root = folders.keys.find { pickedPath.startsWith(it) }
                ?: throw IllegalStateException(
                    "Can't add favorite if it's root is not added"
                )
            val favoriteRelativePath = root.relativize(pickedPath)
            FoldersRepo.instance.addFavorite(root, favoriteRelativePath)
        }
    }

    companion object {
        const val ROOT_PICKED_REQUEST_KEY = "rootPicked"
        const val PICKED_PATH_BUNDLE_KEY = "pickedPath"
        const val ROOT_NOT_FAV_BUNDLE_KEY = "rootNotFav"

        fun newInstance(
            config: ArkFilePickerConfig = ArkFilePickerConfig()
        ) = ArkRootPickerFragment().apply {
            setConfig(
                config.copy(
                    showRoots = false,
                    mode = ArkFilePickerMode.FOLDER,
                    pathPickedRequestKey = "notUsed"
                )
            )
        }
    }
}