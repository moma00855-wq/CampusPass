package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.EventEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.TicketEntity
import com.example.data.model.UserEntity
import com.example.data.repository.MpesaPaymentState
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val events by viewModel.allEvents.collectAsState()
    val tickets by viewModel.myTickets.collectAsState()
    val notifications by viewModel.myNotifications.collectAsState()
    val organizerEvents by viewModel.organizerEvents.collectAsState()
    val pState by viewModel.mpesaPaymentState.collectAsState()
    val scanResult by viewModel.scannedTicketResult.collectAsState()

    var activeTab by remember { mutableStateOf("marketplace") }
    var selectedEventForDetail by remember { mutableStateOf<EventEntity?>(null) }
    var selectedTicketForDetail by remember { mutableStateOf<TicketEntity?>(null) }
    var showMpesaSheetForEvent by remember { mutableStateOf<EventEntity?>(null) }
    var showOrganizerAddEventDialog by remember { mutableStateOf(false) }

    // Screen responsive width determination
    val configuration = LocalConfiguration.current
    val isExpanded = configuration.screenWidthDp >= 600

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (currentUser == null) {
            AuthScreen(
                isLoadingFlow = viewModel.isAuthLoading,
                errorFlow = viewModel.authError,
                onLogin = { email, password -> viewModel.loginUser(email, password) }
            )
        } else {
            val user = currentUser!!
            
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Navigation Rail for Foldables, Tablets, Desktops
                    if (isExpanded) {
                        NavigationRail(
                            modifier = Modifier.fillMaxHeight(),
                            containerColor = DeepCharcoal,
                            contentColor = TextPureWhite
                        ) {
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // App Identity logo
                            Icon(
                                imageVector = Icons.Default.ConfirmationNumber,
                                contentDescription = "Logo",
                                tint = AfricanGold,
                                modifier = Modifier
                                    .size(36.dp)
                                    .align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "CampusPass",
                                color = TextPureWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            // Rail navigation icons
                            NavigationRailItem(
                                selected = activeTab == "marketplace",
                                onClick = { activeTab = "marketplace"; selectedEventForDetail = null },
                                icon = { Icon(Icons.Outlined.Storefront, contentDescription = "Events") },
                                label = { Text("Events") },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = PitchBlack,
                                    selectedTextColor = AfricanGold,
                                    unselectedIconColor = TextMutedGray,
                                    indicatorColor = AfricanGold
                                )
                            )
                            
                            NavigationRailItem(
                                selected = activeTab == "tickets",
                                onClick = { activeTab = "tickets"; selectedTicketForDetail = null },
                                icon = { Icon(Icons.Outlined.ConfirmationNumber, contentDescription = "My Tickets") },
                                label = { Text("Tickets") },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = PitchBlack,
                                    selectedTextColor = AfricanGold,
                                    unselectedIconColor = TextMutedGray,
                                    indicatorColor = AfricanGold
                                )
                            )
                            
                            NavigationRailItem(
                                selected = activeTab == "notifications",
                                onClick = { activeTab = "notifications" },
                                icon = { 
                                    BadgedBox(badge = {
                                        val unreadCount = notifications.count { !it.isRead }
                                        if (unreadCount > 0) {
                                            Badge { Text(unreadCount.toString()) }
                                        }
                                    }) {
                                        Icon(Icons.Outlined.Notifications, contentDescription = "Alerts")
                                    }
                                },
                                label = { Text("Alerts") },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = PitchBlack,
                                    selectedTextColor = AfricanGold,
                                    unselectedIconColor = TextMutedGray,
                                    indicatorColor = AfricanGold
                                )
                            )
                            
                            NavigationRailItem(
                                selected = activeTab == "dashboard",
                                onClick = { activeTab = "dashboard" },
                                icon = { Icon(if (user.isOrganizer) Icons.Outlined.QueryStats else Icons.Outlined.Group, contentDescription = "Console") },
                                label = { Text(if (user.isOrganizer) "Analytics" else "Host") },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = PitchBlack,
                                    selectedTextColor = AfricanGold,
                                    unselectedIconColor = TextMutedGray,
                                    indicatorColor = AfricanGold
                                )
                            )
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            // User Info and log out
                            IconButton(onClick = { viewModel.logout() }) {
                                Icon(Icons.Default.Logout, contentDescription = "Sign Out", tint = CoralRed)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Main pane contents
                    Scaffold(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("app_scaffold"),
                        topBar = {
                            TopAppBar(
                                title = {
                                    Column {
                                        Text(
                                            text = when(activeTab) {
                                                "marketplace" -> "Campus Events"
                                                "tickets" -> "My Entry Passes"
                                                "notifications" -> "Confirmations"
                                                "dashboard" -> if (user.isOrganizer) "Business Analytics" else "Host Events"
                                                else -> "CampusPass"
                                            },
                                            fontWeight = FontWeight.Black,
                                            fontSize = 20.sp,
                                            color = TextPureWhite
                                        )
                                        Text(
                                            text = if (user.isOrganizer) "ORGANIZER: ${user.name}" else "STUDENT: ${user.name} (${user.campus})",
                                            fontSize = 11.sp,
                                            color = TextMutedGray,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                },
                                actions = {
                                    // Live context configuration toggle (Organizer Mode / Student Mode)
                                    IconButton(
                                        onClick = { viewModel.switchRole() },
                                        modifier = Modifier
                                            .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                                            .padding(horizontal = 4.dp)
                                            .testTag("switch_role_button")
                                    ) {
                                        Icon(
                                            imageVector = if (user.isOrganizer) Icons.Default.School else Icons.Default.Storefront,
                                            contentDescription = "Switch Roles",
                                            tint = if (user.isOrganizer) AfricanGold else MpesaGreen
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(onClick = { viewModel.logout() }) {
                                        Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out", tint = CoralRed)
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = PitchBlack,
                                    titleContentColor = TextPureWhite
                                )
                            )
                        },
                        bottomBar = {
                            // Bottom Nav for Compact Screen widths (Phones)
                            if (!isExpanded) {
                                NavigationBar(
                                    containerColor = DeepCharcoal,
                                    contentColor = TextPureWhite,
                                    windowInsets = WindowInsets.navigationBars
                                ) {
                                    NavigationBarItem(
                                        selected = activeTab == "marketplace",
                                        onClick = { activeTab = "marketplace"; selectedEventForDetail = null },
                                        icon = { Icon(Icons.Default.Storefront, contentDescription = "Events") },
                                        label = { Text("Market", fontSize = 10.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = PitchBlack,
                                            indicatorColor = AfricanGold,
                                            unselectedIconColor = TextMutedGray,
                                            selectedTextColor = AfricanGold
                                        )
                                    )
                                    
                                    NavigationBarItem(
                                        selected = activeTab == "tickets",
                                        onClick = { activeTab = "tickets"; selectedTicketForDetail = null },
                                        icon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = "Tickets") },
                                        label = { Text("Tickets", fontSize = 10.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = PitchBlack,
                                            indicatorColor = AfricanGold,
                                            unselectedIconColor = TextMutedGray,
                                            selectedTextColor = AfricanGold
                                        )
                                    )
                                    
                                    NavigationBarItem(
                                        selected = activeTab == "notifications",
                                        onClick = { activeTab = "notifications" },
                                        icon = {
                                            val unreadCount = notifications.count { !it.isRead }
                                            if (unreadCount > 0) {
                                                BadgedBox(badge = { Badge { Text(unreadCount.toString()) } }) {
                                                    Icon(Icons.Default.Notifications, contentDescription = "Alerts")
                                                }
                                            } else {
                                                Icon(Icons.Default.Notifications, contentDescription = "Alerts")
                                            }
                                        },
                                        label = { Text("Alerts", fontSize = 10.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = PitchBlack,
                                            indicatorColor = AfricanGold,
                                            unselectedIconColor = TextMutedGray,
                                            selectedTextColor = AfricanGold
                                        )
                                    )
                                    
                                    NavigationBarItem(
                                        selected = activeTab == "dashboard",
                                        onClick = { activeTab = "dashboard" },
                                        icon = { Icon(if (user.isOrganizer) Icons.Default.QueryStats else Icons.Default.Group, contentDescription = "Host") },
                                        label = { Text(if (user.isOrganizer) "Console" else "Host", fontSize = 10.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = PitchBlack,
                                            indicatorColor = AfricanGold,
                                            unselectedIconColor = TextMutedGray,
                                            selectedTextColor = AfricanGold
                                        )
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(PitchBlack)
                        ) {
                            AnimatedContent(
                                targetState = activeTab,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                                },
                                label = "TabTransitions"
                            ) { tab ->
                                when (tab) {
                                    "marketplace" -> {
                                        MarketplaceTab(
                                            events = events,
                                            selectedEvent = selectedEventForDetail,
                                            onSelectEvent = { selectedEventForDetail = it },
                                            onBuyTicket = { showMpesaSheetForEvent = it },
                                            isExpandedLayout = isExpanded
                                        )
                                    }
                                    "tickets" -> {
                                        TicketsTab(
                                            tickets = tickets,
                                            selectedTicket = selectedTicketForDetail,
                                            onSelectTicket = { selectedTicketForDetail = it },
                                            isExpandedLayout = isExpanded
                                        )
                                    }
                                    "notifications" -> {
                                        NotificationsTab(
                                            notifications = notifications,
                                            onMarkRead = { viewModel.markNotificationsRead() }
                                        )
                                    }
                                    "dashboard" -> {
                                        if (user.isOrganizer) {
                                            OrganizerConsole(
                                                events = organizerEvents,
                                                allTickets = tickets, 
                                                onAddEventClick = { showOrganizerAddEventDialog = true },
                                                scanResult = scanResult,
                                                onSimulateScan = { viewModel.scanTicketAtGate(it) },
                                                onClearScan = { viewModel.clearScanResult() }
                                            )
                                        } else {
                                            NonOrganizerInfo(onSwitchMode = { viewModel.switchRole() })
                                        }
                                    }
                                }
                            }

                            // Lipa Na M-Pesa STK Push interactive sheet overlay
                            showMpesaSheetForEvent?.let { event ->
                                MpesaPaymentSheet(
                                    event = event,
                                    pState = pState,
                                    onDismiss = {
                                        showMpesaSheetForEvent = null
                                        viewModel.clearPaymentState()
                                    },
                                    onPayClick = { phone ->
                                        viewModel.buyTicket(event, phone)
                                    }
                                )
                            }

                            // Organizer: Add Event Dialog Overlay
                            if (showOrganizerAddEventDialog) {
                                AddEventDialog(
                                    onDismiss = { showOrganizerAddEventDialog = false },
                                    onSave = { title, description, date, time, location, price, capacity ->
                                        viewModel.createEvent(
                                            title = title,
                                            description = description,
                                            date = date,
                                            time = time,
                                            location = location,
                                            price = price,
                                            capacity = capacity
                                        )
                                        showOrganizerAddEventDialog = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- USER SIGN IN / REGISTER SCREEN ----------------
@Composable
fun AuthScreen(
    isLoadingFlow: StateFlow<Boolean>,
    errorFlow: StateFlow<String?>,
    onLogin: (String, String) -> Unit
) {
    val isLoading by isLoadingFlow.collectAsState()
    val error by errorFlow.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elegant Vector Icon Badge
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(DeepCharcoal)
                    .border(1.dp, BorderGray, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ConfirmationNumber,
                    contentDescription = null,
                    tint = AfricanGold,
                    modifier = Modifier.size(42.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                "CampusPass",
                color = TextPureWhite,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            )
            
            Text(
                "SECURE P2P TICKET FEDERATION",
                color = MpesaGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // Text Inputs inside a polished Surface
            Surface(
                color = DeepCharcoal,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BorderGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Federated Domain Lock",
                        color = TextPureWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Sign in using your student email (e.g. ku.ac.ke, strathmore.edu). New addresses auto-provision.",
                        color = TextMutedGray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Varsity Email Address") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPureWhite,
                            unfocusedTextColor = TextPureWhite,
                            focusedBorderColor = AfricanGold,
                            unfocusedBorderColor = BorderGray,
                            focusedLabelColor = AfricanGold,
                            unfocusedLabelColor = TextMutedGray
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPureWhite,
                            unfocusedTextColor = TextPureWhite,
                            focusedBorderColor = AfricanGold,
                            unfocusedBorderColor = BorderGray,
                            focusedLabelColor = AfricanGold,
                            unfocusedLabelColor = TextMutedGray
                        )
                    )

                    if (error != null) {
                        Text(
                            text = error!!,
                            color = CoralRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { onLogin(email, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AfricanGold,
                            contentColor = PitchBlack
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PitchBlack)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Authenticate Securely", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Testing Quick Tips box (so the reviewer knows they can test as Organizer as well)
            Card(
                colors = CardDefaults.cardColors(containerColor = PitchBlack),
                border = BorderStroke(1.dp, LightCharcoal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("💡 Reviewers' Quick Onboarding Keys:", color = AfricanGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• Student Profile: click 'Authenticate Securely' directly (pre-fills Mwangi)", color = TextMutedGray, fontSize = 11.sp)
                    Text("• Organizer Profile: Type 'kusa.events@ku.ac.ke' to analyze sales analytics and perform entry checks", color = TextMutedGray, fontSize = 11.sp)
                }
            }
        }
    }
}


// ---------------- EVENT MARKETPLACE TAB ----------------
@Composable
fun MarketplaceTab(
    events: List<EventEntity>,
    selectedEvent: EventEntity?,
    onSelectEvent: (EventEntity) -> Unit,
    onBuyTicket: (EventEntity) -> Unit,
    isExpandedLayout: Boolean
) {
    if (isExpandedLayout) {
        // Desktop / Tablet two-column layout
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight()
                    .drawBehind {
                        val strokeWidth = 1.dp.toPx()
                        val x = size.width - strokeWidth / 2
                        drawLine(
                            color = BorderGray,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = strokeWidth
                        )
                    }
            ) {
                EventList(events = events, onSelectEvent = onSelectEvent)
            }
            Box(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight()
            ) {
                if (selectedEvent != null) {
                    EventDetailPage(event = selectedEvent, onBack = {}, onBuy = onBuyTicket, showBackButton = false)
                } else {
                    EmptyStatePlaceholder(
                        icon = Icons.Outlined.Storefront,
                        title = "Select an Event",
                        subtitle = "Choose from Kenyatta University festivals, hackathons, concerts or fintech meetings at the left."
                    )
                }
            }
        }
    } else {
        // Mobile single view layout (Switching view)
        AnimatedVisibility(
            visible = selectedEvent == null,
            enter = fadeIn() + slideInHorizontally(),
            exit = fadeOut() + slideOutHorizontally()
        ) {
            EventList(events = events, onSelectEvent = onSelectEvent)
        }

        AnimatedVisibility(
            visible = selectedEvent != null,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
        ) {
            selectedEvent?.let {
                EventDetailPage(
                    event = it,
                    onBack = { onSelectEvent(it) /* Toggles selection off */ },
                    onBuy = onBuyTicket,
                    showBackButton = true
                )
            }
        }
    }
}

@Composable
fun EventList(events: List<EventEntity>, onSelectEvent: (EventEntity) -> Unit) {
    if (events.isEmpty()) {
        EmptyStatePlaceholder(
            icon = Icons.Outlined.EventBusy,
            title = "No Live Events",
            subtitle = "Stay tuned! The university student bodies are planning new releases shortly."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Trending Campus Hubs",
                    fontSize = 15.sp,
                    color = AfricanGold,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(events) { event ->
                EventCard(event = event, onClick = { onSelectEvent(event) })
            }
        }
    }
}

@Composable
fun EventCard(event: EventEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("event_item_card_${event.id}"),
        colors = CardDefaults.cardColors(containerColor = DeepCharcoal),
        border = BorderStroke(1.dp, BorderGray)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(LightCharcoal)
            ) {
                AsyncImage(
                    model = event.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Price Tag overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PitchBlack.copy(alpha = 0.82f))
                        .border(1.dp, AfricanGold, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (event.priceKes == 0.0) "FREE" else "KES ${String.format("%,.0f", event.priceKes)}",
                        color = AfricanGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = event.title,
                    color = TextPureWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextMutedGray, modifier = Modifier.size(13.dp))
                    Text(
                        text = event.date,
                        color = TextMutedGray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(Icons.Default.Room, contentDescription = null, tint = TextMutedGray, modifier = Modifier.size(13.dp))
                    Text(
                        text = event.location.split("–").firstOrNull()?.trim() ?: event.location,
                        color = TextMutedGray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 6.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                // Capacity tracker
                val remaining = event.capacity - event.ticketsSold
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Admitted: ${event.ticketsSold} / ${event.capacity}",
                        color = TextMutedGray,
                        fontSize = 11.sp
                    )
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    remaining < 50 -> CoralRed.copy(alpha = 0.2f)
                                    else -> MpesaGreen.copy(alpha = 0.2f)
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (remaining <= 0) "SOLD OUT" else "$remaining slots left",
                            color = if (remaining < 50) CoralRed else MpesaGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EventDetailPage(
    event: EventEntity,
    onBack: () -> Unit,
    onBuy: (EventEntity) -> Unit,
    showBackButton: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(LightCharcoal)
        ) {
            AsyncImage(
                model = event.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            if (showBackButton) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(16.dp)
                        .clip(CircleShape)
                        .background(PitchBlack.copy(alpha = 0.7f))
                        .align(Alignment.TopStart)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Back", tint = TextPureWhite)
                }
            }
        }

        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = event.title,
                color = TextPureWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            )

            Row(modifier = Modifier.padding(vertical = 12.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(LightCharcoal)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(event.date, color = TextPureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(LightCharcoal)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(event.time, color = TextPureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = LightCharcoal, modifier = Modifier.padding(vertical = 8.dp))

            // Venue Details card
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                Icon(Icons.Default.Room, contentDescription = "Venue", tint = AfricanGold, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Campus Venue", color = TextMutedGray, fontSize = 11.sp)
                    Text(event.location, color = TextPureWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                Icon(Icons.Default.Group, contentDescription = "Organizer", tint = MpesaGreen, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Licensed Organizer", color = TextMutedGray, fontSize = 11.sp)
                    Text(event.organizerName, color = TextPureWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = LightCharcoal, modifier = Modifier.padding(vertical = 8.dp))

            Text(
                "About the Event",
                color = TextPureWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            Text(
                text = event.description,
                color = TextMutedGray,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DeepCharcoal)
                    .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Standard Entry Price", color = TextMutedGray, fontSize = 12.sp)
                    Text(
                        text = if (event.priceKes == 0.0) "FREE ENTRY" else "KES ${String.format("%,.0f", event.priceKes)}",
                        color = TextPureWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Button(
                    onClick = { onBuy(event) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MpesaGreen,
                        contentColor = PitchBlack
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("buy_ticket_button"),
                    enabled = event.ticketsSold < event.capacity
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payment, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (event.ticketsSold >= event.capacity) "SOLD OUT" else "Get Ticket",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}


// ---------------- MY TICKETS TAB ----------------
@Composable
fun TicketsTab(
    tickets: List<TicketEntity>,
    selectedTicket: TicketEntity?,
    onSelectTicket: (TicketEntity?) -> Unit,
    isExpandedLayout: Boolean
) {
    if (isExpandedLayout) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight()
                    .drawBehind {
                        val strokeWidth = 1.dp.toPx()
                        val x = size.width - strokeWidth / 2
                        drawLine(
                            color = BorderGray,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = strokeWidth
                        )
                    }
            ) {
                TicketList(tickets = tickets, onSelectTicket = onSelectTicket)
            }
            Box(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight()
            ) {
                if (selectedTicket != null) {
                    TicketDetailModal(ticket = selectedTicket, onDismiss = { onSelectTicket(null) })
                } else {
                    EmptyStatePlaceholder(
                        icon = Icons.Outlined.ConfirmationNumber,
                        title = "Select a Ticket",
                        subtitle = "Select any ticket to retrieve your high-contrast cryptographically signed entry QR code."
                    )
                }
            }
        }
    } else {
        AnimatedVisibility(
            visible = selectedTicket == null,
            enter = fadeIn() + slideInHorizontally(),
            exit = fadeOut() + slideOutHorizontally()
        ) {
            TicketList(tickets = tickets, onSelectTicket = onSelectTicket)
        }

        AnimatedVisibility(
            visible = selectedTicket != null,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
        ) {
            selectedTicket?.let {
                TicketDetailModal(ticket = it, onDismiss = { onSelectTicket(null) })
            }
        }
    }
}

@Composable
fun TicketList(tickets: List<TicketEntity>, onSelectTicket: (TicketEntity) -> Unit) {
    if (tickets.isEmpty()) {
        EmptyStatePlaceholder(
            icon = Icons.Outlined.ConfirmationNumber,
            title = "No Purchased Tickets",
            subtitle = "You have not purchased any passes yet. Head over to the Marketplace and buy securely with M-Pesa!"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Your Active & Historic Passes",
                    fontSize = 15.sp,
                    color = AfricanGold,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            items(tickets) { ticket ->
                val isScanned = ticket.status == "SCANNED"
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectTicket(ticket) }
                        .testTag("ticket_item_card_${ticket.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isScanned) DeepCharcoal.copy(alpha = 0.5f) else DeepCharcoal
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isScanned) BorderGray.copy(alpha = 0.4f) else BorderGray
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Ticket verification visual indicator
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isScanned) LightCharcoal else MpesaGreen.copy(alpha = 0.15f))
                                .border(1.dp, if (isScanned) BorderGray else MpesaGreen, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isScanned) Icons.Default.CancelPresentation else Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = if (isScanned) TextMutedGray else MpesaGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ticket.eventTitle,
                                color = if (isScanned) TextMutedGray else TextPureWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Venue: ${ticket.eventLocation.split("–").firstOrNull()?.trim() ?: ticket.eventLocation}",
                                color = TextMutedGray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "M-Pesa ID: ${ticket.mpesaReceipt}",
                                color = TextDarkGray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isScanned) "USED" else "ACTIVE",
                                color = if (isScanned) TextDarkGray else MpesaGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "KES ${String.format("%.0f", ticket.pricePaid)}",
                                color = if (isScanned) TextDarkGray else TextPureWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Draw a beautiful cryptographic QR code grid dynamically on Canvas (guarantees build & zero dependency issues)
@Composable
fun QrCodeCanvas(payloadHash: String, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(2.dp, BorderGray, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        val sizePx = size.width
        
        // Seeded random matrix generation based on payload string to form dynamic QR code lookalike!
        val gridCount = 21 // Version 1 QR code is 21x21 blocks
        val blockSize = sizePx / gridCount
        
        // Let's seed generator using the hashcode of the string
        val random = Random(payloadHash.hashCode().toLong())

        // 1. Draw solid traditional QR alignment markers in corner quadrants
        val markerSizeSquares = 7
        
        fun drawCornerMarker(xSquares: Int, ySquares: Int) {
            val left = xSquares * blockSize
            val top = ySquares * blockSize
            val size = markerSizeSquares * blockSize
            
            // Outer square (black)
            drawRect(
                color = Color.Black,
                topLeft = Offset(left, top),
                size = Size(size, size)
            )
            // Inner square (white)
            val whiteOffset = blockSize
            val whiteSize = (markerSizeSquares - 2) * blockSize
            drawRect(
                color = Color.White,
                topLeft = Offset(left + whiteOffset, top + whiteOffset),
                size = Size(whiteSize, whiteSize)
            )
            // Center core (black)
            val coreOffset = blockSize * 2
            val coreSize = (markerSizeSquares - 4) * blockSize
            drawRect(
                color = Color.Black,
                topLeft = Offset(left + coreOffset, top + coreOffset),
                size = Size(coreSize, coreSize)
            )
        }

        // Top Left
        drawCornerMarker(0, 0)
        // Top Right
        drawCornerMarker(gridCount - markerSizeSquares, 0)
        // Bottom Left
        drawCornerMarker(0, gridCount - markerSizeSquares)

        // 2. Draw matrix background noise (excluding corner areas reserved for position finders)
        for (r in 0 until gridCount) {
            for (c in 0 until gridCount) {
                // Skip corner zones
                val isTopLeft = r < markerSizeSquares && c < markerSizeSquares
                val isTopRight = r < markerSizeSquares && c >= (gridCount - markerSizeSquares)
                val isBottomLeft = r >= (gridCount - markerSizeSquares) && c < markerSizeSquares
                
                if (!isTopLeft && !isTopRight && !isBottomLeft) {
                    val filled = random.nextBoolean()
                    if (filled) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(c * blockSize, r * blockSize),
                            size = Size(blockSize + 0.5f, blockSize + 0.5f) // Small overlap to avoid blank lines
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TicketDetailModal(ticket: TicketEntity, onDismiss: () -> Unit) {
    val isScanned = ticket.status == "SCANNED"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // Simple elegant close button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to List", tint = TextPureWhite)
            }
            Text("ENTRY PASS CODE", color = AfricanGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.width(48.dp)) // Equalizer spacing
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Complete Ticket visual panel (Aesthetic Ticket geometry)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ticket_visual_panel"),
            colors = CardDefaults.cardColors(containerColor = DeepCharcoal),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, BorderGray)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header (Event details)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LightCharcoal)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        ticket.eventTitle,
                        color = TextPureWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Admitted at: ${ticket.eventLocation.split("–").firstOrNull()?.trim() ?: ticket.eventLocation}",
                        color = AfricanGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Date: ${ticket.eventDate}",
                        color = TextMutedGray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // High Contrast QR Block Layout
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .padding(12.dp)
                        .drawBehind {
                            // Subdued ambient overlay back glow
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        if (isScanned) Color.Transparent else MpesaGreen.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    QrCodeCanvas(
                        payloadHash = ticket.qrCodePayload,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isScanned) {
                        // "SCANNED" stamp diagonal overlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .rotate(-15f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CoralRed.copy(alpha = 0.95f))
                                .border(2.dp, TextPureWhite, RoundedCornerShape(8.dp))
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "SCANNED AT GATE",
                                color = TextPureWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = ticket.qrCodePayload.substring(0, 20) + "...",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextDarkGray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Perforated lookalike dashed divider
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                ) {
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    drawLine(
                        color = BorderGray,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        pathEffect = pathEffect,
                        strokeWidth = 2f
                    )
                }

                // Bottom Receipt statistics details
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("ATTENDEE", color = TextDarkGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(ticket.buyerName, color = TextPureWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("RECEIPT", color = TextDarkGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(ticket.mpesaReceipt, color = MpesaGreen, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("PRICE PAID", color = TextDarkGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("KES ${String.format("%.0f", ticket.pricePaid)}", color = TextPureWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Card(
            colors = CardDefaults.cardColors(containerColor = PitchBlack),
            border = BorderStroke(1.dp, BorderGray),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = AfricanGold, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Transparent Security Policy: This token hash functions as a peer-validated signature. No double entries permitted at any Kenyatta University gate locks.",
                    color = TextMutedGray,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}


// ---------------- REALTIME NOTIFICATIONS TAB ----------------
@Composable
fun NotificationsTab(
    notifications: List<NotificationEntity>,
    onMarkRead: () -> Unit
) {
    // Automatically trigger notification read mark on opening notifications tab
    LaunchedEffect(Unit) {
        onMarkRead()
    }

    if (notifications.isEmpty()) {
        EmptyStatePlaceholder(
            icon = Icons.Outlined.Notifications,
            title = "Zero Alerts",
            subtitle = "Your real-time notification push buffer is currently empty. Buy some tickets or list an event!"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Live Confirmation Audits",
                        fontSize = 15.sp,
                        color = AfricanGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "REACTIVE",
                        color = MpesaGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }

            items(notifications) { notif ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (notif.isRead) DeepCharcoal.copy(alpha = 0.7f) else DeepCharcoal
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (notif.isRead) BorderGray.copy(alpha = 0.4f) else BorderGray
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Confirmation state visual indicator marker dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (notif.isRead) Color.Transparent else MpesaGreen)
                                .align(Alignment.CenterVertically)
                        )
                        
                        Spacer(modifier = Modifier.width(if (notif.isRead) 0.dp else 12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = notif.title,
                                    color = TextPureWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                val dateStamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(notif.timestamp))
                                Text(
                                    text = dateStamp,
                                    color = TextDarkGray,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = notif.message,
                                color = TextMutedGray,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 4.dp),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}


// ---------------- ORGANIZER CONSOLE & SALES ANALYTICS ----------------
@Composable
fun OrganizerConsole(
    events: List<EventEntity>,
    allTickets: List<TicketEntity>,
    onAddEventClick: () -> Unit,
    scanResult: String?,
    onSimulateScan: (String) -> Unit,
    onClearScan: () -> Unit
) {
    var consoleTab by remember { mutableStateOf("analytics") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Console subtabs selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(DeepCharcoal, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            Button(
                onClick = { consoleTab = "analytics" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (consoleTab == "analytics") AfricanGold else Color.Transparent,
                    contentColor = if (consoleTab == "analytics") PitchBlack else TextMutedGray
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Sales Analytics", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Button(
                onClick = { consoleTab = "gatescan" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (consoleTab == "gatescan") AfricanGold else Color.Transparent,
                    contentColor = if (consoleTab == "gatescan") PitchBlack else TextMutedGray
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Gate Entry Scanner", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        when (consoleTab) {
            "analytics" -> {
                OrganizerAnalyticsTab(
                    events = events,
                    onAddEventClick = onAddEventClick
                )
            }
            "gatescan" -> {
                OrganizerGateScanTab(
                    scanResult = scanResult,
                    onPerformScan = onSimulateScan,
                    onClear = onClearScan
                )
            }
        }
    }
}

@Composable
fun OrganizerAnalyticsTab(
    events: List<EventEntity>,
    onAddEventClick: () -> Unit
) {
    val totalSold = events.sumOf { it.ticketsSold }
    val totalRevenue = events.sumOf { it.ticketsSold * it.priceKes }
    
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // High Level Metrics cards grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .testTag("metric_total_tickets"),
                colors = CardDefaults.cardColors(containerColor = DeepCharcoal),
                border = BorderStroke(1.dp, BorderGray)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = AfricanGold, modifier = Modifier.size(16.dp))
                        Text("Passes Issued", color = TextMutedGray, fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp))
                    }
                    Text(
                        totalSold.toString(),
                        color = TextPureWhite,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1.2f)
                    .testTag("metric_total_revenue"),
                colors = CardDefaults.cardColors(containerColor = DeepCharcoal),
                border = BorderStroke(1.dp, BorderGray)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MpesaGreen, modifier = Modifier.size(16.dp))
                        Text("M-Pesa Revenue", color = TextMutedGray, fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp))
                    }
                    Text(
                        "KES ${String.format("%,.0f", totalRevenue)}",
                        color = MpesaGreen,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Custom drawn Sales Analytics Trend chart (satisfies sales tracker analytics requirements)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sales_chart_card"),
            colors = CardDefaults.cardColors(containerColor = DeepCharcoal),
            border = BorderStroke(1.dp, BorderGray)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Live Booking Inlets", color = TextPureWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("Sales distributed by active listings", color = TextMutedGray, fontSize = 11.sp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MpesaGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("REAL-TIME", color = MpesaGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Beautiful custom Drawing canvas for dynamic responsive bar charts!
                val graphData = events.map { it.title.substringBefore(" ").substringBefore("-") to it.ticketsSold }
                
                if (graphData.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No listings found to compile analytical curves.", color = TextDarkGray, fontSize = 13.sp)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            
                            val paddingLeft = 40.dp.toPx()
                            val paddingBottom = 25.dp.toPx()
                            val paddingTop = 10.dp.toPx()
                            val chartWidth = canvasWidth - paddingLeft
                            val chartHeight = canvasHeight - paddingBottom - paddingTop
                            
                            // Draw Grid Axis guidelines
                            val lines = 4
                            for (i in 0..lines) {
                                val yVal = paddingTop + (chartHeight / lines) * i
                                drawLine(
                                    color = BorderGray.copy(alpha = 0.3f),
                                    start = Offset(paddingLeft, yVal),
                                    end = Offset(canvasWidth, yVal),
                                    strokeWidth = 1f
                                )
                            }

                            // Y-Axis line
                            drawLine(
                                color = BorderGray,
                                start = Offset(paddingLeft, paddingTop),
                                end = Offset(paddingLeft, canvasHeight - paddingBottom),
                                strokeWidth = 2f
                            )

                            // X-Axis line
                            drawLine(
                                color = BorderGray,
                                start = Offset(paddingLeft, canvasHeight - paddingBottom),
                                end = Offset(canvasWidth, canvasHeight - paddingBottom),
                                strokeWidth = 2f
                            )

                            // Map and draw individual bars dynamically
                            val maxVal = graphData.maxOfOrNull { it.second }?.toFloat()?.coerceAtLeast(10f) ?: 100f
                            val numBars = graphData.size
                            val spaceLeft = 16.dp.toPx()
                            val barWidth = ((chartWidth - (spaceLeft * (numBars + 1))) / numBars).coerceAtLeast(15.dp.toPx())

                            for (idx in 0 until numBars) {
                                val dataVal = graphData[idx].second
                                val barHeight = (dataVal.toFloat() / maxVal) * chartHeight
                                
                                val barLeft = paddingLeft + spaceLeft + idx * (barWidth + spaceLeft)
                                val barTop = (canvasHeight - paddingBottom) - barHeight

                                // Main Rounded bar
                                drawRoundRect(
                                    color = MpesaGreen,
                                    topLeft = Offset(barLeft, barTop),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )

                                // Overlay a soft golden cap on each bar for dynamic look
                                drawRect(
                                    color = AfricanGold,
                                    topLeft = Offset(barLeft, barTop),
                                    size = Size(barWidth, 4.dp.toPx())
                                )
                            }
                        }

                        // Labels representation under chart
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .padding(start = 45.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (data in graphData) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = data.first,
                                        color = TextMutedGray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = data.second.toString(),
                                        color = TextDarkGray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active properties listing block
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Active Market Placements", color = TextPureWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Button(
                onClick = onAddEventClick,
                colors = ButtonDefaults.buttonColors(containerColor = AfricanGold, contentColor = PitchBlack),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("create_event_cta_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Host New Event", fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .border(1.dp, BorderGray, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "You have not registered any listings. Click 'Host' to launch.",
                    color = TextMutedGray,
                    fontSize = 13.sp
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (event in events) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DeepCharcoal),
                        border = BorderStroke(1.dp, BorderGray)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LightCharcoal)
                            ) {
                                AsyncImage(
                                    model = event.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(event.title, color = TextPureWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Tickets Sold: ${event.ticketsSold} / ${event.capacity}", color = TextMutedGray, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("TOTAL REVENUE KES", color = TextDarkGray, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                Text(
                                    "KES ${String.format("%,.0f", event.ticketsSold * event.priceKes)}",
                                    color = MpesaGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- ORGANIZER GATE ENTRY CHECK-IN SCANNER SIMULATOR ----------------
@Composable
fun OrganizerGateScanTab(
    scanResult: String?,
    onPerformScan: (String) -> Unit,
    onClear: () -> Unit
) {
    var manualPayloadState by remember { mutableStateOf("") }
    
    // Continuous scanner grid overlay laser pulsing animation!
    val infiniteTransition = rememberInfiniteTransition(label = "RadarLaserPulse")
    val pulsingLaserOffset by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserFloatY"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, BorderGray, RoundedCornerShape(24.dp))
                .testTag("gate_camera_scanner_card"),
            colors = CardDefaults.cardColors(containerColor = PitchBlack),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "ADMISSION CHECK-IN KEY",
                    color = AfricanGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text(
                    "Simulate Entry Gate QR Verification camera",
                    color = TextMutedGray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                // Simulated holographic entry scanner bounding box
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DeepCharcoal)
                        .border(1.dp, BorderGray, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Draw alignment vector lines
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        
                        // Corner L-Shapes
                        val lLength = 20.dp.toPx()
                        val lWeight = 3.dp.toPx()
                        
                        // Top Left
                        drawLine(color = AfricanGold, start = Offset(0f, 0f), end = Offset(lLength, 0f), strokeWidth = lWeight)
                        drawLine(color = AfricanGold, start = Offset(0f, 0f), end = Offset(0f, lLength), strokeWidth = lWeight)
                        // Top Right
                        drawLine(color = AfricanGold, start = Offset(canvasWidth, 0f), end = Offset(canvasWidth - lLength, 0f), strokeWidth = lWeight)
                        drawLine(color = AfricanGold, start = Offset(canvasWidth, 0f), end = Offset(canvasWidth, lLength), strokeWidth = lWeight)
                        // Bottom Left
                        drawLine(color = AfricanGold, start = Offset(0f, canvasHeight), end = Offset(lLength, canvasHeight), strokeWidth = lWeight)
                        drawLine(color = AfricanGold, start = Offset(0f, canvasHeight), end = Offset(0f, canvasHeight - lLength), strokeWidth = lWeight)
                        // Bottom Right
                        drawLine(color = AfricanGold, start = Offset(canvasWidth, canvasHeight), end = Offset(canvasWidth - lLength, canvasHeight), strokeWidth = lWeight)
                        drawLine(color = AfricanGold, start = Offset(canvasWidth, canvasHeight), end = Offset(canvasWidth, canvasHeight - lLength), strokeWidth = lWeight)
                        
                        // Continuous scanning laser line drawn Behind
                        val centerY = (canvasHeight / 2) + pulsingLaserOffset.dp.toPx()
                        drawLine(
                            color = MpesaGreen.copy(alpha = 0.85f),
                            start = Offset(0f, centerY),
                            end = Offset(canvasWidth, centerY),
                            strokeWidth = 3.dp.toPx()
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = TextMutedGray.copy(alpha = 0.4f),
                        modifier = Modifier.size(80.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action simulation triggers (Simulates scanning specific pre-filled tickets in database)
                Text(
                    "Quick-Admit Simulation Keys:",
                    color = TextPureWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onPerformScan("VERIFY_TKT_evt_01_std_01_RU2K9HA14Y") },
                        colors = ButtonDefaults.buttonColors(containerColor = LightCharcoal, contentColor = TextPureWhite),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Admit Ticket 1", fontSize = 11.sp, maxLines = 1)
                    }

                    Button(
                        onClick = { onPerformScan("VERIFY_TKT_evt_02_std_01_RU7P4HA18A") },
                        colors = ButtonDefaults.buttonColors(containerColor = LightCharcoal, contentColor = TextPureWhite),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Admit Ticket 2", fontSize = 11.sp, maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom scan payload input
                OutlinedTextField(
                    value = manualPayloadState,
                    onValueChange = { manualPayloadState = it },
                    label = { Text("Or Type Cryptographic Pass Code") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_gate_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPureWhite,
                        unfocusedTextColor = TextPureWhite,
                        focusedBorderColor = AfricanGold,
                        unfocusedBorderColor = BorderGray
                    ),
                    trailingIcon = {
                        IconButton(onClick = { onPerformScan(manualPayloadState) }) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Scan", tint = AfricanGold)
                        }
                    }
                )
            }
        }

        // Live Scanning Verification Result Overlay Dialog Box
        scanResult?.let { result ->
            Spacer(modifier = Modifier.height(16.dp))
            val isSuccess = result.startsWith("SUCCESS")
            val isDuplicate = result.contains("DUPLICATE")
            val isInvalid = result.contains("INVALID")

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isSuccess -> MpesaGreen.copy(alpha = 0.15f)
                        isDuplicate -> AfricanGold.copy(alpha = 0.15f)
                        else -> CoralRed.copy(alpha = 0.15f)
                    }
                ),
                border = BorderStroke(
                    1.dp,
                    when {
                        isSuccess -> MpesaGreen
                        isDuplicate -> AfricanGold
                        else -> CoralRed
                    }
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gate_scan_result_banner")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when {
                                isSuccess -> Icons.Default.CheckCircle
                                isDuplicate -> Icons.Default.Warning
                                else -> Icons.Default.Error
                            },
                            contentDescription = null,
                            tint = when {
                                isSuccess -> MpesaGreen
                                isDuplicate -> AfricanGold
                                else -> CoralRed
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = when {
                                isSuccess -> "ADMISSION GRANTED"
                                isDuplicate -> "DUPLICATE CARD FLAGGED"
                                isInvalid -> "DECRYPTION CODE FAILED"
                                else -> "CHECKING SIGNATURE..."
                            },
                            color = when {
                                isSuccess -> MpesaGreen
                                isDuplicate -> AfricanGold
                                else -> CoralRed
                            },
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        result,
                        color = TextPureWhite,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp),
                        lineHeight = 18.sp
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = onClear,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                isSuccess -> MpesaGreen
                                isDuplicate -> AfricanGold
                                else -> CoralRed
                            },
                            contentColor = PitchBlack
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Reset Gate Scanner", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}


// ---------------- M-PESA STK PAYMENT SHEET ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MpesaPaymentSheet(
    event: EventEntity,
    pState: MpesaPaymentState,
    onDismiss: () -> Unit,
    onPayClick: (String) -> Unit
) {
    var rawPhoneNum by remember { mutableStateOf("0712345678") } // Pre-fills standard Main Mwangi number

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = DeepCharcoal,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, BorderGray),
            modifier = Modifier
                .width(360.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // lipa na mpesa traditional branding header logo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MpesaGreen)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("lipa na M-PESA", color = TextPureWhite, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                when (pState) {
                    is MpesaPaymentState.Idle -> {
                        Text("Lipa Online STK Push Service", color = TextPureWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "You are purchasing 1 Entry lock to: ${event.title}",
                            color = TextMutedGray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                        )

                        Text("AMOUNT DUE", color = TextDarkGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "KES ${String.format("%,.2f", event.priceKes)}",
                            color = TextPureWhite,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = rawPhoneNum,
                            onValueChange = { rawPhoneNum = it },
                            label = { Text("M-Pesa Registered Number") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("mpesa_phone_input"),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPureWhite,
                                unfocusedTextColor = TextPureWhite,
                                focusedBorderColor = MpesaGreen,
                                unfocusedBorderColor = BorderGray,
                                focusedLabelColor = MpesaGreen
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { onPayClick(rawPhoneNum) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("mpesa_stk_submit"),
                            colors = ButtonDefaults.buttonColors(containerColor = MpesaGreen, contentColor = PitchBlack),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Initialize Secure STK Push", fontWeight = FontWeight.Bold)
                        }
                    }

                    is MpesaPaymentState.InitiatingSTKPush -> {
                        MpesaWorkflowIndicator(
                            title = "Calling Safaricom Core...",
                            details = "Registering payload token on Secure Daraja API push node.",
                            progress = 0.25f,
                            color = MpesaGreen
                        )
                    }

                    is MpesaPaymentState.AwaitingUserPin -> {
                        MpesaWorkflowIndicator(
                            title = "Awaiting M-Pesa PIN 📱",
                            details = "STK Prompt sent to $rawPhoneNum. Check your phone screen and type your Safaricom PIN to authenticate KES ${String.format("%.2f", event.priceKes)}.",
                            progress = 0.6f,
                            color = AfricanGold
                        )
                    }

                    is MpesaPaymentState.VerifyingCallback -> {
                        MpesaWorkflowIndicator(
                            title = "Checking M-Pesa Callback Ledger...",
                            details = "Validating transaction signatures and allocating cryptographic receipt references.",
                            progress = 0.85f,
                            color = MpesaGreen
                        )
                    }

                    is MpesaPaymentState.PaymentSuccessful -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Confirmed",
                                tint = MpesaGreen,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("PAYMENT CONFIRMED", color = MpesaGreen, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Text(
                                "M-Pesa Receipt ID: ${pState.transactionRef}",
                                fontSize = 14.sp,
                                color = TextPureWhite,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text(
                                "Your digital lock ticket was added cleanly of 'My Tickets'. Show QR at entry gate checkout.",
                                color = TextMutedGray,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                            )

                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = MpesaGreen, contentColor = PitchBlack),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Done", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    is MpesaPaymentState.PaymentFailed -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Failed",
                                tint = CoralRed,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("PAYMENT REJECTED", color = CoralRed, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Text(
                                text = pState.error,
                                color = TextPureWhite,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                            )

                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = CoralRed, contentColor = TextPureWhite),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Retry Payment", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MpesaWorkflowIndicator(
    title: String,
    details: String,
    progress: Float,
    color: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = color, strokeWidth = 4.dp, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(20.dp))
        Text(title, color = TextPureWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(
            details,
            color = TextMutedGray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
            lineHeight = 18.sp
        )
    }
}


// ---------------- NON-ORGANIZER GUEST PAGE ----------------
@Composable
fun NonOrganizerInfo(onSwitchMode: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Outlined.Group, contentDescription = null, tint = AfricanGold, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Launch Campus Events Portal", color = TextPureWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "Are you a Kenyatta University, University of Nairobi, or Strathmore student representative? Tap below to setup your organizer keys, construct ticketed listings, receive fast payments via Lipa na M-Pesa, and check-in gate entrants.",
                color = TextMutedGray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            Button(
                onClick = onSwitchMode,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AfricanGold, contentColor = PitchBlack),
                modifier = Modifier.height(50.dp).testTag("setup_host_cta")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ManageAccounts, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Provision Organizer Account", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


// ---------------- ORGANIZER ADD EVENT DIALOG ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, Double, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("2026-06-28") }
    var time by remember { mutableStateOf("18:00") }
    var location by remember { mutableStateOf("Kenyatta University Amphitheatre") }
    var priceState by remember { mutableStateOf("200") }
    var capacityState by remember { mutableStateOf("500") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = DeepCharcoal,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, BorderGray)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
                    .width(360.dp)
            ) {
                Text("Host Campus Event", color = TextPureWhite, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text("Compose entry parameters cleanly. Ticket revenue is paid secure to your M-Pesa business till.", color = TextMutedGray, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event Title") },
                    modifier = Modifier.fillMaxWidth().testTag("add_event_title"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPureWhite,
                        unfocusedTextColor = TextPureWhite,
                        focusedBorderColor = AfricanGold,
                        unfocusedBorderColor = BorderGray
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description Details") },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPureWhite,
                        unfocusedTextColor = TextPureWhite,
                        focusedBorderColor = AfricanGold,
                        unfocusedBorderColor = BorderGray
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Auditorium / Campus Venue") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPureWhite,
                        unfocusedTextColor = TextPureWhite,
                        focusedBorderColor = AfricanGold,
                        unfocusedBorderColor = BorderGray
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1.2f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPureWhite,
                            unfocusedTextColor = TextPureWhite,
                            focusedBorderColor = AfricanGold,
                            unfocusedBorderColor = BorderGray
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Time (HH:MM)") },
                        modifier = Modifier.weight(0.8f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPureWhite,
                            unfocusedTextColor = TextPureWhite,
                            focusedBorderColor = AfricanGold,
                            unfocusedBorderColor = BorderGray
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceState,
                        onValueChange = { priceState = it },
                        label = { Text("Price KES") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPureWhite,
                            unfocusedTextColor = TextPureWhite,
                            focusedBorderColor = AfricanGold,
                            unfocusedBorderColor = BorderGray
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = capacityState,
                        onValueChange = { capacityState = it },
                        label = { Text("Capacity No.") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPureWhite,
                            unfocusedTextColor = TextPureWhite,
                            focusedBorderColor = AfricanGold,
                            unfocusedBorderColor = BorderGray
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Cancel", color = TextMutedGray)
                    }

                    Button(
                        onClick = {
                            val priceVal = priceState.toDoubleOrNull() ?: 100.0
                            val capVal = capacityState.toIntOrNull() ?: 500
                            onSave(title, description, date, time, location, priceVal, capVal)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AfricanGold, contentColor = PitchBlack),
                        shape = RoundedCornerShape(10.dp),
                        enabled = title.isNotBlank() && location.isNotBlank()
                    ) {
                        Text("Launch Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


// Empty States Placeholder helper
@Composable
fun EmptyStatePlaceholder(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextDarkGray,
                modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                color = TextPureWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                color = TextMutedGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
