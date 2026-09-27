package com.example.barcodescanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.barcodescanner.databinding.ActivityMainBinding
import java.io.File
import java.util.Date
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private val scanAdapter = ScanAdapter()

    /** Dedupe key ("FORMAT:value") -> prevents the same code being added every frame it's in view. */
    private val seenKeys = HashSet<String>()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(this, getString(R.string.camera_permission_required), Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerScans.layoutManager = LinearLayoutManager(this)
        binding.recyclerScans.adapter = scanAdapter
        binding.tvCount.text = getString(R.string.scanned_count, 0)

        binding.btnExportEmail.setOnClickListener { exportAndEmail() }
        binding.btnClear.setOnClickListener { clearScans() }

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (hasCameraPermission()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = binding.previewView.surfaceProvider
            }

            val analyzer = BarcodeAnalyzer { barcodes ->
                runOnUiThread { handleBarcodes(barcodes) }
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(cameraExecutor, analyzer) }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis
                )
            } catch (exc: Exception) {
                Toast.makeText(this, getString(R.string.camera_start_failed, exc.message), Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun handleBarcodes(barcodes: List<ScannedBarcode>) {
        var added = false
        for (barcode in barcodes) {
            val value = barcode.rawValue ?: continue
            val key = "${barcode.formatName}:$value"
            if (seenKeys.add(key)) {
                val timestamp = DateFormat.format("yyyy-MM-dd HH:mm:ss", Date()).toString()
                scanAdapter.addItem(ScanRecord(value, barcode.formatName, timestamp))
                added = true
            }
        }
        if (added) {
            binding.recyclerScans.scrollToPosition(scanAdapter.itemCount - 1)
            binding.tvCount.text = getString(R.string.scanned_count, scanAdapter.itemCount)
        }
    }

    private fun clearScans() {
        seenKeys.clear()
        scanAdapter.clearAll()
        binding.tvCount.text = getString(R.string.scanned_count, 0)
    }

    private fun exportAndEmail() {
        val records = scanAdapter.currentItems()
        if (records.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_scans_yet), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val exportDir = File(cacheDir, "exports").apply { mkdirs() }
            val fileName = "barcodes_${System.currentTimeMillis()}.csv"
            val csvFile = File(exportDir, fileName)
            CsvExporter.writeCsv(csvFile, records)

            val uri: Uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", csvFile)

            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject))
                putExtra(Intent.EXTRA_TEXT, getString(R.string.email_body, records.size))
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(emailIntent, getString(R.string.send_via)))
        } catch (exc: Exception) {
            Toast.makeText(this, getString(R.string.export_failed, exc.message), Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
