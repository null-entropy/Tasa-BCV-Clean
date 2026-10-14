package com.tasabcv.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** Datos de una versión nueva publicada en el repositorio oficial. */
data class UpdateInfo(
    val versionName: String,
    val notes: String,
    val apkUrl: String
)

/**
 * Estados del botón-ficha "Buscar actualización" en Ajustes.
 * [animationKey] agrupa el progreso de descarga bajo una sola clave para que
 * la transición de entrada ocurra una vez y el porcentaje avance sin rebotar
 * en cada punto porcentual.
 */
sealed class ManualUpdateState(val animationKey: String) {
    data object Idle : ManualUpdateState("idle")
    data object Checking : ManualUpdateState("checking")
    data object UpToDate : ManualUpdateState("up_to_date")
    data object Offline : ManualUpdateState("offline")
    data class Downloading(val progress: Int) : ManualUpdateState("downloading")
}

private sealed interface CheckResult {
    data class Available(val info: UpdateInfo) : CheckResult
    data object UpToDate : CheckResult
    data object Offline : CheckResult
}

/**
 * Sistema ligero de actualización propia:
 *   1. Revisión automática 1 sola vez al día (cada 24 h), 2 segundos después
 *      de abrir la app para no competir con la descarga de las tasas.
 *   2. Revisión manual instantánea desde la 3.ª ficha de Ajustes: si encuentra
 *      versión nueva, la descarga e instala directo mostrando el porcentaje.
 *   3. Usa únicamente HttpURLConnection + org.json + FileProvider del sistema.
 */
object AppUpdater {

    private const val ENDPOINT_LATEST_RELEASE =
        "https://api.github.com/repos/null-entropy/Tasa-BCV-Clean/releases/latest"
    private const val KEY_LAST_UPDATE_CHECK = "last_update_check_time"
    private const val DAILY_INTERVAL_MS = 24L * 60L * 60L * 1000L
    private const val CHECK_TIMEOUT_MS = 6000
    private const val DOWNLOAD_TIMEOUT_MS = 15000
    private const val FEEDBACK_DISPLAY_MS = 2500L
    private const val MIN_CHECKING_VISUAL_MS = 450L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    var manualState: ManualUpdateState by mutableStateOf(ManualUpdateState.Idle)
        private set

    var dailyPromptInfo: UpdateInfo? by mutableStateOf(null)
        private set

    var dialogDownloadProgress: Int? by mutableStateOf(null)
        private set

    var dialogError: Boolean by mutableStateOf(false)
        private set

    private var isDailyCheckRunning = false
    private var pendingApkToInstall: File? = null

