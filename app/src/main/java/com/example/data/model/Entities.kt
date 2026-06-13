package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val name: String,
    val campus: String,
    val phoneNumber: String,
    val isOrganizer: Boolean = false,
    val balance: Double = 0.0
)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val date: String,
    val time: String,
    val location: String,
    val priceKes: Double,
    val imageUrl: String,
    val organizerId: String,
    val organizerName: String,
    val capacity: Int,
    val ticketsSold: Int = 0
)

@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val eventTitle: String,
    val eventDate: String,
    val eventLocation: String,
    val buyerId: String,
    val buyerName: String,
    val pricePaid: Double,
    val mpesaReceipt: String,
    val qrCodePayload: String, // Verification Token
    val status: String, // "VALID", "SCANNED"
    val purchasedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
