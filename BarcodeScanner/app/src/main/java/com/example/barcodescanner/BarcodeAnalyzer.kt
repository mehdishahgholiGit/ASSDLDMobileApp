package com.example.barcodescanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/** Simple carrier so the rest of the app never touches ML Kit's Barcode class directly. */
data class ScannedBarcode(val rawValue: String?, val formatName: String)

/**
 * CameraX ImageAnalysis.Analyzer that runs each camera frame through ML Kit's on-device
 * barcode scanner, restricted to QR Code plus the common 2D symbologies (Data Matrix,
 * Aztec, PDF417). Add Barcode.FORMAT_* constants below if you also need 1D formats
 * such as CODE_128 or EAN_13.
 */
class BarcodeAnalyzer(
    private val onBarcodesDetected: (List<ScannedBarcode>) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_DATA_MATRIX,
            Barcode.FORMAT_AZTEC,
            Barcode.FORMAT_PDF417
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    onBarcodesDetected(
                        barcodes.mapNotNull { barcode ->
                            barcode.rawValue?.let {
                                ScannedBarcode(it, formatName(barcode.format))
                            }
                        }
                    )
                }
            }
            .addOnCompleteListener {
                // Must always close the proxy or the camera pipeline stalls.
                imageProxy.close()
            }
    }

    private fun formatName(format: Int): String = when (format) {
        Barcode.FORMAT_QR_CODE -> "QR_CODE"
        Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
        Barcode.FORMAT_AZTEC -> "AZTEC"
        Barcode.FORMAT_PDF417 -> "PDF417"
        else -> "UNKNOWN"
    }
}
