package fr.sybers.debout.ui.pairing

import android.graphics.ImageFormat
import android.graphics.Rect
import android.media.Image
import androidx.camera.core.ImageInfo
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.nio.ByteBuffer

@RunWith(RobolectricTestRunner::class)
class QrCodeAnalyzerTest {

    private val payload = "debout://v1?mac=AA%3ABB%3ACC%3ADD%3AEE%3AFF&ip=192.168.1.42&name=SALON-PC&os=windows"

    @Test
    fun `decodes a QR code from a frame whose rows are padded`() {
        val decoded = mutableListOf<String>()
        val frame = FakeFrame.of(payload, width = 320, height = 240, rowStride = 384)

        QrCodeAnalyzer { decoded += it }.analyze(frame)

        assertEquals(listOf(payload), decoded)
        assertTrue(frame.closed)
    }

    @Test
    fun `stays silent on a frame without a QR code`() {
        val decoded = mutableListOf<String>()
        val frame = FakeFrame(width = 320, height = 240, rowStride = 320, luminance = ByteArray(320 * 240) { 0x7F })

        QrCodeAnalyzer { decoded += it }.analyze(frame)

        assertEquals(emptyList<String>(), decoded)
        assertTrue(frame.closed)
    }

    /** Only the luminance plane is real: the analyzer reads nothing else. */
    private class FakeFrame(
        private val width: Int,
        private val height: Int,
        private val rowStride: Int,
        private val luminance: ByteArray,
    ) : ImageProxy {
        var closed = false
            private set

        override fun close() {
            closed = true
        }

        override fun getCropRect(): Rect = Rect(0, 0, width, height)
        override fun setCropRect(rect: Rect?) = Unit
        override fun getFormat(): Int = ImageFormat.YUV_420_888
        override fun getHeight(): Int = height
        override fun getWidth(): Int = width
        override fun getImageInfo(): ImageInfo = throw UnsupportedOperationException()
        override fun getImage(): Image? = null

        override fun getPlanes(): Array<ImageProxy.PlaneProxy> = arrayOf(
            object : ImageProxy.PlaneProxy {
                override fun getRowStride(): Int = this@FakeFrame.rowStride
                override fun getPixelStride(): Int = 1
                override fun getBuffer(): ByteBuffer = ByteBuffer.wrap(luminance)
            },
        )

        companion object {
            /** The last row stops at [width], as camera buffers do. */
            fun of(text: String, width: Int, height: Int, rowStride: Int): FakeFrame {
                val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, height, height)
                val left = (width - height) / 2
                val luminance = ByteArray(rowStride * (height - 1) + width) { 0xFF.toByte() }
                for (y in 0 until height) {
                    for (x in 0 until height) {
                        if (matrix[x, y]) luminance[y * rowStride + left + x] = 0
                    }
                }
                return FakeFrame(width, height, rowStride, luminance)
            }
        }
    }
}
