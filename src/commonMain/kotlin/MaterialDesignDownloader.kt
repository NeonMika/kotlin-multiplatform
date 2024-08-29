import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.select.Elements
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

/*
This class is used to download data from https://materialui.co/
 */
class MaterialDesignDownloader {

    companion object {
        fun Element.buttons() = select("button")
        fun Map<*, *>.printEntriesOnLines(target: StringBuilder? = null) {
            val printToTarget: (String) -> Unit = if (target == null) ::println else target::append
            val printlnToTarget: (String) -> Unit = { str ->
                if (target == null) {
                    println(str)
                } else {
                    target.appendLine(str)
                }
            }
            this.forEach { (key, value) ->
                printToTarget("$key: ")
                if (value is Map<*, *>) {
                    printlnToTarget("")
                    value.printEntriesOnLines(target)
                } else {
                    printlnToTarget(value.toString())
                }
            }
        }

        val httpClient = HttpClient()
    }

    private suspend fun getDocument(url: String): Document = Ksoup.parse(httpClient.get(url).bodyAsText()).apply {
        setBaseUri(url)
    }

    suspend fun loadColorListURLs(): Map<String, String> {
        /* Extract list names and links from */
        //Please note that the com.fleeksoft.ksoup:ksoup-network library is required for Ksoup.parseGetRequest.
        val doc: Document = getDocument("https://materialui.co/")


        val sections: Elements = doc.select("main section")

        val colorPaletteSection = sections[0]
        val moreSection = sections[1]

        val links = colorPaletteSection.select("a")

        return links.filter {
            it.attr("href") !in listOf(
                "/color-picker",
                "/colors",
                "https://www.tailwindcolor.com"
            )
        }
            .associate { a ->
                a.attr("href").drop(1) to a.absUrl("href")
            }
    }

    suspend fun loadMaterialColors(): Map<String, Map<String, String>> {
        val doc: Document = getDocument("https://materialui.co/colors")
        val colorPalettes = doc.select(".material-colors label").associate {
            it.text() to mutableMapOf<String, String>()
        }

        doc.selectFirst(".material-colors")!!.buttons().forEach {
            /* find the label before the button (which is the color name written as heading above the buttons) */
            val color = it.previousElementSiblings().first { it.tagName() == "label" }.text()
            val variant = it.selectFirst("span")!!.text()
            val hex = it.attr("style").substringAfter("bgc:").take(7)

            colorPalettes[color]?.set(variant, hex)
        }

        return colorPalettes
    }

    suspend fun loadTailwindColors(): Map<String, Map<String, String>> {
        val doc: Document = getDocument("https://tailwindcolor.com/")
        val colorPalettes = doc.select(".palette-name").associate {
            it.text() to mutableMapOf<String, String>()
        }

        doc.buttons().forEach {
            /* find the label before the button (which is the color name written as heading above the buttons) */
            val color = it.previousElementSiblings().first { it.className() == "palette-name" }.text()
            val variant = it.selectFirst("span")!!.text()
            // val hex = it.attr("style").substringAfter("background-color: ").substringBefore(")") + ")"
            val hex = it.attr("style").substringAfter("background-color:").take(7)



            colorPalettes[color]?.set(variant, hex)
        }

        return colorPalettes

    }

    suspend fun loadColors(url: String): Map<String, String> =
        getDocument(url).getAllElements().first { it.attr("class").endsWith("-colors") }.buttons().associate {
            /* find the label before the button (which is the color name written as heading above the buttons) */
            val name = it.selectFirst("span")!!.text()
            val hex = it.attr("style").substringAfter("bgc:").take(7)
            name to hex
        }

    suspend fun printAll(target: StringBuilder? = null) {
        val pages = loadColorListURLs()
        pages.printEntriesOnLines(target)

        println("Material Colors")
        val materialColors = loadMaterialColors()
        materialColors.printEntriesOnLines(target)

        println("Tailwind Colors")
        val tailwindColors = loadTailwindColors()
        tailwindColors.printEntriesOnLines(target)

        pages.forEach { (name, url) ->
            println(name)
            println(url)
            val colors = loadColors(url)
            colors.printEntriesOnLines(target)
        }
    }
}
