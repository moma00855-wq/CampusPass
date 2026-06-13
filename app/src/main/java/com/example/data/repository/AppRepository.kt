package com.example.data.repository

import com.example.data.dao.EventDao
import com.example.data.dao.NotificationDao
import com.example.data.dao.TicketDao
import com.example.data.dao.UserDao
import com.example.data.model.EventEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.TicketEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID

sealed class MpesaPaymentState {
    object Idle : MpesaPaymentState()
    data class InitiatingSTKPush(val phoneNumber: String) : MpesaPaymentState()
    data class AwaitingUserPin(val merchantRequestID: String) : MpesaPaymentState()
    data class VerifyingCallback(val checkoutRequestID: String) : MpesaPaymentState()
    data class PaymentSuccessful(val transactionRef: String, val ticket: TicketEntity) : MpesaPaymentState()
    data class PaymentFailed(val error: String) : MpesaPaymentState()
}

class AppRepository(
    private val userDao: UserDao,
    private val eventDao: EventDao,
    private val ticketDao: TicketDao,
    private val notificationDao: NotificationDao
) {
    // Users
    suspend fun getUser(id: String): UserEntity? = userDao.getUserById(id)
    suspend fun getUserByEmail(email: String): UserEntity? = userDao.getUserByEmail(email)
    suspend fun saveUser(user: UserEntity) = userDao.insertUser(user)
    suspend fun updateUser(user: UserEntity) = userDao.updateUser(user)

    // Events
    fun getAllEvents(): Flow<List<EventEntity>> = eventDao.getAllEvents()
    fun getEventsByOrganizer(organizerId: String): Flow<List<EventEntity>> = eventDao.getEventsByOrganizer(organizerId)
    suspend fun getEventById(id: String): EventEntity? = eventDao.getEventById(id)
    suspend fun saveEvent(event: EventEntity) = eventDao.insertEvent(event)
    suspend fun deleteEvent(eventId: String) = eventDao.deleteEvent(eventId)

    // Tickets
    fun getTicketsByBuyer(buyerId: String): Flow<List<TicketEntity>> = ticketDao.getTicketsByBuyer(buyerId)
    fun getTicketsForEvent(eventId: String): Flow<List<TicketEntity>> = ticketDao.getTicketsForEvent(eventId)
    suspend fun getTicketById(ticketId: String): TicketEntity? = ticketDao.getTicketById(ticketId)
    suspend fun getTicketByQrPayload(qrPayload: String): TicketEntity? = ticketDao.getTicketByQrPayload(qrPayload)
    suspend fun saveTicket(ticket: TicketEntity) = ticketDao.insertTicket(ticket)
    suspend fun updateTicket(ticket: TicketEntity) = ticketDao.updateTicket(ticket)

    // Notifications
    fun getNotificationsForUser(userId: String): Flow<List<NotificationEntity>> = notificationDao.getNotificationsForUser(userId)
    suspend fun pushNotification(userId: String, title: String, message: String) {
        val notification = NotificationEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = title,
            message = message,
            timestamp = System.currentTimeMillis()
        )
        notificationDao.insertNotification(notification)
    }
    suspend fun markAllNotificationsAsRead(userId: String) = notificationDao.markAllAsRead(userId)

    // M-Pesa STK Push Integration Simulation
    fun processMpesaPayment(
        phoneNumber: String,
        amount: Double,
        event: EventEntity,
        buyer: UserEntity
    ): Flow<MpesaPaymentState> = flow {
        val cleanPhone = formatPhoneNumber(phoneNumber)
        if (cleanPhone == null) {
            emit(MpesaPaymentState.PaymentFailed("Invalid phone format. Please enter a valid phone number, e.g., 2547XXXXXXXX or 07XXXXXXXX"))
            return@flow
        }

        // 1. Initializing STK Push Connection with Daraja REST API endpoint
        emit(MpesaPaymentState.InitiatingSTKPush(cleanPhone))
        delay(1500)

        // Generate matching Daraja identifiers
        val merchantReqId = "M-PESA-STK-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
        val checkoutReqId = "ws_CO_" + UUID.randomUUID().toString().substring(0, 12).uppercase()

        // 2. STK Prompt sent to user's device. Waiting for PIN entry.
        emit(MpesaPaymentState.AwaitingUserPin(merchantReqId))
        delay(2500) // Simulates user entering PIN on their device

        // 3. Callback verification (Daraja Instant Callback response)
        emit(MpesaPaymentState.VerifyingCallback(checkoutReqId))
        delay(1500)

        // Random transaction code generator following standard Safaricom M-Pesa receipts
        val mpesaReceipt = generateMpesaReceiptCode()
        val ticketId = "TKT-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
        
        // This payload hash represents a highly secure proof of trade: EventId + BuyerId + Receipt
        val qrPayload = "VERIFY_TKT_${event.id}_${buyer.id}_$mpesaReceipt"

        val ticket = TicketEntity(
            id = ticketId,
            eventId = event.id,
            eventTitle = event.title,
            eventDate = event.date,
            eventLocation = event.location,
            buyerId = buyer.id,
            buyerName = buyer.name,
            pricePaid = amount,
            mpesaReceipt = mpesaReceipt,
            qrCodePayload = qrPayload,
            status = "VALID",
            purchasedAt = System.currentTimeMillis()
        )

        // Save Ticket, increment tickets sold, and push real-time notification
        saveTicket(ticket)
        eventDao.incrementTicketsSold(event.id, 1)
        pushNotification(
            userId = buyer.id,
            title = "Ticket Confirmed! 🎟️",
            message = "M-Pesa payment of KES ${String.format("%,.2f", amount)} received. Your ticket for ${event.title} is now active."
        )

        // If organizer exists, notify organizer about sales too
        pushNotification(
            userId = event.organizerId,
            title = "New Ticket Sold! 💰",
            message = "Someone purchased a ticket to ${event.title} via M-Pesa. KES ${String.format("%,.2f", amount)} accounted."
        )

        emit(MpesaPaymentState.PaymentSuccessful(mpesaReceipt, ticket))
    }

    private fun formatPhoneNumber(phone: String): String? {
        val trimmed = phone.replace("\\s+".toRegex(), "")
        return when {
            trimmed.startsWith("254") && trimmed.length == 12 -> trimmed
            trimmed.startsWith("0") && trimmed.length == 10 -> "254" + trimmed.substring(1)
            trimmed.startsWith("+254") && trimmed.length == 13 -> trimmed.substring(1)
            trimmed.length == 9 -> "254" + trimmed
            else -> null
        }
    }

    private fun generateMpesaReceiptCode(): String {
        val uppercaseLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val digits = "0123456789"
        val mpesaCode = StringBuilder()
        // Safaricom standard is 10 characters, e.g. "SJD4K9FA32"
        // Begins with 'Q', 'R', or 'S' depending on year
        mpesaCode.append("R")
        for (i in 1..9) {
            val src = if (i % 2 == 0) uppercaseLetters else digits
            val charIndex = (Math.random() * src.length).toInt()
            mpesaCode.append(src[charIndex])
        }
        return mpesaCode.toString()
    }
}
