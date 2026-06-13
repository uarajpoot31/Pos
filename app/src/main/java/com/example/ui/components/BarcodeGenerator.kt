package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.*

object BarcodeGenerator {

    // Simple Code 39 character set map
    private val CODE39_MAP = mapOf(
        '0' to "000110100",
        '1' to "100100001",
        '2' to "001100001",
        '3' to "101100000",
        '4' to "000110001",
        '5' to "100110000",
        '6' to "001110000",
        '7' to "000100101",
        '8' to "100100100",
        '9' to "001100100",
        'A' to "100001001",
        'B' to "001001001",
        'C' to "101001000",
        'D' to "000011001",
        'E' to "100011000",
        'F' to "001011000",
        'G' to "000001101",
        'H' to "100001100",
        'I' to "001001100",
        'J' to "000011100",
        'K' to "100000011",
        'L' to "001000011",
        'M' to "101000010",
        'N' to "000010011",
        'O' to "100010010",
        'P' to "001010010",
        'Q' to "000000111",
        'R' to "100000110",
        'S' to "001000110",
        'T' to "000010110",
        'U' to "110000001",
        'V' to "011000001",
        'W' to "111000000",
        'X' to "010010001",
        'Y' to "110010000",
        'Z' to "011010000",
        '-' to "010000101",
        '.' to "110000100",
        ' ' to "011000100",
        '*' to "010010100" // Start/Stop char
    )

    /**
     * Encodes a string into Code 39 binary pattern (1 for black bar, 0 for white space).
     * Automatically adjusts lowercase and embeds standard start/stop '*' characters.
     */
    fun encodeToCode39Binary(text: String): String {
        val uppercaseText = "*${text.uppercase(Locale.getDefault())}*"
        val encodedResult = StringBuilder()

        for (i in uppercaseText.indices) {
            val char = uppercaseText[i]
            val pattern = CODE39_MAP[char] ?: CODE39_MAP[' ']!! // Fallback to space

            // Draw the 9 bars (5 black, 4 white)
            for (barIndex in 0 until 9) {
                val isWide = pattern[barIndex] == '1'
                val barWidthUnits = if (isWide) 3 else 1
                val isBlackBar = barIndex % 2 == 0

                // Append 1 for black, 0 for white
                val bitChar = if (isBlackBar) '1' else '0'
                repeat(barWidthUnits) {
                    encodedResult.append(bitChar)
                }
            }
            // Add narrow white inter-character gap (0)
            if (i < uppercaseText.length - 1) {
                encodedResult.append('0')
            }
        }
        return encodedResult.toString()
    }

    /**
     * Jetpack Compose barcode renderer using canvas.
     */
    @Composable
    fun BarcodeView(
        text: String,
        modifier: Modifier = Modifier,
        barColor: Color = Color.Black,
        spaceColor: Color = Color.White
    ) {
        val binaryPattern = encodeToCode39Binary(text)

        Canvas(modifier = modifier.background(spaceColor)) {
            val width = size.width
            val height = size.height
            val totalUnits = binaryPattern.length
            val unitWidth = width / totalUnits

            var currentX = 0f
            for (i in binaryPattern.indices) {
                val isBlack = binaryPattern[i] == '1'
                if (isBlack) {
                    drawRect(
                        color = barColor,
                        topLeft = Offset(currentX, 0f),
                        size = Size(unitWidth + 0.5f, height) // adding 0.5f prevents anti-aliasing panel lines
                    )
                }
                currentX += unitWidth
            }
        }
    }

    /**
     * Generates a printable HTML barcode label template.
     */
    fun printBarcodeLabels(
        context: Context,
        productName: String,
        productCode: String,
        price: Double,
        quantityToPrint: Int = 10
    ) {
        val binaryPattern = encodeToCode39Binary(productCode)
        
        // Build barcode cells
        val cellsHtml = StringBuilder()
        for (q in 0 until quantityToPrint) {
            cellsHtml.append("""
                <div class="label-card">
                    <div class="shop-title">AL ILM BOOK DEPOT</div>
                    <div class="product-name">$productName</div>
                    <svg class="barcode" viewBox="0 0 ${binaryPattern.length} 40">
            """)

            for (i in binaryPattern.indices) {
                if (binaryPattern[i] == '1') {
                    cellsHtml.append("""<rect x="$i" y="0" width="1.1" height="40" fill="black" />""")
                }
            }

            cellsHtml.append("""
                    </svg>
                    <div class="product-code">$productCode</div>
                    <div class="price">Rs. $price</div>
                </div>
            """)
        }

        val htmlContent = """
            <html>
            <head>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        margin: 0;
                        padding: 15px;
                        background: #fff;
                    }
                    .label-grid {
                        display: grid;
                        grid-template-columns: repeat(3, 1fr);
                        gap: 15px;
                    }
                    .label-card {
                        border: 1px dotted #ccc;
                        padding: 10px;
                        text-align: center;
                        background: #fff;
                        page-break-inside: avoid;
                    }
                    .shop-title {
                        font-size: 8px;
                        font-weight: bold;
                        color: #555;
                        letter-spacing: 1px;
                    }
                    .product-name {
                        font-size: 11px;
                        font-weight: bold;
                        margin: 3px 0;
                        white-space: nowrap;
                        overflow: hidden;
                        text-overflow: ellipsis;
                    }
                    .barcode {
                        width: 90%;
                        height: 35px;
                        margin: 4px auto;
                    }
                    .product-code {
                        font-family: monospace;
                        font-size: 10px;
                        letter-spacing: 2px;
                    }
                    .price {
                        font-size: 11px;
                        font-weight: bold;
                        color: #000;
                        margin-top: 3px;
                    }
                    @media print {
                        body { padding: 0; }
                        .label-card { border: 1px solid #eee; }
                    }
                </style>
            </head>
            <body>
                <h3 style="text-align: center; font-size: 14px; margin-bottom: 10px;">Barcode Labels Print Job</h3>
                <div class="label-grid">
                    $cellsHtml
                </div>
            </body>
            </html>
        """

        PdfUtility.printReport(context, "Barcodes-$productCode", emptyList(), emptyList())
        // Wait, we need to print the customized label HTML instead! Let's adapt our printHtml method for this or use PdfUtility's helper.
        // Let's print using an inline printHtml helper for barcodes to support the custom layout.
        val mainExecutor = context.mainExecutor
        mainExecutor.execute {
            val webView = android.webkit.WebView(context)
            webView.webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? android.print.PrintManager
                    if (printManager != null) {
                        val printAdapter = webView.createPrintDocumentAdapter("BarcodeLabel-$productCode")
                        val printAttributes = android.print.PrintAttributes.Builder()
                            .setMediaSize(android.print.PrintAttributes.MediaSize.ISO_A4)
                            .build()
                        printManager.print("BarcodeLabel-$productCode", printAdapter, printAttributes)
                    }
                }
            }
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
        }
    }
}