    /**
     * Revisión automática diaria (máximo 1 vez cada 24 h). Espera 2 segundos
     * tras abrir la app para que el dólar y el euro carguen primero sin estorbo.
     */
    fun checkDailyIfNeeded(context: Context) {
        if (isDailyCheckRunning) return
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(DolarRepository.PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong(KEY_LAST_UPDATE_CHECK, 0L)
        if (lastCheck != 0L && System.currentTimeMillis() - lastCheck < DAILY_INTERVAL_MS) {
            return
        }

        isDailyCheckRunning = true
        scope.launch {
            try {
                delay(2_000L)
                val now = System.currentTimeMillis()
                val freshLast = prefs.getLong(KEY_LAST_UPDATE_CHECK, 0L)
                if (freshLast != 0L && now - freshLast < DAILY_INTERVAL_MS) return@launch

                when (val result = withContext(Dispatchers.IO) { checkRemoteRelease(appContext) }) {
                    is CheckResult.Available -> {
                        prefs.edit().putLong(KEY_LAST_UPDATE_CHECK, System.currentTimeMillis()).apply()
                        if (manualState !is ManualUpdateState.Downloading) {
                            dailyPromptInfo = result.info
                        }
                    }
                    CheckResult.UpToDate -> {
                        prefs.edit().putLong(KEY_LAST_UPDATE_CHECK, System.currentTimeMillis()).apply()
                    }
                    CheckResult.Offline -> {
                        // Sin conexión: no gastamos el turno diario para reintentar luego.
                    }
                }
            } finally {
                isDailyCheckRunning = false
            }
        }
    }

    /**
     * Acción al presionar la 3.ª ficha de Ajustes ("Buscar actualización"):
     *   Idle -> Checking ("Buscando…") ->
     *     • Available: descarga directa ("Descargando actualización… X %") e instala
     *     • UpToDate: "Ya tienes la última versión" (2.5 s) -> Idle
     *     • Offline: "Sin conexión" (2.5 s) -> Idle
     */
    fun onManualCardClick(context: Context) {
        if (manualState is ManualUpdateState.Checking || manualState is ManualUpdateState.Downloading) {
            return
        }
        val appContext = context.applicationContext
        manualState = ManualUpdateState.Checking

        scope.launch {
            val start = System.currentTimeMillis()
            val result = withContext(Dispatchers.IO) { checkRemoteRelease(appContext) }
            val elapsed = System.currentTimeMillis() - start
            if (elapsed < MIN_CHECKING_VISUAL_MS) {
                delay(MIN_CHECKING_VISUAL_MS - elapsed)
            }

            val prefs = appContext.getSharedPreferences(DolarRepository.PREFS_NAME, Context.MODE_PRIVATE)
            when (result) {
                is CheckResult.Available -> {
                    prefs.edit().putLong(KEY_LAST_UPDATE_CHECK, System.currentTimeMillis()).apply()
                    dailyPromptInfo = null
                    manualState = ManualUpdateState.Downloading(0)
                    val apkFile = withContext(Dispatchers.IO) {
                        downloadApk(appContext, result.info.apkUrl) { pct ->
                            scope.launch {
                                if (manualState is ManualUpdateState.Downloading) {
                                    manualState = ManualUpdateState.Downloading(pct)
                                }
                            }
                        }
                    }
                    if (apkFile != null) {
                        manualState = ManualUpdateState.Idle
                        installApk(appContext, apkFile)
                    } else {
                        manualState = ManualUpdateState.Offline
                        delay(FEEDBACK_DISPLAY_MS)
                        manualState = ManualUpdateState.Idle
                    }
                }
                CheckResult.UpToDate -> {
                    prefs.edit().putLong(KEY_LAST_UPDATE_CHECK, System.currentTimeMillis()).apply()
                    manualState = ManualUpdateState.UpToDate
                    delay(FEEDBACK_DISPLAY_MS)
                    manualState = ManualUpdateState.Idle
                }
                CheckResult.Offline -> {
                    manualState = ManualUpdateState.Offline
                    delay(FEEDBACK_DISPLAY_MS)
                    manualState = ManualUpdateState.Idle
                }
            }
        }
    }

    /** Inicia la descarga e instalación desde el cartel de aviso diario. */
    fun startDialogUpdate(context: Context) {
        val info = dailyPromptInfo ?: return
        if (dialogDownloadProgress != null) return
        val appContext = context.applicationContext
        dialogError = false
        dialogDownloadProgress = 0

        scope.launch {
            val apkFile = withContext(Dispatchers.IO) {
                downloadApk(appContext, info.apkUrl) { pct ->
                    scope.launch {
                        if (dialogDownloadProgress != null) {
                            dialogDownloadProgress = pct
                        }
                    }
                }
            }
            if (apkFile != null) {
                dialogDownloadProgress = null
                dailyPromptInfo = null
                installApk(appContext, apkFile)
            } else {
                dialogDownloadProgress = null
                dialogError = true
                delay(FEEDBACK_DISPLAY_MS)
                dialogError = false
            }
        }
    }

    /** Cierra el cartel diario ("Más tarde"). */
    fun dismissDailyPrompt() {
        if (dialogDownloadProgress != null) return
        dialogError = false
        dailyPromptInfo = null
    }

    /**
     * Si el usuario acaba de conceder el permiso "Instalar apps desconocidas" y
     * vuelve a la aplicación, continúa automáticamente con la instalación.
     */
    fun onAppResumed(context: Context) {
        val pending = pendingApkToInstall ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            context.packageManager.canRequestPackageInstalls()
        ) {
            pendingApkToInstall = null
            if (pending.exists()) {
                launchPackageInstaller(context, pending)
            }
        }
    }

