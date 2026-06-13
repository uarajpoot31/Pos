package com.example.ui.components

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.Sale
import com.example.data.SaleItem
import java.text.SimpleDateFormat
import java.util.*

object PdfUtility {

    fun printReceipt(
        context: Context,
        sale: Sale,
        items: List<SaleItem>,
        shopName: String,
        owner: String,
        phone: String,
        address: String,
        footer: String
    ) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateStr = sdf.format(Date(sale.saleDate))

        val htmlBuilder = StringBuilder()
        htmlBuilder.append("""
            <html>
            <head>
                <style>
                    body {
                        font-family: 'Courier New', Courier, monospace;
                        font-size: 13px;
                        line-height: 1.4;
                        color: #000;
                        padding: 10px;
                        margin: 0;
                        max-width: 320px;
                    }
                    .text-center { text-align: center; }
                    .text-right { text-align: right; }
                    .header { margin-bottom: 12px; }
                    .header h1 { font-size: 18px; margin: 0 0 4px 0; text-transform: uppercase; }
                    .header p { margin: 2px 0; font-size: 12px; }
                    .divider { border-bottom: 1px dashed #000; margin: 10px 0; }
                    .item-table { width: 100%; border-collapse: collapse; margin-bottom: 8px; }
                    .item-table th, .item-table td { font-size: 11px; padding: 3px 0; }
                    .item-table th { border-bottom: 1px solid #000; text-align: left; }
                    .total-box { margin-top: 8px; font-size: 12px; }
                    .total-row { display: flex; justify-content: space-between; padding: 2px 0; }
                    .footer { text-align: center; font-size: 10px; margin-top: 15px; }
                </style>
            </head>
            <body>
                <div class="header text-center">
                    <h1>$shopName</h1>
                    <p>Prop: $owner</p>
                    <p>Mob: $phone</p>
                    <p>$address</p>
                </div>
                <div class="divider"></div>
                <p><strong>Inv #:</strong> ${sale.invoiceNumber}</p>
                <p><strong>Date:</strong> $dateStr</p>
                <p><strong>Cust:</strong> ${sale.customerName}</p>
                <div class="divider"></div>
                
                <table class="item-table">
                    <thead>
                        <tr>
                            <th style="width: 50%;">Item</th>
                            <th style="width: 15%; text-align: center;">Qty</th>
                            <th style="width: 15%; text-align: right;">Rate</th>
                            <th style="width: 20%; text-align: right;">Total</th>
                        </tr>
                    </thead>
                    <tbody>
        """.trimIndent())

        for (item in items) {
            val totalItemCost = item.saleRate * item.quantity
            htmlBuilder.append("""
                <tr>
                    <td>${item.productName}</td>
                    <td style="text-align: center;">${item.quantity}</td>
                    <td style="text-align: right;">${item.saleRate}</td>
                    <td style="text-align: right;">$totalItemCost</td>
                </tr>
            """)
        }

        htmlBuilder.append("""
                    </tbody>
                </table>
                <div class="divider"></div>
                
                <div class="total-box">
                    <div class="total-row">
                        <span>Subtotal:</span>
                        <span>Rs. ${sale.subTotalAmount}</span>
                    </div>
                    <div class="total-row">
                        <span>Discount:</span>
                        <span>Rs. ${sale.discountAmount}</span>
                    </div>
                    <div class="total-row" style="font-weight: bold; font-size: 14px; border-top: 1px solid #000; padding-top: 4px;">
                        <span>Net Payable:</span>
                        <span>Rs. ${sale.totalAmount}</span>
                    </div>
                    <div class="total-row" style="margin-top: 4px;">
                        <span>Cash Received:</span>
                        <span>Rs. ${sale.cashReceived}</span>
                    </div>
                    <div class="total-row">
                        <span>Balance Change:</span>
                        <span>Rs. ${sale.cashChange}</span>
                    </div>
                </div>
                
                <div class="divider"></div>
                <div class="footer">
                    <p>$footer</p>
                    <p>Thank you for shopping with us!</p>
                </div>
            </body>
            </html>
        """)

