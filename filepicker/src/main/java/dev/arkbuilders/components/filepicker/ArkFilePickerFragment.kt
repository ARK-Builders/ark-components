package dev.arkbuilders.components.filepicker

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.decode.VideoFrameDecoder
import coil.dispose
import coil.load
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import dev.arkbuilders.arklib.utils.DeviceStorageUtils
import dev.arkbuilders.arklib.utils.INTERNAL_STORAGE
import org.orbitmvi.orbit.viewmodel.observe
import dev.arkbuilders.components.filepicker.databinding.ArkFilePickerDialogNewFolderBinding
import dev.arkbuilders.components.filepicker.databinding.ArkFilePickerHostFragmentBinding
import dev.arkbuilders.components.filepicker.databinding.ArkFilePickerItemFileBinding
import dev.arkbuilders.components.filepicker.databinding.ArkFilePickerItemFilesRootsPageBinding
import dev.arkbuilders.components.filepicker.callback.OnFileItemLongClick
import dev.arkbuilders.components.folderstree.FolderTreeView
import dev.arkbuilders.components.utils.args
import dev.arkbuilders.components.utils.dpToPx
import dev.arkbuilders.components.utils.formatSize
import dev.arkbuilders.components.utils.iconForExtension
import dev.arkbuilders.components.utils.setDragSensitivity
import dev.arkbuilders.components.utils.toast
import java.io.File
import java.lang.Exception
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.isHidden
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

