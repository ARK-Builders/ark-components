package dev.arkbuilders.sample.storage

import android.app.Dialog
import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.binding.BindingIndex
import dev.arkbuilders.components.filepicker.ArkFilePickerConfig
import dev.arkbuilders.components.filepicker.ArkFilePickerFragment
import dev.arkbuilders.components.filepicker.ArkFilePickerMode
import dev.arkbuilders.components.filepicker.onArkPathPicked
import dev.arkbuilders.sample.R
import dev.arkbuilders.sample.databinding.EditScoreDialogBinding
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class EditScoreDialog(
    private val root: Path,
    private val onDone: ((id: ResourceId, score: Int) -> Unit)? = null
) : DialogFragment() {

    private var resourceId: ResourceId? = null
    private lateinit var binding: EditScoreDialogBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = EditScoreDialogBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext(), R.style.SampleDialog)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.setContentView(binding.root)
        initView()
        return dialog
    }

    private fun initView() {
        parentFragmentManager.onArkPathPicked(this) { path ->
            val rootResources = BindingIndex.id2path(root)
            for (entry in rootResources) {
                if (entry.value.absolutePathString() == path.absolutePathString()) {
                    this.resourceId = entry.key;
                    return@onArkPathPicked
                }
            }
            this.resourceId = null;
        }

        binding.btnFilePicker.setOnClickListener {
            selectFile.show(parentFragmentManager, null)
        }

        binding.btnConfirm.setOnClickListener {
            val score = binding.edtScore.text.toString().toIntOrNull()
            if (this.resourceId == null || score == null) {
                return@setOnClickListener
            }
            onDone?.invoke(this.resourceId!!, score)
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private val selectFile = ArkFilePickerFragment.newInstance(
        ArkFilePickerConfig(
            showRoots = false,
            initialPath = root,
            rootsFirstPage = false,
            mode = ArkFilePickerMode.FILE,
        )
    )


}