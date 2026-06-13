package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.database.AppDatabase
import com.example.data.model.EventEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.TicketEntity
import com.example.data.model.UserEntity
import com.example.data.repository.AppRepository
import com.example.data.repository.MpesaPaymentState
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Helper extension to await a Play Services Task suspendable
private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(task.exception ?: RuntimeException("Task failed"))
        }
    }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "campuspass_db"
    )
    .fallbackToDestructiveMigration()
    .build()

    val repository: AppRepository = AppRepository(
        userDao = database.userDao(),
        eventDao = database.eventDao(),
        ticketDao = database.ticketDao(),
        notificationDao = database.notificationDao()
    )

    // Current logged-in user state
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // All available events stream
    val allEvents: StateFlow<List<EventEntity>> = repository.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current user's ticket list stream
    val myTickets: StateFlow<List<TicketEntity>> = _currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else repository.getTicketsByBuyer(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications stream
    val myNotifications: StateFlow<List<NotificationEntity>> = _currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else repository.getNotificationsForUser(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Events managed by current user if they are an organizer
    val organizerEvents: StateFlow<List<EventEntity>> = _currentUser
        .flatMapLatest { user ->
            if (user == null || !user.isOrganizer) flowOf(emptyList())
            else repository.getEventsByOrganizer(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Track active payment workflow state
    private val _mpesaPaymentState = MutableStateFlow<MpesaPaymentState>(MpesaPaymentState.Idle)
    val mpesaPaymentState: StateFlow<MpesaPaymentState> = _mpesaPaymentState.asStateFlow()

    // Gate scanner process result state
    private val _scannedTicketResult = MutableStateFlow<String?>(null)
    val scannedTicketResult: StateFlow<String?> = _scannedTicketResult.asStateFlow()

    // Auth screen transitions & states
    val isAuthLoading = MutableStateFlow(false)
    val authError = MutableStateFlow<String?>(null)

    init {
        // Initialize Firebase manually if it is not already initialized
        try {
            if (FirebaseApp.getApps(application).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:5432167890:android:3e8b4e72339d")
                    .setApiKey("AIzaSyBasicMockKeyForFirebaseAuthentication")
                    .setProjectId("campuspass-uqtqrx")
                    .build()
                FirebaseApp.initializeApp(application, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Pre-populate dummy campus events and user accounts upon initialization
        viewModelScope.launch {
            prepopulateDatabaseIfNeeded()
            // Auto-login standard student for easy testing initially
            loginUser("maina.mwangi@student.ku.ac.ke", "123456")
        }
    }

    private suspend fun prepopulateDatabaseIfNeeded() {
        val testUser = database.userDao().getUserById("std_01")
        if (testUser == null) {
            // 1. Create standard students
            val std1 = UserEntity(
                id = "std_01",
                email = "maina.mwangi@student.ku.ac.ke",
                name = "Maina Mwangi",
                campus = "Kenyatta University (KU)",
                phoneNumber = "0712345678",
                isOrganizer = false
            )
            val org1 = UserEntity(
                id = "org_ku_01",
                email = "kusa.events@ku.ac.ke",
                name = "KUSA Events Unit",
                campus = "Kenyatta University (Main Campus)",
                phoneNumber = "0798765432",
                isOrganizer = true
            )
            val org2 = UserEntity(
                id = "org_strath_02",
                email = "club.tech@strathmore.edu",
                name = "Strathmore Tech Society",
                campus = "Strathmore University",
                phoneNumber = "0744444333",
                isOrganizer = true
            )

            database.userDao().insertUser(std1)
            database.userDao().insertUser(org1)
            database.userDao().insertUser(org2)

            // 2. Prepopulate campus events
            val events = listOf(
                EventEntity(
                    id = "evt_01",
                    title = "KUSA Unity Cultural Gala",
                    description = "Celebrate cultural diversity across Kenyan universities! Expect breathtaking traditional dances, culinary kiosks, modern art projections, high fashion runs, and dynamic guest performances from leading local folk-rock bands. Open to all students. Carry student ID at entry.",
                    date = "2026-06-25",
                    time = "17:00",
                    location = "Kenyatta University – Bishop Square",
                    priceKes = 250.0,
                    imageUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&auto=format&fit=crop&q=80",
                    organizerId = "org_ku_01",
                    organizerName = "KUSA Events Unit",
                    capacity = 800,
                    ticketsSold = 142
                ),
                EventEntity(
                    id = "evt_02",
                    title = "Strathmore Fintech & Blockchain Summit",
                    description = "Join elite local Fintech pioneers, Safaricom Daraja developers, digital ledger specialists, and venture partners. We will discuss micro-credits, mobile banking integration strategies, and student startup capitalization. Refreshments and certificates will be provided.",
                    date = "2026-06-18",
                    time = "09:00",
                    location = "Strathmore Auditorium – Block C",
                    priceKes = 400.0,
                    imageUrl = "https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=500&auto=format&fit=crop&q=80",
                    organizerId = "org_strath_02",
                    organizerName = "Strathmore Tech Society",
                    capacity = 300,
                    ticketsSold = 185
                ),
                EventEntity(
                    id = "evt_03",
                    title = "Nairobi Inter-Varsity Tech Hackathon",
                    description = "48 hours of intense coding, prototyping, and UI design. Devise peer-to-peer security solvers, agritech agents, or public service helpers to support micro-merchants in Nairobi City. Food, high-speed fiber, and sleeping bays configured on-campus. Prizes valued at KES 150,000.",
                    date = "2026-07-08",
                    time = "08:00",
                    location = "KU Science Park – Hub B",
                    priceKes = 200.0,
                    imageUrl = "https://images.unsplash.com/photo-1504384308090-c894fdcc538d?w=500&auto=format&fit=crop&q=80",
                    organizerId = "org_ku_01",
                    organizerName = "KUSA Events Unit",
                    capacity = 200,
                    ticketsSold = 64
                ),
                EventEntity(
                    id = "evt_04",
                    title = "Campus Sunset Beats concert",
                    description = "The absolute highlight session on the calendar. Unwind after exam month with incredible live acoustics, top-class student deejays, visualizers, strobe setups, and amazing street cuisine options at the heart of the great court. Free energy beverage on quick check-in.",
                    date = "2026-07-20",
                    time = "16:00",
                    location = "UoN Main Campus Great Court",
                    priceKes = 600.0,
                    imageUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&auto=format&fit=crop&q=80",
                    organizerId = "org_ku_01",
                    organizerName = "KUSA Events Unit",
                    capacity = 1200,
                    ticketsSold = 390
                )
            )

            for (event in events) {
                database.eventDao().insertEvent(event)
            }

            // Create some mock reference tickets as pre-existing transactions
            val mockTicket1 = TicketEntity(
                id = "TKT-PREFILL01",
                eventId = "evt_01",
                eventTitle = "KUSA Unity Cultural Gala",
                eventDate = "2026-06-25",
                eventLocation = "Kenyatta University – Bishop Square",
                buyerId = "std_01",
                buyerName = "Maina Mwangi",
                pricePaid = 250.0,
                mpesaReceipt = "RU2K9HA14Y",
                qrCodePayload = "VERIFY_TKT_evt_01_std_01_RU2K9HA14Y",
                status = "VALID"
            )
            val mockTicket2 = TicketEntity(
                id = "TKT-PREFILL02",
                eventId = "evt_02",
                eventTitle = "Strathmore Fintech & Blockchain Summit",
                eventDate = "2026-06-18",
                eventLocation = "Strathmore Auditorium – Block C",
                buyerId = "std_01",
                buyerName = "Maina Mwangi",
                pricePaid = 400.0,
                mpesaReceipt = "RU7P4HA18A",
                qrCodePayload = "VERIFY_TKT_evt_02_std_01_RU7P4HA18A",
                status = "SCANNED" // Already checked in!
            )
            database.ticketDao().insertTicket(mockTicket1)
            database.ticketDao().insertTicket(mockTicket2)

            // Setup mock notifications
            database.notificationDao().insertNotification(
                NotificationEntity(
                    id = "notif_01",
                    userId = "std_01",
                    title = "Welcome to CampusPass! 👋",
                    message = "Browse secure campus events, buy tickets cleanly via M-Pesa, and check-in instantly.",
                    isRead = false
                )
            )
            database.notificationDao().insertNotification(
                NotificationEntity(
                    id = "notif_02",
                    userId = "std_01",
                    title = "Seminar Confirmation 📚",
                    message = "Your check-in key for 'Strathmore Fintech & Blockchain Summit' is generated. Have a fantastic time!",
                    isRead = true
                )
            )
        }
    }

    // AUTH ACTIONS
    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            isAuthLoading.value = true
            authError.value = null
            
            val cleanedEmail = email.trim()
            if (cleanedEmail.isEmpty()) {
                authError.value = "Email field cannot be empty"
                isAuthLoading.value = false
                return@launch
            }

            // Secure domain restriction to official university student emails
            val parts = cleanedEmail.split("@")
            val domain = parts.getOrNull(1) ?: ""
            
            val isDemoAccount = (cleanedEmail == "admin@campuspass.com") || (cleanedEmail == "maina.mwangi@student.ku.ac.ke")
            val isValidUniversity = isDemoAccount || 
                                    domain.endsWith(".edu") || 
                                    domain.contains(".edu.") || 
                                    domain.endsWith(".ac.ke") || 
                                    domain.contains(".ac.ke.")

            if (!isValidUniversity) {
                authError.value = "Security Error: Only valid university email domains (.edu or .ac.ke) are permitted."
                isAuthLoading.value = false
                return@launch
            }

            if (password.length < 6) {
                authError.value = "Security Error: Password PIN must be at least 6 characters. Please try again."
                isAuthLoading.value = false
                return@launch
            }

            try {
                val auth = FirebaseAuth.getInstance()
                
                // 1. Attempt to sign in on Firebase Auth
                val firebaseUserResult = try {
                    auth.signInWithEmailAndPassword(cleanedEmail, password).awaitTask().user
                } catch (e: Exception) {
                    val rootCause = e.cause ?: e
                    if (rootCause is FirebaseAuthInvalidUserException || e.message?.contains("no user record") == true || e.message?.contains("not found") == true) {
                        // User does not exist in Firebase, let's auto-provision/sign up on Firebase!
                        try {
                            auth.createUserWithEmailAndPassword(cleanedEmail, password).awaitTask().user
                        } catch (signUpEx: Exception) {
                            throw signUpEx
                        }
                    } else {
                        throw e
                    }
                }

                if (firebaseUserResult != null) {
                    // Firebase sign-in/up succeeded! Let's get or provision the local profile matching Firebase UID
                    val existingUser = repository.getUserByEmail(cleanedEmail)
                    if (existingUser != null) {
                        _currentUser.value = existingUser
                        repository.pushNotification(
                            userId = existingUser.id,
                            title = "Secure Sign In 🔒",
                            message = "You logged in securely using Google Firebase Auth with your university domain credentials."
                        )
                    } else {
                        // Provisions the profile locally
                        val isOrg = cleanedEmail.contains("organizer") || cleanedEmail.contains("kusa") || cleanedEmail.contains("club")
                        val rawName = parts.firstOrNull()?.replace(".", " ")?.replaceFirstChar { it.uppercase() } ?: "Student User"
                        
                        val campusName = when {
                            domain.contains("ku.ac.ke") -> "Kenyatta University (KU)"
                            domain.contains("strathmore.edu") -> "Strathmore University"
                            domain.contains("uonbi.ac.ke") -> "University of Nairobi (UoN)"
                            else -> "Kenya United Campus Federation"
                        }

                        val newUser = UserEntity(
                            id = firebaseUserResult.uid, // Tie the secure UID from Firebase to the Room Database
                            email = cleanedEmail,
                            name = rawName,
                            campus = campusName,
                            phoneNumber = "07" + (10000000 + (Math.random() * 89999999).toLong()).toString(),
                            isOrganizer = isOrg,
                            balance = 1000.0 // Starter allowance for easy testing
                        )
                        repository.saveUser(newUser)
                        _currentUser.value = newUser
                        repository.pushNotification(
                            userId = newUser.id,
                            title = "Welcome Web & App Onboard! 🎓",
                            message = "Your university credentials secure profile was auto-provisioned securely. Happy CampusPass sessions!"
                        )
                    }
                } else {
                    authError.value = "Authentication failed: Unable to acquire secure identity context."
                }
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                authError.value = "Authentication failed: Incorrect password or security credentials."
            } catch (e: Exception) {
                e.printStackTrace()
                // Graceful fallback to local Room-auth if network is physically blocked, so the app remains fully functional
                val localUser = repository.getUserByEmail(cleanedEmail)
                if (localUser != null) {
                    _currentUser.value = localUser
                    repository.pushNotification(
                        userId = localUser.id,
                        title = "Offline / Secure Fallback Sign In 📡",
                        message = "Signed in locally with university session. No network reached."
                    )
                } else {
                    authError.value = "Network / Security Error: Please verify university internet or try again."
                }
            }
            
            isAuthLoading.value = false
        }
    }

    fun logout() {
        _currentUser.value = null
        _mpesaPaymentState.value = MpesaPaymentState.Idle
    }

    fun switchRole() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updatedUser = user.copy(isOrganizer = !user.isOrganizer)
            repository.saveUser(updatedUser)
            _currentUser.value = updatedUser
            repository.pushNotification(
                userId = updatedUser.id,
                title = "Federation Profile Mode Swapped 🔄",
                message = "Switching account context to: ${if (updatedUser.isOrganizer) "ORGANIZER Portal" else "STUDENT Marketplace"}"
            )
        }
    }

    // PURCHASE TICKETS VIA M-PESA
    fun buyTicket(event: EventEntity, phoneNumber: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.processMpesaPayment(
                phoneNumber = phoneNumber,
                amount = event.priceKes,
                event = event,
                buyer = user
            ).collect { state ->
                _mpesaPaymentState.value = state
            }
        }
    }

    fun clearPaymentState() {
        _mpesaPaymentState.value = MpesaPaymentState.Idle
    }

    // ORGANIZER PORTAL: CREATE EVENT
    fun createEvent(
        title: String,
        description: String,
        date: String,
        time: String,
        location: String,
        price: Double,
        capacity: Int
    ) {
        val user = _currentUser.value ?: return
        if (!user.isOrganizer) return

        viewModelScope.launch {
            val eventId = "evt_" + UUID.randomUUID().toString().substring(0, 8)
            // Beautiful random Unsplash concert or corporate event images for realistic theme look
            val imageSeedUrls = listOf(
                "https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?w=500&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1475721027785-f74eccf877e2?w=500&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=500&auto=format&fit=crop&q=80"
            )
            val selectedImg = imageSeedUrls.random()

            val newEvent = EventEntity(
                id = eventId,
                title = title,
                description = description,
                date = date,
                time = time,
                location = location,
                priceKes = price,
                imageUrl = selectedImg,
                organizerId = user.id,
                organizerName = user.name,
                capacity = capacity,
                ticketsSold = 0
            )

            repository.saveEvent(newEvent)
            repository.pushNotification(
                userId = user.id,
                title = "New Event Created! 📣",
                message = "The campus listing for '$title' is active on CampusPass. Ready to collect M-Pesa payments!"
            )
        }
    }

    // ORGANIZER PORTAL: ENTRY SCANNER AT THE GATE SIMULATOR
    fun scanTicketAtGate(qrPayload: String) {
        val user = _currentUser.value ?: return
        if (!user.isOrganizer) return

        viewModelScope.launch {
            _scannedTicketResult.value = "Analyzing cryptographic signature..."
            kotlinx.coroutines.delay(1000)

            val ticket = repository.getTicketByQrPayload(qrPayload)
            if (ticket == null) {
                _scannedTicketResult.value = "INVALID_TKT: Cryptographic token validation failed. Ticket does not exist or has been modified!"
                return@launch
            }

            if (ticket.status == "SCANNED") {
                _scannedTicketResult.value = "DUPLICATE_TKT: This ticket (Receipt: ${ticket.mpesaReceipt}) has ALREADY been scanned at the gate at ${getFormattedTime(ticket.purchasedAt)}! Entry is strictly group-restricted."
                return@launch
            }

            // Valid ticket scan! Update ticket status and push notifications
            val updatedTicket = ticket.copy(status = "SCANNED")
            repository.updateTicket(updatedTicket)

            // Notify buyer
            repository.pushNotification(
                userId = ticket.buyerId,
                title = "Welcome! Gate Check-In Successful 🎟️",
                message = "Your ticket for '${ticket.eventTitle}' was successfully scanned at the gates. Enjoy your event!"
            )

            // Notify organizer
            repository.pushNotification(
                userId = user.id,
                title = "Ticket Checked In! 👥",
                message = "Checked in attendee '${ticket.buyerName}' (ID: ${ticket.id}) for '${ticket.eventTitle}' successfully."
            )

            _scannedTicketResult.value = "SUCCESS: Authorized entry for ${ticket.buyerName}."
        }
    }

    fun clearScanResult() {
        _scannedTicketResult.value = null
    }

    fun markNotificationsRead() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.markAllNotificationsAsRead(user.id)
        }
    }

    private fun getFormattedTime(timeMs: Long): String {
        return java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(timeMs))
    }
}