open class ArkFilePickerFragment :
    DialogFragment(R.layout.ark_file_picker_host_fragment) {

    private var titleStringId by args<Int>()
    private var pickButtonStringId by args<Int>()
    private var cancelButtonStringId by args<Int>()
    private var internalStorageStringId by args<Int>()
    private var itemsPluralId by args<Int>()
    private var themeId by args<Int>()
    private var accessDeniedStringId by args<Int>()
    private var mode by args<Int>()
    private var initialPath by args<String?>()
    private var showRoots by args<Boolean>()
    private var pathPickedRequestKey by args<String>()
    private var rootsFirstPage by args<Boolean>()

    private var currentFolder: Path? = null
    val binding by viewBinding(ArkFilePickerHostFragmentBinding::bind)
    private val viewModel by viewModels<ArkFilePickerViewModel> {
        ArkFilePickerViewModelFactory(
            DeviceStorageUtils(requireContext().applicationContext),
            ArkFilePickerMode.values()[mode!!],
            initialPath?.let { Path(it) }
        )
    }

    private val pagesAdapter = ItemAdapter<GenericItem>()

    private val mSharedPref by lazy { activity?.getPreferences(Context.MODE_PRIVATE) }
    private val PREF_LAST_ACTIVE_TAB_INDEX = "pref_last_active_tab_index"

    private val mOnTabSelectListener by lazy {
        object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                mSharedPref?.edit()?.putInt(PREF_LAST_ACTIVE_TAB_INDEX, tab?.position ?: 0)?.apply()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        }
    }

    open fun onFolderChanged(currentFolder: Path) {}
    open fun onPick(pickedPath: Path) {}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        initBackButtonListener()
        viewModel.observe(
            this,
            state = ::render,
            sideEffect = ::handleSideEffect
        )
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.setLayout(
                requireContext().dpToPx(DIALOG_WIDTH),
                WindowManager.LayoutParams.MATCH_PARENT
            )
        }
    }

    private fun initUI() = with(binding) {
        btnPick.text = getString(pickButtonStringId!!)
        btnCancel.text = getString(cancelButtonStringId!!)
        tvTitle.text = getString(titleStringId!!)
        if (mode == ArkFilePickerMode.FILE.ordinal)
            btnPick.isVisible = false

        btnCancel.setOnClickListener {
            dismiss()
        }
        btnPick.setOnClickListener {
            viewModel.onPickBtnClick()
        }
        vp.adapter = FastAdapter.with(pagesAdapter)
        vp.offscreenPageLimit = 2
        if (!showRoots!!) {
            vp.getChildAt(0).apply {
                overScrollMode = View.OVER_SCROLL_NEVER
            }
            vp.setDragSensitivity(10)
        }
        val pages = getPages()

        pagesAdapter.set(pages)
        val tabsTitle = resources
            .getStringArray(R.array.ark_file_picker_tabs)
            .apply {
                if (!rootsFirstPage!!)
                    reverse()
            }

        if (showRoots!!) {
            TabLayoutMediator(tabs, vp) { tab, pos ->
                tab.text = tabsTitle[pos]
            }.attach()
            mSharedPref?.getInt(PREF_LAST_ACTIVE_TAB_INDEX, 0)?.let {
                tabs.getTabAt(it)?.select()
            }
            tabs.addOnTabSelectedListener(mOnTabSelectListener)
        } else {
            tabs.isVisible = false
        }

        if (mode == ArkFilePickerMode.FOLDER.ordinal) {
            binding.ivNewFolder.visibility = View.VISIBLE
            binding.ivNewFolder.setOnClickListener {
                showCreateFolderDialog()
            }
        } else {
            binding.ivNewFolder.visibility = View.GONE
        }
    }

    private fun isARKMode(): Boolean {
        return binding.tabs.selectedTabPosition == 1
    }

    private fun showCreateFolderDialog() {
        val builder = AlertDialog.Builder(activity, android.R.style.ThemeOverlay_Material_Dialog_Alert)
        builder.setTitle(R.string.ark_file_picker_new_folder)
        val binding = ArkFilePickerDialogNewFolderBinding.inflate(layoutInflater)
        builder.setView(binding.root)
        builder.setPositiveButton(android.R.string.ok, null)

        builder.setNegativeButton(android.R.string.cancel) { _, _ -> }
        val dialog = builder.create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val newFolder = File(currentFolder.toString(), binding.editTextFolderName.text.toString().trim())
            if (newFolder.exists()) {
                binding.inputLayoutFolderName.error = getString(R.string.ark_file_picker_folder_existing)
                return@setOnClickListener
            }

            if (newFolder.mkdirs()) {
                if (isARKMode()) {
                    viewModel.pinFolder(newFolder.toPath())
                }
                //Reload current files tree
                currentFolder?.let { viewModel.onItemClick(it) }
                dialog.dismiss()
            }
        }

    }

    private fun render(state: FilePickerState) = binding.apply {

        displayPath(state)

        val deviceText = if (state.currentDevice == INTERNAL_STORAGE)
            getString(internalStorageStringId!!)
        else
            state.currentDevice.last().toString()

        if (state.currentPath.isDirectory()) {
            if (state.currentPath != currentFolder) {
                currentFolder = state.currentPath
                onFolderChanged(currentFolder!!)
            }
        }

        tvDevice.text = deviceText
        if (state.currentPath == state.currentDevice)
            tvDevice.setTextColor(
                resources.getColor(
                    R.color.black,
                    null
                )
            )
        else
            tvDevice.setTextColor(
                resources.getColor(
                    R.color.ark_file_picker_gray,
                    null
                )
            )


        tvDevice.setOnClickListener {
            if (state.devices.size == 1)
                viewModel.onItemClick(state.currentDevice)
            else
                DevicesPopup(
                    requireContext(),
                    state.devices,
                    viewModel
                ).showBelow(it)
        }
    }

    private fun handleSideEffect(effect: FilePickerSideEffect) = when (effect) {
        FilePickerSideEffect.DismissDialog -> dismiss()
        FilePickerSideEffect.ToastAccessDenied -> Toast.makeText(
            requireContext(),
            accessDeniedStringId!!,
            Toast.LENGTH_SHORT
        ).show()

        is FilePickerSideEffect.NotifyPathPicked -> {
            onPick(effect.path)
            setFragmentResult(
                pathPickedRequestKey ?: PATH_PICKED_REQUEST_KEY,
                Bundle().apply {
                    putString(
                        PATH_PICKED_PATH_BUNDLE_KEY,
                        effect.path.toString()
                    )
                })
        }

        FilePickerSideEffect.PinAsRoot ->
            activity?.toast(R.string.ark_file_picker_pinned_as_root)

        FilePickerSideEffect.AlreadyRoot ->
            activity?.toast(R.string.ark_file_picker_already_be_a_root)

        FilePickerSideEffect.PinAsFavorite ->
            activity?.toast(R.string.ark_file_picker_pinned_as_favorite)

        FilePickerSideEffect.AlreadyFavorite ->
            activity?.toast(R.string.ark_file_picker_already_a_favorite)

        FilePickerSideEffect.PinAsFirstRoot -> {
            pagesAdapter.set(getPages())
            activity?.toast(R.string.ark_file_picker_pinned_as_root)
        }

        FilePickerSideEffect.CannotPinFile -> {
            activity?.toast(R.string.ark_file_picker_pin_folder_only)
        }

        FilePickerSideEffect.NestedRootProhibited -> {
            activity?.toast(R.string.ark_file_nested_root_inside)
        }
    }


    private fun displayPath(state: FilePickerState) = binding.apply {
        layoutPath.removeViews(1, layoutPath.childCount - 1)
        val pathWithoutDevice =
            state.currentDevice.relativize(state.currentPath)

        val padding = requireContext().dpToPx(PATH_PART_PADDING)
        var tmpPath = state.currentDevice
        pathWithoutDevice
            .filter { it.toString().isNotEmpty() }
            .forEach { part ->
                tmpPath = tmpPath.resolve(part)
                val fullPathToPart = tmpPath
                val tv = TextView(requireContext())
                val text = "/$part"
                val outValue = TypedValue()
                requireContext().theme.resolveAttribute(
                    android.R.attr.selectableItemBackground,
                    outValue,
                    true
                )
                tv.setBackgroundResource(outValue.resourceId)
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                tv.setPadding(padding, 0, 0, 0)
                tv.isClickable = true
                tv.text = text
                if (pathWithoutDevice.last() == part)
                    tv.setTextColor(
                        resources.getColor(
                            R.color.black,
                            null
                        )
                    )
                else {
                    tv.setTextColor(
                        resources.getColor(
                            R.color.ark_file_picker_gray,
                            null
                        )
                    )
                    tv.setOnClickListener {
                        viewModel.onItemClick(fullPathToPart)
                    }
                }
                layoutPath.addView(tv)
            }
        scrollPath.post {
            scrollPath.fullScroll(ScrollView.FOCUS_RIGHT)
        }
    }

    override fun getTheme() = themeId!!

    private fun initBackButtonListener() {
        requireDialog().setOnKeyListener { _, keyCode, keyEvent ->
            if (keyCode == KeyEvent.KEYCODE_BACK &&
                keyEvent.action == KeyEvent.ACTION_UP
            ) {
                viewModel.onBackClick()
            }
            return@setOnKeyListener true
        }
    }

    private fun getPages() = if (showRoots!! && viewModel.haveRoot()) {
        if (rootsFirstPage!!) {
            listOf(
                RootsPage(this, viewModel),
                FilesPage(this, viewModel, itemsPluralId!!)
            )
        } else {
            listOf(
                FilesPage(this, viewModel, itemsPluralId!!),
                RootsPage(this, viewModel)
            )
        }
    } else {
        listOf(
            FilesPage(this, viewModel, itemsPluralId!!)
        )
    }

    fun setConfig(config: ArkFilePickerConfig) {
        titleStringId = config.titleStringId
        pickButtonStringId = config.pickButtonStringId
        cancelButtonStringId = config.cancelButtonStringId
        internalStorageStringId = config.internalStorageStringId
        accessDeniedStringId = config.accessDeniedStringId
        itemsPluralId = config.itemsPluralId
        themeId = config.themeId
        initialPath = config.initialPath?.toString()
        showRoots = config.showRoots
        pathPickedRequestKey = config.pathPickedRequestKey
        rootsFirstPage = config.rootsFirstPage
        mode = config.mode.ordinal
    }

    companion object {
        const val PATH_PICKED_REQUEST_KEY = "arkFilePickerPathPicked"
        const val PATH_PICKED_PATH_BUNDLE_KEY = "arkFilePickerPathPickedPathKey"

        fun newInstance(config: ArkFilePickerConfig) =
            ArkFilePickerFragment().apply {
                setConfig(config)
            }

        private const val DIALOG_WIDTH = 300f
        private const val PATH_PART_PADDING = 4f
    }

}

