package dev.arkbuilders.sample.storage

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import dev.arkbuilders.arklib.binding.BindingIndex
import dev.arkbuilders.arklib.data.storage.FileStorage
import dev.arkbuilders.arklib.user.score.RootScoreStorage
import dev.arkbuilders.arklib.user.score.Score
import dev.arkbuilders.sample.R
import dev.arkbuilders.sample.databinding.StorageDemoBinding
import dev.arkbuilders.sample.extension.getAbsolutePath
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

class StorageDemoFragment : DialogFragment() {
    private var rootDir: String? = null;
    private lateinit var storage: FileStorage<Score>
    private lateinit var binding: StorageDemoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(STYLE_NORMAL, R.style.Theme_ArkComponents)
        super.onCreate(savedInstanceState)
    }

    @SuppressLint("UseGetLayoutInflater")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val layoutInflater =
            context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as? LayoutInflater
        binding = StorageDemoBinding.inflate(layoutInflater ?: LayoutInflater.from(context))
        initViews()
        return binding.root
    }

    private fun initViews() {
        binding.btnRootDir.setOnClickListener {
            selectRootDir.launch(null)
        }

        binding.btnEditScore.setOnClickListener {
            if (this.rootDir != null) {
                EditScoreDialog(root = Path(rootDir!!), onDone = { id, score ->
                    storage.setValue(id, score)
                    refreshScoreMap()
                    runBlocking {
                        storage.persist() // TODO: FIND OUT WHY IT IS NOT PERSISTING ON DISK
                    }
                }).show(parentFragmentManager, null)
            }
        }
    }

    private val selectRootDir =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let {
                // call this to persist permission across device reboots
                context?.contentResolver?.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                rootDir = uri.getAbsolutePath()
                storage = RootScoreStorage(root = Path(rootDir!!), scope = lifecycleScope)
                refreshScoreMap()
                refreshFilesTree()
            }
        }

    private fun refreshFilesTree() {
        binding.tvRootDirPath.text = rootDir
        try {
            val currentDir = File(rootDir!!)
            val listFiles = currentDir.listFiles() ?: return
            val resultBuilder = StringBuilder()
            for (file in listFiles) {
                if (file.isFile) { // hiding subdirectories
                    resultBuilder.append(file.name).append("\n")
                }
            }
            binding.tvRootDirFileTree.text = resultBuilder.toString()
        } catch (e: Exception) {
            // pass
        }
    }

    private fun refreshScoreMap() {
        try {
            val resultBuilder = StringBuilder()
            val rootResources = BindingIndex.id2path(Path(rootDir!!))
            val currentDir = File(rootDir!!)
            val listFiles = currentDir.listFiles() ?: return
            for (file in listFiles) {
                if (file.isFile) { // hiding subdirectories
                    var score = 0
                    for (entry in rootResources) {
                        val path = entry.value
                        val resourceId = entry.key
                        if (file.absolutePath == path.absolutePathString()) {
                            score = storage.getValue(resourceId)
                            break
                        }
                    }
                    resultBuilder.append(file.name).append("-> $score").append('\n')
                }
            }
            binding.tvScoreMap.text = resultBuilder.toString()
        } catch (e: Exception) {
            // pass
        }
    }

}