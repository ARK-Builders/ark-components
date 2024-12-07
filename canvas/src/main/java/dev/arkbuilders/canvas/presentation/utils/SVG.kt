package dev.arkbuilders.canvas.presentation.utils

import android.util.Log
import android.util.Xml
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.Path as ComposePath
import dev.arkbuilders.canvas.presentation.drawing.DrawPath
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlSerializer
import java.nio.file.Path
import kotlin.io.path.reader
import kotlin.io.path.writer


class SVG {
    private var strokeColor: ULong = Color.Black.value
    private var strokeSize: Float = SVGCommand.DEFAULT_BRUSH_SIZE
    private var fill = "none"
    private var viewBox = ViewBox()
    private val commands = ArrayDeque<SVGCommand>()
    private val paths = ArrayDeque<DrawPath>()

    private val paint
        get() = Paint().also {
            it.style = PaintingStyle.Stroke
            it.strokeWidth = strokeSize
            it.strokeCap = StrokeCap.Round
            it.strokeJoin = StrokeJoin.Round
            it.isAntiAlias = true
        }

    fun addCommand(command: SVGCommand) {
        commands.addLast(command)
    }

    fun getCommands(): ArrayDeque<SVGCommand> {
        return commands
    }

    fun addPath(path: DrawPath) {
        paths.addLast(path)
    }

    fun generate(path: Path): XmlSerializer? {
        if (commands.isNotEmpty()) {
            val xmlSerializer = Xml.newSerializer()
            val pathData = commands.joinToString()
            xmlSerializer.apply {
                setOutput(path.writer())
                startDocument("utf-8", false)
                startTag("", SVG_TAG)
                attribute("", Attributes.VIEW_BOX, viewBox.toString())
                attribute("", Attributes.XML_NS_URI, XML_NS_URI)
                startTag("", PATH_TAG)
                attribute("", Attributes.Path.STROKE, strokeColor.toString())
                attribute("", Attributes.Path.FILL, fill)
                attribute("", Attributes.Path.DATA, pathData)
                endTag("", PATH_TAG)
                endTag("", SVG_TAG)
                endDocument()
            }
            return xmlSerializer
        }
        return null
    }

    fun getPaths(): Collection<DrawPath> = paths

    fun copy(): SVG = SVG().apply {
        strokeColor = this@SVG.strokeColor
        fill = this@SVG.fill
        viewBox = this@SVG.viewBox
        commands.addAll(this@SVG.commands)
        paths.addAll(this@SVG.paths)
    }

    private fun createCanvasPaths() {
        if (commands.isNotEmpty()) {
            if (paths.isNotEmpty()) paths.clear()
            var path = ComposePath()
            commands.forEach { command ->
                strokeColor = command.paintColor
                strokeSize = command.brushSize
                when (command) {
                    is SVGCommand.MoveTo -> {
                        path = ComposePath()
                        path.moveTo(command.x, command.y)
                    }

                    is SVGCommand.AbsQuadTo -> {
                        path.quadraticTo(command.x1, command.y1, command.x2, command.y2)
                    }

                    is SVGCommand.AbsLineTo -> {
                        path.lineTo(command.x, command.y)
                    }
                }

                paths.addLast(
                    DrawPath(
                        path = path,
                        paint = paint.apply {
                            color = Color(command.paintColor)
                            strokeWidth = 3f
                        }
                    )
                )
            }
        }
    }

    fun addAll(commands: ArrayDeque<SVGCommand>) {
        commands.addAll(commands)
    }

