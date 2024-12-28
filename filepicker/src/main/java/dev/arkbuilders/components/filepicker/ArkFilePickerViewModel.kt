package dev.arkbuilders.components.filepicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.arkbuilders.arklib.arkGlobal
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import dev.arkbuilders.arklib.data.folders.FoldersRepo
import dev.arkbuilders.arklib.utils.DeviceStorageUtils
import dev.arkbuilders.arklib.utils.listChildren
import dev.arkbuilders.components.utils.hasNestedRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

enum class ArkFilePickerMode {
    FILE, FOLDER, ANY
}

internal data class FilePickerState(
    val devices: List<Path>,
    val selectedDevicePos: Int,
    val currentPath: Path,
    val files: List<Path>,
    val rootsWithFavs: Map<Path, List<Path>>
) {
    val currentDevice get() = devices[selectedDevicePos]
}

internal sealed class FilePickerSideEffect {
    object DismissDialog : FilePickerSideEffect()
    object ToastAccessDenied : FilePickerSideEffect()
    class NotifyPathPicked(val path: Path) : FilePickerSideEffect()
    data object PinAsRoot : FilePickerSideEffect()
    data object AlreadyRoot : FilePickerSideEffect()
    data object PinAsFavorite : FilePickerSideEffect()
    data object AlreadyFavorite : FilePickerSideEffect()
    data object PinAsFirstRoot : FilePickerSideEffect()
    data object CannotPinFile : FilePickerSideEffect()
    data object NestedRootProhibited : FilePickerSideEffect()

}

internal class ArkFilePickerViewModel(
    private val deviceStorageUtils: DeviceStorageUtils,
    private val mode: ArkFilePickerMode,
    private val initialPath: Path?
): ViewModel(), ContainerHost<FilePickerState, FilePickerSideEffect> {

    private val foldersRepo = FoldersRepo.instance

    override val container: Container<FilePickerState, FilePickerSideEffect> =
        container(initialState())

    init {
        viewModelScope.launch {
            refreshRootsWithFavs()
        }
    }

    private suspend fun addRoot(root:Path) = withContext(Dispatchers.IO) {
        foldersRepo.addRoot(root)
        refreshRootsWithFavs()
    }

    private fun addFavorite(favorite: Path) {
        viewModelScope.launch(Dispatchers.IO) {
            val favoritePath = favorite.toRealPath()
            val folders = foldersRepo.provideFolders()
            val root = folders.keys.find { favoritePath.startsWith(it) }
                ?: throw IllegalStateException(
                    "Can't add favorite if it's root is not added"
                )
            val favoriteRelativePath = root.relativize(favoritePath)
            if (folders[root]?.contains(favoriteRelativePath) == true) {
                throw AssertionError("Path must be checked in RootPicker")
            }
            foldersRepo.addFavorite(root, favoriteRelativePath)
            refreshRootsWithFavs()
        }
    }

    private suspend fun refreshRootsWithFavs() {
        val rootsWithFavs = foldersRepo.provideFolders()
        intent {
            reduce {
                state.copy(rootsWithFavs = rootsWithFavs)
            }
        }
    }

    fun onItemClick(path: Path) = intent {
        if (path.isDirectory()) {
            try {
                reduce {
                    state.copy(
                        currentPath = path,
                        files = formatChildren(path)
                    )
                }
            } catch (e: Exception) {
                postSideEffect(FilePickerSideEffect.ToastAccessDenied)
            }
            return@intent
        }

        if (mode != ArkFilePickerMode.FOLDER)
            onPathPicked(path)
    }

    fun onPickBtnClick() = intent {
        onPathPicked(state.currentPath)
    }

    fun onDeviceSelected(selectedDevicePos: Int) = intent {
        val selectedDevice = state.devices[selectedDevicePos]
        reduce {
            state.copy(
                selectedDevicePos = selectedDevicePos,
                currentPath = selectedDevice,
                files = formatChildren(selectedDevice)
            )
        }
    }

    fun onBackClick() = intent {
        val isDevice = state.devices.any { device -> device == state.currentPath }
        if (isDevice) {
            postSideEffect(FilePickerSideEffect.DismissDialog)
            return@intent
        }
        val parent = state.currentPath.parent

        reduce {
            state.copy(
                currentPath = parent,
                files = formatChildren(parent)
            )
        }
    }

    private fun onPathPicked(path: Path) = intent {
        postSideEffect(FilePickerSideEffect.NotifyPathPicked(path))
        postSideEffect(FilePickerSideEffect.DismissDialog)
    }

    private fun initialState(): FilePickerState {
        val devices = deviceStorageUtils.listStorages()
        val currentPath = initialPath ?: devices[0]
        val selectedDevice =
            devices.find { currentPath.startsWith(it) } ?: devices[0]
        val selectedDevicePos = devices.indexOf(selectedDevice)
        return FilePickerState(
            devices,
            selectedDevicePos,
            currentPath,
            formatChildren(currentPath),
            emptyMap()
        )
    }

    private fun formatChildren(path: Path): List<Path> {
        val (dirs, files) = listChildren(path)

        val children = mutableListOf<Path>()
        children.addAll(dirs.sorted())
        children.addAll(files.sorted())

        return children
    }

    fun haveRoot(): Boolean {
        val arkGlobal = deviceStorageUtils.listStorages().firstOrNull()?.arkGlobal()
        return arkGlobal?.exists() == true
    }

    fun pinFolder(file: Path) = intent {
        if (!file.isDirectory()) {
            postSideEffect(FilePickerSideEffect.CannotPinFile)
            return@intent
        }

        val rootsWithFavorites = container.stateFlow.value.rootsWithFavs
        val roots = rootsWithFavorites.keys
        val root = roots.find { root -> file.startsWith(root) }
        val favorites = rootsWithFavorites[root]?.flatten()

        val hasNestedRoot = file.hasNestedRoot(roots)

        if (hasNestedRoot) {
            postSideEffect(FilePickerSideEffect.NestedRootProhibited)
            return@intent
        }

        val haveRoot = haveRoot()

        root?.let {
            //Make sure file isn't inside a root folder
            if (root != file) {
                val foundAsFavorite = favorites?.any { file.endsWith(it) } ?: false

                if (!foundAsFavorite) {
                    addFavorite(file)
                    postSideEffect(FilePickerSideEffect.PinAsFavorite)
                } else {
                    postSideEffect(FilePickerSideEffect.AlreadyFavorite)
                }
            } else {
                postSideEffect(FilePickerSideEffect.AlreadyRoot)
            }
        } ?: let {

            if (rootsWithFavorites.keys.contains(file)) {
                postSideEffect(FilePickerSideEffect.AlreadyRoot)
            } else {

                addRoot(file)
                if (!haveRoot) {
                    postSideEffect(FilePickerSideEffect.PinAsFirstRoot)
                } else {
                    postSideEffect(FilePickerSideEffect.PinAsRoot)
                }
            }
        }
    }
}

internal class ArkFilePickerViewModelFactory(
    private val deviceStorageUtils: DeviceStorageUtils,
    private val mode: ArkFilePickerMode,
    private val initialPath: Path?
): ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ArkFilePickerViewModel(deviceStorageUtils, mode, initialPath) as T
}