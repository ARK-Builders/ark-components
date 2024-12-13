package dev.arkbuilders.sample.storage

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import dev.arkbuilders.core.FileStorage
import dev.arkbuilders.sample.R
import dev.arkbuilders.sample.databinding.FragmentStorageDemoBinding
import dev.arkbuilders.sample.extension.getAbsolutePath
import java.util.UUID

class StorageDemoFragment : DialogFragment() {

    private val TAG = StorageDemoFragment::class.java.name

    private lateinit var binding: FragmentStorageDemoBinding
    private var workingDir: String = "/"
    private var storage: FileStorage? = null

    private val selectDirRequest =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let {
                // call this to persist permission across device reboots
                context?.contentResolver?.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                workingDir = uri.getAbsolutePath()
                val storageName = UUID.randomUUID().toString().take(6)
                binding.edtStorageName.setText(storageName)
                newStorage(storageName)
                updateDisplayMap()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(STYLE_NORMAL, R.style.Theme_ArkComponents)
        super.onCreate(savedInstanceState)
    }

    @SuppressLint("UseGetLayoutInflater")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val layoutInflater =
            context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as? LayoutInflater
        binding = FragmentStorageDemoBinding.inflate(layoutInflater ?: LayoutInflater.from(context))
        initViews(binding)
        return binding.root
    }

    private fun initViews(binding: FragmentStorageDemoBinding) {
        binding.btnWorkingDir.setOnClickListener {
            selectDirRequest.launch(null)
        }

        binding.edtStorageName.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val storageName = v.text.toString()
                newStorage(storageName)
                return@setOnEditorActionListener true
            }
            false
        }

        binding.btnSetEntry.setOnClickListener {
            MapEntryDialog(
                isDelete = false,
                onDone = { key, value ->
                    storage?.set(key, value)
                    updateDisplayMap()
                }
            ).show(parentFragmentManager, MapEntryDialog::class.java.name)
        }

        binding.btnDeleteEntry.setOnClickListener {
            MapEntryDialog(
                isDelete = true,
                onDone = { key, _ ->
                    storage?.remove(key)
                    updateDisplayMap()
                }
            ).show(parentFragmentManager, MapEntryDialog::class.java.name)
        }

        binding.btnErase.setOnClickListener {
            storage?.erase()
            storage = null
            binding.tvCurrentAbsolutePath.text = workingDir
            binding.edtStorageName.setText("")
            updateDisplayMap()
        }

        binding.btnWriteFs.setOnClickListener {
            storage?.writeFS()
        }
    }

    private fun newStorage(name: String) {
        val absolutePath = "$workingDir/$name"
        storage = FileStorage(name, absolutePath)
        binding.tvCurrentAbsolutePath.text = absolutePath
        updateDisplayMap()
    }

    private fun updateDisplayMap() {
        storage ?: let {
            binding.tvMapValues.text = getString(R.string.empty_map)
            return
        }
        val mapEntries = StringBuilder()
        storage!!.iterator().forEach { (key, value) ->
            mapEntries.append(key).append(" -> ").append(value).append("\n")
        }
        binding.tvMapValues.text = mapEntries.toString().ifEmpty { getString(R.string.empty_map) }
    }
}