    companion object {
        fun parse(path: Path): SVG = SVG().apply {
            val xmlParser = Xml.newPullParser()
            var pathData = ""

            xmlParser.apply {
                setInput(path.reader())

                var event = xmlParser.eventType
                var pathCount = 0
                while (event != XmlPullParser.END_DOCUMENT) {
                    val tag = xmlParser.name
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            when (tag) {
                                SVG_TAG -> {
                                    viewBox = ViewBox.fromString(
                                        getAttributeValue("", Attributes.VIEW_BOX)
                                    )
                                }

                                PATH_TAG -> {
                                    pathCount += 1
                                    strokeColor =
                                        getAttributeValue("", Attributes.Path.STROKE).toULong()
                                    fill = getAttributeValue("", Attributes.Path.FILL)
                                    pathData = getAttributeValue("", Attributes.Path.DATA)
                                }
                            }
                            if (pathCount > 1) {
                                Log.d("svg", "found more than 1 path in file")
                                break
                            }
                        }
                    }

                    event = next()
                }
                fun extractStrokeFromCommand(originalCommand: String, commandElements: List<String>) {
                    if (commandElements.size > 3) {
                        strokeColor = commandElements[3].toULong()
                    }
                    if (commandElements.size > 4) {
                        strokeSize = commandElements[4].toFloat()
                    }
                    commands.addLast(SVGCommand.MoveTo.fromString(originalCommand).apply {
                        paintColor = strokeColor
                        brushSize = strokeSize
                    })
                }
                pathData.split(COMMA).forEach {
                    val command = it.trim()
                    if (command.isEmpty()) return@forEach
                    val commandElements = command.split(" ")

                    when (command.first()) {
                        SVGCommand.MoveTo.CODE -> {
                            extractStrokeFromCommand(originalCommand = command, commandElements = commandElements)
                        }
                        SVGCommand.AbsLineTo.CODE -> {
                            extractStrokeFromCommand(originalCommand = command, commandElements = commandElements)
                        }
                        SVGCommand.AbsQuadTo.CODE -> {
                            if (commandElements.size > 5) {
                                strokeColor = commandElements[5].toULong()
                            }
                            if (commandElements.size > 6) {
                                strokeSize = commandElements[6].toFloat()
                            }
                            commands.addLast(SVGCommand.AbsQuadTo.fromString(command).apply {
                                paintColor = strokeColor
                                brushSize = strokeSize
                            })
                        }

                        else -> {}
                    }
                }

                createCanvasPaths()
            }
        }

        private object Attributes {
            const val VIEW_BOX = "viewBox"
            const val XML_NS_URI = "xmlns"

            object Path {
                const val STROKE = "stroke"
                const val FILL = "fill"
                const val DATA = "d"
            }
        }
    }
}

data class ViewBox(
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 100f,
    val height: Float = 100f
) {
    override fun toString(): String = "$x $y $width $height"

    companion object {
        fun fromString(string: String): ViewBox {
            val viewBox = string.split(" ")
            return ViewBox(
                viewBox[0].toFloat(),
                viewBox[1].toFloat(),
                viewBox[2].toFloat(),
                viewBox[3].toFloat()
            )
        }
    }
}

sealed class SVGCommand {

    companion object {
        const val DEFAULT_BRUSH_SIZE = 3.0f
    }

    var paintColor: ULong = Color.Black.value
    var brushSize: Float = 3.0f

    class MoveTo(
        val x: Float,
        val y: Float
    ) : SVGCommand() {
        override fun toString(): String = "$CODE $x $y $paintColor $brushSize"

        companion object {
            const val CODE = 'M'

            fun fromString(string: String): SVGCommand {
                val params = string.removePrefix("$CODE").trim().split(" ")
                val x = params[0].toFloat()
                val y = params[1].toFloat()
                val colorCode = if (params.size > 2) params[2].toULong() else Color.Black.value
                val strokeSize =
                    if (params.size > 3) params[3].toFloat() else DEFAULT_BRUSH_SIZE
                return MoveTo(x, y).apply {
                    paintColor = colorCode
                    brushSize = strokeSize
                }
            }
        }
    }

    class AbsLineTo(
        val x: Float,
        val y: Float
    ) : SVGCommand() {
        override fun toString(): String = "$CODE $x $y $paintColor $brushSize"

        companion object {
            const val CODE = 'L'

            fun fromString(string: String): SVGCommand {
                val params = string.removePrefix("$CODE").trim().split(" ")
                val x = params[0].toFloat()
                val y = params[1].toFloat()
                val colorCode = if (params.size > 2) params[2].toULong() else Color.Black.value
                val strokeSize =
                    if (params.size > 3) params[3].toFloat() else DEFAULT_BRUSH_SIZE
                return AbsLineTo(x, y).apply {
                    paintColor = colorCode
                    brushSize = strokeSize
                }
            }
        }
    }

    class AbsQuadTo(
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float
    ) : SVGCommand() {
        override fun toString(): String = "$CODE $x1 $y1 $x2 $y2 $paintColor $brushSize"

        companion object {
            const val CODE = 'Q'

            fun fromString(string: String): SVGCommand {
                val params = string.removePrefix("$CODE").trim().split(" ")
                val x1 = params[0].toFloat()
                val y1 = params[1].toFloat()
                val x2 = params[2].toFloat()
                val y2 = params[3].toFloat()
                val colorCode = if (params.size > 4) params[4].toULong() else Color.Black.value
                val strokeSize =
                    if (params.size > 5) params[5].toFloat() else DEFAULT_BRUSH_SIZE
                return AbsQuadTo(x1, y1, x2, y2).apply {
                    paintColor = colorCode
                    brushSize = strokeSize
                }
            }
        }
    }
}

private const val COMMA = ","
private const val XML_NS_URI = "http://www.w3.org/2000/svg"
private const val SVG_TAG = "svg"
private const val PATH_TAG = "path"