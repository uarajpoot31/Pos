package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Product
import com.example.data.Purchase
import com.example.ui.PosViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PurchaseScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsState()
    val purchases by viewModel.purchases.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredPurchases = purchases.filter {
        it.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                it.supplierName.contains(searchQuery, ignoreCase = true) ||
                it.productName.contains(searchQuery, ignoreCase = true) ||
                it.productCode.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("purchases_root")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Purchase Module",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
            FilledTonalButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("record_purchase_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Purchase")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        // Search purchases
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search purchase history (invoice, product, supplier)...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Purchases History",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (filteredPurchases.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "No receipts",
                        modifier = Modifier.size(48.dp),
                        tint = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No purchase transactions found", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredPurchases) { purchase ->
                    PurchaseRowItem(purchase = purchase, onDelete = { viewModel.deletePurchase(purchase) })
                }
            }
        }
    }

    // New Purchase form dialog
    if (showAddDialog) {
        var supplierName by remember { mutableStateOf("") }
        var invoiceNumber by remember { mutableStateOf("") }
        var productCode by remember { mutableStateOf("") }
        var quantityInput by remember { mutableStateOf("") }
        var priceInput by remember { mutableStateOf("") }

        var suggestionsExpanded by remember { mutableStateOf(false) }

        val matchedProduct = products.find { it.code == productCode }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Add Stock & Record Purchase",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Supplier name
                    OutlinedTextField(
                        value = supplierName,
                        onValueChange = { supplierName = it },
                        label = { Text("Supplier Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Invoice number
                    OutlinedTextField(
                        value = invoiceNumber,
                        onValueChange = { invoiceNumber = it },
                        label = { Text("Invoice Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Product Code selection with recommendations
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = productCode,
                            onValueChange = {
                                productCode = it
                                suggestionsExpanded = true
                            },
                            label = { Text("Product Code / Barcode *") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { suggestionsExpanded = !suggestionsExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Products")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        DropdownMenu(
                            expanded = suggestionsExpanded && products.isNotEmpty(),
                            onDismissRequest = { suggestionsExpanded = false }
                        ) {
                            products.filter {
                                productCode.isBlank() ||
                                        it.name.contains(productCode, ignoreCase = true) ||
                                        it.code.contains(productCode, ignoreCase = true)
                            }.take(5).forEach { prod ->
                                DropdownMenuItem(
                                    text = { Text("${prod.name} (${prod.code})") },
                                    onClick = {
                                        productCode = prod.code
                                        priceInput = prod.purchaseRate.toString()
                                        suggestionsExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (matchedProduct != null) {
                        Text(
                            text = "Detected Item: ${matchedProduct.name} | Current Stock: ${matchedProduct.currentStock}",
                            color = Color(0xFF137333),
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = quantityInput,
                            onValueChange = { quantityInput = it },
                            label = { Text("Quantity") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = priceInput,
                            onValueChange = { priceInput = it },
                            label = { Text("Purchase Price") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val qty = quantityInput.toIntOrNull() ?: 0
                                var prc = priceInput.toDoubleOrNull() ?: 0.0
                                if (matchedProduct != null && prc <= 0.0) {
                                    prc = matchedProduct.purchaseRate
                                }

                                if (productCode.isBlank() || qty <= 0) {
                                    viewModel.showToast("Product code and positive quantity are required.")
                                } else {
                                    viewModel.recordPurchase(supplierName, invoiceNumber, productCode, qty, prc)
                                    showAddDialog = false
                                }
                            }
                        ) {
                            Text("Receive Stock")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PurchaseRowItem(
    purchase: Purchase,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = purchase.productName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Invoice: ${purchase.invoiceNumber}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Supplier: ${purchase.supplierName}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(purchase.purchaseDate)),
                    fontSize = 10.sp,
                    color = Color.LightGray
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Qty: +${purchase.productQuantity}",
                    color = Color(0xFF0D47A1),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Total: Rs. ${purchase.productQuantity * purchase.purchasePrice}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
