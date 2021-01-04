package com.mark

import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import javax.swing.JOptionPane
import javax.xml.bind.DatatypeConverter
import kotlin.math.roundToInt
import kotlin.system.exitProcess
import java.io.IOException
import java.nio.file.Paths

class CacheDownloader(
    val path: String,
    val url: String,
    var updateCheck: Boolean = true,
    val writeOnlineHash: Boolean = false,
    private val listener: Progress? = object: Progress() {
        override fun update(progress: Int, message: String) {
           print("\r [$progress] $message")
        }
    }
) {

    var update: MutableList<String> = mutableListOf()

    init {
        if (writeOnlineHash) {
            updateCheck = false
            writeHashes()
        }
        initialize()
    }

    private fun initialize() {
        listener?.update(0, "Looking for Updates...")

        if (updateFiles() && updateCheck) {
            listener?.update(0, "Updates found...")
            generateList()
            update.forEach {
                download(url + it, path + it)
                val done = listener?.totalDone!!.plus(1)
                listener?.totalDone = done
            }

            writeHashes()
        }
    }

    private fun generateList() {
        listener?.update(0, "Generating Patch List...")
        val jsonParser = JSONParser()

        val link = URL(url + "online_hashes.json")
        val inputStream = link.openConnection()
        val obj: Any = jsonParser.parse(InputStreamReader(inputStream.getInputStream()).readText())
        val list = obj as JSONArray

        list.forEach {
            val raw: JSONObject = it as JSONObject
            val data = raw["data"] as JSONObject
            val name = data["name"] as String
            val hash = data["hash"] as String
            val location = data["location"] as String
            if (needsDownload(location, hash) && name != "hashes.json") {
                update.add(location)
            }
        }
        listener?.size = update.size
    }

    private fun needsDownload(location: String, hash: String): Boolean {
        return if (!File(path, location).exists()) {
            true
        } else getHash(File(path, location).toPath()) != hash
    }

    private fun updateFiles(): Boolean {
        return if (!File(path, "hashes.json").exists()) {
            true
        } else {
            getOnlineHash(URL(url + "cache.php?name=online_hashes.json")) != getHash(File(path, "hashes.json").toPath())
        }
    }

    private fun getOnlineHash(url: URL): String {
        if ((url.openConnection() as HttpURLConnection).responseCode == 404) {
            error("Could not locate cache.php at $url")
            exitProcess(0)
        }
        val hash = InputStreamReader(url.openStream()).readText()
        if (hash.isBlank()) {
            error("Could not find the file you requested")
            exitProcess(0)
        }
        return hash
    }

    private fun getHash(file: Path): String {
        val md = MessageDigest.getInstance("MD5")
        md.update(Files.readAllBytes(file))
        val digest = md.digest()
        return DatatypeConverter.printHexBinary(digest).toLowerCase()
    }

    private fun download(name: String, path: String) {
        var inputStream: InputStream?
        var outputStream: OutputStream?

        try {
            val url = URL(name.replace(" ", "%20"))
            val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36"
            val con: URLConnection = url.openConnection()
            con.setRequestProperty("User-Agent", userAgent)
            val contentLength: Int = con.contentLength

            inputStream = con.getInputStream()
            File(path.substringBeforeLast("/")).mkdirs()
            outputStream = FileOutputStream(path)

            val buffer = ByteArray(4096)
            var length: Int
            var downloaded = 0

            while (inputStream.read(buffer).also { length = it } != -1) {
                outputStream.write(buffer, 0, length)
                downloaded += length

                listener?.fileName = name.substringAfterLast("/")
                listener?.link = name

                listener?.update(
                    ((downloaded.toDouble() * 100.0) / (contentLength * 1.0)).roundToInt(),
                    "Downloading Update File ${name}..."
                )
            }
            outputStream?.close()
            inputStream?.close()
        } catch (ex: Exception) {
            error(ex)
            outputStream?.close()
            inputStream?.close()
        }

    }

    private fun error(message: String) {
        val optionPane = JOptionPane("Please Report this error to staff $message", JOptionPane.ERROR_MESSAGE)
        val dialog = optionPane.createDialog("Failure")
        dialog.isAlwaysOnTop = true
        dialog.isVisible = true
        exitProcess(0)
    }

    private fun writeHashes() {
        val list = JSONArray()
        Files.walk(Paths.get(path)).map { it.toFile() }.filter { !it.isDirectory && !it.name.contains("hashes.json") }.forEach {
            val data = JSONObject()
            data["name"] = it.name
            data["location"] = File(path).toURI().relativize(File(it.path).toURI()).path
            data["hash"] = getHash(it.toPath())
            val write = JSONObject()
            write["data"] = data
            list.add(write)
        }
        try {
            FileWriter(if (writeOnlineHash) "online_hashes.json" else path + "hashes.json").use { file ->
                file.write(list.toJSONString())
                file.flush()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        if (writeOnlineHash) {
            exitProcess(0)
        }
    }

}
