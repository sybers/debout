package fr.sybers.debout.ui.pairing

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

/** Decodes QR codes from the luminance plane of each camera frame. Runs on the analysis executor. */
class QrCodeAnalyzer(private val onDecoded: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val reader = QRCodeReader()
    private val hints = mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE))
    private var luminance = ByteArray(0)

    override fun analyze(image: ImageProxy) {
        image.use {
            val plane = it.planes[0]
            val rowStride = plane.rowStride
            val size = rowStride * it.height
            if (luminance.size != size) luminance = ByteArray(size)
            // The last row of the plane may stop at the image width, short of a full row stride.
            val pixels = plane.buffer
            pixels.rewind()
            pixels.get(luminance, 0, minOf(pixels.remaining(), size))

            val source = PlanarYUVLuminanceSource(luminance, rowStride, it.height, 0, 0, it.width, it.height, false)
            val text = try {
                reader.decode(BinaryBitmap(HybridBinarizer(source)), hints).text
            } catch (_: ReaderException) {
                null
            } finally {
                reader.reset()
            }
            if (text != null) onDecoded(text)
        }
    }
}
