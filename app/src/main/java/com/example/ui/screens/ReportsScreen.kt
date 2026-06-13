package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.data.Purchase
import com.example.data.Sale
import com.example.ui.PosViewModel
import com.example.ui.components.PdfUtility
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportsScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val purchases by viewModel.purchases.collectAsState()

    var activeReportType by remember { mutableStateOf("Sales") }

    val reportTypes = listOf("Sales", "Profits", "Purchases", "Stock Alerts")

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("reports_root")
    ) {
        Text(
            text = "Reports & Analytics Module",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal Selection tabs for Reports
        ScrollableTabRow(
            selectedTabIndex = reportTypes.indexOf(activeReportType).coerceAtLeast(0),
            modifier = Modifier.fillMaxWidth()
        ) {
            reportTypes.forEach { type ->
                Tab(
                    selected = activeReportType == type,
                    onClick = { activeReportType = type },
                    text = { Text(type, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons for CSV Export & PDF Print
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    triggerPrintReport(context, activeReportType, products, sales, purchases)
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Print")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Print Report (PDF)")
            }

            ElevatedButton(
                onClick = {
                    val csvStr = generateReportCsv(activeReportType, products, sales, purchases)
                    copyToClipboard(context, csvStr)
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Excel")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export Excel (CSV)")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Scrollable Report Viewer Port
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                item {
                    Text(
                        text = "$activeReportType Report Log Records",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                when (activeReportType) {
                    "Sales" -> {
                        if (sales.isEmpty()) {
                            item { EmptyReportState() }
                        } else {
                            item {
                                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Invoice No & Date", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("Net Paid Total", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(end = 4.dp))
                                }
                                HorizontalDivider()
                            }
                            items(sales) { s ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(s.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(sdf.format(Date(s.saleDate)) + " | " + s.customerName, fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Text("Rs. ${s.totalAmount}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF137333))
                                }
                                HorizontalDivider(thickness = 0.5.dp)
                            }
                        }
                    }

                    "Profits" -> {
                        val totalProf = sales.sumOf { it.profitAmount }
                        if (sales.isEmpty()) {
                            item { EmptyReportState() }
                        } else {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Accumulated Gross Profit Valuation", fontSize = 11.sp)
                                        Text("Rs. ${String.format("%.1f", totalProf)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF137333))
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Sales Invoice", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("Calculated Margin Value", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                HorizontalDivider()
                            }
                            items(sales) { s ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(s.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Subtotal: Rs. ${s.subTotalAmount}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Text("+ Rs. ${s.profitAmount}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF137333))
                                }
                                HorizontalDivider(thickness = 0.5.dp)
                            }
                        }
                    }

                    "Purchases" -> {
                        if (purchases.isEmpty()) {
                            item { EmptyReportState() }
                        } else {
                            item {
                                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Product & Code", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("Purch Cost", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                HorizontalDivider()
                            }
                            items(purchases) { p ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(p.productName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Inv: ${p.invoiceNumber} | Supplier: ${p.supplierName}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Text("Rs. ${p.productQuantity * p.purchasePrice}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                                HorizontalDivider(thickness = 0.5.dp)
                            }
                        }
                    }

                    "Stock Alerts" -> {
                        val lowStock = products.filter { it.currentStock <= it.minimumStockLevel }
                        if (lowStock.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Safe", tint = Color(0xFF137333), modifier = Modifier.size(40.dp))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("All item stocks are within safe levels!", fontSize = 13.sp, color = Color.Gray)
                                    }
                                }
                            }
                        } else {
                            item {
                                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Product Name & Barcode", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("Curr vs Min Level", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                HorizontalDivider()
                            }
                            items(lowStock) { p ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("⚠ ${p.name}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                        Text("Barcode: ${p.code} | Cap: ${p.supplierName}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Text("${p.currentStock} (Min: ${p.minimumStockLevel})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Red)
                                }
                                HorizontalDivider(thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyReportState() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Info, contentDescription = "None", tint = Color.LightGray, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text("No log transactions found for report", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

private fun triggerPrintReport(
    context: Context,
    reportType: String,
    products: List<Product>,
    sales: List<Sale>,
    purchases: List<Purchase>
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    when (reportType) {
        "Sales" -> {
            val headers = listOf("Invoice Number", "Date", "Customer Name", "Total Paid")
            val rows = sales.map { s ->
                listOf(s.invoiceNumber, sdf.format(Date(s.saleDate)), s.customerName, "Rs. ${s.totalAmount}")
            }
            val summary = mapOf(
                "Total Invoices Issued" to "${sales.size}",
                "Total Net Cash Sales" to "Rs. ${sales.sumOf { it.totalAmount }}"
            )
            PdfUtility.printReport(context, "Sales Activity Log", headers, rows, summary)
        }

        "Profits" -> {
            val headers = listOf("Invoice No", "Customer", "Subtotal Cost", "Net Recv. Total", "Net Profit Margin")
            val rows = sales.map { s ->
                listOf(s.invoiceNumber, s.customerName, "Rs. ${s.subTotalAmount}", "Rs. ${s.totalAmount}", "Rs. ${s.profitAmount}")
            }
            val summary = mapOf(
                "Total Profit Earned" to "Rs. ${sales.sumOf { it.profitAmount }}"
            )
            PdfUtility.printReport(context, "Profit Valuation Ledger", headers, rows, summary)
        }

        "Purchases" -> {
            val headers = listOf("Invoice ID", "Supplier Name", "Product Name", "Bar quantity", "Purchase Total")
            val rows = purchases.map { p ->
                listOf(p.invoiceNumber, p.supplierName, p.productName, "${p.productQuantity}", "Rs. ${p.productQuantity * p.purchasePrice}")
            }
            val summary = mapOf(
                "Purchases Recorded" to "${purchases.size}",
                "Total Invested Outflow" to "Rs. ${purchases.sumOf { it.productQuantity * it.purchasePrice }}"
            )
            PdfUtility.printReport(context, "Supplier Outflow Ledger", headers, rows, summary)
        }

        "Stock Alerts" -> {
            val lowStock = products.filter { it.currentStock <= it.minimumStockLevel }
            val headers = listOf("Barcode", "Product Name", "Category", "Supplier", "Stock Level", "Min Requirement")
            val rows = lowStock.map { p ->
                listOf(p.code, p.name, p.category, p.supplierName, "${p.currentStock}", "${p.minimumStockLevel}")
            }
            val summary = mapOf(
                "Low Stock Warnings" to "${lowStock.size} Alert Flags active"
            )
            PdfUtility.printReport(context, "Inventory Low Stock Alerts", headers, rows, summary)
        }
    }
}

private fun generateReportCsv(
    reportType: String,
    products: List<Product>,
    sales: List<Sale>,
    purchases: List<Purchase>
): String {
    val sb = StringBuilder()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    when (reportType) {
        "Sales" -> {
            sb.append("Invoice,Date,Customer,Total Amount\n")
            sales.forEach { s ->
                sb.append("${s.invoiceNumber},${sdf.format(Date(s.saleDate))},\"${s.customerName}\",${s.totalAmount}\n")
            }
        }
        "Profits" -> {
            sb.append("Invoice,Customer,Subtotal,Discount,Net Revenue,Profit\n")
            sales.forEach { s ->
                sb.append("${s.invoiceNumber},\"${s.customerName}\",${s.subTotalAmount},${s.discountAmount},${s.totalAmount},${s.profitAmount}\n")
            }
        }
        "Purchases" -> {
            sb.append("Invoice,Supplier,Product,Quantity,Purchase Rate,Total Cost\n")
            purchases.forEach { p ->
                sb.append("${p.invoiceNumber},\"${p.supplierName}\",\"${p.productName}\",${p.productQuantity},${p.purchasePrice},${p.productQuantity * p.purchasePrice}\n")
            }
        }
        "Stock Alerts" -> {
            sb.append("Barcode,Product Name,Category,Supplier,Current Stock,Min Stock\n")
            products.filter { it.currentStock <= it.minimumStockLevel }.forEach { p ->
                sb.append("${p.code},\"${p.name}\",\"${p.category}\",\"${p.supplierName}\",${p.currentStock},${p.minimumStockLevel}\n")
            }
        }
    }
    return sb.toString()
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText("POS Report CSV", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Report CSV copied to clipboard!", Toast.LENGTH_SHORT).show()
}
