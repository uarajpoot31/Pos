package com.example.data

import android.content.Context
import androidx.room.withTransaction
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class PosRepository(private val database: AppDatabase) {
    private val productDao = database.productDao()
    private val purchaseDao = database.purchaseDao()
    private val saleDao = database.saleDao()
    private val saleItemDao = database.saleItemDao()

    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val allPurchases: Flow<List<Purchase>> = purchaseDao.getAllPurchases()
    val allSales: Flow<List<Sale>> = saleDao.getAllSales()
    val allSaleItems: Flow<List<SaleItem>> = saleItemDao.getAllSaleItems()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val backupAdapter = moshi.adapter(BackupPayload::class.java)

    // Product methods
    suspend fun getProductById(id: Int): Product? = withContext(Dispatchers.IO) {
        productDao.getProductById(id)
    }

    suspend fun getProductByCode(code: String): Product? = withContext(Dispatchers.IO) {
        productDao.getProductByCode(code)
    }

    suspend fun insertProduct(product: Product): Long = withContext(Dispatchers.IO) {
        productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.deleteProduct(product)
    }

    suspend fun updateStock(productId: Int, newStock: Int) = withContext(Dispatchers.IO) {
        productDao.updateStock(productId, newStock)
    }

    // Purchase methods
    suspend fun insertPurchase(purchase: Purchase): Long = withContext(Dispatchers.IO) {
        database.withTransaction {
            val purchaseId = purchaseDao.insertPurchase(purchase)
            // Auto stock increase
            val product = productDao.getProductByCode(purchase.productCode)
            if (product != null) {
                val newStock = product.currentStock + purchase.productQuantity
                productDao.updateStock(product.id, newStock)
            }
            purchaseId
        }
    }

    suspend fun updatePurchase(oldPurchase: Purchase, newPurchase: Purchase) = withContext(Dispatchers.IO) {
        database.withTransaction {
            // First revert old stock change
            val product = productDao.getProductByCode(oldPurchase.productCode)
            if (product != null) {
                val revertedStock = product.currentStock - oldPurchase.productQuantity
                productDao.updateStock(product.id, revertedStock)
            }

            // Update purchase
            purchaseDao.updatePurchase(newPurchase)

            // Apply new stock change
            val newProduct = productDao.getProductByCode(newPurchase.productCode)
            if (newProduct != null) {
                val updatedStock = newProduct.currentStock + newPurchase.productQuantity
                productDao.updateStock(newProduct.id, updatedStock)
            }
        }
    }

    suspend fun deletePurchase(purchase: Purchase) = withContext(Dispatchers.IO) {
        database.withTransaction {
            // Revert stock change
            val product = productDao.getProductByCode(purchase.productCode)
            if (product != null) {
                val revertedStock = product.currentStock - purchase.productQuantity
                productDao.updateStock(product.id, revertedStock.coerceAtLeast(0))
            }
            purchaseDao.deletePurchase(purchase)
        }
    }

    // Sale methods
    suspend fun checkoutSale(sale: Sale, items: List<SaleItem>): Long = withContext(Dispatchers.IO) {
        database.withTransaction {
            val saleId = saleDao.insertSale(sale)
            for (item in items) {
                val itemWithSaleId = item.copy(saleId = saleId.toInt())
                saleItemDao.insertSaleItem(itemWithSaleId)

                // Auto stock decrease
                val product = productDao.getProductByCode(item.productCode)
                if (product != null) {
                    val newStock = product.currentStock - item.quantity
                    productDao.updateStock(product.id, newStock.coerceAtLeast(0))
                }
            }
            saleId
        }
    }

    suspend fun deleteSale(sale: Sale) = withContext(Dispatchers.IO) {
        database.withTransaction {
            // Restore product stock
            val items = saleItemDao.getSaleItemsForSale(sale.id)
            for (item in items) {
                val product = productDao.getProductByCode(item.productCode)
                if (product != null) {
                    val restoredStock = product.currentStock + item.quantity
                    productDao.updateStock(product.id, restoredStock)
                }
            }
            // Delete items and sale record
            saleItemDao.deleteSaleItemsBySaleId(sale.id)
            saleDao.deleteSale(sale)
        }
    }

    suspend fun getSaleItemsForSale(saleId: Int): List<SaleItem> = withContext(Dispatchers.IO) {
        saleItemDao.getSaleItemsForSale(saleId)
    }

    // Backup & Restore
    suspend fun exportDatabaseToJson(): String = withContext(Dispatchers.IO) {
        val payload = BackupPayload(
            products = productDao.getAllProducts().first(),
            purchases = purchaseDao.getAllPurchases().first(),
            sales = saleDao.getAllSales().first(),
            saleItems = saleItemDao.getAllSaleItems().first()
        )
        backupAdapter.toJson(payload)
    }

    suspend fun importDatabaseFromJson(jsonStr: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = backupAdapter.fromJson(jsonStr) ?: return@withContext false
            database.withTransaction {
                // Clear existing tables
                database.clearAllTables()

                // Insert products
                for (prod in payload.products) {
                    productDao.insertProduct(prod.copy(id = 0)) // Insert fresh to avoid ID matching conflicts if desired, or keep IDs
                }
                // Insert purchases
                for (purch in payload.purchases) {
                    purchaseDao.insertPurchase(purch.copy(id = 0))
                }
                // Insert sales
                for (sale in payload.sales) {
                    val oldId = sale.id
                    val newSaleId = saleDao.insertSale(sale.copy(id = 0))
                    
                    // Remap sale items
                    val items = payload.saleItems.filter { it.saleId == oldId }
                    for (item in items) {
                        saleItemDao.insertSaleItem(item.copy(id = 0, saleId = newSaleId.toInt()))
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Auto-backup to file
    suspend fun triggerAutoBackup(context: Context) = withContext(Dispatchers.IO) {
        try {
            val json = exportDatabaseToJson()
            val backupFile = File(context.filesDir, "auto_backup_pos.json")
            backupFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun restoreFromAutoBackup(context: Context): Boolean {
        return try {
            val backupFile = File(context.filesDir, "auto_backup_pos.json")
            if (backupFile.exists()) {
                val json = backupFile.readText()
                importDatabaseFromJson(json)
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