        printHtml(context, "Receipt-${sale.invoiceNumber}", htmlBuilder.toString())
    }

    fun printReport(
        context: Context,
        reportTitle: String,
        headers: List<String>,
        rows: List<List<String>>,
        summary: Map<String, String> = emptyMap()
    ) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val generatedDate = sdf.format(Date())

        val htmlBuilder = StringBuilder()
        htmlBuilder.append("""
            <html>
            <head>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        font-size: 12px;
                        color: #333;
                        padding: 15px;
                        margin: 0;
                    }
                    .text-center { text-align: center; }
                    .text-right { text-align: right; }
                    .header { margin-bottom: 20px; border-bottom: 2px solid #555; padding-bottom: 10px; }
                    .header h1 { font-size: 20px; margin: 0 0 5px 0; color: #1a73e8; }
                    .header p { margin: 2px 0; font-size: 12px; font-weight: bold; }
                    .report-table { width: 100%; border-collapse: collapse; margin-top: 10px; margin-bottom: 15px; }
                    .report-table th, .report-table td { border: 1px solid #ddd; padding: 8px; font-size: 11px; text-align: left; }
                    .report-table th { background-color: #f1f3f4; color: #333; font-weight: bold; }
                    .report-table tr:nth-child(even) { background-color: #fafafa; }
                    .summary-box { background-color: #e8f0fe; padding: 12px; border-radius: 4px; display: inline-block; min-width: 250px; margin-top: 10px; }
                    .summary-row { display: flex; justify-content: space-between; border-bottom: 1px solid #c3ecf6; padding: 4px 0; margin-bottom: 4px; }
                    .footer { text-align: center; font-size: 10px; margin-top: 30px; border-top: 1px solid #ddd; padding-top: 10px; color: #777; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>AL ILM BOOK DEPOT</h1>
                    <p>Professional Business Reports</p>
                    <div style="display: flex; justify-content: space-between; font-weight: normal; margin-top: 5px; color: #555;">
                        <span><strong>Report:</strong> $reportTitle</span>
                        <span><strong>Generated:</strong> $generatedDate</span>
                    </div>
                </div>
                
                <table class="report-table">
                    <thead>
                        <tr>
        """.trimIndent())

        for (h in headers) {
            htmlBuilder.append("<th>$h</th>")
        }
        htmlBuilder.append("</tr></thead><tbody>")

        for (row in rows) {
            htmlBuilder.append("<tr>")
            for (cell in row) {
                htmlBuilder.append("<td>$cell</td>")
            }
            htmlBuilder.append("</tr>")
        }

        htmlBuilder.append("</tbody></table>")

        if (summary.isNotEmpty()) {
            htmlBuilder.append("""<div class="summary-box">""")
            for ((k, v) in summary) {
                htmlBuilder.append("""
                    <div class="summary-row">
                        <strong>$k:</strong>
                        <span>$v</span>
                    </div>
                """)
            }
            htmlBuilder.append("</div>")
        }

        htmlBuilder.append("""
                <div class="footer">
                    <p>AL ILM BOOK DEPOT &copy; 2026. Made with &hearts; by ARFI. All rights reserved.</p>
                </div>
            </body>
            </html>
        """)

        printHtml(context, "Report-${reportTitle.replace(" ", "_")}", htmlBuilder.toString())
    }

    private fun printHtml(context: Context, docName: String, htmlContent: String) {
        val mainExecutor = context.mainExecutor
        mainExecutor.execute {
            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    // Initialize print manager
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    if (printManager != null) {
                        val printAdapter = webView.createPrintDocumentAdapter(docName)
                        val printAttributes = PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .build()
                        printManager.print(docName, printAdapter, printAttributes)
                    }
                }
            }
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
        }
    }
}
