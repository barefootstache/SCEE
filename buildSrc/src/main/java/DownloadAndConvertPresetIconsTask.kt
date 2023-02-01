import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Attr
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.math.max

/** Download the SVG preset icons referred to by the iD presets and convert them to Android
 *  drawables. */
open class DownloadAndConvertPresetIconsTask : DefaultTask() {
    @get:Input var targetDir: String? = null
    @get:Input var version: String? = null
    @get:Input var iconSize: Int = 14
    @get:Input var transformName: (String) -> String = { it }
    @get:Input var indexFile: String? = null

    @TaskAction fun run() {
        val targetDir = targetDir ?: return
        val indexFile = indexFile ?: return
        val version = version ?: return

        val icons = getIconNames(version)

        val index = ArrayList<String>(icons.size)
        val indexTargetFile = File(indexFile)
        indexTargetFile.parentFile.mkdirs()

        val prefix = transformName("")
        for (file in File(targetDir).listFiles { _, s -> s.startsWith(prefix) }!!) {
            file.delete()
        }

        for (icon in icons) {
            val urls = getDownloadUrls(icon) ?: continue

            val iconName = transformName(icon)
            val targetFile = File("$targetDir/$iconName.xml")
            targetFile.parentFile.mkdirs()

            var message: String = ""
            var iconWasFound = false
            for (url in urls) {

                try {
                    URL(url).openStream().use { input ->
                        val factory = DocumentBuilderFactory.newInstance()
                        factory.isIgnoringComments = true
                        val svg = factory.newDocumentBuilder().parse(input)

                        val drawable = createAndroidDrawable(svg)

                        writeXml(drawable, targetFile)
                    }
                    index.add(iconName)
                    iconWasFound = true
                    break
                } catch (e: IOException) {
                    message += "$icon not found in $url\n"
                } catch (e: IllegalArgumentException) {
                    message += "$icon not supported: ${e.message}\n"
                }
            }
            if (!iconWasFound) {
                print(message)
            }
        }

        index.sort()
        writeIndexFile(index, indexTargetFile)
    }

    private fun writeIndexFile(index: List<String>, indexTargetFile: File) {
        indexTargetFile.writeText("""
            package de.westnordost.streetcomplete.view

            import de.westnordost.streetcomplete.R

            // DO NOT MODIFY! Generated by DownloadAndConvertPresetIconsTask
            val presetIconIndex = mapOf(
                ${index.joinToString(separator = ",\n                ") { "\"$it\" to R.drawable.$it" }}
            )

        """.trimIndent())
    }

    private fun createAndroidDrawable(svg: Document): Document {
        val drawable = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()

        var root: Element? = null
        for (i in 0..svg.childNodes.length) {
            val node = svg.childNodes.item(i)
            if (node is Element) {
                root = node
                break
            }
        }

        require(root != null) { "No root node found" }
        require(root.tagName  == "svg") { "Root must be <svg>" }

        val viewBox = root.getAttribute("viewBox")
        require(viewBox.isNotEmpty()) { "viewBox is missing" }
        val rect = viewBox.split(' ')

        require(rect.size == 4) { "Expected viewBox to have 4 values" }
        require(rect[0] == "0") { "unsupported viewBox x" }
        require(rect[1] == "0") { "unsupported viewBox y" }
        val width = rect[2]
        val height = rect[3]

        val x = root.getAttribute("x")
        require(x == "" || x == "0" || x == "0px") { "x must be 0" }
        val y = root.getAttribute("y")
        require(y == "" || y == "0" || y == "0px") { "y must be 0" }

        val width2 = root.getAttribute("width")
        val height2 = root.getAttribute("height")

        require(width2 == "" || width2 == width) { "expect viewBox width and width to be identical" }
        require(height2 == "" || height2 == height) { "expect viewBox height and height to be identical" }

        val vector = drawable.createElement("vector")
        vector.setAttribute("xmlns:android", "http://schemas.android.com/apk/res/android")
        val widthF = width.toFloat()
        val heightF = height.toFloat()
        val size = max(widthF, heightF)
        val iconWidth = iconSize * widthF / size
        val iconHeight = iconSize * heightF / size
        vector.setAttribute("android:width", "${iconWidth}dp")
        vector.setAttribute("android:height", "${iconHeight}dp")
        vector.setAttribute("android:viewportWidth", width)
        vector.setAttribute("android:viewportHeight", height)
        vector.setAttribute("android:tint", "?attr/colorControlNormal")
        drawable.appendChild(vector)

        for (i in 0 until root.childNodes.length) {
            val element = root.childNodes.item(i) as? Element ?: continue
            require(element.tagName == "path") { "Only paths are supported" }
            for (a in 0 until element.attributes.length) {
                val attr = element.attributes.item(a) as Attr
                require (attr.name in supportedPathAttributes) { "path attribute '${attr.name}' not supported" }
            }
            val d = element.getAttribute("d")
            require(d != "") { "no path defined" }

            val path = drawable.createElement("path")
            path.setAttribute("android:fillColor", "@android:color/white")
            path.setAttribute("android:pathData", makePathCompatible(d))
            vector.appendChild(path)
        }

        return drawable
    }

