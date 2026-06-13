package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Product
import com.example.data.Sale
import com.example.ui.CartItem
import com.example.ui.PosViewModel
import com.example.ui.components.PdfUtility
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SalesScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val sales by viewModel.sales.collectAsState()

    val posSearchQuery by viewModel.posSearchQuery.collectAsState()
    val barcodeInput by viewModel.posBarcodeScannerInput.collectAsState()

    val discount by viewModel.discountAmount.collectAsState()
    val customerName by viewModel.walkInCustomerName.collectAsState()
    val cashRecv by viewModel.cashReceived.collectAsState()

    // Dialog state for completed receipt printing
    var completedSaleDialogForPrint by remember { mutableStateOf<Sale?>(null) }
    var selectedSaleToReprint by remember { mutableStateOf<Sale?>(null) }

    // Calc aggregates
    val subtotal = cart.sumOf { it.totalSaleValue }
    val totalPayable = (subtotal - discount).coerceAtLeast(0.0)
    val balanceChange = (cashRecv - totalPayable).coerceAtLeast(0.0)

    val searchResults = products.filter {
        posSearchQuery.isNotEmpty() && (
                it.name.contains(posSearchQuery, ignoreCase = true) ||
                        it.code.contains(posSearchQuery, ignoreCase = true)
                )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
            .testTag("pos_billing_root")
    ) {
        // Upper POS panel (Split Scan vs Search)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Barcode Input
                    OutlinedTextField(
                        value = barcodeInput,
                        onValueChange = { viewModel.posBarcodeScannerInput.value = it },
                        label = { Text("Scanner Barcode Input") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Scanner") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                viewModel.handleBarcodeScan(barcodeInput)
                            }
                        ),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("barcode_scanner_input")
                    )
                    Button(
                        onClick = { viewModel.handleBarcodeScan(barcodeInput) },
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Text("Scan")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Manual search fallback
                OutlinedTextField(
                    value = posSearchQuery,
                    onValueChange = { viewModel.posSearchQuery.value = it },
                    placeholder = { Text("Search product name to add manually...") },
                    leadingIcon = { Icon(Icons.Default.List, contentDescription = "Product Tag") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pos_manual_search_input")
                )

                // Search Results
                if (searchResults.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(8.dp)
                    ) {
                        LazyColumn {
                            items(searchResults) { prod ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addProductToCart(prod)
                                            viewModel.posSearchQuery.value = "" // Reset
                                        }
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${prod.name} (${prod.code})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Rs. ${prod.saleRate} (stock: ${prod.currentStock})", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                }
                                HorizontalDivider(thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        // Left Side / Center: Cart Items
        Text(
            text = "Active Billing Items (${cart.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))

        if (cart.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Empty Basket",
                        modifier = Modifier.size(48.dp),
                        tint = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("No billing items added", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(cart) { item ->
                    CartRowItem(
                        item = item,
                        onUpdateQty = { newQty -> viewModel.updateCartQuantity(item.product, newQty) },
                        onRemove = { viewModel.removeProductFromCart(item.product) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Billing Summary & Checkout Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Customer Name Box
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { viewModel.walkInCustomerName.value = it },
                        label = { Text("Customer Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    // Discount Box
                    OutlinedTextField(
                        value = if (discount == 0.0) "" else discount.toString(),
                        onValueChange = { viewModel.discountAmount.value = it.toDoubleOrNull() ?: 0.0 },
                        label = { Text("Disc (Rs)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Price Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtotal", fontSize = 13.sp, color = Color.DarkGray)
                    Text("Rs. $subtotal", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Discount Applied", fontSize = 13.sp, color = Color.DarkGray)
                    Text("- Rs. $discount", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Net Total Due", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Rs. $totalPayable", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(8.dp))
                // Payment Change Calculator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = if (cashRecv == 0.0) "" else cashRecv.toString(),
                        onValueChange = { viewModel.cashReceived.value = it.toDoubleOrNull() ?: 0.0 },
                        label = { Text("Received Cash") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                    ) {
                        Text("Balance Change", fontSize = 11.sp, color = Color.Gray)
                        Text("Rs. $balanceChange", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF137333))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.clearCart() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        modifier = Modifier.weight(0.7f)
                    ) {
                        Text("Reset Cart")
                    }

                    Button(
                        onClick = {
                            viewModel.checkoutActiveSale(onSuccess = { createdSale ->
                                completedSaleDialogForPrint = createdSale
                            })
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("checkout_invoice_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Checkout")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Checkout & Bill")
                    }
                }
            }
        }

        // Quick bottom collapse History to reprint previous bills
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Reprint Existing Receipts/Bills",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sales.take(8)) { sale ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .clickable { selectedSaleToReprint = sale }
                        .padding(horizontal = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.List, contentDescription = "Reprint", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(sale.invoiceNumber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Checkout Complete Direct Print Dialog
    completedSaleDialogForPrint?.let { sale ->
        AlertDialog(
            onDismissRequest = { completedSaleDialogForPrint = null },
            title = { Text("Checkout Successful") },
            text = { Text("Invoice ${sale.invoiceNumber} recorded. Would you like to print the customer receipt?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.viewModelScope.launch {
                            val retrievedItems = viewModel.getSaleItemsForSale(sale.id)
                            PdfUtility.printReceipt(
                                context = context,
                                sale = sale,
                                items = retrievedItems,
                                shopName = viewModel.shopName,
                                owner = viewModel.ownerName,
                                phone = viewModel.phoneNumber,
                                address = viewModel.shopAddress,
                                footer = viewModel.footerText
                            )
                        }
                        completedSaleDialogForPrint = null
                    }
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Print")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Print Receipt")
                }
            },
            dismissButton = {
                TextButton(onClick = { completedSaleDialogForPrint = null }) {
                    Text("Done")
                }
            }
        )
    }

    // Reprint Bill Dialog
    selectedSaleToReprint?.let { sale ->
        AlertDialog(
            onDismissRequest = { selectedSaleToReprint = null },
            title = { Text("Reprint Document") },
            text = { Text("Invoice: ${sale.invoiceNumber}\nDate: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(sale.saleDate))}\nTotal: Rs. ${sale.totalAmount}\n\nDo you want to print or view this receipt?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.viewModelScope.launch {
                            val itemsList = viewModel.getSaleItemsForSale(sale.id)
                            PdfUtility.printReceipt(
                                context = context,
                                sale = sale,
                                items = itemsList,
                                shopName = viewModel.shopName,
                                owner = viewModel.ownerName,
                                phone = viewModel.phoneNumber,
                                address = viewModel.shopAddress,
                                footer = viewModel.footerText
                            )
                        }
                        selectedSaleToReprint = null
                    }
                ) {
                    Text("Print")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedSaleToReprint = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Row padding helper since modify modifier2 is not a real companion
private fun Modifier.padding(vertical: Int) = this.padding(vertical = vertical.dp)

@Composable
fun CartRowItem(
    item: CartItem,
    onUpdateQty: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    text = item.product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Rs. ${item.product.saleRate} | Code: ${item.product.code}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Total Item Cost: Rs. ${item.totalSaleValue}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Qty controller
            Row(
                modifier = Modifier.weight(0.8f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { onUpdateQty(item.quantity - 1) }) {
                    Text("-", fontWeight = FontWeight.Black, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = "${item.quantity}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(onClick = { onUpdateQty(item.quantity + 1) }) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = Color.Red,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
