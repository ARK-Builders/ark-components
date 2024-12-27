package dev.arkbuilders.sample

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import dev.arkbuilders.arklib.data.folders.FoldersRepo
import dev.arkbuilders.arklib.data.index.ResourceIndex
import dev.arkbuilders.arklib.data.index.ResourceIndexRepo
import dev.arkbuilders.arklib.user.score.ScoreStorage
import dev.arkbuilders.arklib.user.score.ScoreStorageRepo
import dev.arkbuilders.components.filepicker.ArkFilePickerConfig
import dev.arkbuilders.components.filepicker.ArkFilePickerFragment
import dev.arkbuilders.components.filepicker.ArkFilePickerMode
import dev.arkbuilders.components.filepicker.onArkPathPicked
import dev.arkbuilders.components.scorewidget.HorizontalScoreWidgetComposable
import dev.arkbuilders.components.scorewidget.ScoreWidgetController
import dev.arkbuilders.components.scorewidget.VerticalScoreWidgetComposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.name

class ScoreActivity : AppCompatActivity() {
    private var rootFolder: Path? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_score)

        val btnPickRoot = findViewById<MaterialButton>(R.id.btn_pick_root)
        val btnPickResource = findViewById<MaterialButton>(R.id.btn_pick_resource)

        supportFragmentManager.onArkPathPicked(
            this,
            customRequestKey = PICK_ROOT_KEY
        ) { root ->
            rootFolder = root
            btnPickRoot.text = root.name
        }

        supportFragmentManager.onArkPathPicked(
            this,
            customRequestKey = PICK_RESOURCE_KEY
        ) { resourcePath ->
            rootFolder?.let {
                onResourcePicked(root = it, resourcePath)
            }
        }

        btnPickRoot.setOnClickListener {
            ArkFilePickerFragment
                .newInstance(ArkFilePickerConfig(pathPickedRequestKey = PICK_ROOT_KEY))
                .show(supportFragmentManager, null)
        }

        btnPickResource.setOnClickListener {
            ArkFilePickerFragment
                .newInstance(
                    ArkFilePickerConfig(
                        pathPickedRequestKey = PICK_RESOURCE_KEY,
                        mode = ArkFilePickerMode.FILE
                    )
                ).show(supportFragmentManager, null)
        }
    }

    private fun onResourcePicked(
        root: Path,
        resourcePath: Path
    ) = lifecycleScope.launch {
        val (index, scoreStorage) = setupIndexAndScoreStorage(root)
        val id = index.allPaths().toList()
            .find { it.second == resourcePath }?.first

        id ?: let {
            Toast.makeText(
                this@ScoreActivity,
                "File does not belong to root",
                Toast.LENGTH_SHORT
            ).show()
            return@launch
        }

        findViewById<MaterialButton>(R.id.btn_pick_resource).text = resourcePath.name

        val scoreWidgetController = ScoreWidgetController(
            lifecycleScope,
            getCurrentId = { id },
            onScoreChanged = {}
        )
        scoreWidgetController.init(scoreStorage)

        val horizontal = findViewById<ComposeView>(R.id.score_widget_horizontal)
        val vertical = findViewById<ComposeView>(R.id.score_widget_vertical)

        horizontal.disposeComposition()
        horizontal.setContent {
            HorizontalScoreWidgetComposable(
                size = DpSize(200.dp, 80.dp),
                controller = scoreWidgetController
            )
        }

        vertical.disposeComposition()
        vertical.setContent {
            VerticalScoreWidgetComposable(
                modifier = Modifier.padding(40.dp),
                size = DpSize(50.dp, 120.dp),
                controller = scoreWidgetController
            )
        }

        scoreWidgetController.setVisible(true)
        scoreWidgetController.displayScore()
    }

    private suspend fun setupIndexAndScoreStorage(
        root: Path
    ): Pair<ResourceIndex, ScoreStorage> = withContext(Dispatchers.IO) {
        val foldersRepo = FoldersRepo(applicationContext)
        val index = ResourceIndexRepo(foldersRepo).provide(root)
        val scoreStorage = ScoreStorageRepo(lifecycleScope).provide(index)
        return@withContext index to scoreStorage
    }

    companion object {
        private val PICK_ROOT_KEY = "pickRootKey"
        private val PICK_RESOURCE_KEY = "pickResourceKey"
    }
}