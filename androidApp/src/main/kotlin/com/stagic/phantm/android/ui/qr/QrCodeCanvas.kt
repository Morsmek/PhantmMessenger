package com.stagic.phantm.android.ui.qr

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Renders [data] as a QR code using ZXing. Cell color matches MaterialTheme [Color.onBackground].
 * Background is transparent so callers control the surface color.
 */
@Composable
fun QrCodeCanvas(
    data: String,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    cellColor: Color = MaterialTheme.colorScheme.onBackground,
) {
    val bitMatrix = remember(data) {
        runCatching {
            QRCodeWriter().encode(
                data,
                BarcodeFormat.QR_CODE,
                512, 512,
                mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M),
            )
        }.getOrNull()
    }

    Canvas(modifier = modifier.size(size)) {
        val matrix = bitMatrix ?: return@Canvas
        val cellSize = this.size.width / matrix.width

        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix[x, y]) {
                    drawRect(
                        color = cellColor,
                        topLeft = Offset(x * cellSize, y * cellSize),
                        size = Size(cellSize, cellSize),
                    )
                }
            }
        }
    }
}
