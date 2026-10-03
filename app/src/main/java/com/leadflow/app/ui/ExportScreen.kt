package com.leadflow.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.leadflow.app.LeadViewModel
import com.leadflow.app.data.Lead
import com.leadflow.app.util.Exporter
import com.leadflow.app.util.FileStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val MIME_CSV = "text/csv"
private const val MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
private const val MIME_PDF = "application/pdf"

@Composable
fun ExportScreen(vm: LeadViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    var shareFile by remember { mutableStateOf<File?>(null) }
    var shareMime by remember { mutableStateOf(MIME_CSV) }
    var busy by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Only used on Android 9 and below.
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pending?.invoke()   // run the export either way; the app-private copy always works
        pending = null
    }

    fun withStoragePermission(action: () -> Unit) {
        val needsPermission = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pending = action
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            action()
        }
    }

    fun export(fileName: String, mime: String, build: (List<Lead>) -> ByteArray) {
        withStoragePermission {
            scope.launch {
                busy = true
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val leads = vm.allLeads()
                        if (leads.isEmpty()) null else FileStore.save(context, fileName, mime, build(leads))
                    }
                }
                result.onSuccess { saved ->
                    if (saved == null) {
                        status = "No leads to export yet."
                        shareFile = null
                    } else {
                        shareFile = saved.shareFile
                        shareMime = mime
                        status = if (saved.inDownloads) "Saved to Downloads/LeadFlow/$fileName"
                        else "File created, but it could not be copied to Downloads. You can still share it."
                    }
                }.onFailure { status = "Export failed: ${it.message}" }
                busy = false
            }
        }
    }

    LfScaffold(title = "Export Data", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Columns: Business Name, Address, Phone, Notes, Date Added",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = { export("leads.csv", MIME_CSV) { Exporter.toCsv(it) } },
                enabled = !busy, modifier = Modifier.fillMaxWidth()
            ) { Text("Export as CSV") }
            Button(
                onClick = { export("leads.xlsx", MIME_XLSX) { Exporter.toXlsx(it) } },
                enabled = !busy, modifier = Modifier.fillMaxWidth()
            ) { Text("Export as Excel (.xlsx)") }
            Button(
                onClick = { export("leads.pdf", MIME_PDF) { Exporter.toPdf(it) } },
                enabled = !busy, modifier = Modifier.fillMaxWidth()
            ) { Text("Export as PDF") }

            status?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

            shareFile?.let { file ->
                OutlinedButton(
                    onClick = {
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = shareMime
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(send, "Share ${file.name}"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Share ${file.name}") }
            }
        }
    }
}
