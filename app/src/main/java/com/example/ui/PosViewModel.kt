package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class ActiveTab {
    Dashboard,
    POS,
    Products,
    Purchases,
    Reports,
    Settings
}

data class CartItem(
    val product: Product,
    val quantity: Int
) {
    val totalSaleValue: Double get() = product.saleRate * quantity
    val totalPurchaseValue: Double get() = product.purchaseRate * quantity
}

class PosViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = PosRepository(database)

    // UI state streams from DB
    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchases: StateFlow<List<Purchase>> = repository.allPurchases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sales: StateFlow<List<Sale>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val saleItems: StateFlow<List<SaleItem>> = repository.allSaleItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Interactive States
    var currentTab = MutableStateFlow(ActiveTab.Dashboard)
        private set

    // POS Cart State
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    val discountAmount = MutableStateFlow(0.0)
    val walkInCustomerName = MutableStateFlow("Walk-in Customer")
    val cashReceived = MutableStateFlow(0.0)

    // Search and Filter states
    val productSearchQuery = MutableStateFlow("")
    val productCategoryFilter = MutableStateFlow("All")

    val posSearchQuery = MutableStateFlow("")
    val posBarcodeScannerInput = MutableStateFlow("")

    // Notification message
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    // Shop Profile Info
    val shopName = "AL ILM BOOK DEPOT"
    val ownerName = "Rana Sami"
    val phoneNumber = "0308-4361860"
    val shopAddress = "Main Market Bhobatian"
    val footerText = "MADE WITH ❤️ BY ARFI"

    init {
        // Trigger auto-restore if database is completely empty
        viewModelScope.launch {
            products.collect { prods ->
                if (prods.isEmpty()) {
                    // Try to restore from internal backup if it exists
                    repository.restoreFromAutoBackup(getApplication())
                }
            }
        }
    }

    fun selectTab(tab: ActiveTab) {
        currentTab.value = tab
    }

    fun showToast(msg: String) {
        viewModelScope.launch {
            _toastMessage.emit(msg)
        }
    }

    // --- POS Cart Actions ---
    fun addProductToCart(product: Product) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == product.id }

        if (index != -1) {
            val oldItem = currentList[index]
            if (oldItem.quantity >= product.currentStock) {
                showToast("Cannot exceed available stock (${product.currentStock})")
                return
            }
            currentList[index] = oldItem.copy(quantity = oldItem.quantity + 1)
        } else {
            if (product.currentStock <= 0) {
                showToast("Product is out of stock!")
                return
            }
            currentList.add(CartItem(product, 1))
        }
        _cart.value = currentList
        showToast("Added ${product.name} to cart")
    }

    fun handleBarcodeScan(barcode: String) {
        if (barcode.isBlank()) return
        viewModelScope.launch {
            val product = repository.getProductByCode(barcode.trim())
            if (product != null) {
                addProductToCart(product)
                posBarcodeScannerInput.value = ""
            } else {
                showToast("No product found with barcode $barcode")
            }
        }
    }

    fun removeProductFromCart(product: Product) {
        val currentList = _cart.value.filter { it.product.id != product.id }
        _cart.value = currentList
    }

    fun updateCartQuantity(product: Product, quantity: Int) {
        if (quantity <= 0) {
            removeProductFromCart(product)
            return
        }
        if (quantity > product.currentStock) {
            showToast("Only ${product.currentStock} items in stock")
            return
        }
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == product.id }
        if (index != -1) {
            currentList[index] = currentList[index].copy(quantity = quantity)
            _cart.value = currentList
        }
    }

    fun clearCart() {
        _cart.value = emptyList()
        discountAmount.value = 0.0
        walkInCustomerName.value = "Walk-in Customer"
        cashReceived.value = 0.0
    }

    fun checkoutActiveSale(onSuccess: (Sale) -> Unit) {
        val items = _cart.value
        if (items.isEmpty()) {
            showToast("Cart is empty!")
            return
        }

        viewModelScope.launch {
            val subtotal = items.sumOf { it.totalSaleValue }
            val discount = discountAmount.value
            val total = (subtotal - discount).coerceAtLeast(0.0)
            val totalCost = items.sumOf { it.totalPurchaseValue }
            val profit = (total - totalCost).coerceAtLeast(0.0)

            val invoiceNo = "INV-${System.currentTimeMillis().toString().takeLast(6)}"
            val sale = Sale(
                invoiceNumber = invoiceNo,
                saleDate = System.currentTimeMillis(),
                subTotalAmount = subtotal,
                discountAmount = discount,
                totalAmount = total,
                profitAmount = profit,
                cashReceived = cashReceived.value,
                cashChange = (cashReceived.value - total).coerceAtLeast(0.0),
                customerName = walkInCustomerName.value
            )

            val saleItemsList = items.map { item ->
                SaleItem(
                    saleId = 0,
                    productCode = item.product.code,
                    productName = item.product.name,
                    quantity = item.quantity,
                    purchaseRate = item.product.purchaseRate,
                    saleRate = item.product.saleRate
                )
            }

            val saleId = repository.checkoutSale(sale, saleItemsList)
            if (saleId > 0) {
                val completedSale = sale.copy(id = saleId.toInt())
                showToast("Sale successful! Invoice: $invoiceNo")
                clearCart()
                // Auto backup
                repository.triggerAutoBackup(getApplication())
                onSuccess(completedSale)
            } else {
                showToast("Checkout failed. Try again.")
            }
        }
    }

    fun deleteSaleRecord(sale: Sale) {
        viewModelScope.launch {
            repository.deleteSale(sale)
            showToast("Sale ${sale.invoiceNumber} deleted and stock restored")
            repository.triggerAutoBackup(getApplication())
        }
    }

    // --- Product Actions ---
    fun addProduct(
        name: String,
        code: String,
        category: String,
        purchaseRate: Double,
        saleRate: Double,
        currentStock: Int,
        minimumStockLevel: Int,
        supplierName: String
    ) {
        if (name.isBlank() || code.isBlank() || category.isBlank()) {
            showToast("Please fill all required fields")
            return
        }
        viewModelScope.launch {
            // Check barcode duplication
            val existing = repository.getProductByCode(code.trim())
            if (existing != null) {
                showToast("Barcode/Code '$code' already exists for ${existing.name}")
                return@launch
            }

            val product = Product(
                name = name.trim(),
                code = code.trim(),
                category = category.trim(),
                purchaseRate = purchaseRate,
                saleRate = saleRate,
                currentStock = currentStock,
                minimumStockLevel = minimumStockLevel,
                supplierName = supplierName.trim(),
                dateAdded = System.currentTimeMillis()
            )
            repository.insertProduct(product)
            showToast("Product '${product.name}' added successfully")
            repository.triggerAutoBackup(getApplication())
        }
    }

    fun updateProduct(product: Product) {
        if (product.name.isBlank() || product.code.isBlank()) {
            showToast("Name and Code cannot be empty")
            return
        }
        viewModelScope.launch {
            repository.updateProduct(product)
            showToast("Updated ${product.name}")
            repository.triggerAutoBackup(getApplication())
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            showToast("Deleted ${product.name}")
            repository.triggerAutoBackup(getApplication())
        }
    }

    // --- Purchase Actions ---
    fun recordPurchase(
        supplierName: String,
        invoiceNo: String,
        productCode: String,
        quantity: Int,
        purchasePrice: Double
    ) {
        if (productCode.isBlank() || quantity <= 0) {
            showToast("Missing product selection or invalid quantity")
            return
        }
        viewModelScope.launch {
            val product = repository.getProductByCode(productCode)
            if (product == null) {
                showToast("Product with barcode/code $productCode not found. Please add the product first")
                return@launch
            }

            val purchase = Purchase(
                supplierName = supplierName.trim().ifEmpty { "Default Supplier" },
                invoiceNumber = invoiceNo.trim().ifEmpty { "INV-${System.currentTimeMillis().toString().takeLast(6)}" },
                purchaseDate = System.currentTimeMillis(),
                productName = product.name,
                productCode = productCode,
                productQuantity = quantity,
                purchasePrice = if (purchasePrice <= 0.0) product.purchaseRate else purchasePrice
            )

            // Auto increments stock inside repo
            repository.insertPurchase(purchase)
            showToast("Purchase recorded! Stock of ${product.name} increased by $quantity")
            repository.triggerAutoBackup(getApplication())
        }
    }

    fun deletePurchase(purchase: Purchase) {
        viewModelScope.launch {
            repository.deletePurchase(purchase)
            showToast("Purchase deleted. Stock reverted.")
            repository.triggerAutoBackup(getApplication())
        }
    }

    // --- Export Excel (CSV) Functions ---
    fun exportProductsToCsv(): String {
        val prods = products.value
        val sb = java.lang.StringBuilder()
        sb.append("Product Name,Barcode,Category,Purchase Rate,Sale Rate,Current Stock,Minimum Stock,Supplier,Date Added\n")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        for (p in prods) {
            sb.append("\"${escapeCsv(p.name)}\",")
                .append("\"${escapeCsv(p.code)}\",")
                .append("\"${escapeCsv(p.category)}\",")
                .append("${p.purchaseRate},")
                .append("${p.saleRate},")
                .append("${p.currentStock},")
                .append("${p.minimumStockLevel},")
                .append("\"${escapeCsv(p.supplierName)}\",")
                .append("\"${sdf.format(Date(p.dateAdded))}\"\n")
        }
        return sb.toString()
    }

    fun exportSalesToCsv(): String {
        val sls = sales.value
        val sb = java.lang.StringBuilder()
        sb.append("Invoice Number,Date,Customer,Subtotal,Discount,Total Paid,Profit Earned\n")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        for (s in sls) {
            sb.append("\"${escapeCsv(s.invoiceNumber)}\",")
                .append("\"${sdf.format(Date(s.saleDate))}\",")
                .append("\"${escapeCsv(s.customerName)}\",")
                .append("${s.subTotalAmount},")
                .append("${s.discountAmount},")
                .append("${s.totalAmount},")
                .append("${s.profitAmount}\n")
        }
        return sb.toString()
    }

    fun exportPurchasesToCsv(): String {
        val purchs = purchases.value
        val sb = java.lang.StringBuilder()
        sb.append("Invoice ID,Supplier Name,Date,Product Name,Barcode,Quantity,Purchase Rate,Total Amount\n")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        for (p in purchs) {
            sb.append("\"${escapeCsv(p.invoiceNumber)}\",")
                .append("\"${escapeCsv(p.supplierName)}\",")
                .append("\"${sdf.format(Date(p.purchaseDate))}\",")
                .append("\"${escapeCsv(p.productName)}\",")
                .append("\"${escapeCsv(p.productCode)}\",")
                .append("${p.productQuantity},")
                .append("${p.purchasePrice},")
                .append("${p.productQuantity * p.purchasePrice}\n")
        }
        return sb.toString()
    }

    // --- Import Excel (CSV) Functions ---
    fun importProductsFromCsv(csvText: String): Boolean {
        if (csvText.isBlank()) return false
        try {
            val lines = csvText.lines()
            if (lines.size <= 1) return false
            var successCount = 0
            var updateCount = 0

            viewModelScope.launch {
                for (i in 1 until lines.size) {
                    val line = lines[i].trim()
                    if (line.isBlank()) continue
                    val parts = parseCsvLine(line)
                    if (parts.size >= 6) {
                        val name = parts[0].trim()
                        val code = parts[1].trim()
                        val colCategory = parts.getOrNull(2)?.trim() ?: "Stationery"
                        val pRate = parts.getOrNull(3)?.toDoubleOrNull() ?: 0.0
                        val sRate = parts.getOrNull(4)?.toDoubleOrNull() ?: 0.0
                        val stock = parts.getOrNull(5)?.toIntOrNull() ?: 0
                        val minStock = parts.getOrNull(6)?.toIntOrNull() ?: 5
                        val supplier = parts.getOrNull(7)?.trim() ?: "Imported Supplier"

                        if (name.isNotEmpty() && code.isNotEmpty()) {
                            val existing = repository.getProductByCode(code)
                            if (existing != null) {
                                // Overwrite / update parameters
                                repository.updateProduct(
                                    existing.copy(
                                        name = name,
                                        category = colCategory,
                                        purchaseRate = pRate,
                                        saleRate = sRate,
                                        currentStock = stock,
                                        minimumStockLevel = minStock,
                                        supplierName = supplier
                                    )
                                )
                                updateCount++
                            } else {
                                val product = Product(
                                    name = name,
                                    code = code,
                                    category = colCategory,
                                    purchaseRate = pRate,
                                    saleRate = sRate,
                                    currentStock = stock,
                                    minimumStockLevel = minStock,
                                    supplierName = supplier,
                                    dateAdded = System.currentTimeMillis()
                                )
                                repository.insertProduct(product)
                                successCount++
                            }
                        }
                    }
                }
                showToast("Imported successfully! Added $successCount, Updated $updateCount products")
                repository.triggerAutoBackup(getApplication())
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Failed to parse CSV: ${e.message}")
            return false
        }
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var curVal = java.lang.StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (inQuotes) {
                if (ch == '\"') {
                    if (i + 1 < line.length && line[i + 1] == '\"') {
                        curVal.append('\"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    curVal.append(ch)
                }
            } else {
                if (ch == '\"') {
                    inQuotes = true
                } else if (ch == ',') {
                    result.add(curVal.toString())
                    curVal = java.lang.StringBuilder()
                } else {
                    curVal.append(ch)
                }
            }
            i++
        }
        result.add(curVal.toString())
        return result
    }

    // --- Direct JSON Backup & Restore ---
    fun exportBackupJson(): String? {
        var json: String? = null
        val latch = java.util.concurrent.CountDownLatch(1)
        viewModelScope.launch {
            try {
                json = repository.exportDatabaseToJson()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                latch.countDown()
            }
        }
        latch.await()
        return json
    }

    fun importBackupJson(jsonStr: String) {
        viewModelScope.launch {
            val ok = repository.importDatabaseFromJson(jsonStr)
            if (ok) {
                showToast("Backup restored successfully!")
                repository.triggerAutoBackup(getApplication())
            } else {
                showToast("Failed to restore backup. Invalid file format.")
            }
        }
    }

    suspend fun getSaleItemsForSale(saleId: Int): List<com.example.data.SaleItem> {
        return repository.getSaleItemsForSale(saleId)
    }
}
