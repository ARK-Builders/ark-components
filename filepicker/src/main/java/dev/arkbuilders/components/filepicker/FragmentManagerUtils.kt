package dev.arkbuilders.components.filepicker

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import java.nio.file.Path
import kotlin.io.path.Path

fun FragmentManager.onArkPathPicked(
    lifecycleOwner: LifecycleOwner,
    customRequestKey: String? = null,
    listener: (Path) -> Unit,
) {
    setFragmentResultListener(
        customRequestKey ?: ArkFilePickerFragment.PATH_PICKED_REQUEST_KEY,
        lifecycleOwner
    ) { _, bundle ->
        listener(
            Path(
                bundle.getString(ArkFilePickerFragment.PATH_PICKED_PATH_BUNDLE_KEY)!!
            )
        )
    }
}

fun FragmentManager.onArkRootOrFavPicked(
    lifecycleOwner: LifecycleOwner,
    listener: (path: Path, rootNotFavorite: Boolean) -> Unit
) {
    setFragmentResultListener(
        ArkRootPickerFragment.ROOT_PICKED_REQUEST_KEY,
        lifecycleOwner
    ) { _, bundle ->
        listener(
            Path(
                bundle.getString(ArkRootPickerFragment.PICKED_PATH_BUNDLE_KEY)!!
            ),
            bundle.getBoolean(ArkRootPickerFragment.ROOT_NOT_FAV_BUNDLE_KEY)
        )
    }
}