private fun pickerImageLoader(ctx: Context) = ImageLoader.Builder(ctx)
    .components {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            add(ImageDecoderDecoder.Factory())
        } else {
            add(GifDecoder.Factory())
        }
        add(SvgDecoder.Factory())
        add(VideoFrameDecoder.Factory())
    }
    .memoryCache {
        MemoryCache.Builder(ctx)
            .maxSizePercent(0.10)
            .build()
    }
    .diskCachePolicy(CachePolicy.DISABLED)
    .build()

internal class FilesPage(
    private val fragment: Fragment,
    private val viewModel: ArkFilePickerViewModel,
    private val itemsPluralId: Int
) : AbstractBindingItem<ArkFilePickerItemFilesRootsPageBinding>() {
    private val filesAdapter = ItemAdapter<FileItem>()
    private var currentFiles = emptyList<Path>()
    private val imageLoader = pickerImageLoader(fragment.requireContext())

    override val type = 0

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ) = ArkFilePickerItemFilesRootsPageBinding.inflate(inflater, parent, false)

    private var mBinding: ArkFilePickerItemFilesRootsPageBinding? = null

    override fun bindView(
        binding: ArkFilePickerItemFilesRootsPageBinding,
        payloads: List<Any>
    ) = with(binding) {
        mBinding = binding
        rvFiles.adapter = FastAdapter.with(filesAdapter)
        viewModel.observe(fragment, state = ::render)
    }

    private fun render(state: FilePickerState) {
        if (currentFiles == state.files) return

        filesAdapter.setNewList(state.files.map {
            FileItem(it, viewModel, itemsPluralId, imageLoader,
                onLongClick = object : OnFileItemLongClick {
                override fun onLongClick(file: Path) {
                    val currentItem = filesAdapter.itemList.items.firstOrNull { item ->
                        item.getFile().toString() == file.toString()
                    }
                    currentItem?.let {
                        val anchorView = mBinding?.rvFiles?.findViewHolderForAdapterPosition(
                            filesAdapter.getAdapterPosition(currentItem))?.itemView
                        anchorView?.let { anchor ->
                            val popupMenu = PopupMenu(fragment.activity, anchor, Gravity.END)
                            popupMenu.menuInflater.inflate(R.menu.file_select_menu, popupMenu.menu)
                            popupMenu.setOnMenuItemClickListener {
                                viewModel.pinFolder(file)
                                true
                            }
                            popupMenu.show()
                        }


                    } ?: let { return@let }
                }
            })
        })

        currentFiles = state.files
    }
}

