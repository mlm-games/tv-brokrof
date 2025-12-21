package org.mlm.browkorftv.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.Html
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.mlm.browkorftv.R
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class UpdateChecker(val currentVersionCode: Int) {
    var versionCheckResult: UpdateCheckResult? = null

    class ChangelogEntry(val versionCode: Int, val versionName: String, val changes: String)
    class UpdateCheckResult(
        val latestVersionCode: Int,
        val latestVersionName: String,
        val channel: String,
        val url: String,
        val changelog: ArrayList<ChangelogEntry>,
        val availableChannels: Array<String>
    )

    interface DialogCallback {
        fun download()
        fun later()
        fun settings()
    }

    suspend fun check(urlOfVersionFile: String, channelsToCheck: Array<String>): Boolean = withContext(Dispatchers.IO) {
        var urlConnection: HttpURLConnection? = null
        try {
            urlConnection = URL(urlOfVersionFile).openConnection() as HttpURLConnection
            val content = urlConnection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(content)

            val channelsJson = json.getJSONArray("channels")
            var latestVersionCode = 0
            var latestVersionName = ""
            var url = ""
            var latestVersionChannelName = ""
            val availableChannels = ArrayList<String>()
            val currentCPUArch = Build.SUPPORTED_ABIS[0]

            for (i in 0 until channelsJson.length()) {
                val channelJson = channelsJson.getJSONObject(i)
                availableChannels.add(channelJson.getString("name"))
                if (channelsToCheck.contains(channelJson.getString("name"))) {
                    val minAPI = if (channelJson.has("minAPI")) channelJson.getInt("minAPI") else 21
                    if (latestVersionCode < channelJson.getInt("latestVersionCode") &&
                        minAPI <= Build.VERSION.SDK_INT
                    ) {
                        latestVersionCode = channelJson.getInt("latestVersionCode")
                        latestVersionName = channelJson.getString("latestVersionName")
                        url = channelJson.getString("url")

                        // Architecture check
                        if (channelJson.has("urls")) {
                            val urls = channelJson.getJSONArray("urls")
                            for (j in 0 until urls.length()) {
                                val fileUrl = urls.getString(j)
                                if (fileUrl.endsWith("$currentCPUArch.apk")) {
                                    url = fileUrl
                                    break
                                }
                            }
                        }
                        latestVersionChannelName = channelJson.getString("name")
                    }
                }
            }

            val changelogJson = json.getJSONArray("changelog")
            val changelog = ArrayList<ChangelogEntry>()
            for (i in 0 until changelogJson.length()) {
                val versionChangesJson = changelogJson.getJSONObject(i)
                changelog.add(ChangelogEntry(
                    versionChangesJson.getInt("versionCode"),
                    versionChangesJson.getString("versionName"),
                    versionChangesJson.getString("changes")
                ))
            }
            versionCheckResult = UpdateCheckResult(
                latestVersionCode,
                latestVersionName,
                latestVersionChannelName,
                url,
                changelog,
                availableChannels.toTypedArray()
            )
            return@withContext true // Success
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false // Failed
        } finally {
            urlConnection?.disconnect()
        }
    }

    fun showUpdateDialog(context: Activity, callback: DialogCallback) {
        val version = versionCheckResult ?: return
        if (version.latestVersionCode <= currentVersionCode) return

        val message = StringBuilder()
        for (changelogEntry in version.changelog) {
            if (changelogEntry.versionCode > currentVersionCode) {
                message.append("<b>${changelogEntry.versionName}</b><br>")
                    .append(changelogEntry.changes.replace("\n", "<br>")).append("<br>")
            }
        }

        val textView = TextView(context)
        val padding = Utils.D2P(context, 25f).toInt()
        textView.setPadding(padding, padding, padding, padding)

        textView.text =
            Html.fromHtml(message.toString(), Html.FROM_HTML_MODE_COMPACT)

        AlertDialog.Builder(context)
            .setTitle(R.string.new_version_dialog_title)
            .setView(textView)
            .setPositiveButton(R.string.download) { _, _ -> callback.download() }
            .setNegativeButton(R.string.later) { _, _ -> callback.later() }
            .setNeutralButton(R.string.settings) { _, _ -> callback.settings() }
            .show()
    }

    suspend fun downloadUpdate(context: Activity, modelScope: CoroutineScope) {
        val update = versionCheckResult ?: return

        val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)
        progressBar.isIndeterminate = true // Start indeterminate until we know file size
        val padding = 40
        progressBar.setPadding(padding, padding, padding, padding)

        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.downloading_file) // "Downloading..."
            .setView(progressBar)
            .setCancelable(false)
            .create()

        dialog.show()

        val downloadedFile = Utils.createTempFile(context, UPDATE_APK_FILE_NAME)
        var downloaded = false

        // Run download in IO context
        val job = modelScope.launch(Dispatchers.IO) {
            var input: InputStream? = null
            var output: OutputStream? = null
            var connection: HttpURLConnection? = null
            try {
                val url = URL(update.url)
                connection = url.openConnection() as HttpURLConnection
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error: ${connection.responseCode}", Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                    }
                    return@launch
                }

                val fileLength = connection.contentLength.toLong()

                // Switch to determinate mode if we know the size
                if (fileLength != -1L) {
                    withContext(Dispatchers.Main) {
                        progressBar.isIndeterminate = false
                        progressBar.max = 100
                    }
                }

                input = connection.inputStream
                output = downloadedFile.outputStream()
                val data = ByteArray(4096)
                var total: Long = 0
                var count: Int

                // Throttle UI updates to avoid freezing
                var lastUiUpdate = 0L

                while (input.read(data).also { count = it } != -1) {
                    if (!isActive) return@launch
                    total += count.toLong()
                    output.write(data, 0, count)

                    if (fileLength > 0 && System.currentTimeMillis() - lastUiUpdate > 100) {
                        val progress = (total * 100 / fileLength).toInt()
                        withContext(Dispatchers.Main) {
                            progressBar.setProgress(progress, true)
                        }
                        lastUiUpdate = System.currentTimeMillis()
                    }
                }
                downloaded = true
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                output?.close()
                input?.close()
                connection?.disconnect()
            }
        }

        dialog.setOnCancelListener { job.cancel() }
        job.join() // Wait for download to finish
        dialog.dismiss()

        if (downloaded) {
            installApk(context, downloadedFile)
        }
    }

    private fun installApk(context: Context, file: File) {
        try {
            val apkURI = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider", // Ensure this matches AndroidManifest.xml
                file
            )

            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(apkURI, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Error installing: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    fun hasUpdate(): Boolean {
        val versionCheckResult = this.versionCheckResult ?: return false
        return versionCheckResult.latestVersionCode > currentVersionCode
    }

    companion object {
        private const val UPDATE_APK_FILE_NAME = "update.apk"
        fun clearTempFilesIfAny(context: Context) {
            var tempFile = File(context.cacheDir, UPDATE_APK_FILE_NAME)
            if (tempFile.exists()) {
                tempFile.delete()
            }
            tempFile = File(context.externalCacheDir, UPDATE_APK_FILE_NAME)
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }
}