    private fun checkRemoteRelease(context: Context): CheckResult {
        var connection: HttpURLConnection? = null
        return try {
            val installedVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
            } catch (_: Exception) {
                "0.0.0"
            }
            connection = (URL(ENDPOINT_LATEST_RELEASE).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CHECK_TIMEOUT_MS
                readTimeout = CHECK_TIMEOUT_MS
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", DolarRepository.buildUserAgent(context))
                instanceFollowRedirects = true
            }
            val code = connection.responseCode
            // 404 en /releases/latest significa que el repositorio respondió
            // pero aún no hay una publicación nueva colgada: la app está al día.
            if (code == HttpURLConnection.HTTP_NOT_FOUND) return CheckResult.UpToDate
            if (code != HttpURLConnection.HTTP_OK) return CheckResult.Offline

            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(body)
            val rawTag = json.optString("tag_name", "").trim()
            val remoteVersion = rawTag.removePrefix("v").removePrefix("V").trim()
            if (remoteVersion.isEmpty()) return CheckResult.UpToDate

            if (!isNewerVersion(installedVersion, remoteVersion)) {
                return CheckResult.UpToDate
            }

            var apkUrl = ""
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        if (apkUrl.isNotBlank()) break
                    }
                }
            }
            if (apkUrl.isBlank()) return CheckResult.UpToDate

            val rawNotes = json.optString("body", "").trim()
            val notes = rawNotes.ifBlank {
                context.getString(R.string.update_dialog_default_notes)
            }
            CheckResult.Available(UpdateInfo(remoteVersion, notes, apkUrl))
        } catch (_: Exception) {
            CheckResult.Offline
        } finally {
            connection?.disconnect()
        }
    }

    /** Compara versiones numéricas tipo "4.8.1" vs "4.9.0". */
    internal fun isNewerVersion(installed: String, remote: String): Boolean {
        val cleanInstalled = installed.substringBefore("-").trim()
        val cleanRemote = remote.substringBefore("-").trim()
        if (cleanInstalled == cleanRemote) return false
        val installedParts = cleanInstalled.split(".").map { it.toIntOrNull() ?: 0 }
        val remoteParts = cleanRemote.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(installedParts.size, remoteParts.size)
        for (i in 0 until maxLen) {
            val cur = installedParts.getOrElse(i) { 0 }
            val rem = remoteParts.getOrElse(i) { 0 }
            if (rem > cur) return true
            if (rem < cur) return false
        }
        return false
    }

    private fun downloadApk(
        context: Context,
        apkUrl: String,
        onProgress: (Int) -> Unit
    ): File? {
        var connection: HttpURLConnection? = null
        return try {
            val updatesDir = File(context.cacheDir, "updates")
            if (updatesDir.exists()) {
                updatesDir.listFiles()?.forEach { it.delete() }
            } else {
                updatesDir.mkdirs()
            }
            val targetFile = File(updatesDir, "TasaBCV-Clean-update.apk")

            connection = (URL(apkUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = DOWNLOAD_TIMEOUT_MS
                readTimeout = DOWNLOAD_TIMEOUT_MS
                setRequestProperty("User-Agent", DolarRepository.buildUserAgent(context))
                instanceFollowRedirects = true
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null

            val totalBytes = connection.contentLengthLong
            var downloadedBytes = 0L
            var lastReportedPct = 0

            connection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalBytes > 0L) {
                            val pct = ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
                            if (pct != lastReportedPct) {
                                lastReportedPct = pct
                                onProgress(pct)
                            }
                        }
                    }
                    output.flush()
                }
            }
            if (!targetFile.exists() || targetFile.length() <= 0L) return null
            onProgress(100)
            targetFile
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            pendingApkToInstall = apkFile
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                launchPackageInstaller(context, apkFile)
            }
            return
        }
        launchPackageInstaller(context, apkFile)
    }

    private fun launchPackageInstaller(context: Context, apkFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}

/**
 * Cartel de cristal que aparece cuando la revisión automática diaria encuentra
 * una versión nueva: muestra número de versión, resumen de mejoras y los
 * botones "Actualizar ahora" y "Más tarde".
 */
@Composable
fun UpdatePromptDialog(info: UpdateInfo) {
    val colors = appColors
    val context = LocalContext.current
    val downloadProgress = AppUpdater.dialogDownloadProgress
    val hasError = AppUpdater.dialogError
    val isDownloading = downloadProgress != null

    Dialog(
        onDismissRequest = {
            if (!isDownloading) AppUpdater.dismissDailyPrompt()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isDownloading,
            dismissOnClickOutside = !isDownloading
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassRaised(colors, RoundedCornerShape(32.dp), elevation = 16.dp)
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.update_dialog_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .background(colors.accent.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    .border(1.dp, colors.accent.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "v${info.versionName}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent
                )
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = colors.divider)
            Spacer(Modifier.height(12.dp))

            Text(
                text = info.notes,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = colors.muted,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState())
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = colors.divider)
            Spacer(Modifier.height(14.dp))

            AnimatedContent(
                targetState = when {
                    hasError -> "error"
                    isDownloading -> "downloading"
                    else -> "buttons"
                },
                transitionSpec = {
                    (fadeIn(tween(180)) +
                        slideInVertically(MotionSpringIntOffset) { it / 3 } +
                        scaleIn(PopSpring, initialScale = 0.85f)) togetherWith
                        (fadeOut(tween(140)) + scaleOut(targetScale = 0.92f, animationSpec = tween(140)))
                },
                label = "UpdateDialogFooter"
            ) { footerState ->
                when (footerState) {
                    "error" -> {
                        Text(
                            text = stringResource(R.string.update_card_offline),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        )
                    }
                    "downloading" -> {
                        Text(
                            text = stringResource(
                                R.string.update_card_downloading,
                                downloadProgress ?: 0
                            ),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.accent,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        )
                    }
                    else -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .glassInset(colors, RoundedCornerShape(20.dp))
                                    .pulseClick(pressedScale = 0.96f) {
                                        AppUpdater.dismissDailyPrompt()
                                    }
                                    .padding(vertical = 11.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.update_dialog_later),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.muted
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .glassRaised(colors, RoundedCornerShape(20.dp), elevation = 6.dp)
                                    .pulseClick(pressedScale = 0.96f) {
                                        AppUpdater.startDialogUpdate(context)
                                    }
                                    .padding(vertical = 11.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.update_dialog_now),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.accent
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
