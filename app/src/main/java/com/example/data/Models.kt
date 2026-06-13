package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "products")
@JsonClass(generateAdapter = true)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val code: String,
    val category: String,
    val purchaseRate: Double,
    val saleRate: Double,
    val currentStock: Int,
    val minimumStockLevel: Int,
    val supplierName: String,
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "purchases")
@JsonClass(generateAdapter = true)
data class Purchase(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val supplierName: String,
    val invoiceNumber: String,
    val purchaseDate: Long,
    val productName: String,
    val productCode: String,
    val productQuantity: Int,
    val purchasePrice: Double,
    val dateRecorded: Long = System.currentTimeMillis()
)

@Entity(tableName = "sales")
@JsonClass(generateAdapter = true)
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val invoiceNumber: String,
    val saleDate: Long,
    val subTotalAmount: Double,
    val discountAmount: Double,
    val totalAmount: Double,
    val profitAmount: Double,
    val cashReceived: Double = 0.0,
    val cashChange: Double = 0.0,
    val customerName: String = "Walk-in Customer"
)

@Entity(tableName = "sale_items")
@JsonClass(generateAdapter = true)
data class SaleItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val saleId: Int,
    val productCode: String,
    val productName: String,
    val quantity: Int,
    val purchaseRate: Double,
    val saleRate: Double
)

@JsonClass(generateAdapter = true)
data class BackupPayload(
    val products: List<Product>,
    val purchases: List<Purchase>,
    val sales: List<Sale>,
    val saleItems: List<SaleItem>
)

