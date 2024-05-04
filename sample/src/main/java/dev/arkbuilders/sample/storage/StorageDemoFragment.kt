package dev.arkbuilders.sample.storage

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import dev.arkbuilders.sample.R
import dev.arkbuilders.sample.databinding.FragmentStorageDemoBinding
import dev.arkbuilders.sample.extension.getAbsolutePath
import java.io.File

class StorageDemoFragment: DialogFragment() {

    private val TAG = StorageDemoFragment::class.java.name

    private lateinit var binding: FragmentStorageDemoBinding
    private val map by lazy { mutableMapOf<String, String>() }
    private var workingDir: String = "/"

    private val selectDirRequest = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            // call this to persist permission across device reboots
            context?.contentResolver?.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            workingDir = uri.getAbsolutePath()
            refreshFilesTree()
        }
    }

    private fun getCurrentAbsolutePath(): String {
        return workingDir + "/" + binding.edtStoragePath.text.toString()
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
        val layoutInflater = context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as? LayoutInflater
        binding = FragmentStorageDemoBinding.inflate(layoutInflater ?: LayoutInflater.from(context))
        initViews(binding)
        return binding.root
    }

    private fun initViews(binding: FragmentStorageDemoBinding) {
        binding.btnWorkingDir.setOnClickListener {
            selectDirRequest.launch(null)
        }

        binding.edtStoragePath.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                refreshFilesTree()
                return@setOnEditorActionListener true
            }
            false
        }

        binding.btnNewMapEntry.setOnClickListener {
            MapEntryDialog(isDelete = false, onDone = { key, value ->
                map[key] = value ?: ""
                refreshMap()
            }).show(parentFragmentManager, MapEntryDialog::class.java.name)
        }

        binding.btnDeleteEntry.setOnClickListener {
            MapEntryDialog(isDelete = true, onDone = { key, value ->
                map.remove(key)
                refreshMap()
            }).show(parentFragmentManager, MapEntryDialog::class.java.name)
        }

        binding.btnClearMap.setOnClickListener {
            map.clear()
            refreshMap()
        }
    }

    private fun refreshFilesTree() {
        val currentAbsolutePath = getCurrentAbsolutePath()
        binding.tvCurrentAbsolutePath.text = currentAbsolutePath

        try {
            val currentDir = File(currentAbsolutePath)
            currentDir.mkdirs()
            val listFiles = currentDir.listFiles() ?: return
            val fileTreeBuilder = StringBuilder()
            for (file in listFiles) {
                fileTreeBuilder.append(file.name).append("\n")
            }
            binding.tvCurrentFileTree.text = fileTreeBuilder.toString()
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
        }
    }

    private fun refreshMap() {
        if (map.isEmpty()) {
            binding.tvMapValues.text = getString(R.string.empty_map)
            return
        }
        val mapEntries = StringBuilder()
        for (entry in map) {
            mapEntries.append(entry.key).append(" -> ").append(entry.value).append("\n")
        }
        binding.tvMapValues.text = mapEntries.toString()
    }
}