    private val supportedPathAttributes = setOf("d", "id")

    private fun makePathCompatible(path: String): String {
        val scientificNotation = Regex("\\d*\\.\\d+e-\\d+")
        // likely only used for very small numbers, just round to 0
        var result = scientificNotation.replace(path, "0")

        val zeroBeforeDot = Regex("(?<before>[- ,a-zA-Z])\\.")
        result = zeroBeforeDot.replace(result, "\${before}0.")

        val spaceAfterDecimal = Regex("(\\d+\\.\\d+)\\.")
        var i = 0
        var previousPath: String
        do {
            if (i++ > 3) throw IllegalStateException()
            previousPath = result
            result = spaceAfterDecimal.replace(previousPath, "\$1 0.")
        } while (result != previousPath)
        return result
    }

    private fun writeXml(xml: Document, targetFile: File) {
        FileOutputStream(targetFile).use { output ->
            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            val source = DOMSource(xml)
            val result = StreamResult(output)
            transformer.transform(source, result)
        }
    }

    private fun getIconNames(version: String): Set<String> {
        val presetsUrl = URL("https://raw.githubusercontent.com/openstreetmap/id-tagging-schema/$version/dist/presets.json")
        val presetsJson = Parser.default().parse(presetsUrl.openStream()) as JsonObject
        val icons = HashSet<String>()
        for (value in presetsJson.values) {
            val preset = value as? JsonObject ?: continue
            val icon = preset["icon"] as? String ?: continue
            icons.add(icon)
        }
        return icons
    }

    private fun getDownloadUrls(icon: String): List<String>? {
        val prefix = icon.substringBefore('-', "")
        val file = icon.substringAfter('-')
        return when (prefix) {
            "iD" -> listOf("https://raw.githubusercontent.com/openstreetmap/iD/develop/svg/iD-sprite/presets/$file.svg")
            "maki" -> listOf("https://raw.githubusercontent.com/mapbox/maki/main/icons/$file.svg")
            "temaki" -> listOf("https://raw.githubusercontent.com/ideditor/temaki/main/icons/$file.svg")
            // Font awesome is special...
            "fas" -> listOf(
                "https://raw.githubusercontent.com/FortAwesome/Font-Awesome/6.x/svgs/solid/$file.svg",
                "https://raw.githubusercontent.com/FortAwesome/Font-Awesome/master/svgs/solid/$file.svg"
            )
            "far" -> listOf(
                "https://raw.githubusercontent.com/FortAwesome/Font-Awesome/6.x/svgs/regular/$file.svg",
                "https://raw.githubusercontent.com/FortAwesome/Font-Awesome/master/svgs/regular/$file.svg",
            )
            "fab" -> listOf(
                "https://raw.githubusercontent.com/FortAwesome/Font-Awesome/6.x/svgs/brands/$file.svg",
                "https://raw.githubusercontent.com/FortAwesome/Font-Awesome/master/svgs/brands/$file.svg",
            )
            "roentgen" -> listOf(
                "https://raw.githubusercontent.com/openstreetmap/iD/develop/svg/roentgen/$file.svg"
            )
            else -> null
        }
    }
}
