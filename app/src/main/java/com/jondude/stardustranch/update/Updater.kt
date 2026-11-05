package com.jondude.stardustranch.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.jondude.stardustranch.BuildConfig
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Checks the public GitHub repo's latest release and installs a newer APK. */
object Updater {
    const val REPO = "Jondude1/Stardust-Ranch-"
    private const val CHECK_INTERVAL_MS = 30 * 60 * 1000L
    private var lastCheck = 0L

    data class Release(val version: Int, val tag: String, val apkUrl: String, val notes: String)

    @Volatile var available: Release? = null
    @Volatile var status: String = "Not checked yet"
    @Volatile var progress: Float = -1f
    @Volatile var busy = false

    val installedVersion: String get() = BuildConfig.VERSION_NAME

    fun checkQuietly(onFound: (Release) -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastCheck < CHECK_INTERVAL_MS) return
        lastCheck = now
        check(onFound) {}
    }

    fun check(onFound: (Release) -> Unit, onNone: () -> Unit) {
        Thread {
            try {
                status = "Checking..."
                val conn = URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                conn.connectTimeout = 10000; conn.readTimeout = 10000
                if (conn.responseCode != 200) { status = "No releases found (${conn.responseCode})"; onNone(); return@Thread }
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val tag = json.optString("tag_name")
                val ver = Regex("(\\d+)$").find(tag)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val assets = json.optJSONArray("assets")
                var apk = ""
                if (assets != null) for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.optString("name").endsWith(".apk")) { apk = a.optString("browser_download_url"); break }
                }
                if (ver > BuildConfig.VERSION_CODE && apk.isNotEmpty()) {
                    val r = Release(ver, tag, apk, json.optString("body"))
                    available = r
                    status = "Update $tag available"
                    onFound(r)
                } else {
                    status = "Up to date (v${BuildConfig.VERSION_NAME})"
                    onNone()
                }
            } catch (e: Exception) {
                status = "Check failed: ${e.message}"
                onNone()
            }
        }.start()
    }

    fun downloadAndInstall(ctx: Context, r: Release, onDone: (Boolean) -> Unit) {
        if (busy) return
        busy = true
        Thread {
            try {
                val dir = File(ctx.cacheDir, "updates").apply { mkdirs() }
                dir.listFiles()?.forEach { it.delete() }
                val out = File(dir, "StardustRanch-${r.tag}.apk")
                var url = r.apkUrl
                var conn: HttpURLConnection
                var hops = 0
                while (true) {
                    conn = URL(url).openConnection() as HttpURLConnection
                    conn.instanceFollowRedirects = false
                    conn.connectTimeout = 15000; conn.readTimeout = 30000
                    val code = conn.responseCode
                    if (code in 300..399 && hops < 6) {
                        url = conn.getHeaderField("Location"); hops++; conn.disconnect(); continue
                    }
                    break
                }
                val total = conn.contentLengthLong
                conn.inputStream.use { input ->
                    out.outputStream().use { o ->
                        val buf = ByteArray(64 * 1024)
                        var read = 0L
                        while (true) {
                            val n = input.read(buf); if (n < 0) break
                            o.write(buf, 0, n); read += n
                            if (total > 0) progress = read.toFloat() / total
                        }
                    }
                }
                progress = 1f
                status = "Downloaded ${r.tag}"
                install(ctx, out)
                onDone(true)
            } catch (e: Exception) {
                status = "Download failed: ${e.message}"
                onDone(false)
            } finally {
                busy = false
            }
        }.start()
    }

    private fun install(ctx: Context, apk: File) {
        if (!ctx.packageManager.canRequestPackageInstalls()) {
            val i = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(i)
            status = "Allow installs from this app, then tap Update again"
            return
        }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.updates", apk)
        val i = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(i)
    }
}