internal class FileItem(
    private val file: Path,
    private val viewModel: ArkFilePickerViewModel,
    private val itemsPluralId: Int,
    private val imageLoader: ImageLoader,
    private val onLongClick: OnFileItemLongClick? = null
) : AbstractBindingItem<ArkFilePickerItemFileBinding>() {
    override val type = 0

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ) = ArkFilePickerItemFileBinding.inflate(inflater, parent, false)

    override fun bindView(
        binding: ArkFilePickerItemFileBinding,
        payloads: List<Any>
    ) = with(binding) {
        root.setOnClickListener {
            viewModel.onItemClick(file)
        }

        root.setOnLongClickListener {
            onLongClick?.onLongClick(file)
            true
        }
        binding.tvName.text = file.name
        if (file.isDirectory()) bindFolder(file, this)
        else bindRegularFile(file, this)
        return@with
    }

    private fun bindRegularFile(
        file: Path,
        binding: ArkFilePickerItemFileBinding
    ) = with(binding) {
        binding.tvDetails.text = file.fileSize().formatSize()
        binding.iv.load(file.toFile(), imageLoader) {
            size(200)
            placeholder(binding.iv.iconForExtension(file.extension.lowercase()))
            crossfade(true)
        }
    }

    private fun bindFolder(
        folder: Path,
        binding: ArkFilePickerItemFileBinding
    ) = with(binding) {
        val childrenCount = try {
            folder.listDirectoryEntries().filter { !it.isHidden() }.size
        } catch (e: Exception) {
            0
        }
        binding.tvDetails.text = binding.root.context.resources.getQuantityString(
            itemsPluralId,
            childrenCount,
            childrenCount
        )

        binding.iv.dispose()
        binding.iv.setImageResource(R.drawable.ark_file_picker_ic_folder)
    }

    fun getFile(): Path {
        return file
    }
}

internal class RootsPage(
    private val fragment: Fragment,
    private val viewModel: ArkFilePickerViewModel
) : AbstractBindingItem<ArkFilePickerItemFilesRootsPageBinding>() {
    private lateinit var folderTreeView: FolderTreeView
    private var currentRootsWithFavs = mapOf<Path, List<Path>>()

    override val type = 1

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ) = ArkFilePickerItemFilesRootsPageBinding.inflate(inflater, parent, false)

    override fun bindView(
        binding: ArkFilePickerItemFilesRootsPageBinding,
        payloads: List<Any>
    ) = with(binding) {
        folderTreeView = FolderTreeView(
            rvFiles,
            onNavigateClick = { node -> viewModel.onItemClick(node.path) },
            onAddClick = {},
            onForgetClick = {},
            showOptions = false
        )
        viewModel.observe(fragment, state = ::render)
    }

    private fun render(state: FilePickerState) {
        if (currentRootsWithFavs == state.rootsWithFavs) return

        folderTreeView.set(state.devices, state.rootsWithFavs)

        currentRootsWithFavs = state.rootsWithFavs
    }
}