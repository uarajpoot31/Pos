package com.example.ui.screens

import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Product
import com.example.ui.PosViewModel
import com.example.ui.components.BarcodeGenerator
import com.example.ui.components.BarcodeGenerator.BarcodeView

@Composable
fun ProductManagementScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsState()
    val searchQuery by viewModel.productSearchQuery.collectAsState()
    val selectedCategory by viewModel.productCategoryFilter.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var viewingBarcodeProduct by remember { mutableStateOf<Product?>(null) }

    // Categories
    val categories = listOf("All", "Books", "Pens & Pencils", "Notebooks", "Geometry Boxes", "Art Supplies", "Office Stationery", "Other")

    // Sort products logically
    val filteredProducts = products.filter {
        val matchesSearch = it.name.contains(searchQuery, ignoreCase = true) ||
                it.code.contains(searchQuery, ignoreCase = true) ||
                it.supplierName.contains(searchQuery, ignoreCase = true)
        val matchesFilter = selectedCategory == "All" || it.category.equals(selectedCategory, ignoreCase = true)
        matchesSearch && matchesFilter
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("product_management_root")
    ) {
        // Actions Header Row (Search, Filter, Export, Import)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Inventory Directory",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
            FilledTonalButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_product_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Entry")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        // Search and Filters bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.productSearchQuery.value = it },
            placeholder = { Text("Search by name or barcode...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("inventory_search_input")
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal scrolling for Category Filters
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { viewModel.productCategoryFilter.value = cat },
                    label = { Text(cat) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Import/Export Quick Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier
                    .weight(1.0f)
                    .clickable {
                        val csvData = viewModel.exportProductsToCsv()
                        android.print.PrintAttributes.Builder() // or simple clipboard
                        copyToClipboard(context, csvData)
                        Toast
                            .makeText(context, "Products exported and copied to clipboard as CSV/Excel format!", Toast.LENGTH_LONG)
                            .show()
                    }
                    .padding(vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Export")
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Export Excel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Save CSV structure", fontSize = 10.sp, color = Color.DarkGray)
                    }
                }
            }

            // CSV Import card
            var showImportDialog by remember { mutableStateOf(false) }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                modifier = Modifier
                    .weight(1.0f)
                    .clickable { showImportDialog = true }
                    .padding(vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Import")
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Import Excel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Bulk Upload CSV", fontSize = 10.sp, color = Color.DarkGray)
                    }
                }
            }

            if (showImportDialog) {
                var pastedCsv by remember { mutableStateOf("") }
                Dialog(onDismissRequest = { showImportDialog = false }) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Bulk Product Import", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Format: Name, Barcode, Category, PurchaseRate, SaleRate, Stock, MinimumStock, Supplier",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = pastedCsv,
                                onValueChange = { pastedCsv = it },
                                placeholder = { Text("Paste CSV contents here...") },
                                minLines = 5,
                                maxLines = 8,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(onClick = { showImportDialog = false }) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = {
                                        val ok = viewModel.importProductsFromCsv(pastedCsv)
                                        if (ok) showImportDialog = false
                                    }
                                ) {
                                    Text("Import Bulk")
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Product list table rows
        if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Empty",
                        modifier = Modifier.size(48.dp),
                        tint = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No products in directory", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredProducts) { product ->
                    ProductRowCard(
                        product = product,
                        onEdit = { editingProduct = product },
                        onDelete = { viewModel.deleteProduct(product) },
                        onBarcode = { viewingBarcodeProduct = product }
                    )
                }
            }
        }
    }

    // Add Product Dialog
    if (showAddDialog) {
        ProductFormDialog(
            title = "Add Product Entry",
            onDismiss = { showAddDialog = false },
            onSubmit = { n, c, cat, pr, sr, stock, ms, sup ->
                viewModel.addProduct(n, c, cat, pr, sr, stock, ms, sup)
                showAddDialog = false
            }
        )
    }

    // Edit Product Dialog
    editingProduct?.let { product ->
        ProductFormDialog(
            title = "Edit ${product.name}",
            product = product,
            onDismiss = { editingProduct = null },
            onSubmit = { n, c, cat, pr, sr, stock, ms, sup ->
                viewModel.updateProduct(
                    product.copy(
                        name = n, code = c, category = cat,
                        purchaseRate = pr, saleRate = sr, currentStock = stock,
                        minimumStockLevel = ms, supplierName = sup
                    )
                )
                editingProduct = null
            }
        )
    }

    // Barcode Viewer Dialog
    viewingBarcodeProduct?.let { p ->
        var printQuantityInput by remember { mutableStateOf("12") }
        Dialog(onDismissRequest = { viewingBarcodeProduct = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Barcode Label Generator",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(15.dp))
                    Text(text = p.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "Code: " + p.code, fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    BarcodeView(
                        text = p.code,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = printQuantityInput,
                        onValueChange = { printQuantityInput = it },
                        label = { Text("Label Sheet Count") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { viewingBarcodeProduct = null }) {
                            Text("Close")
                        }
                        Button(
                            onClick = {
                                val qty = printQuantityInput.toIntOrNull() ?: 12
                                BarcodeGenerator.printBarcodeLabels(
                                    context = context,
                                    productName = p.name,
                                    productCode = p.code,
                                    price = p.saleRate,
                                    quantityToPrint = qty
                                )
                                viewingBarcodeProduct = null
                            }
                        ) {
                            Text("Print Labels")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductRowCard(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBarcode: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(product.category, fontSize = 9.sp, modifier = Modifier.padding(2.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Code: ${product.code} | Cap: ${product.supplierName}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text("Pur. Rate", fontSize = 10.sp, color = Color.Gray)
                        Text("Rs. ${product.purchaseRate}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Column {
                        Text("Sale Rate", fontSize = 10.sp, color = Color.Gray)
                        Text("Rs. ${product.saleRate}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Column {
                        Text("Profit/Item", fontSize = 10.sp, color = Color.Gray)
                        Text("Rs. ${product.saleRate - product.purchaseRate}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF137333))
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Stock Badge
                val isLow = product.currentStock <= product.minimumStockLevel
                val containerColor = if (isLow) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                val textColor = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(containerColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Stock: ${product.currentStock}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }

                IconButton(onClick = { showMenu = !showMenu }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Product") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Edit") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Generate Barcode") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = "Barcode") },
                            onClick = {
                                showMenu = false
                                onBarcode()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Product", color = Color.Red) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductFormDialog(
    title: String,
    product: Product? = null,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, Double, Double, Int, Int, String) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var code by remember { mutableStateOf(product?.code ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "Books") }
    var purchaseRate by remember { mutableStateOf(product?.purchaseRate?.toString() ?: "") }
    var saleRate by remember { mutableStateOf(product?.saleRate?.toString() ?: "") }
    var currentStock by remember { mutableStateOf(product?.currentStock?.toString() ?: "") }
    var minimumStockLevel by remember { mutableStateOf(product?.minimumStockLevel?.toString() ?: "5") }
    var supplierName by remember { mutableStateOf(product?.supplierName ?: "") }

    val categories = listOf("Books", "Pens & Pencils", "Notebooks", "Geometry Boxes", "Art Supplies", "Office Stationery", "Other")
    var catExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(10.dp))
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Product Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Product Code / Barcode *") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { code = (100000..999999).random().toString() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Generate Code")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = {
                                IconButton(onClick = { catExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = catExpanded,
                            onDismissRequest = { catExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        catExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = purchaseRate,
                            onValueChange = { purchaseRate = it },
                            label = { Text("Purchase Rate *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = saleRate,
                            onValueChange = { saleRate = it },
                            label = { Text("Sale Rate *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = currentStock,
                            onValueChange = { currentStock = it },
                            label = { Text("Initial Stock") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = minimumStockLevel,
                            onValueChange = { minimumStockLevel = it },
                            label = { Text("Min Stock Alert") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = supplierName,
                        onValueChange = { supplierName = it },
                        label = { Text("Supplier Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val pRate = purchaseRate.toDoubleOrNull() ?: 0.0
                                val sRate = saleRate.toDoubleOrNull() ?: 0.0
                                val stock = currentStock.toIntOrNull() ?: 0
                                val minStock = minimumStockLevel.toIntOrNull() ?: 5
                                onSubmit(name, code, category, pRate, sRate, stock, minStock, supplierName)
                            }
                        ) {
                            Text("Submit")
                        }
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText("POS Backups", text)
    clipboard.setPrimaryClip(clip)
}
