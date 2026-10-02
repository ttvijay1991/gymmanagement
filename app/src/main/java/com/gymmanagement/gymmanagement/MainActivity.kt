package com.gymmanagement.gymmanagement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalContext
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.graphics.BitmapFactory
import java.io.File
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState

import androidx.compose.runtime.saveable.rememberSaveable


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.lazy.items
import android.app.Activity
import android.view.View
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import android.content.Intent
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.os.Environment
import java.util.Calendar
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.io.FileInputStream
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.content.FileProvider
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.Dp
import java.io.FileOutputStream



class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize preferences first, then lock the app to the correct orientation.
        ServerConfig.init(this)
        DataStorageConfig.init(this)
        CheckInModeConfig.init(this)

        // Normal Management Mode = Portrait only.
        // Check-In Mode = Landscape only.
        requestedOrientation = if (CheckInModeConfig.isEnabled()) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        // Existing databases are migrated here so Height/Weight are available
        // without changing the separate DatabaseHelper.kt file.
        ensureMemberBodyColumns()

        DatabaseHelper(this).createDefaultAdmin()
        AutoBackupManager.ensureScheduled(this)
        scheduleAutoClockOut(this)
        if (CheckInModeConfig.isEnabled()) {
            enterCheckInKiosk()
        }

        setContent {
            GymManagementApp()
        }
    }

    private fun ensureMemberBodyColumns() {
        try {
            val db = DatabaseHelper(this).writableDatabase
            val columns = mutableSetOf<String>()
            db.rawQuery("PRAGMA table_info(members)", null).use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameIndex >= 0) columns.add(cursor.getString(nameIndex))
                }
            }

            if ("height_cm" !in columns) {
                db.execSQL("ALTER TABLE members ADD COLUMN height_cm REAL")
            }
            if ("weight_kg" !in columns) {
                db.execSQL("ALTER TABLE members ADD COLUMN weight_kg REAL")
            }
        } catch (_: Exception) {
            // Keep the app startup safe if the database is being created for the first time.
        }
    }

    private fun enterCheckInKiosk() {
        // Check-In Mode is landscape-only.
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
    }

    private fun exitCheckInKiosk() {
        // Return to normal management mode in portrait.
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }


    /* =========================================================
   SERVER CONFIGURATION
   ========================================================= */

    object ServerConfig {
        private const val PREFS = "gym_tablet_config"
        private const val KEY_GYM_ID = "gym_id"
        private const val KEY_GYM_NAME = "gym_name"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MEMBER_PREFIX = "member_prefix"

        private lateinit var prefs: SharedPreferences

        fun init(context: Context) {
            prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }

        fun isConfigured(): Boolean =
            ::prefs.isInitialized &&
                    prefs.getString(KEY_SERVER_URL, "").orEmpty().isNotBlank() &&
                    prefs.getString(KEY_API_KEY, "").orEmpty().isNotBlank()

        fun gymId(): String = prefs.getString(KEY_GYM_ID, "").orEmpty()
        fun gymName(): String = prefs.getString(KEY_GYM_NAME, "").orEmpty()
        fun serverUrl(): String = prefs.getString(KEY_SERVER_URL, "").orEmpty().trimEnd('/')
        fun apiKey(): String = prefs.getString(KEY_API_KEY, "").orEmpty()
        fun memberPrefix(): String = prefs.getString(KEY_MEMBER_PREFIX, "").orEmpty().uppercase()

        fun save(
            gymId: String,
            gymName: String,
            serverUrl: String,
            apiKey: String,
            memberPrefix: String
        ) {
            prefs.edit()
                .putString(KEY_GYM_ID, gymId.trim().uppercase())
                .putString(KEY_GYM_NAME, gymName.trim())
                .putString(KEY_SERVER_URL, serverUrl.trim().trimEnd('/'))
                .putString(KEY_API_KEY, apiKey.trim())
                .putString(KEY_MEMBER_PREFIX, memberPrefix.trim().uppercase())
                .apply()
        }

        fun fullMemberId(numberOnly: String): String =
            memberPrefix() + numberOnly.trim()

        fun clear() {
            prefs.edit().clear().apply()
        }
    }


    /* =========================================================
   DATA
   ========================================================= */

    data class Member(

        val id: String,

        val name: String,

        val plan: String,

        val expiryDate: String,

        val remainingDays: Int,

        val checkedIn: Boolean,

        val checkInTime: String,

        val membershipActive: Boolean,

        val isTrial: Boolean = false

    )


    /* =========================================================
   APP / STORAGE ARCHITECTURE
   ========================================================= */

    enum class DataStorageMode { LOCAL, SERVER }

    object DataStorageConfig {
        private const val PREFS = "gym_data_storage_config"
        private const val KEY_MODE = "mode"
        private const val KEY_CONFIGURED = "configured"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_API_KEY = "api_key"

        private lateinit var prefs: SharedPreferences

        fun init(context: Context) {
            prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }

        fun isConfigured(): Boolean =
            ::prefs.isInitialized && prefs.getBoolean(KEY_CONFIGURED, false)

        fun mode(): DataStorageMode =
            if (prefs.getString(KEY_MODE, "LOCAL") == "SERVER") DataStorageMode.SERVER
            else DataStorageMode.LOCAL

        fun serverUrl(): String = prefs.getString(KEY_SERVER_URL, "").orEmpty().trimEnd('/')
        fun apiKey(): String = prefs.getString(KEY_API_KEY, "").orEmpty()

        fun saveLocal() {
            prefs.edit()
                .putBoolean(KEY_CONFIGURED, true)
                .putString(KEY_MODE, "LOCAL")
                .apply()
        }

        fun saveServer(serverUrl: String, apiKey: String) {
            prefs.edit()
                .putBoolean(KEY_CONFIGURED, true)
                .putString(KEY_MODE, "SERVER")
                .putString(KEY_SERVER_URL, serverUrl.trim().trimEnd('/'))
                .putString(KEY_API_KEY, apiKey.trim())
                .apply()
        }

        fun clear() {
            prefs.edit().clear().apply()
        }
    }

    object CheckInModeConfig {
        private const val PREFS = "gym_checkin_mode"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_PASSWORD = "password"
        private const val KEY_SUCCESS_MESSAGE = "success_message"
        private lateinit var prefs: SharedPreferences
        fun init(context: Context) {
            prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }

        fun isEnabled(): Boolean = ::prefs.isInitialized && prefs.getBoolean(KEY_ENABLED, false)
        fun password(): String =
            if (::prefs.isInitialized) prefs.getString(KEY_PASSWORD, "1234").orEmpty() else "1234"

        fun setEnabled(enabled: Boolean) {
            prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        }

        fun setPassword(password: String) {
            prefs.edit().putString(KEY_PASSWORD, password).apply()
        }

        fun successMessage(): String = if (::prefs.isInitialized) prefs.getString(
            KEY_SUCCESS_MESSAGE,
            "CHECK-IN DONE! HAVE A GREAT WORKOUT."
        ).orEmpty() else "CHECK-IN DONE! HAVE A GREAT WORKOUT."

        fun setSuccessMessage(message: String) {
            prefs.edit().putString(KEY_SUCCESS_MESSAGE, message).apply()
        }
    }

    @Composable
    fun ResponsiveLayout(
        portraitContent: @Composable () -> Unit,
        landscapeContent: @Composable () -> Unit
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {

            val isLandscape =
                LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

            AnimatedContent(
                targetState = isLandscape,
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(250)
                    ) + slideInHorizontally(
                        animationSpec = tween(250),
                        initialOffsetX = {
                            if (targetState) it / 4 else -it / 4
                        }
                    ) togetherWith
                            fadeOut(
                                animationSpec = tween(200)
                            )
                },
                label = "orientation_transition"
            ) { landscape ->

                if (landscape) {
                    landscapeContent()
                } else {
                    portraitContent()
                }
            }
        }
    }

    @Composable
    fun GymManagementApp() {
        val context = LocalContext.current
        val db = remember { DatabaseHelper(context) }
        var configured by remember {
            mutableStateOf(
                DataStorageConfig.isConfigured() && db.getGymSettings().isNotEmpty()
            )
        }

        if (!configured) {
            UnifiedSetupScreen(onConfigured = { configured = true })
            return
        }

        StandaloneModeApp(onBackToModeSelection = {
            configured = false
            DataStorageConfig.clear()
        })
    }

    @Composable
    fun UnifiedSetupScreen(onConfigured: () -> Unit) {
        val context = LocalContext.current
        val db = remember { DatabaseHelper(context) }

        var gymName by remember { mutableStateOf(db.getGymSettings()["name"].orEmpty()) }
        var gymPhone by remember { mutableStateOf(db.getGymSettings()["phone"].orEmpty()) }
        var gymAddress by remember { mutableStateOf(db.getGymSettings()["address"].orEmpty()) }
        var prefix by remember {
            mutableStateOf(
                db.getGymSettings()["prefix"].orEmpty().ifBlank { "AC" })
        }
        var storage by remember { mutableStateOf(DataStorageMode.LOCAL) }
        var serverUrl by remember { mutableStateOf("") }
        var apiKey by remember { mutableStateOf("") }
        var error by remember { mutableStateOf("") }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(18.dp))
            Text("GYM MANAGEMENT SYSTEM", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Set up your gym and choose where your data will be stored.")
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                gymName,
                { gymName = it; error = "" },
                label = { Text("Gym Name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                gymPhone,
                { gymPhone = it },
                label = { Text("Gym Phone") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                gymAddress,
                { gymAddress = it },
                label = { Text("Gym Address") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                prefix,
                { prefix = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(6) },
                label = { Text("Member ID Prefix *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(22.dp))
            Text("DATA STORAGE", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("You can start locally and add server sync later.", fontSize = 13.sp)

            Spacer(Modifier.height(8.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            storage == DataStorageMode.LOCAL,
                            { storage = DataStorageMode.LOCAL })
                        Column(Modifier.weight(1f)) {
                            Text("LOCAL DATABASE", fontWeight = FontWeight.Bold)
                            Text("Save gym data on this Android device.", fontSize = 13.sp)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            storage == DataStorageMode.SERVER,
                            { storage = DataStorageMode.SERVER })
                        Column(Modifier.weight(1f)) {
                            Text("SERVER DATABASE", fontWeight = FontWeight.Bold)
                            Text("Use the Windows gym server as the data source.", fontSize = 13.sp)
                        }
                    }
                }
            }

            if (storage == DataStorageMode.SERVER) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    serverUrl,
                    { serverUrl = it },
                    label = { Text("Server Address") },
                    placeholder = { Text("http://192.168.x.x:8765") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    apiKey,
                    { apiKey = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text("Server connection and data sync will use these settings.", fontSize = 12.sp)
            }

            if (error.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(error, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = {
                    when {
                        gymName.isBlank() -> error = "Gym Name is required."
                        prefix.isBlank() -> error = "Member ID Prefix is required."
                        storage == DataStorageMode.SERVER && serverUrl.isBlank() -> error =
                            "Server Address is required."

                        storage == DataStorageMode.SERVER && apiKey.isBlank() -> error =
                            "API Key is required."

                        else -> {
                            db.saveGymSettings(gymName, gymPhone, gymAddress, prefix)
                            if (storage == DataStorageMode.LOCAL) DataStorageConfig.saveLocal()
                            else DataStorageConfig.saveServer(serverUrl, apiKey)
                            db.createDefaultAdmin()
                            onConfigured()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) { Text("SAVE & CONTINUE") }
        }
    }


    @Composable
    fun StandaloneModeApp(onBackToModeSelection: () -> Unit) {

        val context = LocalContext.current
        val db = remember { DatabaseHelper(context) }

        var page by rememberSaveable {
            mutableStateOf(if (CheckInModeConfig.isEnabled()) "Check-In Mode" else "Dashboard")
        }
        var refresh by remember { mutableIntStateOf(0) }

        val drawerState = rememberDrawerState(
            initialValue = DrawerValue.Closed
        )


        val scope = rememberCoroutineScope()

        fun go(p: String) {
            page = p
            refresh++
        }

        val navigationItems = listOf(
            "Dashboard",
            "Members",
            "Plans",
            "Payments",
            "Attendance",
            "Reports",
            "Settings",
            "Backup/Restore"
        )

        ModalNavigationDrawer(

            drawerState = drawerState,

            // Swipe from the left edge in BOTH portrait
            // and landscape.
            gesturesEnabled = page != "Check-In Mode",

            drawerContent = {

                ModalDrawerSheet(
                    modifier = Modifier.width(292.dp),
                    drawerContainerColor = Color(0xFFF7F9FC)
                ) {
                    val navColors = listOf(
                        Color(0xFF2563EB), Color(0xFF7C3AED), Color(0xFF059669),
                        Color(0xFFEA580C), Color(0xFF0891B2), Color(0xFFDB2777),
                        Color(0xFF4F46E5), Color(0xFF475569)
                    )
                    val navIcons = listOf("⌂", "👥", "♛", "₹", "◉", "▣", "⚙", "↕")

                    Column(Modifier.fillMaxSize()) {
                        // Premium navigation header using the actual Aesthetic Club logo.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(148.dp)
                                .background(Color.Black)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.aesthetic_club_logo),
                                contentDescription = "Aesthetic Club",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )

                            // Subtle dark gradient at the bottom for a clean transition.
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color(0xCC07111F))
                                        )
                                    )
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "MAIN MENU",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF64748B),
                            letterSpacing = 1.5.sp
                        )

                        navigationItems.forEachIndexed { index, item ->
                            val accent = navColors[index]
                            val selected = page == item

                            NavigationDrawerItem(
                                icon = {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(if (selected) accent else accent.copy(alpha = 0.10f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            navIcons[index],
                                            fontSize = if (index == 1) 16.sp else 18.sp,
                                            color = if (selected) Color.White else accent,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                },
                                label = {
                                    Column {
                                        Text(
                                            item,
                                            fontSize = 14.sp,
                                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            color = if (selected) accent else Color(0xFF334155)
                                        )
                                        if (selected) {
                                            Text(
                                                "CURRENT PAGE",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = accent,
                                                letterSpacing = 0.8.sp
                                            )
                                        }
                                    }
                                },
                                selected = selected,
                                onClick = {
                                    go(item)
                                    scope.launch { drawerState.close() }
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = accent.copy(alpha = 0.10f),
                                    selectedTextColor = accent,
                                    selectedIconColor = accent,
                                    unselectedTextColor = Color(0xFF334155),
                                    unselectedIconColor = accent
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }

                        Spacer(Modifier.weight(1f))
                        HorizontalDivider(Modifier.padding(horizontal = 18.dp))
                        Text(
                            "Gym Management • Local / Server",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                            fontSize = 9.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

        ) {

            Column(
                modifier = Modifier.fillMaxSize()
            ) {

                // Management controls are completely hidden while Check-In Mode is active.
                // This gives the kiosk screen the full tablet display.
                if (page != "Check-In Mode") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            }
                        ) {
                            Text(text = "☰", fontSize = 27.sp)
                        }
                        Column {
                            Text(text = page, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                text = if (page == "Dashboard") "Your gym at a glance" else "Manage your gym operations",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // =========================================
                // PAGE CONTENT
                // =========================================

                AnimatedContent(

                    targetState = page,

                    transitionSpec = {

                        fadeIn(
                            animationSpec = tween(250)
                        ) +
                                slideInHorizontally(
                                    animationSpec = tween(250),
                                    initialOffsetX = { it / 6 }
                                ) togetherWith
                                fadeOut(
                                    animationSpec = tween(200)
                                )
                    },

                    label = "page_transition",

                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()

                ) { currentPage ->

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (currentPage == "Check-In Mode") 0.dp else 16.dp)
                    ) {

                        when (currentPage) {

                            "Dashboard" -> {

                                ResponsiveDashboard(
                                    db = db,
                                    onNavigate = { go(it) }
                                )
                            }

                            "Members" -> {
                                StandaloneMembers(
                                    db = db,
                                    refresh = refresh,
                                    changed = {
                                        refresh++
                                    }
                                )
                            }

                            "Plans" -> {
                                MembershipPlansScreen(
                                    db = db,
                                    refresh = refresh,
                                    changed = { refresh++ }
                                )
                            }

                            "Payments" -> {
                                StandalonePayments(
                                    db = db,
                                    refresh = refresh,
                                    changed = { refresh++ }
                                )
                            }

                            "Attendance" -> {
                                StandaloneAttendance(
                                    db = db,
                                    refresh = refresh,
                                    changed = { refresh++ }
                                )
                            }

                            "Reports" -> {
                                StandaloneReports(
                                    db = db,
                                    refresh = refresh
                                )
                            }

                            "Settings" -> {
                                GymSettingsScreen(db = db, onEnterCheckInMode = {
                                    CheckInModeConfig.setEnabled(true)
                                    enterCheckInKiosk()
                                    page = "Check-In Mode"
                                    refresh++
                                })
                            }

                            "Check-In Mode" -> {
                                CheckInModeScreen(db = db, onExit = { page = "Dashboard" })
                            }

                            "Backup/Restore" -> {
                                BackupRestoreScreen(db = db)
                            }

                            else -> {

                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {

                                    Text(
                                        text = "$currentPage screen",
                                        fontSize = 26.sp,
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

    fun csvEscape(value: String): String = "\"${value.replace("\"", "\"\"")}\""

    @Composable
    fun DashboardCard(
        title: String,
        value: String,
        modifier: Modifier = Modifier
    ) {
        val cardColor = when {
            title.contains("Check", true) -> Color(0xFFE8F5E9)
            title.contains("Active", true) -> Color(0xFFE3F2FD)
            title.contains("Trial", true) -> Color(0xFFF3E5F5)
            title.contains("Expiring", true) -> Color(0xFFFFF3E0)
            title.contains("Expired", true) -> Color(0xFFFFEBEE)
            title.contains("Collection", true) -> Color(0xFFE0F2F1)
            else -> MaterialTheme.colorScheme.surface
        }
        val accentColor = when {
            title.contains("Check", true) -> Color(0xFF2E7D32)
            title.contains("Active", true) -> Color(0xFF1565C0)
            title.contains("Trial", true) -> Color(0xFF7B1FA2)
            title.contains("Expiring", true) -> Color(0xFFEF6C00)
            title.contains("Expired", true) -> Color(0xFFC62828)
            title.contains("Collection", true) -> Color(0xFF00796B)
            else -> MaterialTheme.colorScheme.primary
        }
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when {
                            title.contains("Check", true) -> "✓"
                            title.contains("Active", true) -> "★"
                            title.contains("Trial", true) -> "T"
                            title.contains("Expiring", true) -> "!"
                            title.contains("Expired", true) -> "×"
                            title.contains("Collection", true) -> "₹"
                            else -> "•"
                        },
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(value, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }

    @Composable
    fun ResponsiveDashboard(
        db: DatabaseHelper,
        onNavigate: (String) -> Unit
    ) {

        val context = LocalContext.current

        val settings = db.getGymSettings()
        val gymName = settings["name"].orEmpty().ifBlank {
            "GYM MANAGEMENT"
        }

        val today = java.text.SimpleDateFormat(
            "dd MMM yyyy",
            java.util.Locale.US
        ).format(java.util.Date())

        val todayCheckIns = db.scalarInt(
            """
        SELECT COUNT(*)
        FROM attendance
        WHERE date(check_in) = date('now')
        """.trimIndent()
        )
        val birthdayMembers = run {
            val result = mutableListOf<Array<String>>()

            val c = db.readableDatabase.rawQuery(
                """
        SELECT member_code, name, phone
        FROM members
        WHERE active = 1
          AND dob IS NOT NULL
          AND TRIM(dob) != ''
          AND strftime('%m-%d', dob) =
              strftime('%m-%d', 'now', 'localtime')
        ORDER BY name COLLATE NOCASE ASC
        """.trimIndent(),
                null
            )

            c.use {
                while (it.moveToNext()) {
                    result.add(
                        arrayOf(
                            it.getString(0) ?: "",
                            it.getString(1) ?: "",
                            it.getString(2) ?: ""
                        )
                    )
                }
            }

            result
        }

        val activeMembers = db.scalarInt(
            """
        SELECT COUNT(DISTINCT m.id)
        FROM members m
        JOIN memberships ms ON ms.member_id = m.id
        WHERE m.active = 1
          AND ms.status = 'Active'
          AND ms.frozen = 0
          AND date(ms.start_date) <= date('now')
          AND date(ms.end_date) >= date('now')
        """.trimIndent()
        )

        val trialMembers = db.scalarInt(
            """
        SELECT COUNT(DISTINCT m.id)
        FROM members m
        JOIN memberships ms ON ms.member_id = m.id
        WHERE m.active = 1
          AND ms.status = 'Trial'
          AND ms.frozen = 0
          AND date(ms.end_date) >= date('now')
        """.trimIndent()
        )

        val expiringSoon = db.scalarInt(
            """
        SELECT COUNT(DISTINCT m.id)
        FROM members m
        JOIN memberships ms ON ms.member_id = m.id
        WHERE m.active = 1
          AND ms.is_trial = 0
          AND ms.frozen = 0
          AND date(ms.end_date) >= date('now')
          AND date(ms.end_date) <= date('now','+10 day')
        """.trimIndent()
        )

        val expiredMembers = db.scalarInt(
            """
        SELECT COUNT(DISTINCT m.id)
        FROM members m
        JOIN memberships ms ON ms.member_id = m.id
        WHERE ms.frozen = 0
          AND date(ms.end_date) < date('now')
          AND date(ms.end_date) >= date('now','-10 day')
        """.trimIndent()
        )

        val todayCollection = db.scalarDouble(
            """
        SELECT COALESCE(SUM(amount),0)
        FROM payments
        WHERE date(paid_at) = date('now')
        """.trimIndent()
        )

        val totalMembers = db.count("members")
        val totalPlans = db.count("membership_plans")
        val totalTrainers = db.count("trainers")

        val totalExpenses = db.scalarDouble(
            """
        SELECT COALESCE(SUM(amount),0)
        FROM expenses
        WHERE date(expense_date) = date('now')
        """.trimIndent()
        )

        val netToday = todayCollection - totalExpenses

        val currentlyCheckedIn = db.scalarInt(
            """
        SELECT COUNT(*)
        FROM attendance
        WHERE check_out IS NULL
        """.trimIndent()
        )

        val newMembers = db.scalarInt(
            """
        SELECT COUNT(*)
        FROM members
        WHERE date(created_at) >= date('now','-1 day')
        """.trimIndent()
        )
        val checkedInMembers = run {
            val result = mutableListOf<Array<String>>()

            val c = db.readableDatabase.rawQuery(
                """
        SELECT
            m.member_code,
            m.name,
            a.check_in
        FROM attendance a
        JOIN members m ON m.id = a.member_id
        WHERE a.check_out IS NULL
        ORDER BY a.check_in ASC
        """.trimIndent(),
                null
            )

            c.use {
                while (it.moveToNext()) {
                    result.add(
                        arrayOf(
                            it.getString(0) ?: "",
                            it.getString(1) ?: "",
                            it.getString(2) ?: ""
                        )
                    )
                }
            }

            result
        }

        val trialCheckedInMembers = run {
            val result = mutableListOf<Array<String>>()

            val c = db.readableDatabase.rawQuery(
                """
        SELECT
            m.member_code,
            m.name,
            a.check_in
        FROM attendance a
        JOIN members m ON m.id = a.member_id
        JOIN memberships ms ON ms.member_id = m.id
        WHERE a.check_out IS NULL
          AND ms.is_trial = 1
          AND ms.id = (
              SELECT MAX(ms2.id)
              FROM memberships ms2
              WHERE ms2.member_id = m.id
          )
        ORDER BY a.check_in ASC
        """.trimIndent(),
                null
            )

            c.use {
                while (it.moveToNext()) {
                    result.add(
                        arrayOf(
                            it.getString(0) ?: "",
                            it.getString(1) ?: "",
                            it.getString(2) ?: ""
                        )
                    )
                }
            }

            result
        }

        val expiringMembers = run {
            val result = mutableListOf<Array<String>>()

            val c = db.readableDatabase.rawQuery(
                """
        SELECT
            m.member_code,
            m.name,
            ms.end_date,
            CAST(
                julianday(ms.end_date) - julianday(date('now'))
                AS INTEGER
            ) AS days_left
        FROM members m
        JOIN memberships ms ON ms.member_id = m.id
        WHERE ms.is_trial = 0
          AND ms.frozen = 0
          AND date(ms.end_date) >= date('now')
          AND date(ms.end_date) <= date('now','+10 day')
          AND ms.id = (
              SELECT MAX(ms2.id)
              FROM memberships ms2
              WHERE ms2.member_id = m.id
          )
        ORDER BY date(ms.end_date) ASC
        """.trimIndent(),
                null
            )

            c.use {
                while (it.moveToNext()) {
                    result.add(
                        arrayOf(
                            it.getString(0) ?: "",
                            it.getString(1) ?: "",
                            it.getString(2) ?: "",
                            it.getString(3) ?: "0"
                        )
                    )
                }
            }

            result
        }

        val recentlyExpiredMembers = run {
            val result = mutableListOf<Array<String>>()

            val c = db.readableDatabase.rawQuery(
                """
        SELECT
            m.member_code,
            m.name,
            ms.end_date
        FROM members m
        JOIN memberships ms ON ms.member_id = m.id
        WHERE ms.is_trial = 0
          AND date(ms.end_date) < date('now')
          AND date(ms.end_date) >= date('now','-10 day')
          AND ms.id = (
              SELECT MAX(ms2.id)
              FROM memberships ms2
              WHERE ms2.member_id = m.id
          )
        ORDER BY date(ms.end_date) DESC
        """.trimIndent(),
                null
            )

            c.use {
                while (it.moveToNext()) {
                    result.add(
                        arrayOf(
                            it.getString(0) ?: "",
                            it.getString(1) ?: "",
                            it.getString(2) ?: ""
                        )
                    )
                }
            }

            result
        }
        ResponsiveLayout(

            // =====================================================
            // PORTRAIT
            // =====================================================

            portraitContent = {

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {

                        Text(
                            text = "Welcome Back, Admin!",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = gymName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                today,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(
                            modifier = Modifier.height(18.dp)
                        )

                        // ==============================
                        // STATISTICS
                        // ==============================

                        Text(
                            text = "Overview",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        DashboardCard(
                            title = "Today's Check-ins",
                            value = todayCheckIns.toString()
                        )

                        DashboardCard(
                            title = "Active Members",
                            value = activeMembers.toString()
                        )

                        DashboardCard(
                            title = "Trial Members",
                            value = trialMembers.toString()
                        )

                        DashboardCard(
                            title = "Expiring Soon",
                            value = expiringSoon.toString()
                        )

                        DashboardCard(
                            title = "Expired Members",
                            value = expiredMembers.toString()
                        )

                        DashboardCard(
                            title = "Today's Collection",
                            value = "₹%.2f".format(todayCollection)
                        )

                        Spacer(
                            modifier = Modifier.height(18.dp)
                        )

                        // ==============================
                        // QUICK ACTIONS
                        // ==============================

                        Text(
                            text = "Member Activity",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                DashboardMemberActivityCard(
                                    title = "Today's Birthdays",
                                    count = birthdayMembers.size,
                                    icon = "🎂",
                                    members = birthdayMembers,
                                    type = "BIRTHDAY"
                                )
                            }

                            item {
                                DashboardMemberActivityCard(
                                    title = "Currently Checked In",
                                    count = checkedInMembers.size,
                                    icon = "🟢",
                                    members = checkedInMembers,
                                    type = "CHECKED_IN"
                                )
                            }

                            item {
                                DashboardMemberActivityCard(
                                    title = "Trial Members Checked In",
                                    count = trialCheckedInMembers.size,
                                    icon = "🟣",
                                    members = trialCheckedInMembers,
                                    type = "TRIAL"
                                )
                            }

                            item {
                                DashboardMemberActivityCard(
                                    title = "Expiring Within 10 Days",
                                    count = expiringMembers.size,
                                    icon = "🟠",
                                    members = expiringMembers,
                                    type = "EXPIRING"
                                )
                            }

                            item {
                                DashboardMemberActivityCard(
                                    title = "Expired Last 10 Days",
                                    count = recentlyExpiredMembers.size,
                                    icon = "🔴",
                                    members = recentlyExpiredMembers,
                                    type = "EXPIRED"
                                )
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )


                        // ==============================
                        // BUSINESS SUMMARY
                        // ==============================

                        DashboardInfoCard(
                            title = "Business Summary"
                        ) {

                            DashboardInfoRow(
                                "Total Members",
                                totalMembers.toString()
                            )

                            DashboardInfoRow(
                                "Membership Plans",
                                totalPlans.toString()
                            )

                            DashboardInfoRow(
                                "Trainers",
                                totalTrainers.toString()
                            )

                            DashboardInfoRow(
                                "Currently Checked In",
                                currentlyCheckedIn.toString()
                            )

                            DashboardInfoRow(
                                "New Members",
                                newMembers.toString()
                            )

                            DashboardInfoRow(
                                "Today's Expenses",
                                "₹%.2f".format(totalExpenses)
                            )

                            DashboardInfoRow(
                                "Today's Net",
                                "₹%.2f".format(netToday)
                            )
                        }

                        Spacer(
                            modifier = Modifier.height(18.dp)
                        )

                        DashboardInfoCard(
                            title = "Membership Alerts"
                        ) {

                            DashboardInfoRow(
                                "Expiring within 10 days",
                                expiringSoon.toString()
                            )

                            DashboardInfoRow(
                                "Expired in last 10 days",
                                expiredMembers.toString()
                            )

                            DashboardInfoRow(
                                "Trial Members",
                                trialMembers.toString()
                            )
                        }

                        Spacer(
                            modifier = Modifier.height(20.dp)
                        )
                    }


                }
            },

            // =====================================================
            // LANDSCAPE
            // =====================================================

            landscapeContent = {

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                    ) {

                        Text(
                            text = "Welcome Back, Admin!",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = gymName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                today,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(
                            modifier = Modifier.height(18.dp)
                        )

                        // ==============================
                        // TOP STATISTICS
                        // ==============================

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(12.dp)
                        ) {

                            DashboardCard(
                                title = "Today's Check-ins",
                                value = todayCheckIns.toString(),
                                modifier = Modifier.weight(1f)
                            )

                            DashboardCard(
                                title = "Active Members",
                                value = activeMembers.toString(),
                                modifier = Modifier.weight(1f)
                            )

                            DashboardCard(
                                title = "Trial Members",
                                value = trialMembers.toString(),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(12.dp)
                        ) {

                            DashboardCard(
                                title = "Expiring Soon",
                                value = expiringSoon.toString(),
                                modifier = Modifier.weight(1f)
                            )

                            DashboardCard(
                                title = "Expired Members",
                                value = expiredMembers.toString(),
                                modifier = Modifier.weight(1f)
                            )

                            DashboardCard(
                                title = "Today's Collection",
                                value = "₹%.2f".format(todayCollection),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(
                            modifier = Modifier.height(20.dp)
                        )

                        // ==============================
                        // QUICK ACTIONS
                        // ==============================

                        Text(
                            text = "Member Activity",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {

                            item {

                                DashboardMemberActivityCard(
                                    title = "Currently Checked In",
                                    count = checkedInMembers.size,
                                    icon = "🟢",
                                    members = checkedInMembers,
                                    type = "CHECKED_IN"
                                )
                            }

                            item {

                                DashboardMemberActivityCard(
                                    title = "Trial Members Checked In",
                                    count = trialCheckedInMembers.size,
                                    icon = "🟣",
                                    members = trialCheckedInMembers,
                                    type = "TRIAL"
                                )
                            }

                            item {

                                DashboardMemberActivityCard(
                                    title = "Expiring Within 10 Days",
                                    count = expiringMembers.size,
                                    icon = "🟠",
                                    members = expiringMembers,
                                    type = "EXPIRING"
                                )
                            }

                            item {

                                DashboardMemberActivityCard(
                                    title = "Expired Last 10 Days",
                                    count = recentlyExpiredMembers.size,
                                    icon = "🔴",
                                    members = recentlyExpiredMembers,
                                    type = "EXPIRED"
                                )
                            }
                        }

                        // ==============================
                        // SUMMARY
                        // ==============================

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(20.dp)
                        ) {

                            DashboardInfoCard(
                                title = "Business Summary",
                                modifier = Modifier.weight(1f)
                            ) {

                                DashboardInfoRow(
                                    "Total Members",
                                    totalMembers.toString()
                                )

                                DashboardInfoRow(
                                    "Membership Plans",
                                    totalPlans.toString()
                                )

                                DashboardInfoRow(
                                    "Trainers",
                                    totalTrainers.toString()
                                )

                                DashboardInfoRow(
                                    "Currently Checked In",
                                    currentlyCheckedIn.toString()
                                )

                                DashboardInfoRow(
                                    "New Members",
                                    newMembers.toString()
                                )

                                DashboardInfoRow(
                                    "Today's Expenses",
                                    "₹%.2f".format(totalExpenses)
                                )

                                DashboardInfoRow(
                                    "Today's Net",
                                    "₹%.2f".format(netToday)
                                )
                            }

                            DashboardInfoCard(
                                title = "Membership Alerts",
                                modifier = Modifier.weight(1f)
                            ) {

                                DashboardInfoRow(
                                    "Expiring within 10 days",
                                    expiringSoon.toString()
                                )

                                DashboardInfoRow(
                                    "Expired last 10 days",
                                    expiredMembers.toString()
                                )

                                DashboardInfoRow(
                                    "Trial Members",
                                    trialMembers.toString()
                                )
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(20.dp)
                        )
                    }

                    // ==========================================
                    // ORIENTATION BUTTON
                    // ==========================================


                }
            }
        )
    }

    @Composable
    fun DashboardMemberActivityCard(
        title: String,
        count: Int,
        icon: String,
        members: List<Array<String>>,
        type: String
    ) {

        Card(
            modifier = Modifier
                .width(310.dp)
                .height(250.dp)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = icon,
                        fontSize = 20.sp
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Text(
                        text = title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "Total: $count",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                HorizontalDivider()

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                if (members.isEmpty()) {

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "No members",
                            fontSize = 14.sp
                        )
                    }

                } else {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(
                                rememberScrollState()
                            )
                    ) {

                        members.take(8).forEach { member ->

                            when (type) {

                                "CHECKED_IN",
                                "TRIAL" -> {

                                    DashboardMemberRow(
                                        memberId = member[0],
                                        name = member[1],
                                        detail = "Check-in ${member[2]}"
                                    )
                                }

                                "EXPIRING" -> {

                                    DashboardMemberRow(
                                        memberId = member[0],
                                        name = member[1],
                                        detail = "${member[2]} • ${member[3]} days"
                                    )
                                }
                                "BIRTHDAY" -> {

                                    DashboardBirthdayRow(
                                        memberId = member[0],
                                        name = member[1],
                                        phone = member[2]
                                    )
                                }

                                "EXPIRED" -> {

                                    DashboardMemberRow(
                                        memberId = member[0],
                                        name = member[1],
                                        detail = "Expired ${member[2]}"
                                    )
                                }
                            }
                        }
                    }

                    if (members.size > 8) {

                        Text(
                            text = "Swipe/scroll to view more",
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun DashboardMemberRow(
        memberId: String,
        name: String,
        detail: String
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {

            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Text(
                text = "$memberId • $detail",
                fontSize = 11.sp
            )
        }
    }
    @Composable
    fun DashboardBirthdayRow(
        memberId: String,
        name: String,
        phone: String
    ) {
        val context = LocalContext.current

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Text(
                    text = memberId,
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = {

                    var whatsappNumber =
                        phone.filter { it.isDigit() }

                    if (whatsappNumber.length == 10) {
                        whatsappNumber = "91$whatsappNumber"
                    }

                    if (whatsappNumber.isBlank()) {
                        return@Button
                    }

                    val message =
                        """
                    🎉 Happy Birthday, $name! 🎂

                    Wishing you a wonderful birthday filled with happiness, good health and success! 💪❤️

                    Have a great year ahead! Keep rocking your fitness journey! 🏋️🔥

                    – Aesthetic Club Gym
                    """.trimIndent()

                    val url =
                        "https://wa.me/$whatsappNumber?text=" +
                                java.net.URLEncoder.encode(
                                    message,
                                    "UTF-8"
                                )

                    try {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            android.net.Uri.parse(url)
                        )

                        context.startActivity(intent)

                    } catch (e: Exception) {
                        // WhatsApp is not available
                    }
                },
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(
                    horizontal = 10.dp,
                    vertical = 0.dp
                )
            ) {
                Text(
                    text = "WhatsApp",
                    fontSize = 11.sp
                )
            }
        }
    }
    @Composable
    fun DashboardInfoCard(
        title: String,
        modifier: Modifier = Modifier,
        content: @Composable ColumnScope.() -> Unit
    ) {

        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 3.dp
            )
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                content()
            }
        }
    }


    @Composable
    fun DashboardInfoRow(
        title: String,
        value: String
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = title,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp
            )

            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }


    @Composable
    fun StandalonePayments(
        db: DatabaseHelper,
        refresh: Int,
        changed: () -> Unit
    ) {
        val context = LocalContext.current
        var search by rememberSaveable { mutableStateOf("") }
        var dateFilter by rememberSaveable { mutableStateOf("TODAY") }
        var customDate by rememberSaveable { mutableStateOf("") }
        var typeFilter by rememberSaveable { mutableStateOf("ALL") }
        var methodFilter by rememberSaveable { mutableStateOf("ALL") }
        var showPaymentFilters by rememberSaveable { mutableStateOf(false) }
        var message by rememberSaveable { mutableStateOf("") }
        var expandedPaymentId by rememberSaveable { mutableStateOf("") }

        fun dateValue(offsetDays: Int): String {
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, offsetDays)
            return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(cal.time)
        }

        fun mondayOfThisWeek(): String {
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
            return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(cal.time)
        }

        fun firstDayOfMonth(): String {
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
            return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(cal.time)
        }

        val rows = remember(refresh, search, dateFilter, customDate, typeFilter, methodFilter) {
            val result = mutableListOf<Array<String>>()
            val where = StringBuilder(
                """
                WHERE (
                    ? = '' OR
                    m.member_code LIKE ? OR
                    m.name LIKE ? OR
                    m.phone LIKE ? OR
                    p.reference LIKE ?
                )
            """.trimIndent()
            )

            val args = mutableListOf<String>()
            val q = "%${search.trim()}%"
            args.add(search.trim())
            args.add(q)
            args.add(q)
            args.add(q)
            args.add(q)

            when (dateFilter) {
                "TODAY" -> {
                    where.append(" AND date(p.paid_at) = ?")
                    args.add(dateValue(0))
                }

                "YESTERDAY" -> {
                    where.append(" AND date(p.paid_at) = ?")
                    args.add(dateValue(-1))
                }

                "THIS_WEEK" -> {
                    where.append(" AND date(p.paid_at) >= ? AND date(p.paid_at) <= ?")
                    args.add(mondayOfThisWeek())
                    args.add(dateValue(0))
                }

                "THIS_MONTH" -> {
                    where.append(" AND date(p.paid_at) >= ? AND date(p.paid_at) <= ?")
                    args.add(firstDayOfMonth())
                    args.add(dateValue(0))
                }

                "CUSTOM" -> if (customDate.isNotBlank()) {
                    where.append(" AND date(p.paid_at) = ?")
                    args.add(customDate)
                }

                "ALL" -> Unit
            }

            when (typeFilter) {
                "NEW" -> {
                    where.append(" AND lower(COALESCE(p.notes,'')) LIKE '%initial%'")
                }

                "BALANCE" -> {
                    where.append(" AND lower(COALESCE(p.notes,'')) LIKE '%balance%'")
                }

                "RENEWAL" -> {
                    where.append(" AND lower(COALESCE(p.notes,'')) LIKE '%renew%'")
                }

                "ALL" -> Unit
            }

            if (methodFilter != "ALL") {
                where.append(" AND p.method = ?")
                args.add(methodFilter)
            }

            val sql = """
                SELECT
                    p.id,
                    m.member_code,
                    m.name,
                    m.phone,
                    COALESCE(mp.name, 'No Plan'),
                    COALESCE(ms.start_date, ''),
                    COALESCE(ms.end_date, ''),
                    p.amount,
                    p.method,
                    COALESCE(p.reference, ''),
                    COALESCE(p.notes, ''),
                    p.paid_at,
                    COALESCE(ms.total_amount, 0),
                    COALESCE(ms.paid_amount, 0),
                    COALESCE(ms.balance_amount, 0),
                    COALESCE(ms.status, '')
                FROM payments p
                JOIN members m ON m.id = p.member_id
                LEFT JOIN memberships ms ON ms.id = p.membership_id
                LEFT JOIN membership_plans mp ON mp.id = ms.plan_id
                ${where}
                ORDER BY p.id DESC
            """.trimIndent()

            val cursor = db.readableDatabase.rawQuery(sql, args.toTypedArray())
            cursor.use {
                while (it.moveToNext()) {
                    result.add(
                        arrayOf(
                            it.getString(0), it.getString(1), it.getString(2),
                            it.getString(3), it.getString(4), it.getString(5),
                            it.getString(6), it.getString(7), it.getString(8),
                            it.getString(9), it.getString(10), it.getString(11),
                            it.getString(12), it.getString(13), it.getString(14),
                            it.getString(15)
                        )
                    )
                }
            }
            result
        }

        fun whatsappSent(paymentId: String): Boolean {
            return context.getSharedPreferences(
                "gym_payment_whatsapp",
                Context.MODE_PRIVATE
            ).getBoolean("sent_$paymentId", false)
        }

        fun openPaymentWhatsApp(row: Array<String>) {
            try {
                val paymentId = row[0]
                val memberCode = row[1]
                val name = row[2]
                val phone = row[3]
                val plan = row[4]
                val startDate = row[5]
                val endDate = row[6]
                val paymentAmount = row[7].toDoubleOrNull() ?: 0.0
                val method = row[8]
                val reference = row[9]
                val notes = row[10]
                val paidAt = row[11]
                val total = row[12].toDoubleOrNull() ?: 0.0
                val totalPaid = row[13].toDoubleOrNull() ?: 0.0
                val balance = row[14].toDoubleOrNull() ?: 0.0
                val status = row[15]

                var whatsappNumber = phone.filter { it.isDigit() }
                if (whatsappNumber.length == 10) whatsappNumber = "91$whatsappNumber"
                if (whatsappNumber.isBlank()) {
                    message = "Phone number is not available for $name."
                    return
                }

                val paymentType = when {
                    notes.contains("balance", true) -> "Balance Payment"
                    notes.contains("renew", true) -> "Renewal Payment"
                    notes.contains("initial", true) -> "Membership Payment"
                    else -> "Payment"
                }

                val text = buildString {
                    append("Hi $name,\n\n")
                    append("Your $paymentType has been recorded successfully. 💪\n\n")
                    append("Member ID: $memberCode\n")
                    append("Plan: $plan\n")
                    append("Payment Amount: ₹%.2f\n".format(paymentAmount))
                    append("Payment Method: $method\n")
                    append("Total Amount: ₹%.2f\n".format(total))
                    append("Total Paid: ₹%.2f\n".format(totalPaid))
                    append("Balance: ₹%.2f\n".format(balance))
                    if (startDate.isNotBlank()) append("Start Date: $startDate\n")
                    if (endDate.isNotBlank()) append("Valid Until: $endDate\n")
                    append("Status: ${if (status.isBlank()) "UPDATED" else status}\n")
                    append("Payment Date: $paidAt\n")
                    if (reference.isNotBlank()) append("Reference: $reference\n")
                    append("\nThank you! 💪")
                    append("\nShare your GYM Experience on google https://g.page/r/CYFBIcYM55V_ECE/review")
                }

                val uri = Uri.parse(
                    "https://wa.me/$whatsappNumber?text=${Uri.encode(text)}"
                )
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))

                context.getSharedPreferences(
                    "gym_payment_whatsapp",
                    Context.MODE_PRIVATE
                ).edit().putBoolean("sent_$paymentId", true).apply()
            } catch (e: Exception) {

            }
        }

        fun openCustomDatePicker() {
            val cal = java.util.Calendar.getInstance()
            if (customDate.isNotBlank()) {
                try {
                    val parsed = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        .parse(customDate)
                    if (parsed != null) cal.time = parsed
                } catch (_: Exception) {
                }
            }
            android.app.DatePickerDialog(
                context,
                { _, year, month, day ->
                    val picked = java.util.Calendar.getInstance().apply {
                        set(year, month, day)
                    }
                    customDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        .format(picked.time)
                    dateFilter = "CUSTOM"
                },
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH),
                cal.get(java.util.Calendar.DAY_OF_MONTH)
            ).show()
        }

        fun dateLabel(): String = when (dateFilter) {
            "TODAY" -> "Today"
            "YESTERDAY" -> "Yesterday"
            "THIS_WEEK" -> "This Week"
            "THIS_MONTH" -> "This Month"
            "CUSTOM" -> if (customDate.isBlank()) "Custom Date" else customDate
            else -> "All Dates"
        }

        fun typeLabel(): String = when (typeFilter) {
            "NEW" -> "New Membership"
            "BALANCE" -> "Balance Payment"
            "RENEWAL" -> "Renewal"
            else -> "All Types"
        }

        val totalCollected = rows.sumOf { it[7].toDoubleOrNull() ?: 0.0 }
        val newPayments = rows.count { it[10].contains("initial", true) }
        val balancePayments = rows.count { it[10].contains("balance", true) }
        val renewalPayments = rows.count { it[10].contains("renew", true) }

        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1565C0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Payments & Receipts",
                                        color = Color.White,
                                        fontSize = 27.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Track collections, payments and member transactions",
                                        color = Color.White.copy(alpha = 0.88f),
                                        fontSize = 13.sp
                                    )
                                }
                                Box(
                                    Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "₹",
                                        color = Color.White,
                                        fontSize = 27.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PaymentSummaryCard(
                            title = "Collected",
                            value = "₹%.2f".format(totalCollected),
                            subtitle = "Filtered period",
                            accent = Color(0xFF16A34A),
                            icon = "₹",
                            modifier = Modifier.weight(1f)
                        )
                        PaymentSummaryCard(
                            title = "Transactions",
                            value = rows.size.toString(),
                            subtitle = "Payments",
                            accent = Color(0xFF2563EB),
                            icon = "#",
                            modifier = Modifier.weight(1f)
                        )
                        PaymentSummaryCard(
                            title = "Balance",
                            value = balancePayments.toString(),
                            subtitle = "Balance payments",
                            accent = Color(0xFFF59E0B),
                            icon = "!",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PaymentTypeMiniCard(
                            "NEW",
                            newPayments,
                            Color(0xFF7C3AED),
                            Modifier.weight(1f)
                        )
                        PaymentTypeMiniCard(
                            "RENEWAL",
                            renewalPayments,
                            Color(0xFF0891B2),
                            Modifier.weight(1f)
                        )
                        PaymentTypeMiniCard(
                            "BALANCE",
                            balancePayments,
                            Color(0xFFEA580C),
                            Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        label = { Text("Search Member / ID / Phone / Reference") },
                        leadingIcon = { Text("⌕", fontSize = 23.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "FILTER PAYMENTS",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF475569)
                                    )
                                    Text(
                                        "${dateLabel()}  •  ${typeLabel()}  •  ${if (methodFilter == "ALL") "All Methods" else methodFilter}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Button(
                                    onClick = { showPaymentFilters = !showPaymentFilters },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(if (showPaymentFilters) "HIDE" else "FILTER")
                                }
                            }

                            AnimatedVisibility(
                                visible = showPaymentFilters,
                                enter = fadeIn() + scaleIn(),
                                exit = fadeOut() + scaleOut()
                            ) {
                                Column(Modifier.fillMaxWidth()) {
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "PAYMENT DATE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF64748B)
                                    )
                                    Spacer(Modifier.height(5.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                        item {
                                            FilterChip(
                                                selected = dateFilter == "TODAY",
                                                onClick = { dateFilter = "TODAY" },
                                                label = { Text("Today") })
                                        }
                                        item {
                                            FilterChip(
                                                selected = dateFilter == "YESTERDAY",
                                                onClick = { dateFilter = "YESTERDAY" },
                                                label = { Text("Yesterday") })
                                        }
                                        item {
                                            FilterChip(
                                                selected = dateFilter == "THIS_WEEK",
                                                onClick = { dateFilter = "THIS_WEEK" },
                                                label = { Text("This Week") })
                                        }
                                        item {
                                            FilterChip(
                                                selected = dateFilter == "THIS_MONTH",
                                                onClick = { dateFilter = "THIS_MONTH" },
                                                label = { Text("This Month") })
                                        }
                                        item {
                                            FilterChip(
                                                selected = dateFilter == "CUSTOM",
                                                onClick = { openCustomDatePicker() },
                                                label = { Text(if (dateFilter == "CUSTOM") dateLabel() else "📅 Custom") })
                                        }
                                        item {
                                            FilterChip(
                                                selected = dateFilter == "ALL",
                                                onClick = { dateFilter = "ALL" },
                                                label = { Text("All Dates") })
                                        }
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        "PAYMENT TYPE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF64748B)
                                    )
                                    Spacer(Modifier.height(5.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                        item {
                                            FilterChip(
                                                selected = typeFilter == "ALL",
                                                onClick = { typeFilter = "ALL" },
                                                label = { Text("All Types") })
                                        }
                                        item {
                                            FilterChip(
                                                selected = typeFilter == "NEW",
                                                onClick = { typeFilter = "NEW" },
                                                label = { Text("New Membership") })
                                        }
                                        item {
                                            FilterChip(
                                                selected = typeFilter == "BALANCE",
                                                onClick = { typeFilter = "BALANCE" },
                                                label = { Text("Balance Payment") })
                                        }
                                        item {
                                            FilterChip(
                                                selected = typeFilter == "RENEWAL",
                                                onClick = { typeFilter = "RENEWAL" },
                                                label = { Text("Renewal") })
                                        }
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        "PAYMENT METHOD",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF64748B)
                                    )
                                    Spacer(Modifier.height(5.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                        listOf(
                                            "ALL",
                                            "Cash",
                                            "UPI",
                                            "Card",
                                            "Bank Transfer"
                                        ).forEach { method ->
                                            item {
                                                FilterChip(
                                                    selected = methodFilter == method,
                                                    onClick = { methodFilter = method },
                                                    label = { Text(if (method == "ALL") "All Methods" else method) }
                                                )
                                            }
                                        }
                                    }
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = {
                                            dateFilter = "TODAY"
                                            customDate = ""
                                            typeFilter = "ALL"
                                            methodFilter = "ALL"
                                        }) { Text("CLEAR FILTERS") }
                                    }
                                }
                            }
                        }
                    }
                }

                if (message.isNotBlank()) {
                    item {
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                        ) {
                            Text(
                                message,
                                Modifier.padding(12.dp),
                                color = Color(0xFF166534),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (rows.isEmpty()) {
                    item {
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                        ) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(35.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "₹",
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF94A3B8)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "No payment records found",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Try changing the date or payment filters.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                } else {
                    items(count = rows.size, key = { rows[it][0] }) { index ->
                        val row = rows[index]
                        val sent = whatsappSent(row[0])
                        val amount = row[7].toDoubleOrNull() ?: 0.0
                        val paymentType = when {
                            row[10].contains("balance", true) -> "BALANCE PAYMENT"
                            row[10].contains("renew", true) -> "RENEWAL"
                            row[10].contains("initial", true) -> "NEW MEMBERSHIP"
                            else -> "PAYMENT"
                        }
                        val accent = when {
                            paymentType == "BALANCE PAYMENT" -> Color(0xFFEA580C)
                            paymentType == "RENEWAL" -> Color(0xFF0891B2)
                            paymentType == "NEW MEMBERSHIP" -> Color(0xFF7C3AED)
                            else -> Color(0xFF2563EB)
                        }

                        val expanded = expandedPaymentId == row[0]
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedPaymentId = if (expanded) "" else row[0]
                                },
                            shape = RoundedCornerShape(if (expanded) 24.dp else 18.dp),
                            // elevation = CardDefaults.cardElevation(defaultElevation = if (expanded) 7.dp else 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (expanded) accent.copy(alpha = 0.045f) else Color.White
                            )
                        ) {
                            Row(Modifier.fillMaxWidth()) {
                                Box(Modifier
                                    .width(7.dp)
                                    .fillMaxHeight()
                                    .background(accent))
                                Column(Modifier.padding(15.dp).weight(1f)) {
                                    // COLLAPSED: show only Name, Member ID and Amount.
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                row[2],
                                                fontSize = if (expanded) 20.sp else 17.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Spacer(Modifier.height(3.dp))
                                            Text(
                                                "Member ID: ${row[1]}",
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                "₹%.2f".format(amount),
                                                fontSize = if (expanded) 23.sp else 20.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = accent
                                            )
                                            Text(
                                                if (expanded) "▲ HIDE DETAILS" else "▼ VIEW DETAILS",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = accent
                                            )
                                        }
                                    }

                                    AnimatedVisibility(
                                        visible = expanded,
                                        enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                                        exit = fadeOut(tween(120)) + shrinkVertically(tween(180))
                                    ) {
                                        Column {
                                            Spacer(Modifier.height(12.dp))
                                            HorizontalDivider()
                                            Spacer(Modifier.height(12.dp))

                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                PaymentDetailChip(
                                                    "DATE",
                                                    row[11].ifBlank { "-" },
                                                    Color(0xFF2563EB),
                                                    Modifier.weight(1f)
                                                )
                                                PaymentDetailChip(
                                                    "METHOD",
                                                    row[8].ifBlank { "-" },
                                                    Color(0xFF16A34A),
                                                    Modifier.weight(1f)
                                                )
                                                PaymentDetailChip(
                                                    "TYPE",
                                                    paymentType,
                                                    accent,
                                                    Modifier.weight(1f)
                                                )
                                            }

                                            Spacer(Modifier.height(10.dp))
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                PaymentDetailChip(
                                                    "REFERENCE",
                                                    row[9].ifBlank { "-" },
                                                    Color(0xFF7C3AED),
                                                    Modifier.weight(1f)
                                                )
                                                PaymentDetailChip(
                                                    "PLAN",
                                                    row[4].ifBlank { "-" },
                                                    Color(0xFF0891B2),
                                                    Modifier.weight(1f)
                                                )
                                            }

                                            Spacer(Modifier.height(10.dp))
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                PaymentAmountInfo(
                                                    "TOTAL",
                                                    "₹%.2f".format(row[12].toDoubleOrNull() ?: 0.0),
                                                    Modifier.weight(1f)
                                                )
                                                PaymentAmountInfo(
                                                    "PAID",
                                                    "₹%.2f".format(row[13].toDoubleOrNull() ?: 0.0),
                                                    Modifier.weight(1f)
                                                )
                                                PaymentAmountInfo(
                                                    "BALANCE",
                                                    "₹%.2f".format(row[14].toDoubleOrNull() ?: 0.0),
                                                    Modifier.weight(1f)
                                                )
                                            }

                                            if (row[10].isNotBlank()) {
                                                Spacer(Modifier.height(8.dp))
                                                Text(
                                                    "Notes: ${row[10]}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }

                                            Spacer(Modifier.height(10.dp))
                                            Text(
                                                "${row[15].ifBlank { "UPDATED" }} • Valid until ${row[6].ifBlank { "-" }}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF475569)
                                            )

                                            Spacer(Modifier.height(10.dp))
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                if (!sent) {
                                                    Button(
                                                        onClick = { openPaymentWhatsApp(row) },
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Text("WHATSAPP")
                                                    }
                                                } else {
                                                    OutlinedButton(
                                                        onClick = { },
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Text("SENT ✓")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }
    }

    @Composable
    fun PaymentSummaryCard(
        title: String,
        value: String,
        subtitle: String,
        accent: Color,
        icon: String,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.10f)),
//            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accent),
                        contentAlignment = Alignment.Center
                    ) { Text(icon, color = Color.White, fontWeight = FontWeight.ExtraBold) }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent,
                    maxLines = 1
                )
                Text(subtitle, fontSize = 9.sp, color = Color(0xFF64748B))
            }
        }
    }

    @Composable
    fun PaymentTypeMiniCard(
        title: String,
        count: Int,
        accent: Color,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(accent))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        count.toString(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent
                    )
                }
            }
        }
    }

    @Composable
    fun PaymentDetailChip(
        label: String,
        value: String,
        accent: Color,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.08f))
                .padding(9.dp)
        ) {
            Text(label, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = accent)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }

    @Composable
    fun PaymentAmountInfo(
        label: String,
        value: String,
        modifier: Modifier = Modifier
    ) {
        Column(modifier = modifier) {
            Text(
                label,
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF64748B)
            )
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }

    @Composable
    fun StandaloneAttendance(
        db: DatabaseHelper,
        refresh: Int,
        changed: () -> Unit
    ) {
        val context = LocalContext.current


        var search by rememberSaveable { mutableStateOf("") }
        var dateFilter by rememberSaveable { mutableStateOf("TODAY") }
        var customDate by rememberSaveable { mutableStateOf("") }
        var message by rememberSaveable { mutableStateOf("") }
        var memberCode by rememberSaveable { mutableStateOf("") }

        fun dateValue(offsetDays: Int): String {
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, offsetDays)
            return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(cal.time)
        }

        fun mondayOfThisWeek(): String {
            val cal = java.util.Calendar.getInstance()
            cal.firstDayOfWeek = java.util.Calendar.MONDAY
            cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
            return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(cal.time)
        }

        val rows = remember(refresh, search, dateFilter, customDate) {
            val result = mutableListOf<Array<String>>()
            val args = mutableListOf<String>()
            val where = StringBuilder("WHERE 1=1")
            val q = "%${search.trim()}%"
            if (search.isNotBlank()) {
                where.append(" AND (m.member_code LIKE ? OR m.name LIKE ? OR m.phone LIKE ?)")
                args.add(q); args.add(q); args.add(q)
            }
            when (dateFilter) {
                "TODAY" -> {
                    where.append(" AND date(a.check_in)=?"); args.add(dateValue(0))
                }

                "YESTERDAY" -> {
                    where.append(" AND date(a.check_in)=?"); args.add(dateValue(-1))
                }

                "THIS_WEEK" -> {
                    where.append(" AND date(a.check_in)>=? AND date(a.check_in)<=?"); args.add(
                        mondayOfThisWeek()
                    ); args.add(dateValue(0))
                }

                "THIS_MONTH" -> {
                    val cal = java.util.Calendar.getInstance(); cal.set(
                        java.util.Calendar.DAY_OF_MONTH,
                        1
                    )
                    val first = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        .format(cal.time)
                    where.append(" AND date(a.check_in)>=? AND date(a.check_in)<=?"); args.add(first); args.add(
                        dateValue(0)
                    )
                }

                "CUSTOM" -> if (customDate.isNotBlank()) {
                    where.append(" AND date(a.check_in)=?"); args.add(customDate)
                }
            }
            val sql = """
                SELECT a.id, m.member_code, m.name, COALESCE(mp.name,'No Plan'),
       CASE WHEN COALESCE(ms.is_trial,0)=1 THEN 'TRIAL'
            WHEN COALESCE(ms.status,'')='Pending' THEN 'BALANCE PENDING'
            WHEN COALESCE(m.active,1)=0 THEN 'INACTIVE' ELSE 'ACTIVE' END,
       a.check_in,
       COALESCE(a.check_out,''),
       COALESCE(a.workout_hours,0),
       COALESCE(a.source,'Manual')
                FROM attendance a JOIN members m ON m.id=a.member_id
                LEFT JOIN memberships ms ON ms.id=(SELECT MAX(ms2.id) FROM memberships ms2 WHERE ms2.member_id=m.id)
                LEFT JOIN membership_plans mp ON mp.id=ms.plan_id $where ORDER BY a.id DESC
            """.trimIndent()
            db.readableDatabase.rawQuery(sql, args.toTypedArray()).use {
                while (it.moveToNext()) result.add(Array(9) { index -> it.getString(index) ?: "" })
            }
            result
        }

        fun checkIn(code: String) {
            try {
                val ok = db.checkIn(code); message =
                    if (ok) "${code.uppercase()} checked in successfully." else "${code.uppercase()} is already checked in."; changed()
            } catch (e: Exception) {
                message = e.message.orEmpty().ifBlank { "Unable to check in." }
            }
        }

        fun checkOut(code: String) {
            try {
                val ok = db.checkOut(code); message =
                    if (ok) "${code.uppercase()} checked out successfully." else "No open check-in found."; changed()
            } catch (e: Exception) {
                message = e.message.orEmpty().ifBlank { "Unable to check out." }
            }
        }

        val checkedInCount = rows.count { it[6].isBlank() }
        val checkedOutCount = rows.count { it[6].isNotBlank() }
        val trialCount = rows.count { it[4] == "TRIAL" }
        val activeCount = rows.count { it[4] == "ACTIVE" }


        Column(Modifier.fillMaxSize()) {
            Card(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF2563EB)), contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "✓",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Attendance",
                            fontSize = 27.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            "Track daily check-ins and check-outs",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Surface(shape = RoundedCornerShape(14.dp), color = Color.White) {
                        Text(
                            "${rows.size} RECORDS",
                            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF2563EB)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AttendanceStatCard(
                    "CHECKED IN",
                    checkedInCount.toString(),
                    "Currently inside",
                    Color(0xFFDCFCE7),
                    Color(0xFF15803D),
                    Modifier.weight(1f)
                )
                AttendanceStatCard(
                    "CHECKED OUT",
                    checkedOutCount.toString(),
                    "Completed visits",
                    Color(0xFFE0F2FE),
                    Color(0xFF0369A1),
                    Modifier.weight(1f)
                )
                AttendanceStatCard(
                    "ACTIVE",
                    activeCount.toString(),
                    "Membership status",
                    Color(0xFFEDE9FE),
                    Color(0xFF7C3AED),
                    Modifier.weight(1f)
                )
                AttendanceStatCard(
                    "TRIAL",
                    trialCount.toString(),
                    "Trial visits",
                    Color(0xFFFFF7ED),
                    Color(0xFFEA580C),
                    Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        "QUICK ATTENDANCE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF475569)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            memberCode,
                            { memberCode = it.uppercase() },
                            label = { Text("Member ID") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        Button(
                            onClick = { if (memberCode.isNotBlank()) checkIn(memberCode) },
                            modifier = Modifier.height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                        ) { Text("CHECK IN", fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = { if (memberCode.isNotBlank()) checkOut(memberCode) },
                            modifier = Modifier.height(54.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("CHECK OUT", fontWeight = FontWeight.Bold) }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "DATE FILTER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF475569),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Row(
                            Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            listOf(
                                "TODAY" to "Today",
                                "YESTERDAY" to "Yesterday",
                                "THIS_WEEK" to "This Week",
                                "THIS_MONTH" to "This Month",
                                "CUSTOM" to "Custom"
                            ).forEach { (key, label) ->
                                if (dateFilter == key) Button(
                                    onClick = { dateFilter = key },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(
                                        horizontal = 13.dp,
                                        vertical = 7.dp
                                    )
                                ) { Text(label, fontSize = 11.sp) }
                                else OutlinedButton(
                                    onClick = { dateFilter = key },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(
                                        horizontal = 13.dp,
                                        vertical = 7.dp
                                    )
                                ) { Text(label, fontSize = 11.sp) }
                            }
                        }
                    }
                    if (dateFilter == "CUSTOM") {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            customDate,
                            { customDate = it },
                            label = { Text("Date (yyyy-MM-dd)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                search,
                { search = it },
                label = { Text("Search Member ID, Name or Phone") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            if (message.isNotBlank()) {
                Spacer(Modifier.height(6.dp)); Text(
                    message,
                    color = Color(0xFF2563EB),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(10.dp))

            Box(Modifier.fillMaxSize()) {
                Column(Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState())) {
                    val minWidth = 1120.dp
                    Surface(
                        Modifier.width(minWidth),
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "MEMBER ID",
                                Modifier.width(110.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(
                                "MEMBER",
                                Modifier.width(180.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(
                                "PLAN",
                                Modifier.width(150.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(
                                "STATUS",
                                Modifier.width(150.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(
                                "CHECK IN",
                                Modifier.width(150.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(
                                "CHECK OUT",
                                Modifier.width(150.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(
                                "WORK HOURS",
                                Modifier.width(120.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(
                                "SOURCE",
                                Modifier.width(100.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    if (rows.isEmpty()) {
                        Card(
                            Modifier.width(minWidth),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                        ) {
                            Text(
                                "No attendance records found.",
                                Modifier.padding(24.dp),
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            rows.forEach { row ->
                                val statusColor = when (row[4]) {
                                    "TRIAL" -> Color(0xFFEA580C); "BALANCE PENDING" -> Color(
                                        0xFFD97706
                                    ); "INACTIVE" -> Color(0xFFDC2626); else -> Color(0xFF16A34A)
                                }
                                Card(
                                    Modifier.width(minWidth),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(1.dp)
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            row[1],
                                            Modifier.width(110.dp),
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF1E3A8A)
                                        )
                                        Text(
                                            row[2],
                                            Modifier.width(180.dp),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            row[3],
                                            Modifier.width(150.dp),
                                            color = Color(0xFF475569)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = statusColor.copy(alpha = 0.12f),
                                            modifier = Modifier.width(150.dp)
                                        ) {
                                            Text(
                                                row[4],
                                                Modifier.padding(
                                                    horizontal = 9.dp,
                                                    vertical = 6.dp
                                                ),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = statusColor
                                            )
                                        }
                                        Text(row[5], Modifier.width(150.dp), fontSize = 12.sp)
                                        Text(
                                            if (row[6].isBlank()) "OPEN" else row[6],
                                            Modifier.width(150.dp),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (row[6].isBlank()) {
                                                Color(0xFF16A34A)
                                            } else {
                                                Color(0xFF475569)
                                            }
                                        )

                                        Text(
                                            if (row[6].isBlank()) {
                                                "IN PROGRESS"
                                            } else {
                                                val hours = row[7].toDoubleOrNull() ?: 0.0
                                                val totalMinutes = (hours * 60).toInt()
                                                val h = totalMinutes / 60
                                                val m = totalMinutes % 60

                                                if (h > 0) {
                                                    "$h hr $m min"
                                                } else {
                                                    "$m min"
                                                }
                                            },
                                            Modifier.width(120.dp),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2563EB)
                                        )

                                        Text(
                                            row[8],
                                            Modifier.width(100.dp),
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(5.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun AttendanceStatCard(
        title: String,
        value: String,
        subtitle: String,
        background: Color,
        accent: Color,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier,
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = background),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(title, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = accent)
                Spacer(Modifier.height(4.dp)); Text(
                value,
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A)
            ); Text(subtitle, fontSize = 10.sp, color = Color(0xFF64748B))
            }
        }
    }

    @Composable
    fun PlanSummaryCard(
        title: String,
        value: String,
        background: Color,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(containerColor = background),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(value, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }

    @Composable
    fun MembershipPlansScreen(
        db: DatabaseHelper,
        refresh: Int,
        changed: () -> Unit
    ) {
        var showAddPlan by rememberSaveable { mutableStateOf(false) }
        var search by rememberSaveable { mutableStateOf("") }
        var editPlanId by rememberSaveable { mutableStateOf<Long?>(null) }

        val plans = remember(refresh) { db.getPlans() }
        val filteredPlans = plans.filter {
            search.isBlank() || it.getOrNull(1).orEmpty().contains(search.trim(), true)
        }

        if (editPlanId != null) {
            EditMembershipPlanScreen(
                db = db,
                planId = editPlanId!!,
                onBack = { editPlanId = null },
                onSaved = {
                    editPlanId = null
                    changed()
                }
            )
            return
        }

        if (showAddPlan) {
            AddMembershipPlanScreen(
                db = db,
                onBack = { showAddPlan = false },
                onSaved = {
                    showAddPlan = false
                    changed()
                }
            )
            return
        }

        ResponsiveLayout(
            portraitContent = {
                MembershipPlansContent(
                    plans = filteredPlans,
                    search = search,
                    onSearch = { search = it },
                    onAdd = { showAddPlan = true },
                    onEdit = { editPlanId = it },
                    onToggle = {
                        db.togglePlan(it)
                        changed()
                    },
                    landscape = false
                )
            },
            landscapeContent = {
                MembershipPlansContent(
                    plans = filteredPlans,
                    search = search,
                    onSearch = { search = it },
                    onAdd = { showAddPlan = true },
                    onEdit = { editPlanId = it },
                    onToggle = {
                        db.togglePlan(it)
                        changed()
                    },
                    landscape = true
                )
            }
        )
    }

    @Composable
    fun MembershipPlansContent(
        plans: List<Array<String>>,
        search: String,
        onSearch: (String) -> Unit,
        onAdd: () -> Unit,
        onEdit: (Long) -> Unit,
        onToggle: (Long) -> Unit,
        landscape: Boolean = false
    ) {
        val keyboardController = LocalSoftwareKeyboardController.current
        val searchFocusRequester = remember { FocusRequester() }
        val activeCount = plans.count { it.getOrNull(4) == "1" }
        val trialCount = plans.count { it.getOrNull(5) == "1" }
        val coupleGroupCount = plans.count {
            val type = it.getOrNull(6).orEmpty().uppercase()
            type == "COUPLE" || type == "GROUP"
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(if (landscape) 20.dp else 16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Membership Plans",
                        fontSize = if (landscape) 28.sp else 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "Create and manage your gym membership offers",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onAdd,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("+ ADD PLAN", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PlanSummaryCard(
                    "TOTAL PLANS",
                    plans.size.toString(),
                    Color(0xFFE8F0FF),
                    Modifier.weight(1f)
                )
                PlanSummaryCard(
                    "ACTIVE",
                    activeCount.toString(),
                    Color(0xFFE5F8ED),
                    Modifier.weight(1f)
                )
                PlanSummaryCard(
                    "TRIAL",
                    trialCount.toString(),
                    Color(0xFFFFF4D8),
                    Modifier.weight(1f)
                )
                PlanSummaryCard(
                    "COUPLE / GROUP",
                    coupleGroupCount.toString(),
                    Color(0xFFF0E9FF),
                    Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = search,
                onValueChange = onSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(searchFocusRequester),
                singleLine = true,
                label = { Text("Search Plans") },
                placeholder = { Text("Search by plan name") },
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { keyboardController?.hide() }
                )
            )

            Spacer(Modifier.height(16.dp))

            if (plans.isEmpty()) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F7FB)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "NO MEMBERSHIP PLANS",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Create your first plan to start assigning memberships.",
                            fontSize = 13.sp
                        )
                    }
                }
            } else if (landscape) {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    plans.chunked(2).forEach { rowPlans ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowPlans.forEach { plan ->
                                MembershipPlanCard(
                                    plan = plan,
                                    onEdit = onEdit,
                                    onToggle = onToggle,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowPlans.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    plans.forEach { plan ->
                        MembershipPlanCard(plan = plan, onEdit = onEdit, onToggle = onToggle)
                    }
                }
            }
        }
    }

    @Composable
    fun MembershipPlanCard(
        plan: Array<String>,
        onEdit: (Long) -> Unit,
        onToggle: (Long) -> Unit,
        modifier: Modifier = Modifier
    ) {
        val id = plan.getOrNull(0)?.toLongOrNull() ?: return
        val name = plan.getOrNull(1).orEmpty().ifBlank { "Unnamed Plan" }
        val months = plan.getOrNull(2)?.toIntOrNull() ?: 0
        val price = plan.getOrNull(3)?.toDoubleOrNull() ?: 0.0
        val active = plan.getOrNull(4) == "1"
        val trial = plan.getOrNull(5) == "1"
        val planType = plan.getOrNull(6).orEmpty().ifBlank { "INDIVIDUAL" }.uppercase()
        val memberCount = plan.getOrNull(7)?.toIntOrNull() ?: 1

        val accent = when {
            trial -> Color(0xFFF59E0B)
            !active -> Color(0xFFEF4444)
            planType == "COUPLE" -> Color(0xFF8B5CF6)
            planType == "GROUP" -> Color(0xFF14B8A6)
            else -> Color(0xFF2563EB)
        }
        val soft = when {
            trial -> Color(0xFFFFF7E0)
            !active -> Color(0xFFFFECEC)
            planType == "COUPLE" -> Color(0xFFF3EEFF)
            planType == "GROUP" -> Color(0xFFE9FBF8)
            else -> Color(0xFFEDF4FF)
        }

        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = soft),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Column(Modifier
                .fillMaxWidth()
                .padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            when (planType) {
                                "COUPLE" -> "2"
                                "GROUP" -> memberCount.toString()
                                else -> "1"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            when {
                                trial -> "TRIAL PLAN"
                                planType == "COUPLE" -> "COUPLE • 2 MEMBERS"
                                planType == "GROUP" -> "GROUP • $memberCount MEMBERS"
                                months == 1 -> "INDIVIDUAL • 1 MONTH"
                                else -> "INDIVIDUAL • $months MONTHS"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                    }
                    Surface(
                        color = if (active) Color(0xFFDDF7E8) else Color(0xFFFFDCDC),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            if (active) "ACTIVE" else "INACTIVE",
                            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (active) Color(0xFF16803C) else Color(0xFFC62828)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text("PLAN PRICE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (trial) "FREE" else "₹%.2f".format(price),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accent
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("DURATION", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (trial) "Trial" else "$months month${if (months == 1) "" else "s"}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            when (planType) {
                                "COUPLE" -> "Shared plan • 2 members"
                                "GROUP" -> "Shared plan • up to $memberCount members"
                                else -> "Single member plan"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (trial) "Automatic trial membership" else "Available for assignment",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!trial) {
                        Switch(checked = active, onCheckedChange = { onToggle(id) })
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { onEdit(id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("EDIT PLAN", fontWeight = FontWeight.Bold)
                }
            }
        }
    }


    @Composable
    fun EditMembershipPlanScreen(
        db: DatabaseHelper,
        planId: Long,
        onBack: () -> Unit,
        onSaved: () -> Unit
    ) {
        var name by rememberSaveable { mutableStateOf("") }
        var monthsText by rememberSaveable { mutableStateOf("") }
        var priceText by rememberSaveable { mutableStateOf("") }
        var planType by rememberSaveable { mutableStateOf("INDIVIDUAL") }
        var memberCountText by rememberSaveable { mutableStateOf("2") }
        var error by rememberSaveable { mutableStateOf("") }

        LaunchedEffect(planId) {
            val c = db.readableDatabase.rawQuery(
                "SELECT name,months,price,plan_type,member_count FROM membership_plans WHERE id=?",
                arrayOf(planId.toString())
            )

            c.use {
                if (it.moveToFirst()) {
                    name = it.getString(0) ?: ""
                    monthsText = it.getInt(1).toString()
                    priceText = it.getDouble(2).toString()
                    planType = it.getString(3) ?: "INDIVIDUAL"
                    memberCountText = it.getInt(4).toString()
                }
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onBack) {
                    Text("← Back")
                }

                Spacer(Modifier.width(12.dp))

                Text(
                    "Edit Membership Plan",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(20.dp))

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "Plan Details",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            error = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Plan Name *") }
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = monthsText,
                        onValueChange = {
                            monthsText = it.filter(Char::isDigit)
                            error = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Duration (Months) *") }
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = priceText,
                        onValueChange = {
                            priceText = it.filter { c ->
                                c.isDigit() || c == '.'
                            }
                            error = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Price (₹) *") }
                    )

                    Spacer(Modifier.height(16.dp))
                    Text("PLAN TYPE", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("INDIVIDUAL", "COUPLE", "GROUP").forEach { type ->
                            FilterChip(
                                selected = planType == type,
                                onClick = {
                                    planType = type
                                    if (type == "INDIVIDUAL") memberCountText = "1"
                                    else if ((memberCountText.toIntOrNull()
                                            ?: 0) < 2
                                    ) memberCountText = "2"
                                    error = ""
                                },
                                label = { Text(type) }
                            )
                        }
                    }

                    if (planType != "INDIVIDUAL") {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = memberCountText,
                            onValueChange = {
                                memberCountText = it.filter(Char::isDigit); error = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Maximum Members *") }
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (planType == "COUPLE") "Couple plan requires exactly 2 members."
                            else "Group plan allows up to the configured number of members.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (error.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val months = monthsText.toIntOrNull()
                            val price = priceText.toDoubleOrNull()
                            val memberCount = memberCountText.toIntOrNull() ?: 0

                            when {
                                name.isBlank() ->
                                    error = "Plan name is required."

                                months == null || months < 1 ->
                                    error = "Duration must be at least 1 month."

                                price == null || price < 0 ->
                                    error = "Enter a valid price."

                                planType != "INDIVIDUAL" && memberCount < 2 ->
                                    error = "Couple/Group plans must have at least 2 members."

                                planType == "COUPLE" && memberCount != 2 ->
                                    error = "Couple plan must have exactly 2 members."

                                else -> {
                                    try {
                                        val values = android.content.ContentValues().apply {
                                            put("name", name.trim())
                                            put("months", months)
                                            put("price", price)
                                            put("plan_type", planType)
                                            put("member_count", memberCount)
                                        }

                                        db.writableDatabase.update(
                                            "membership_plans",
                                            values,
                                            "id=?",
                                            arrayOf(planId.toString())
                                        )

                                        onSaved()
                                    } catch (e: Exception) {
                                        error = e.message
                                            ?: "Unable to update plan."
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("SAVE CHANGES")
                    }
                }
            }
        }
    }

    @Composable
    fun AddMembershipPlanScreen(
        db: DatabaseHelper,
        onBack: () -> Unit,
        onSaved: () -> Unit
    ) {
        var name by rememberSaveable { mutableStateOf("") }
        var monthsText by rememberSaveable { mutableStateOf("") }
        var priceText by rememberSaveable { mutableStateOf("") }
        var planType by rememberSaveable { mutableStateOf("INDIVIDUAL") }
        var memberCountText by rememberSaveable { mutableStateOf("1") }
        var error by rememberSaveable { mutableStateOf("") }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onBack) {
                    Text("← Back")
                }

                Spacer(Modifier.width(12.dp))

                Text(
                    "Add Membership Plan",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(20.dp))

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "Plan Details",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            error = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Plan Name *") },
                        placeholder = { Text("Example: Monthly") }
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = monthsText,
                        onValueChange = {
                            monthsText = it.filter(Char::isDigit)
                            error = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Duration (Months) *") },
                        placeholder = { Text("Example: 1") }
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = priceText,
                        onValueChange = {
                            priceText = it.filter { c ->
                                c.isDigit() || c == '.'
                            }
                            error = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Price (₹) *") },
                        placeholder = { Text("Example: 1500") }
                    )

                    Spacer(Modifier.height(16.dp))
                    Text("PLAN TYPE", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("INDIVIDUAL", "COUPLE", "GROUP").forEach { type ->
                            FilterChip(
                                selected = planType == type,
                                onClick = {
                                    planType = type
                                    if (type == "INDIVIDUAL") memberCountText = "1"
                                    else if ((memberCountText.toIntOrNull()
                                            ?: 0) < 2
                                    ) memberCountText = "2"
                                },
                                label = { Text(type) }
                            )
                        }
                    }

                    if (planType != "INDIVIDUAL") {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = memberCountText,
                            onValueChange = {
                                memberCountText = it.filter(Char::isDigit); error = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Maximum Members *") },
                            placeholder = { Text(if (planType == "COUPLE") "2" else "Example: 4") }
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (planType == "COUPLE") "This Couple plan will allow exactly 2 members."
                            else "This Group plan will allow the configured number of members.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Discounts are applied during Assign/Renew.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (error.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val months = monthsText.toIntOrNull()
                            val price = priceText.toDoubleOrNull()
                            val memberCount = memberCountText.toIntOrNull() ?: 0

                            when {
                                name.isBlank() ->
                                    error = "Plan name is required."

                                months == null || months < 1 ->
                                    error = "Duration must be at least 1 month."

                                price == null || price < 0 ->
                                    error = "Enter a valid price."

                                planType != "INDIVIDUAL" && (memberCountText.toIntOrNull()
                                    ?: 0) < 2 ->
                                    error = "Couple/Group plans must have at least 2 members."

                                planType == "COUPLE" && (memberCountText.toIntOrNull() ?: 0) != 2 ->
                                    error = "Couple plan must have exactly 2 members."

                                else -> {
                                    try {
                                        db.addPlan(
                                            name.trim(),
                                            months,
                                            price,
                                            planType,
                                            memberCount
                                        )
                                        onSaved()
                                    } catch (e: Exception) {
                                        error = e.message
                                            ?: "Unable to save plan."
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("SAVE PLAN")
                    }
                }
            }
        }
    }

    @Composable
    fun MemberKpiCard(
        title: String,
        value: String,
        accent: Color,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier.height(78.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.10f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.20f))
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        title,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(value, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = accent)
                }
            }
        }
    }

    @Composable
    fun StandaloneMembers(
        db: DatabaseHelper,
        refresh: Int,
        changed: () -> Unit
    ) {
        var search by rememberSaveable { mutableStateOf("") }
        var statusFilter by rememberSaveable { mutableStateOf("ALL") }
        var planFilter by rememberSaveable { mutableStateOf("ALL") }
        var balanceFilter by rememberSaveable { mutableStateOf("ALL") }
        var showMemberFilters by rememberSaveable { mutableStateOf(false) }
        var selectedMemberId by rememberSaveable { mutableStateOf<String?>(null) }
        var showAddMember by rememberSaveable { mutableStateOf(false) }

        val rows = remember(search, refresh) { db.getMembers(search) }
        val plans = remember(refresh) { db.getPlans() }

        if (showAddMember) {
            AddMemberScreen(
                db = db,
                onBack = { showAddMember = false },
                onSaved = {
                    showAddMember = false
                    changed()
                }
            )
            return
        }

        AnimatedContent(
            targetState = selectedMemberId,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally(initialOffsetX = { it / 2 }, animationSpec = tween(350)) +
                            fadeIn(animationSpec = tween(300)) +
                            scaleIn(initialScale = 0.82f, animationSpec = tween(350))) togetherWith
                            (fadeOut(animationSpec = tween(180)) +
                                    scaleOut(targetScale = 0.92f, animationSpec = tween(180)))
                } else {
                    (slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) +
                            fadeIn(animationSpec = tween(250)) +
                            scaleIn(initialScale = 0.95f, animationSpec = tween(300))) togetherWith
                            (fadeOut(animationSpec = tween(180)) +
                                    scaleOut(targetScale = 0.94f, animationSpec = tween(180)))
                }
            },
            label = "member_profile_animation"
        ) { memberId ->
            if (memberId != null) {
                MemberProfile(
                    db = db,
                    memberCode = memberId,
                    onBack = { selectedMemberId = null },
                    changed = changed
                )
                return@AnimatedContent
            }

            val filteredRows = rows.filter { member ->
                val memberActive = member.getOrNull(5) == "1"
                val membershipStatus = member.getOrNull(6)?.trim()?.uppercase() ?: ""
                val planName = member.getOrNull(3)?.trim() ?: ""
                val balance = member.getOrNull(7)?.toDoubleOrNull() ?: 0.0
                val statusMatches = when (statusFilter) {
                    "ACTIVE" -> memberActive && membershipStatus == "ACTIVE"
                    "INACTIVE" -> !memberActive
                    "TRIAL" -> membershipStatus == "TRIAL"
                    "EXPIRED" -> membershipStatus == "EXPIRED"
                    else -> true
                }
                val planMatches = planFilter == "ALL" || planName.equals(planFilter, true)
                val balanceMatches = when (balanceFilter) {
                    "BALANCE" -> balance > 0.0
                    "NO_BALANCE" -> balance <= 0.0
                    else -> true
                }
                statusMatches && planMatches && balanceMatches
            }

            val activeCount = filteredRows.count {
                it.getOrNull(5) == "1" && it.getOrNull(6)?.equals("ACTIVE", true) == true
            }
            val trialCount = filteredRows.count { it.getOrNull(6)?.equals("TRIAL", true) == true }
            val balanceCount =
                filteredRows.count { (it.getOrNull(7)?.toDoubleOrNull() ?: 0.0) > 0.0 }
            val expiredCount =
                filteredRows.count { it.getOrNull(6)?.equals("EXPIRED", true) == true }

            // Gender totals are read directly from the existing members table.
            // No database schema or member-management logic is changed.
            val maleCount = remember(refresh) {
                db.scalarInt("SELECT COUNT(*) FROM members WHERE lower(COALESCE(gender, '')) = 'male'")
            }
            val femaleCount = remember(refresh) {
                db.scalarInt("SELECT COUNT(*) FROM members WHERE lower(COALESCE(gender, '')) = 'female'")
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Members", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "Manage members, memberships and payments",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(onClick = { showMemberFilters = !showMemberFilters }) {
                        Text(if (showMemberFilters) "HIDE FILTERS" else "FILTERS")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { showAddMember = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("+ ADD MEMBER", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MemberKpiCard(
                        "TOTAL",
                        filteredRows.size.toString(),
                        Color(0xFF2563EB),
                        Modifier.weight(1f)
                    )
                    MemberKpiCard(
                        "MALE",
                        maleCount.toString(),
                        Color(0xFF2563EB),
                        Modifier.weight(1f)
                    )
                    MemberKpiCard(
                        "FEMALE",
                        femaleCount.toString(),
                        Color(0xFFDB2777),
                        Modifier.weight(1f)
                    )
                    MemberKpiCard(
                        "ACTIVE",
                        activeCount.toString(),
                        Color(0xFF16A34A),
                        Modifier.weight(1f)
                    )
                    MemberKpiCard(
                        "TRIAL",
                        trialCount.toString(),
                        Color(0xFF9333EA),
                        Modifier.weight(1f)
                    )
                    MemberKpiCard(
                        "BALANCE DUE",
                        balanceCount.toString(),
                        Color(0xFFF59E0B),
                        Modifier.weight(1f)
                    )
                    MemberKpiCard(
                        "EXPIRED",
                        expiredCount.toString(),
                        Color(0xFFDC2626),
                        Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Search Members") },
                            placeholder = { Text("Name, Member ID or Phone") },
                            leadingIcon = { Text("🔎", fontSize = 18.sp) },
                            shape = RoundedCornerShape(12.dp)
                        )

                        AnimatedVisibility(
                            visible = showMemberFilters,
                            enter = fadeIn(animationSpec = tween(180)) + scaleIn(
                                initialScale = 0.98f,
                                animationSpec = tween(180)
                            ),
                            exit = fadeOut(animationSpec = tween(140)) + scaleOut(
                                targetScale = 0.98f,
                                animationSpec = tween(140)
                            )
                        ) {
                            Column(Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)) {
                                Text(
                                    "STATUS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "ALL",
                                        "ACTIVE",
                                        "INACTIVE",
                                        "TRIAL",
                                        "EXPIRED"
                                    ).forEach { status ->
                                        FilterChip(
                                            selected = statusFilter == status,
                                            onClick = { statusFilter = status },
                                            label = { Text(status) }
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    var planExpanded by remember { mutableStateOf(false) }
                                    Box(Modifier.weight(1f)) {
                                        OutlinedButton(
                                            onClick = { planExpanded = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(if (planFilter == "ALL") "All Plans" else planFilter)
                                        }
                                        DropdownMenu(
                                            expanded = planExpanded,
                                            onDismissRequest = { planExpanded = false }) {
                                            DropdownMenuItem(
                                                text = { Text("All Plans") },
                                                onClick = {
                                                    planFilter = "ALL"; planExpanded = false
                                                })
                                            plans.forEach { plan ->
                                                val name = plan.getOrNull(1) ?: return@forEach
                                                DropdownMenuItem(
                                                    text = { Text(name) },
                                                    onClick = {
                                                        planFilter = name; planExpanded = false
                                                    })
                                            }
                                        }
                                    }
                                    var balanceExpanded by remember { mutableStateOf(false) }
                                    Box(Modifier.weight(1f)) {
                                        OutlinedButton(
                                            onClick = { balanceExpanded = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                when (balanceFilter) {
                                                    "BALANCE" -> "Balance Due"; "NO_BALANCE" -> "No Balance"; else -> "All Payments"
                                                }
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = balanceExpanded,
                                            onDismissRequest = { balanceExpanded = false }) {
                                            DropdownMenuItem(
                                                text = { Text("All Payments") },
                                                onClick = {
                                                    balanceFilter = "ALL"; balanceExpanded = false
                                                })
                                            DropdownMenuItem(
                                                text = { Text("Balance Due") },
                                                onClick = {
                                                    balanceFilter = "BALANCE"; balanceExpanded =
                                                    false
                                                })
                                            DropdownMenuItem(
                                                text = { Text("No Balance") },
                                                onClick = {
                                                    balanceFilter = "NO_BALANCE"; balanceExpanded =
                                                    false
                                                })
                                        }
                                    }
                                    TextButton(onClick = {
                                        statusFilter = "ALL"; planFilter = "ALL"; balanceFilter =
                                        "ALL"
                                    }) {
                                        Text("CLEAR")
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    items(
                        count = filteredRows.size,
                        key = { index -> filteredRows[index].getOrNull(0) ?: index }
                    ) { index ->
                        val member = filteredRows[index]
                        val memberId = member.getOrNull(0) ?: ""
                        val name = member.getOrNull(1) ?: ""
                        val phone = member.getOrNull(2) ?: ""
                        val plan = member.getOrNull(3) ?: "No Plan"
                        val expiry = member.getOrNull(4) ?: "-"
                        val memberActive = member.getOrNull(5) == "1"
                        val membershipStatus = member.getOrNull(6)?.trim()?.uppercase() ?: ""
                        val balance = member.getOrNull(7)?.toDoubleOrNull() ?: 0.0
                        val statusColor = when {
                            membershipStatus == "EXPIRED" || !memberActive -> Color(0xFFDC2626)
                            membershipStatus == "TRIAL" -> Color(0xFF9333EA)
                            membershipStatus == "ACTIVE" -> Color(0xFF16A34A)
                            else -> Color(0xFF64748B)
                        }
                        val cardBackground = when {
                            membershipStatus == "TRIAL" -> Color(0xFFFFFBEB)
                            membershipStatus == "EXPIRED" || !memberActive -> Color(0xFFFEF2F2)
                            else -> Color.White
                        }

                        SwipeLeftMemberCard(onSwipedLeft = { selectedMemberId = memberId }) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBackground),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(statusColor.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            name.trim().firstOrNull()?.uppercase() ?: "M",
                                            fontSize = 21.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = statusColor
                                        )
                                    }
                                    Spacer(Modifier.width(13.dp))
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                name,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = statusColor.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    if (membershipStatus.isBlank()) "NO PLAN" else membershipStatus,
                                                    modifier = Modifier.padding(
                                                        horizontal = 9.dp,
                                                        vertical = 4.dp
                                                    ),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = statusColor
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "$memberId  •  ${phone.ifBlank { "No phone" }}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(5.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            Text(
                                                "PLAN: $plan",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                "EXPIRY: $expiry",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (balance > 0) Text(
                                                "BALANCE: ₹%.2f".format(balance),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFD97706)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    OutlinedButton(
                                        onClick = { selectedMemberId = memberId },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("VIEW", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun AddMemberScreen(
        db: DatabaseHelper,
        onBack: () -> Unit,
        onSaved: () -> Unit,
        requirePhoto: Boolean = false
    ) {
        val context = LocalContext.current
        var photoPath by rememberSaveable { mutableStateOf("") }
        var errorMessage by rememberSaveable {
            mutableStateOf("")
        }

        var cameraImageUri by remember {
            mutableStateOf<Uri?>(null)
        }

        val cameraLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                cameraImageUri?.let { uri ->
                    try {
                        val file = File(
                            context.filesDir,
                            "member_${System.currentTimeMillis()}.jpg"
                        )

                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(file).use { output ->
                                input.copyTo(output)
                            }
                        }

                        photoPath = file.absolutePath

                        // Delete temporary camera file
                        context.contentResolver.delete(uri, null, null)

                    } catch (e: Exception) {
                        errorMessage = "Unable to save captured photo."
                    }
                }
            }
        }

        var name by rememberSaveable {
            mutableStateOf("")
        }

        var phone by rememberSaveable {
            mutableStateOf("")
        }

        var email by rememberSaveable {
            mutableStateOf("")
        }

        var gender by rememberSaveable {
            mutableStateOf("Male")
        }

        var height by rememberSaveable {
            mutableStateOf("")
        }

        var weight by rememberSaveable {
            mutableStateOf("")
        }

        var dob by rememberSaveable {
            mutableStateOf("")
        }

        // DOB picker state
        var showDobPicker by remember {
            mutableStateOf(false)
        }
        var showJoiningDatePicker by remember {
            mutableStateOf(false)
        }

        var address by rememberSaveable {
            mutableStateOf("")
        }

        var emergencyContact by rememberSaveable {
            mutableStateOf("")
        }

        var emergencyPhone by rememberSaveable {
            mutableStateOf("")
        }

        var joiningDate by rememberSaveable {
            mutableStateOf(
                java.text.SimpleDateFormat(
                    "yyyy-MM-dd",
                    java.util.Locale.US
                ).format(java.util.Date())
            )
        }

        var notes by rememberSaveable {
            mutableStateOf("")
        }


        var genderExpanded by remember {
            mutableStateOf(false)
        }

        // =========================
        // PHOTO PICKER
        // =========================

        /*
 * PHOTO PICKER
 */
        /*
  * CAMERA
  */

        // =========================
        // DOB DATE PICKER
        // =========================

        if (showDobPicker) {

            val calendar = java.util.Calendar.getInstance()

            if (dob.isNotBlank()) {

                val parts = dob.split("-")

                if (parts.size == 3) {

                    calendar.set(
                        parts[0].toIntOrNull()
                            ?: calendar.get(java.util.Calendar.YEAR),

                        (parts[1].toIntOrNull() ?: 1) - 1,

                        parts[2].toIntOrNull()
                            ?: calendar.get(java.util.Calendar.DAY_OF_MONTH)
                    )
                }
            }

            val datePickerDialog = android.app.DatePickerDialog(
                context,

                { _, year, month, dayOfMonth ->

                    dob = String.format(
                        java.util.Locale.US,
                        "%04d-%02d-%02d",
                        year,
                        month + 1,
                        dayOfMonth
                    )

                    showDobPicker = false
                },

                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
            )

            // Future DOB is not allowed
            datePickerDialog.datePicker.maxDate =
                System.currentTimeMillis()

            datePickerDialog.setOnCancelListener {
                showDobPicker = false
            }

            datePickerDialog.show()
        }
        if (showJoiningDatePicker) {

            val calendar = java.util.Calendar.getInstance()

            if (joiningDate.isNotBlank()) {

                val parts = joiningDate.split("-")

                if (parts.size == 3) {

                    calendar.set(
                        parts[0].toIntOrNull()
                            ?: calendar.get(java.util.Calendar.YEAR),

                        (parts[1].toIntOrNull() ?: 1) - 1,

                        parts[2].toIntOrNull()
                            ?: calendar.get(java.util.Calendar.DAY_OF_MONTH)
                    )
                }
            }

            val datePickerDialog = android.app.DatePickerDialog(
                context,

                { _, year, month, dayOfMonth ->

                    joiningDate = String.format(
                        java.util.Locale.US,
                        "%04d-%02d-%02d",
                        year,
                        month + 1,
                        dayOfMonth
                    )

                    showJoiningDatePicker = false
                },

                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
            )

            datePickerDialog.setOnCancelListener {
                showJoiningDatePicker = false
            }

            datePickerDialog.show()
        }

        // =========================
        // YOUR EXISTING UI CONTINUES HERE
        /*
         * SAVE MEMBER
         */
        fun saveMember() {

            if (name.trim().isBlank()) {
                errorMessage = "Member name is required."
                return
            }

            if (phone.trim().isBlank()) {
                errorMessage = "Phone number is required."
                return
            }

            // if (requirePhoto && photoPath.isBlank()) {
            //     errorMessage = "Profile photo is required for Check-In Mode registration."
            //     return
            // }

            try {

                val settings = db.getGymSettings()

                val prefix =
                    settings["prefix"]
                        .orEmpty()
                        .ifBlank { "AC" }

                val memberCode =
                    db.addMember(
                        name = name,
                        phone = phone,
                        email = email,
                        gender = gender,
                        dob = dob,
                        address = address,
                        emergency = emergencyContact,
                        emergencyPhone = emergencyPhone,
                        joiningDate = joiningDate,
                        notes = notes,
                        photoPath = photoPath,
                        prefix = prefix
                    )

                // Store Height and Weight in the member record.
                // Values are optional and are kept as REAL numbers.
                val bodyValues = android.content.ContentValues().apply {
                    height.trim().toDoubleOrNull()?.let { put("height_cm", it) }
                        ?: putNull("height_cm")
                    weight.trim().toDoubleOrNull()?.let { put("weight_kg", it) }
                        ?: putNull("weight_kg")
                }
                db.writableDatabase.update(
                    "members",
                    bodyValues,
                    "member_code=?",
                    arrayOf(memberCode)
                )

                /*
                 * Member is automatically created
                 * with Trial membership.
                 */
                onSaved()

            } catch (e: Exception) {

                errorMessage =
                    e.message ?: "Unable to add member."
            }
        }

        /*
         * PAGE
         */
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(16.dp)
        ) {

            /*
             * HEADER
             */
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Add Member",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Register a new gym member",
                        fontSize = 13.sp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = onBack
                ) {
                    Text("BACK")
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            /*
             * PERSONAL INFORMATION
             */
            Text(
                text = "Personal Information",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Full Name *")
                },
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                        .filter { c ->
                            c.isDigit() ||
                                    c == '+' ||
                                    c == ' ' ||
                                    c == '-'
                        }

                    errorMessage = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Phone *")
                },
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Email")
                },
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            /*
             * GENDER
             */
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {

                OutlinedButton(
                    onClick = {
                        genderExpanded = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        text = "Gender: $gender"
                    )
                }

                DropdownMenu(
                    expanded = genderExpanded,
                    onDismissRequest = {
                        genderExpanded = false
                    }
                ) {

                    listOf(
                        "Male",
                        "Female",
                        "Other"
                    ).forEach { option ->

                        DropdownMenuItem(
                            text = {
                                Text(option)
                            },
                            onClick = {

                                gender = option
                                genderExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it.filter { c -> c.isDigit() || c == '.' } },
                    modifier = Modifier.weight(1f),
                    label = { Text("Height (cm)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                    modifier = Modifier.weight(1f),
                    label = { Text("Weight (kg)") },
                    singleLine = true
                )
            }

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showDobPicker = true
                    }
            ) {
                OutlinedTextField(
                    value = dob,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = {
                        Text("Date of Birth")
                    },
                    placeholder = {
                        Text("Select date of birth")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }


            Spacer(
                modifier = Modifier.height(10.dp)
            )

            OutlinedTextField(
                value = address,
                onValueChange = {
                    address = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Address")
                },
                minLines = 3
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            /*
             * EMERGENCY CONTACT
             */
            Text(
                text = "Emergency Contact",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            OutlinedTextField(
                value = emergencyContact,
                onValueChange = {
                    emergencyContact = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Emergency Contact Name")
                },
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            OutlinedTextField(
                value = emergencyPhone,
                onValueChange = {
                    emergencyPhone = it
                        .filter { c ->
                            c.isDigit() ||
                                    c == '+' ||
                                    c == ' ' ||
                                    c == '-'
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Emergency Contact Phone")
                },
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            /*
             * MEMBERSHIP INFORMATION
             */
            Text(
                text = "Registration",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showJoiningDatePicker = true
                    }
            ) {
                OutlinedTextField(
                    value = joiningDate,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = {
                        Text("Joining Date")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            /*
             * PHOTO
             */
            OutlinedButton(
                onClick = {
                    val photoFile = File(
                        context.cacheDir,
                        "camera_${System.currentTimeMillis()}.jpg"
                    )

                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        photoFile
                    )

                    cameraImageUri = uri
                    cameraLauncher.launch(uri)
                },
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    if (photoPath.isBlank())
                        "ADD MEMBER PHOTO"
                    else
                        "PHOTO SELECTED ✓"
                )
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            /*
             * NOTES
             */
            OutlinedTextField(
                value = notes,
                onValueChange = {
                    notes = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Notes")
                },
                minLines = 3
            )

            Spacer(
                modifier = Modifier.height(15.dp)
            )

            /*
             * ERROR
             */
            if (errorMessage.isNotBlank()) {

                Text(
                    text = errorMessage,
                    color =
                        MaterialTheme
                            .colorScheme
                            .error,
                    fontWeight = FontWeight.Medium
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )
            }

            /*
             * SAVE
             */
            Button(
                onClick = {
                    saveMember()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {

                Text(
                    text = "SAVE MEMBER",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(30.dp)
            )
        }
    }

    @Composable
    fun MemberProfile(
        db: DatabaseHelper,
        memberCode: String,
        onBack: () -> Unit,
        changed: () -> Unit
    ) {
        val context = LocalContext.current

        // =====================================================
        // MEMBER DETAILS
        // =====================================================
        val memberCursor = db.readableDatabase.rawQuery(
            """
            SELECT id,member_code,name,phone,email,gender,height_cm,weight_kg,dob,address,
                   emergency_contact,emergency_phone,joining_date,notes,
                   photo_path,active
            FROM members WHERE member_code=? LIMIT 1
            """.trimIndent(),
            arrayOf(memberCode)
        )

        var memberId = 0L
        var name = ""
        var phone = ""
        var email = ""
        var gender = ""
        var height = ""
        var weight = ""
        var dob = ""
        var address = ""
        var emergencyContact = ""
        var emergencyPhone = ""
        var joiningDate = ""
        var notes = ""
        var photoPath = ""
        var accessActive = true

        memberCursor.use {
            if (it.moveToFirst()) {
                memberId = it.getLong(0)
                name = it.getString(2) ?: ""
                phone = it.getString(3) ?: ""
                email = it.getString(4) ?: ""
                gender = it.getString(5) ?: ""
                height = if (it.isNull(6)) "" else it.getString(6)
                weight = if (it.isNull(7)) "" else it.getString(7)
                dob = it.getString(8) ?: ""
                address = it.getString(9) ?: ""
                emergencyContact = it.getString(10) ?: ""
                emergencyPhone = it.getString(11) ?: ""
                joiningDate = it.getString(12) ?: ""
                notes = it.getString(13) ?: ""
                photoPath = it.getString(14) ?: ""
                accessActive = it.getInt(15) == 1
            }
        }

        val profileBitmap = remember(photoPath) {
            if (photoPath.isNotBlank() && File(photoPath).exists()) {
                BitmapFactory.decodeFile(photoPath)
            } else null
        }

        // =====================================================
        // MEMBERSHIP
        // =====================================================
        var planName = "No Plan"
        var startDate = ""
        var endDate = ""
        var planAmount = 0.0
        var discountAmount = 0.0
        var totalAmount = 0.0
        var paidAmount = 0.0
        var balanceAmount = 0.0
        var membershipStatus = "Inactive"
        var isTrial = false

        // Observe this state so incrementing it after payment triggers a
        // recomposition of this Member Profile page.
//        membershipRefresh
//
        val membershipCursor = db.readableDatabase.rawQuery(
            """
            SELECT p.name,ms.start_date,ms.end_date,ms.plan_amount,
                   ms.discount_amount,ms.total_amount,ms.paid_amount,
                   ms.balance_amount,ms.status,ms.is_trial
            FROM memberships ms
            JOIN membership_plans p ON p.id=ms.plan_id
            WHERE ms.member_id=? ORDER BY ms.id DESC LIMIT 1
            """.trimIndent(),
            arrayOf(memberId.toString())
        )
        membershipCursor.use {
            if (it.moveToFirst()) {
                planName = it.getString(0) ?: "No Plan"
                startDate = it.getString(1) ?: ""
                endDate = it.getString(2) ?: ""
                planAmount = it.getDouble(3)
                discountAmount = it.getDouble(4)
                totalAmount = it.getDouble(5)
                paidAmount = it.getDouble(6)
                balanceAmount = it.getDouble(7)
                membershipStatus = it.getString(8) ?: "Inactive"
                isTrial = it.getInt(9) == 1
            }
        }

        var currentAccessActive by remember(memberId) { mutableStateOf(accessActive) }
        var selectedSection by rememberSaveable(memberId) { mutableStateOf("MEMBERSHIP") }
        var message by remember { mutableStateOf("") }
        var showEditDetails by rememberSaveable { mutableStateOf(false) }
        var showAssignMembership by rememberSaveable { mutableStateOf(false) }
        var showRenewMembership by rememberSaveable { mutableStateOf(false) }
        var showPayBalance by rememberSaveable { mutableStateOf(false) }
        var showPaymentDone by rememberSaveable { mutableStateOf(false) }
        var paymentDoneType by rememberSaveable { mutableStateOf("ASSIGN") }
        var showEditMembership by rememberSaveable { mutableStateOf(false) }
        var membershipRefresh by remember { mutableIntStateOf(0) }
        var membershipActionError by rememberSaveable { mutableStateOf("") }

        // =====================================================
        // GOVERNMENT DOCUMENTS
        // =====================================================
        var documentName by rememberSaveable { mutableStateOf("") }
        var showDocumentNameDialog by rememberSaveable { mutableStateOf(false) }
        var documentRefresh by remember { mutableIntStateOf(0) }
        var pendingDownloadPath by remember { mutableStateOf<String?>(null) }

        val downloadLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/octet-stream")
        ) { uri ->
            if (uri != null && pendingDownloadPath != null) {
                try {
                    val source = File(pendingDownloadPath!!)
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        source.inputStream().use { input -> input.copyTo(output) }
                    }
                    message = "Document downloaded successfully."
                } catch (e: Exception) {
                    message = e.message ?: "Unable to download document."
                }
            }
            pendingDownloadPath = null
        }

        fun mimeTypeForDocument(path: String): String = when {
            path.endsWith(".pdf", true) -> "application/pdf"
            path.endsWith(".jpg", true) || path.endsWith(".jpeg", true) -> "image/jpeg"
            path.endsWith(".png", true) -> "image/png"
            path.endsWith(".webp", true) -> "image/webp"
            else -> "*/*"
        }

        fun viewDocument(path: String) {
            try {
                val file = File(path)
                if (!file.exists()) {
                    message = "Document file not found."; return
                }
                val uri = FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeTypeForDocument(path))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Open Document"))
            } catch (e: Exception) {
                message = "No app is available to open this document."
            }
        }

        fun downloadDocument(path: String, docName: String) {
            try {
                val file = File(path)
                if (!file.exists()) {
                    message = "Document file not found."; return
                }
                val extension = path.substringAfterLast('.', "file")
                val safeName = docName.trim().ifBlank { "Government Document" }
                    .replace(Regex("[^A-Za-z0-9._ -]"), "_")
                pendingDownloadPath = path
                downloadLauncher.launch("$safeName.$extension")
            } catch (e: Exception) {
                pendingDownloadPath = null
                message = e.message ?: "Unable to download document."
            }
        }

        val documentPicker = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                try {
                    val safeName = documentName.trim().ifBlank { "Government Document" }
                    val extension = if (uri.toString().contains("pdf", true)) ".pdf" else ".file"
                    val file = File(
                        context.filesDir,
                        "member_${memberId}_doc_${System.currentTimeMillis()}$extension"
                    )
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(file).use { output -> input.copyTo(output) }
                    }
                    val values = android.content.ContentValues().apply {
                        put("member_id", memberId)
                        put("document_name", safeName)
                        put("document_path", file.absolutePath)
                        put(
                            "uploaded_at", java.text.SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss", java.util.Locale.US
                            ).format(java.util.Date())
                        )
                    }
                    db.writableDatabase.insertOrThrow("member_documents", null, values)
                    documentName = ""
                    documentRefresh++

                } catch (e: Exception) {
                    message = e.message ?: "Unable to upload document."
                }
            }
        }

        val documents = remember(memberId, documentRefresh) {
            val rows = mutableListOf<Array<String>>()
            val cursor = db.readableDatabase.rawQuery(
                "SELECT id,document_name,document_path,uploaded_at FROM member_documents WHERE member_id=? ORDER BY id DESC",
                arrayOf(memberId.toString())
            )
            cursor.use {
                while (it.moveToNext()) {
                    rows.add(
                        arrayOf(
                            it.getString(0),
                            it.getString(1),
                            it.getString(2),
                            it.getString(3)
                        )
                    )
                }
            }
            rows
        }

        fun selectSection(section: String) {
            selectedSection = section
            message = ""
        }

        // =====================================================
        // ASSIGN / RENEW MEMBERSHIP
        // =====================================================
        val latestRegularMembership = run {
            var result: Array<String>? = null
            val c = db.readableDatabase.rawQuery(
                """
                SELECT ms.id, ms.start_date, ms.end_date, ms.status, ms.balance_amount,
                       ms.total_amount, ms.paid_amount, p.name
                FROM memberships ms
                JOIN membership_plans p ON p.id=ms.plan_id
                WHERE ms.member_id=? AND ms.is_trial=0
                ORDER BY ms.id DESC
                LIMIT 1
                """.trimIndent(),
                arrayOf(memberId.toString())
            )
            c.use {
                if (it.moveToFirst()) {
                    result = arrayOf(
                        it.getString(0) ?: "",
                        it.getString(1) ?: "",
                        it.getString(2) ?: "",
                        it.getString(3) ?: "",
                        it.getString(4) ?: "0",
                        it.getString(5) ?: "0",
                        it.getString(6) ?: "0",
                        it.getString(7) ?: ""
                    )
                }
            }
            result
        }

        val hasPendingBalance =
            latestRegularMembership?.getOrNull(4)?.toDoubleOrNull()?.let { it > 0.005 } == true &&
                    latestRegularMembership?.getOrNull(3) == "Pending"
        val hasActiveRegularPlan = latestRegularMembership?.getOrNull(3) == "Active" &&
                latestRegularMembership?.getOrNull(4)?.toDoubleOrNull()?.let { it <= 0.005 } == true


        val regularPlans = remember(documentRefresh) {
            db.getPlans().filter {
                it.getOrNull(4) == "1" && it.getOrNull(5) != "1"
            }
        }

        fun calculateEndDate(start: String, months: Int): String {
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val date = formatter.parse(start) ?: java.util.Date()
            val cal = java.util.Calendar.getInstance()
            cal.time = date
            cal.add(java.util.Calendar.MONTH, months)
            return formatter.format(cal.time)
        }

        fun nextDay(date: String): String {
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val parsed = formatter.parse(date) ?: java.util.Date()
            val cal = java.util.Calendar.getInstance()
            cal.time = parsed
            cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
            return formatter.format(cal.time)
        }

        fun openPaymentAcknowledgement(messageType: String) {
            try {
                val c = db.readableDatabase.rawQuery(
                    """
                    SELECT p.name, ms.start_date, ms.end_date, ms.total_amount,
                           ms.paid_amount, ms.balance_amount, ms.status, ms.is_trial
                    FROM memberships ms
                    JOIN membership_plans p ON p.id=ms.plan_id
                    WHERE ms.member_id=?
                    ORDER BY ms.id DESC LIMIT 1
                    """.trimIndent(),
                    arrayOf(memberId.toString())
                )
                var plan = ""
                var start = ""
                var end = ""
                var total = 0.0
                var paid = 0.0
                var balance = 0.0
                var status = ""
                var trial = false
                c.use {
                    if (!it.moveToFirst()) {
                        message = "No membership found."; return
                    }
                    plan = it.getString(0) ?: ""
                    start = it.getString(1) ?: ""
                    end = it.getString(2) ?: ""
                    total = it.getDouble(3)
                    paid = it.getDouble(4)
                    balance = it.getDouble(5)
                    status = it.getString(6) ?: ""
                    trial = it.getInt(7) == 1
                }
                val title = when (messageType) {
                    "RENEW" -> "Membership Renewal Acknowledgement"
                    "PAY_BALANCE" -> "Balance Payment Acknowledgement"
                    else -> "Membership Assignment Acknowledgement"
                }
                val text = buildString {
                    append(title).append("\n\n")
                    append("Member: $name\n")
                    append("Member ID: $memberCode\n")
                    append("Plan: $plan\n")
                    append("Amount Paid: ₹%.2f\n".format(paid))
                    append("Total Amount: ₹%.2f\n".format(total))
                    append("Balance: ₹%.2f\n".format(balance))
                    if (start.isNotBlank()) append("Start Date: $start\n")
                    if (end.isNotBlank()) append("Valid Until: $end\n")
                    append("Status: ${if (trial) "TRIAL" else status}\n")
                    append("\nThank you for choosing us!")
                }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(intent, "Send Acknowledgement"))
            } catch (e: Exception) {
                message = "Unable to create acknowledgement."
            }
        }

        fun openWhatsAppMembershipMessage(messageType: String) {
            try {
                val rawPhone = phone.trim()
                var whatsappNumber = rawPhone.filter { it.isDigit() }

                // Default to India when a 10-digit local number is stored.
                if (whatsappNumber.length == 10) {
                    whatsappNumber = "91$whatsappNumber"
                } else if (rawPhone.startsWith("+")) {
                    whatsappNumber = rawPhone.filter { it.isDigit() }
                }

                if (whatsappNumber.isBlank()) {
                    message = "Member phone number is not available for WhatsApp."
                    return
                }

                val c = db.readableDatabase.rawQuery(
                    """
                    SELECT ms.id, p.name, ms.start_date, ms.end_date, ms.total_amount,
                           ms.paid_amount, ms.balance_amount, ms.status, ms.is_trial
                    FROM memberships ms
                    JOIN membership_plans p ON p.id=ms.plan_id
                    WHERE ms.member_id=?
                    ORDER BY ms.id DESC LIMIT 1
                    """.trimIndent(),
                    arrayOf(memberId.toString())
                )

                var membershipId = 0L
                var plan = ""
                var membershipStart = ""
                var membershipEnd = ""
                var total = 0.0
                var paid = 0.0
                var balance = 0.0
                var status = ""
                var trial = false

                c.use {
                    if (!it.moveToFirst()) {
                        message = "No membership found."
                        return
                    }
                    membershipId = it.getLong(0)
                    plan = it.getString(1) ?: ""
                    membershipStart = it.getString(2) ?: ""
                    membershipEnd = it.getString(3) ?: ""
                    total = it.getDouble(4)
                    paid = it.getDouble(5)
                    balance = it.getDouble(6)
                    status = it.getString(7) ?: ""
                    trial = it.getInt(8) == 1
                }

                val statusText =
                    if (trial) "TRIAL" else status.uppercase(java.util.Locale.getDefault())
                var latestPaymentAmount = paid
                if (membershipId > 0L) {
                    val paymentCursor = db.readableDatabase.rawQuery(
                        "SELECT amount FROM payments WHERE membership_id=? ORDER BY id DESC LIMIT 1",
                        arrayOf(membershipId.toString())
                    )
                    paymentCursor.use {
                        if (it.moveToFirst()) latestPaymentAmount = it.getDouble(0)
                    }
                }

                val text = when (messageType) {
                    "PAY_BALANCE" -> """
                        Hi $name,

                        Your membership payment has been updated successfully. 💪

                        Plan: $plan
                        Balance Paid: ₹%.2f
                        Total Paid: ₹%.2f
                        Balance: ₹%.2f
                        Status: $statusText
                        Valid Until: $membershipEnd

                        Thank you for choosing us!
                        \nShare your GYM Experience on google https://g.page/r/CYFBIcYM55V_ECE/review
                    """.trimIndent().format(latestPaymentAmount, paid, balance)

                    "RENEW" -> """
                        Hi $name,

                        Your gym membership has been renewed successfully. 💪

                        Plan: $plan
                        Amount Paid: ₹%.2f
                        Total Amount: ₹%.2f
                        Balance: ₹%.2f
                        Start Date: $membershipStart
                        Valid Until: $membershipEnd
                        Status: $statusText

                        Thank you for choosing us!
                        Share your GYM Experience on google https://g.page/r/CYFBIcYM55V_ECE/review
                    """.trimIndent().format(latestPaymentAmount, total, balance)

                    else -> """
                        Hi $name,

                        Your gym membership has been assigned successfully. 💪

                        Plan: $plan
                        Amount Paid: ₹%.2f
                        Total Amount: ₹%.2f
                        Balance: ₹%.2f
                        Start Date: $membershipStart
                        Valid Until: $membershipEnd
                        Status: $statusText

                        Thank you for choosing us!
                        Share your GYM Experience on google https://g.page/r/CYFBIcYM55V_ECE/review
                    """.trimIndent().format(latestPaymentAmount, total, balance)
                }

                val whatsappUri = Uri.parse(
                    "https://wa.me/$whatsappNumber?text=${Uri.encode(text)}"
                )
                val intent = Intent(Intent.ACTION_VIEW, whatsappUri)
                context.startActivity(intent)

            } catch (e: Exception) {
                message = "Unable to open WhatsApp."
            }
        }

        ResponsiveLayout(
            portraitContent = {
                MemberProfileWorkspace(
                    db = db,
                    memberId = memberId,
                    name = name,
                    memberCode = memberCode,
                    profileBitmap = profileBitmap,
                    phone = phone,
                    email = email,
                    gender = gender,
                    height = height,
                    weight = weight,
                    dob = dob,
                    joiningDate = joiningDate,
                    address = address,
                    emergencyContact = emergencyContact,
                    emergencyPhone = emergencyPhone,
                    notes = notes,
                    planName = planName,
                    startDate = startDate,
                    endDate = endDate,
                    planAmount = planAmount,
                    discountAmount = discountAmount,
                    totalAmount = totalAmount,
                    paidAmount = paidAmount,
                    balanceAmount = balanceAmount,
                    membershipStatus = membershipStatus,
                    isTrial = isTrial,
                    currentAccessActive = currentAccessActive,
                    selectedSection = selectedSection,
                    documents = documents,
                    hasActiveRegularPlan = hasActiveRegularPlan,
                    hasPendingBalance = hasPendingBalance,
                    message = message,
                    onSectionSelected = { selectSection(it) },
                    onAccessChange = { newValue ->
                        val values = android.content.ContentValues().apply {
                            put("active", if (newValue) 1 else 0)
                        }
                        db.writableDatabase.update(
                            "members", values, "id=?", arrayOf(memberId.toString())
                        )
                        currentAccessActive = newValue
                        message = if (newValue) "Member activated." else "Member deactivated."
                        changed()
                    },
                    onBack = onBack,
                    onAssign = {
                        membershipActionError = ""
                        showAssignMembership = true
                    },
                    onRenew = {
                        membershipActionError = ""
                        showRenewMembership = true
                    },
                    onPayBalance = {
                        membershipActionError = ""
                        showPayBalance = true
                    },
                    onEditMembership = {
                        membershipActionError = ""
                        showEditMembership = true
                    },
                    onEdit = { showEditDetails = true },
                    onAddDocument = { showDocumentNameDialog = true },
                    onViewDocument = ::viewDocument,
                    onDownloadDocument = ::downloadDocument
                )
            },
            landscapeContent = {
                MemberProfileWorkspace(
                    db = db,
                    memberId = memberId,
                    name = name,
                    memberCode = memberCode,
                    profileBitmap = profileBitmap,
                    phone = phone,
                    email = email,
                    gender = gender,
                    height = height,
                    weight = weight,
                    dob = dob,
                    joiningDate = joiningDate,
                    address = address,
                    emergencyContact = emergencyContact,
                    emergencyPhone = emergencyPhone,
                    notes = notes,
                    planName = planName,
                    startDate = startDate,
                    endDate = endDate,
                    planAmount = planAmount,
                    discountAmount = discountAmount,
                    totalAmount = totalAmount,
                    paidAmount = paidAmount,
                    balanceAmount = balanceAmount,
                    membershipStatus = membershipStatus,
                    isTrial = isTrial,
                    currentAccessActive = currentAccessActive,
                    selectedSection = selectedSection,
                    documents = documents,
                    hasActiveRegularPlan = hasActiveRegularPlan,
                    hasPendingBalance = hasPendingBalance,
                    message = message,
                    onSectionSelected = { selectSection(it) },
                    onAccessChange = { newValue ->
                        val values = android.content.ContentValues().apply {
                            put("active", if (newValue) 1 else 0)
                        }
                        db.writableDatabase.update(
                            "members", values, "id=?", arrayOf(memberId.toString())
                        )
                        currentAccessActive = newValue
                        message = if (newValue) "Member activated." else "Member deactivated."
                        changed()
                    },
                    onBack = onBack,
                    onAssign = {
                        membershipActionError = ""
                        showAssignMembership = true
                    },
                    onRenew = {
                        membershipActionError = ""
                        showRenewMembership = true
                    },
                    onPayBalance = {
                        membershipActionError = ""
                        showPayBalance = true
                    },
                    onEditMembership = {
                        membershipActionError = ""
                        showEditMembership = true
                    },
                    onEdit = { showEditDetails = true },
                    onAddDocument = { showDocumentNameDialog = true },
                    onViewDocument = ::viewDocument,
                    onDownloadDocument = ::downloadDocument
                )
            }
        )

        if (showEditDetails) {
            EditMemberDetailsScreen(
                db = db,
                memberId = memberId,
                onBack = { showEditDetails = false },
                onSaved = {
                    showEditDetails = false
                    changed()
                }
            )
            return
        }


        if (showAssignMembership) {
            AssignMembershipDialog(
                db = db,
                memberCode = memberCode,
                plans = regularPlans,
                errorMessage = membershipActionError,
                onError = { membershipActionError = it },
                onDismiss = {
                    showAssignMembership = false
                    membershipActionError = ""
                },
                onSaved = {
                    showAssignMembership = false
                    membershipActionError = ""
                    membershipRefresh++
                    changed()
                    paymentDoneType = "ASSIGN"
                    showPaymentDone = true
                }
            )
        }

        if (showEditMembership && latestRegularMembership != null) {
            EditMembershipDialog(
                db = db,
                membershipId = latestRegularMembership.getOrNull(0)?.toLongOrNull() ?: 0L,
                memberCode = memberCode,
                plans = regularPlans,
                errorMessage = membershipActionError,
                onError = { membershipActionError = it },
                onDismiss = {
                    showEditMembership = false
                    membershipActionError = ""
                },
                onSaved = {
                    showEditMembership = false
                    membershipActionError = ""
                    membershipRefresh++
                    changed()
                }
            )
        }

        if (showRenewMembership && hasActiveRegularPlan && latestRegularMembership != null) {
            RenewMembershipDialog(
                db = db,
                memberCode = memberCode,
                activeEndDate = latestRegularMembership.getOrNull(2) ?: "",
                plans = regularPlans,
                errorMessage = membershipActionError,
                onError = { membershipActionError = it },
                onDismiss = {
                    showRenewMembership = false
                    membershipActionError = ""
                },
                onSaved = {
                    showRenewMembership = false
                    membershipActionError = ""
                    membershipRefresh++
                    changed()
                    paymentDoneType = "RENEW"
                    showPaymentDone = true
                }
            )
        }

        if (showPayBalance && hasPendingBalance && latestRegularMembership != null) {
            PayMembershipBalanceDialog(
                db = db,
                memberCode = memberCode,
                membershipId = latestRegularMembership.getOrNull(0)?.toLongOrNull() ?: 0L,
                balanceAmount = latestRegularMembership.getOrNull(4)?.toDoubleOrNull() ?: 0.0,
                errorMessage = membershipActionError,
                onError = { membershipActionError = it },
                onDismiss = {
                    showPayBalance = false
                    membershipActionError = ""
                },
                onSaved = {
                    showPayBalance = false
                    membershipActionError = ""
                    selectedSection = "MEMBERSHIP"
                    membershipRefresh++
                    changed()
                    paymentDoneType = "PAY_BALANCE"
                    showPaymentDone = true
                }
            )
        }

        if (showPaymentDone) {
            AlertDialog(
                onDismissRequest = { showPaymentDone = false },
                title = { Text("PAYMENT DONE", fontWeight = FontWeight.ExtraBold) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            when (paymentDoneType) {
                                "RENEW" -> "Membership renewed successfully."
                                "PAY_BALANCE" -> "Membership balance payment recorded successfully."
                                else -> "Membership assigned successfully."
                            },
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16803C)
                        )
                        Spacer(Modifier.height(12.dp))
                        ProfileInfoRow("Member", name)
                        ProfileInfoRow("Member ID", memberCode)
                        ProfileInfoRow("Plan", planName)
                        ProfileInfoRow("Total Amount", "₹%.2f".format(totalAmount))
                        ProfileInfoRow("Total Paid", "₹%.2f".format(paidAmount))
                        ProfileInfoRow("Balance", "₹%.2f".format(balanceAmount))
                        if (endDate.isNotBlank()) ProfileInfoRow("Valid Until", endDate)
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        openPaymentAcknowledgement(paymentDoneType)
                    }) { Text("ACK OUT") }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            openWhatsAppMembershipMessage(paymentDoneType)
                        }) { Text("WHATSAPP") }
                        TextButton(onClick = { showPaymentDone = false }) { Text("CLOSE") }
                    }
                }
            )
        }

        if (showDocumentNameDialog) {
            AlertDialog(
                onDismissRequest = { showDocumentNameDialog = false },
                title = { Text("Add Government Document") },
                text = {
                    OutlinedTextField(
                        value = documentName,
                        onValueChange = { documentName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Document Name") },
                        placeholder = { Text("Aadhaar / PAN / Driving Licence") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (documentName.trim().isBlank()) message = "Document name is required."
                        else {
                            showDocumentNameDialog = false
                            documentPicker.launch(arrayOf("application/pdf", "image/*"))
                        }
                    }) { Text("CHOOSE FILE") }
                },
                dismissButton = {
                    OutlinedButton(onClick = {
                        showDocumentNameDialog = false
                    }) { Text("CANCEL") }
                }
            )
        }
    }


    @Composable
    fun EditMemberDetailsScreen(
        db: DatabaseHelper,
        memberId: Long,
        onBack: () -> Unit,
        onSaved: () -> Unit
    ) {
        val context = LocalContext.current

        var name by rememberSaveable { mutableStateOf("") }
        var phone by rememberSaveable { mutableStateOf("") }
        var email by rememberSaveable { mutableStateOf("") }
        var gender by rememberSaveable { mutableStateOf("Male") }
        var height by rememberSaveable { mutableStateOf("") }
        var weight by rememberSaveable { mutableStateOf("") }
        var dob by rememberSaveable { mutableStateOf("") }
        var address by rememberSaveable { mutableStateOf("") }
        var emergencyContact by rememberSaveable { mutableStateOf("") }
        var emergencyPhone by rememberSaveable { mutableStateOf("") }
        var joiningDate by rememberSaveable { mutableStateOf("") }
        var notes by rememberSaveable { mutableStateOf("") }
        var photoPath by rememberSaveable { mutableStateOf("") }
        var error by rememberSaveable { mutableStateOf("") }
        var genderExpanded by remember { mutableStateOf(false) }

        LaunchedEffect(memberId) {
            val c = db.readableDatabase.rawQuery(
                """
                SELECT name,phone,email,gender,height_cm,weight_kg,dob,address,emergency_contact,
                       emergency_phone,joining_date,notes,photo_path
                FROM members WHERE id=? LIMIT 1
                """.trimIndent(),
                arrayOf(memberId.toString())
            )
            c.use {
                if (it.moveToFirst()) {
                    name = it.getString(0) ?: ""
                    phone = it.getString(1) ?: ""
                    email = it.getString(2) ?: ""
                    gender = it.getString(3) ?: "Male"
                    height = if (it.isNull(4)) "" else it.getString(4)
                    weight = if (it.isNull(5)) "" else it.getString(5)
                    dob = it.getString(6) ?: ""
                    address = it.getString(7) ?: ""
                    emergencyContact = it.getString(8) ?: ""
                    emergencyPhone = it.getString(9) ?: ""
                    joiningDate = it.getString(10) ?: ""
                    notes = it.getString(11) ?: ""
                    photoPath = it.getString(12) ?: ""
                }
            }
        }

        val photoPicker = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                try {
                    val file = File(
                        context.filesDir,
                        "member_${memberId}_profile_${System.currentTimeMillis()}.jpg"
                    )
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(file).use { output -> input.copyTo(output) }
                    }
                    photoPath = file.absolutePath
                } catch (e: Exception) {
                    error = "Unable to save photo."
                }
            }
        }

        fun save() {
            if (name.trim().isBlank()) {
                error = "Member name is required."
                return
            }
            if (phone.trim().isBlank()) {
                error = "Phone number is required."
                return
            }

            try {
                val values = android.content.ContentValues().apply {
                    put("name", name.trim())
                    put("phone", phone.trim())
                    put("email", email.trim())
                    put("gender", gender.trim())
                    height.trim().toDoubleOrNull()?.let { put("height_cm", it) }
                        ?: putNull("height_cm")
                    weight.trim().toDoubleOrNull()?.let { put("weight_kg", it) }
                        ?: putNull("weight_kg")
                    put("dob", dob.trim())
                    put("address", address.trim())
                    put("emergency_contact", emergencyContact.trim())
                    put("emergency_phone", emergencyPhone.trim())
                    put("joining_date", joiningDate.trim())
                    put("notes", notes.trim())
                    put("photo_path", photoPath)
                }

                db.writableDatabase.update(
                    "members",
                    values,
                    "id=?",
                    arrayOf(memberId.toString())
                )

                onSaved()
            } catch (e: Exception) {
                error = e.message ?: "Unable to update member."
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onBack) { Text("← BACK") }
                Spacer(Modifier.width(12.dp))
                Text("Edit Member Details", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("Personal Information", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; error = "" },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Name *") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it; error = "" },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Phone *") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))

                    Box {
                        OutlinedButton(
                            onClick = { genderExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Gender: $gender")
                        }
                        DropdownMenu(
                            expanded = genderExpanded,
                            onDismissRequest = { genderExpanded = false }
                        ) {
                            listOf("Male", "Female", "Other").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        gender = option
                                        genderExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = height,
                            onValueChange = { height = it.filter { c -> c.isDigit() || c == '.' } },
                            modifier = Modifier.weight(1f),
                            label = { Text("Height (cm)") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                            modifier = Modifier.weight(1f),
                            label = { Text("Weight (kg)") },
                            singleLine = true
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = dob,
                        onValueChange = { dob = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Date of Birth") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Address") },
                        minLines = 2
                    )

                    Spacer(Modifier.height(16.dp))
                    Text("Emergency Contact", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = emergencyContact,
                        onValueChange = { emergencyContact = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Emergency Contact Name") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = emergencyPhone,
                        onValueChange = { emergencyPhone = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Emergency Contact Phone") },
                        singleLine = true
                    )

                    Spacer(Modifier.height(16.dp))
                    Text("Registration", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = joiningDate,
                        onValueChange = { joiningDate = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Joining Date") },
                        singleLine = true
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { photoPicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (photoPath.isBlank()) "CHANGE MEMBER PHOTO" else "CHANGE PHOTO ✓")
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Notes") },
                        minLines = 3
                    )

                    if (error.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = ::save,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("SAVE CHANGES", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    @Composable
    fun AssignMembershipDialog(
        db: DatabaseHelper,
        memberCode: String,
        plans: List<Array<String>>,
        errorMessage: String,
        onError: (String) -> Unit,
        onDismiss: () -> Unit,
        onSaved: () -> Unit
    ) {
        var selectedPlanId by rememberSaveable { mutableStateOf<Long?>(null) }
        var discountType by rememberSaveable { mutableStateOf("%") }
        var discountText by rememberSaveable { mutableStateOf("0") }
        var paymentText by rememberSaveable { mutableStateOf("0") }
        var paymentMethod by rememberSaveable { mutableStateOf("Cash") }
        var planExpanded by remember { mutableStateOf(false) }
        var discountExpanded by remember { mutableStateOf(false) }
        var paymentExpanded by remember { mutableStateOf(false) }
        var showGroupAssignment by rememberSaveable { mutableStateOf(false) }

        val selectedPlan = plans.firstOrNull { it.getOrNull(0)?.toLongOrNull() == selectedPlanId }
            ?: plans.firstOrNull()

        LaunchedEffect(plans) {
            if (selectedPlanId == null) {
                selectedPlanId = selectedPlan?.getOrNull(0)?.toLongOrNull()
            }
        }

        val price = selectedPlan?.getOrNull(3)?.toDoubleOrNull() ?: 0.0
        val discountValue = discountText.toDoubleOrNull() ?: 0.0
        val discountAmount = if (discountType == "%") {
            price * (discountValue.coerceIn(0.0, 100.0) / 100.0)
        } else {
            discountValue.coerceIn(0.0, price)
        }
        val total = (price - discountAmount).coerceAtLeast(0.0)
        val initialPayment = paymentText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

        if (showGroupAssignment) {
            GroupMembershipDialog(
                db = db,
                preselectedMemberCode = memberCode,
                selectedPlan = selectedPlan,
                onDismiss = { showGroupAssignment = false },
                onError = onError,
                onSaved = { showGroupAssignment = false; onSaved() }
            )
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Assign Membership") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (plans.isEmpty()) {
                        Text("No active regular membership plans are available.")
                    } else {
                        Box {
                            OutlinedButton(
                                onClick = { planExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Plan: ${selectedPlan?.getOrNull(1) ?: "Select Plan"}")
                            }
                            DropdownMenu(
                                expanded = planExpanded,
                                onDismissRequest = { planExpanded = false }
                            ) {
                                plans.forEach { plan ->
                                    DropdownMenuItem(
                                        text = {
                                            Text("${plan.getOrNull(1)} • ₹${plan.getOrNull(3)}")
                                        },
                                        onClick = {
                                            selectedPlanId = plan.getOrNull(0)?.toLongOrNull()
                                            planExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        val isGroupPlan = (selectedPlan?.getOrNull(6) ?: "INDIVIDUAL") in listOf(
                            "COUPLE",
                            "GROUP"
                        )
                        if (isGroupPlan) {
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { showGroupAssignment = true },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("ASSIGN TO MULTIPLE MEMBERS") }
                            Text(
                                "Shared payment: one payment for the selected members.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        ProfileInfoRow("Plan Amount", "₹%.2f".format(price))

                        Spacer(Modifier.height(10.dp))
                        Box {
                            OutlinedButton(
                                onClick = { discountExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Discount Type: $discountType")
                            }
                            DropdownMenu(
                                expanded = discountExpanded,
                                onDismissRequest = { discountExpanded = false }
                            ) {
                                listOf("%", "FIXED").forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(if (option == "%") "Percentage (%)" else "Fixed Amount (₹)") },
                                        onClick = {
                                            discountType = option
                                            discountExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = discountText,
                            onValueChange = {
                                discountText = it.filter { c -> c.isDigit() || c == '.' }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text(if (discountType == "%") "Discount (%)" else "Discount (₹)")
                            },
                            singleLine = true
                        )

                        Spacer(Modifier.height(8.dp))
                        ProfileInfoRow("Discount", "₹%.2f".format(discountAmount))
                        ProfileInfoRow("Total Payable", "₹%.2f".format(total))

                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = paymentText,
                            onValueChange = {
                                paymentText = it.filter { c -> c.isDigit() || c == '.' }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Initial Payment (₹)") },
                            singleLine = true
                        )

                        Spacer(Modifier.height(10.dp))
                        Box {
                            OutlinedButton(
                                onClick = { paymentExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Payment Method: $paymentMethod")
                            }
                            DropdownMenu(
                                expanded = paymentExpanded,
                                onDismissRequest = { paymentExpanded = false }
                            ) {
                                listOf("Cash", "UPI", "Card", "Bank Transfer").forEach { method ->
                                    DropdownMenuItem(
                                        text = { Text(method) },
                                        onClick = {
                                            paymentMethod = method
                                            paymentExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        ProfileInfoRow(
                            "Balance",
                            "₹%.2f".format(
                                (total - initialPayment.coerceAtMost(total)).coerceAtLeast(
                                    0.0
                                )
                            )
                        )
                    }

                    if (errorMessage.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = plans.isNotEmpty(),
                    onClick = {
                        try {
                            val planId = selectedPlanId
                            if (planId == null) {
                                onError("Please select a membership plan.")
                                return@Button
                            }

                            val balance = db.assignMembership(
                                memberCode = memberCode,
                                planId = planId,
                                discountType = discountType,
                                discountValue = discountValue,
                                initialPayment = initialPayment.coerceAtMost(total),
                                paymentMethod = paymentMethod
                            )

                            if (balance > 0.005) {
                                onError("Membership created with ₹%.2f balance.".format(balance))
                                onSaved()
                            } else {
                                onSaved()
                            }
                        } catch (e: Exception) {
                            onError(e.message ?: "Unable to assign membership.")
                        }
                    }
                ) {
                    Text("ASSIGN")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss) { Text("CANCEL") }
            }
        )
    }

    @Composable
    fun GroupMembershipDialog(
        db: DatabaseHelper,
        preselectedMemberCode: String,
        selectedPlan: Array<String>?,
        onDismiss: () -> Unit,
        onError: (String) -> Unit,
        onSaved: () -> Unit
    ) {
        val members = remember { db.getMembersForGroup() }
        val selected =
            remember { mutableStateListOf<String>().apply { add(preselectedMemberCode) } }
        var search by rememberSaveable { mutableStateOf("") }
        var discountType by rememberSaveable { mutableStateOf("%") }
        var discountText by rememberSaveable { mutableStateOf("0") }
        var paymentText by rememberSaveable { mutableStateOf("0") }
        var paymentMethod by rememberSaveable { mutableStateOf("Cash") }
        var discountExpanded by remember { mutableStateOf(false) }
        var paymentExpanded by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf("") }

        val planId = selectedPlan?.getOrNull(0)?.toLongOrNull()
        val planName = selectedPlan?.getOrNull(1) ?: "Group Plan"
        val planType = selectedPlan?.getOrNull(6) ?: "GROUP"
        val configuredMemberCount = selectedPlan?.getOrNull(7)?.toIntOrNull() ?: 2
        val price = selectedPlan?.getOrNull(3)?.toDoubleOrNull() ?: 0.0
        val discountValue = discountText.toDoubleOrNull() ?: 0.0
        val discountAmount = if (discountType == "%") price * (discountValue.coerceIn(
            0.0,
            100.0
        ) / 100.0) else discountValue.coerceIn(0.0, price)
        val total = (price - discountAmount).coerceAtLeast(0.0)
        val paid = (paymentText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0).coerceAtMost(total)
        val balance = (total - paid).coerceAtLeast(0.0)
        val filtered = members.filter {
            it[0].contains(search, true) || it[1].contains(
                search,
                true
            ) || it[2].contains(search, true)
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Assign $planName") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("SELECT MEMBERS (${selected.size})", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search member") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    filtered.forEach { m ->
                        val code = m[0];
                        val checked = selected.contains(code)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (checked) {
                                        if (code != preselectedMemberCode) selected.remove(code)
                                    } else if (selected.size < configuredMemberCount) selected.add(
                                        code
                                    )
                                }
                                .padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = checked, onCheckedChange = {
                                if (it) {
                                    if (selected.size < configuredMemberCount) selected.add(code)
                                } else if (code != preselectedMemberCode) selected.remove(code)
                            })
                            Column {
                                Text(
                                    "${m[0]} - ${m[1]}",
                                    fontWeight = FontWeight.SemiBold
                                ); Text(
                                m[2],
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            }
                        }
                    }
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    ProfileInfoRow("Plan Amount", "₹%.2f".format(price))
                    Box {
                        OutlinedButton(
                            onClick = { discountExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Discount Type: $discountType") }; DropdownMenu(
                        expanded = discountExpanded,
                        onDismissRequest = { discountExpanded = false }) {
                        listOf(
                            "%",
                            "FIXED"
                        ).forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(if (opt == "%") "Percentage (%)" else "Fixed Amount (₹)") },
                                onClick = { discountType = opt; discountExpanded = false })
                        }
                    }
                    }
                    OutlinedTextField(
                        value = discountText,
                        onValueChange = {
                            discountText = it.filter { c -> c.isDigit() || c == '.' }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(if (discountType == "%") "Discount (%)" else "Discount (₹)") },
                        singleLine = true
                    )
                    ProfileInfoRow("Discount", "₹%.2f".format(discountAmount))
                    ProfileInfoRow("Shared Total", "₹%.2f".format(total))
                    OutlinedTextField(
                        value = paymentText,
                        onValueChange = {
                            paymentText = it.filter { c -> c.isDigit() || c == '.' }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Shared Initial Payment (₹)") },
                        singleLine = true
                    )
                    Box {
                        OutlinedButton(
                            onClick = { paymentExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Payment Method: $paymentMethod") }; DropdownMenu(
                        expanded = paymentExpanded,
                        onDismissRequest = { paymentExpanded = false }) {
                        listOf(
                            "Cash",
                            "UPI",
                            "Card",
                            "Bank Transfer"
                        ).forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m) },
                                onClick = { paymentMethod = m; paymentExpanded = false })
                        }
                    }
                    }
                    ProfileInfoRow("Shared Balance", "₹%.2f".format(balance))
                    if (error.isNotBlank()) Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = selected.size >= 2 && selected.size <= configuredMemberCount && planId != null,
                    onClick = {
                        try {
                            val b = db.assignGroupMembership(
                                selected.toList(),
                                planId!!,
                                discountType,
                                discountValue,
                                paid,
                                paymentMethod
                            );
                            onSaved()
                        } catch (e: Exception) {
                            error =
                                e.message ?: "Unable to assign group membership."; onError(error)
                        }
                    }) { Text("ASSIGN TO ${selected.size} MEMBERS") }
            },
            dismissButton = { OutlinedButton(onClick = onDismiss) { Text("CANCEL") } }
        )
    }

    @Composable
    fun EditMembershipDialog(
        db: DatabaseHelper,
        membershipId: Long,
        memberCode: String,
        plans: List<Array<String>>,
        errorMessage: String,
        onError: (String) -> Unit,
        onDismiss: () -> Unit,
        onSaved: () -> Unit
    ) {
        var selectedPlanId by rememberSaveable { mutableStateOf<Long?>(null) }
        var discountType by rememberSaveable { mutableStateOf("%") }
        var discountText by rememberSaveable { mutableStateOf("0") }
        var planAmountText by rememberSaveable { mutableStateOf("0") }
        var startDate by rememberSaveable { mutableStateOf("") }
        var paidAmount by rememberSaveable { mutableStateOf(0.0) }
        var membershipStatus by rememberSaveable { mutableStateOf("Active") }
        var isTrial by rememberSaveable { mutableStateOf(false) }
        var loaded by rememberSaveable { mutableStateOf(false) }
        var planExpanded by remember { mutableStateOf(false) }
        var discountExpanded by remember { mutableStateOf(false) }
        var statusExpanded by remember { mutableStateOf(false) }
        val context = LocalContext.current

        fun cleanNumber(value: String): String =
            value.filter { it.isDigit() || it == '.' }

        fun addMonthsToDate(date: String, months: Int): String {
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            formatter.isLenient = false
            val parsed = try {
                formatter.parse(date)
            } catch (_: Exception) {
                null
            }
                ?: java.util.Date()
            val cal = java.util.Calendar.getInstance()
            cal.time = parsed
            cal.add(java.util.Calendar.MONTH, months)
            return formatter.format(cal.time)
        }

        fun openStartDatePicker() {
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            formatter.isLenient = false
            val parsed = try {
                formatter.parse(startDate)
            } catch (_: Exception) {
                null
            }
            val cal = java.util.Calendar.getInstance()
            if (parsed != null) cal.time = parsed

            android.app.DatePickerDialog(
                context,
                { _, year, month, day ->
                    val picked = java.util.Calendar.getInstance().apply {
                        set(year, month, day)
                    }
                    startDate = formatter.format(picked.time)
                },
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH),
                cal.get(java.util.Calendar.DAY_OF_MONTH)
            ).show()
        }

        LaunchedEffect(membershipId) {
            val c = db.readableDatabase.rawQuery(
                "SELECT plan_id,start_date,plan_amount,discount_type,discount_value,paid_amount,status,is_trial FROM memberships WHERE id=? LIMIT 1",
                arrayOf(membershipId.toString())
            )
            c.use {
                if (it.moveToFirst()) {
                    selectedPlanId = it.getLong(0)
                    startDate = it.getString(1) ?: ""
                    planAmountText = it.getDouble(2).toString()
                    discountType = it.getString(3) ?: "%"
                    discountText = it.getDouble(4).toString()
                    paidAmount = it.getDouble(5)
                    membershipStatus = it.getString(6) ?: "Active"
                    isTrial = it.getInt(7) == 1
                }
            }
            loaded = true
        }

        val selectedPlan = plans.firstOrNull { it.getOrNull(0)?.toLongOrNull() == selectedPlanId }
            ?: plans.firstOrNull()
        val planMonths = selectedPlan?.getOrNull(2)?.toIntOrNull() ?: 0
        val planDefaultPrice = selectedPlan?.getOrNull(3)?.toDoubleOrNull() ?: 0.0
        val planAmount =
            if (isTrial) 0.0 else (planAmountText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
        val discountValue = discountText.toDoubleOrNull() ?: 0.0
        val discountAmount = if (discountType == "%") {
            planAmount * (discountValue.coerceIn(0.0, 100.0) / 100.0)
        } else {
            discountValue.coerceIn(0.0, planAmount)
        }
        val total = (planAmount - discountAmount).coerceAtLeast(0.0)
        val balance = (total - paidAmount).coerceAtLeast(0.0)
        val calculatedEndDate = if (isTrial) {
            "9999-12-31"
        } else if (startDate.isNotBlank()) {
            addMonthsToDate(startDate, planMonths)
        } else {
            ""
        }

        LaunchedEffect(selectedPlanId) {
            if (loaded && selectedPlanId != null && selectedPlanId != selectedPlan?.getOrNull(0)
                    ?.toLongOrNull()
            ) {
                // No-op: selected plan is already loaded.
            }
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Edit Membership") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("Member: $memberCode", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))

                    if (plans.isEmpty()) {
                        Text("No active regular membership plans are available.")
                    } else {
                        Box {
                            OutlinedButton(
                                onClick = { planExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Plan: ${selectedPlan?.getOrNull(1) ?: "Select Plan"}")
                            }
                            DropdownMenu(
                                expanded = planExpanded,
                                onDismissRequest = { planExpanded = false }
                            ) {
                                plans.forEach { plan ->
                                    DropdownMenuItem(
                                        text = { Text("${plan.getOrNull(1)} • ₹${plan.getOrNull(3)}") },
                                        onClick = {
                                            selectedPlanId = plan.getOrNull(0)?.toLongOrNull()
                                            val newPrice =
                                                plan.getOrNull(3)?.toDoubleOrNull() ?: 0.0
                                            if (!isTrial) planAmountText = newPrice.toString()
                                            planExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = planAmountText,
                            onValueChange = { planAmountText = cleanNumber(it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Plan Amount (₹)") },
                            singleLine = true,
                            enabled = !isTrial
                        )

                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { openStartDatePicker() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("START DATE: ${startDate.ifBlank { "Select Date" }}")
                        }

                        Spacer(Modifier.height(8.dp))
                        ProfileInfoRow("Expiry Date", calculatedEndDate.ifBlank { "-" })
                        if (!isTrial) {
                            Text(
                                "Expiry is calculated automatically from the selected plan duration.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        Box {
                            OutlinedButton(
                                onClick = { discountExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Discount Type: $discountType")
                            }
                            DropdownMenu(
                                expanded = discountExpanded,
                                onDismissRequest = { discountExpanded = false }
                            ) {
                                listOf("%", "FIXED").forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(if (option == "%") "Percentage (%)" else "Fixed Amount (₹)") },
                                        onClick = {
                                            discountType = option
                                            discountExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = discountText,
                            onValueChange = { discountText = cleanNumber(it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(if (discountType == "%") "Discount (%)" else "Discount (₹)") },
                            singleLine = true,
                            enabled = !isTrial
                        )

                        Spacer(Modifier.height(8.dp))
                        ProfileInfoRow("Discount Amount", "₹%.2f".format(discountAmount))
                        ProfileInfoRow("Total Payable", "₹%.2f".format(total))
                        ProfileInfoRow("Paid Amount", "₹%.2f".format(paidAmount))
                        ProfileInfoRow("Balance", "₹%.2f".format(balance))

                        Spacer(Modifier.height(10.dp))
                        Box {
                            OutlinedButton(
                                onClick = { statusExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Status: $membershipStatus")
                            }
                            DropdownMenu(
                                expanded = statusExpanded,
                                onDismissRequest = { statusExpanded = false }
                            ) {
                                listOf(
                                    "Active",
                                    "Pending",
                                    "Frozen",
                                    "Cancelled"
                                ).forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            membershipStatus = option
                                            statusExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Paid amount is kept from the existing payment history. Changing the plan, amount, discount or start date recalculates the balance and expiry date.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (errorMessage.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = loaded && selectedPlanId != null && plans.isNotEmpty() && startDate.isNotBlank(),
                    onClick = {
                        try {
                            val planId = selectedPlanId
                                ?: throw IllegalArgumentException("Please select a membership plan.")
                            val amount = if (isTrial) 0.0 else (planAmountText.toDoubleOrNull()
                                ?: throw IllegalArgumentException("Enter a valid plan amount."))
                            if (amount < 0) throw IllegalArgumentException("Plan amount cannot be negative.")
                            if (startDate.isBlank()) throw IllegalArgumentException("Please select a start date.")

                            val planCursor = db.readableDatabase.rawQuery(
                                "SELECT months,is_trial,active FROM membership_plans WHERE id=? LIMIT 1",
                                arrayOf(planId.toString())
                            )
                            var months = 0
                            var selectedTrial = false
                            var active = false
                            planCursor.use {
                                if (!it.moveToFirst()) throw IllegalArgumentException("Plan not found.")
                                months = it.getInt(0)
                                selectedTrial = it.getInt(1) == 1
                                active = it.getInt(2) == 1
                            }
                            if (!active) throw IllegalArgumentException("This plan is inactive.")
                            if (selectedTrial) {
                                isTrial = true
                            }

                            val finalAmount = if (selectedTrial) 0.0 else amount
                            val safeDiscountValue = if (selectedTrial) 0.0 else discountValue
                            val type =
                                if (selectedTrial) "NONE" else discountType.uppercase(java.util.Locale.getDefault())
                            val discount = if (selectedTrial) 0.0 else if (type == "%") {
                                finalAmount * (safeDiscountValue.coerceIn(0.0, 100.0) / 100.0)
                            } else {
                                safeDiscountValue.coerceIn(0.0, finalAmount)
                            }
                            val newTotal = (finalAmount - discount).coerceAtLeast(0.0)
                            val newBalance = (newTotal - paidAmount).coerceAtLeast(0.0)
                            val endDate = if (selectedTrial) "9999-12-31" else addMonthsToDate(
                                startDate,
                                months
                            )
                            val newStatus = if (selectedTrial) "Trial" else membershipStatus

                            val values = android.content.ContentValues().apply {
                                put("plan_id", planId)
                                put("start_date", startDate)
                                put("end_date", endDate)
                                put("plan_amount", finalAmount)
                                put("discount_type", type)
                                put("discount_value", safeDiscountValue)
                                put("discount_amount", discount)
                                put("total_amount", newTotal)
                                put("paid_amount", paidAmount)
                                put("balance_amount", newBalance)
                                put("status", newStatus)
                                put("is_trial", if (selectedTrial) 1 else 0)
                            }
                            val rows = db.writableDatabase.update(
                                "memberships", values, "id=?", arrayOf(membershipId.toString())
                            )
                            if (rows != 1) throw IllegalArgumentException("Unable to update membership.")
                            onSaved()
                        } catch (e: Exception) {
                            onError(e.message ?: "Unable to edit membership.")
                        }
                    }
                ) { Text("SAVE CHANGES") }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss) { Text("CANCEL") }
            }
        )
    }

    @Composable
    fun RenewMembershipDialog(
        db: DatabaseHelper,
        memberCode: String,
        activeEndDate: String,
        plans: List<Array<String>>,
        errorMessage: String,
        onError: (String) -> Unit,
        onDismiss: () -> Unit,
        onSaved: () -> Unit
    ) {
        var selectedPlanId by rememberSaveable { mutableStateOf<Long?>(null) }
        var discountType by rememberSaveable { mutableStateOf("%") }
        var discountText by rememberSaveable { mutableStateOf("0") }
        var paymentText by rememberSaveable { mutableStateOf("0") }
        var paymentMethod by rememberSaveable { mutableStateOf("Cash") }
        var planExpanded by remember { mutableStateOf(false) }
        var discountExpanded by remember { mutableStateOf(false) }
        var paymentExpanded by remember { mutableStateOf(false) }

        val selectedPlan = plans.firstOrNull { it.getOrNull(0)?.toLongOrNull() == selectedPlanId }
            ?: plans.firstOrNull()

        LaunchedEffect(plans) {
            if (selectedPlanId == null) {
                selectedPlanId = selectedPlan?.getOrNull(0)?.toLongOrNull()
            }
        }

        val price = selectedPlan?.getOrNull(3)?.toDoubleOrNull() ?: 0.0
        val discountValue = discountText.toDoubleOrNull() ?: 0.0
        val discountAmount = if (discountType == "%") {
            price * (discountValue.coerceIn(0.0, 100.0) / 100.0)
        } else {
            discountValue.coerceIn(0.0, price)
        }
        val total = (price - discountAmount).coerceAtLeast(0.0)
        val initialPayment = paymentText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

        fun nextDay(date: String): String {
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val parsed = formatter.parse(date) ?: java.util.Date()
            val cal = java.util.Calendar.getInstance()
            cal.time = parsed
            cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
            return formatter.format(cal.time)
        }

        fun addMonths(date: String, months: Int): String {
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val parsed = formatter.parse(date) ?: java.util.Date()
            val cal = java.util.Calendar.getInstance()
            cal.time = parsed
            cal.add(java.util.Calendar.MONTH, months)
            return formatter.format(cal.time)
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Renew Membership") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "Current membership expires: $activeEndDate",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "New membership starts: ${nextDay(activeEndDate)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(10.dp))

                    if (plans.isEmpty()) {
                        Text("No active regular membership plans are available.")
                    } else {
                        Box {
                            OutlinedButton(
                                onClick = { planExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Plan: ${selectedPlan?.getOrNull(1) ?: "Select Plan"}")
                            }
                            DropdownMenu(
                                expanded = planExpanded,
                                onDismissRequest = { planExpanded = false }
                            ) {
                                plans.forEach { plan ->
                                    DropdownMenuItem(
                                        text = {
                                            Text("${plan.getOrNull(1)} • ₹${plan.getOrNull(3)}")
                                        },
                                        onClick = {
                                            selectedPlanId = plan.getOrNull(0)?.toLongOrNull()
                                            planExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        ProfileInfoRow("Plan Amount", "₹%.2f".format(price))

                        Spacer(Modifier.height(10.dp))
                        Box {
                            OutlinedButton(
                                onClick = { discountExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Discount Type: $discountType")
                            }
                            DropdownMenu(
                                expanded = discountExpanded,
                                onDismissRequest = { discountExpanded = false }
                            ) {
                                listOf("%", "FIXED").forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(if (option == "%") "Percentage (%)" else "Fixed Amount (₹)") },
                                        onClick = {
                                            discountType = option
                                            discountExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = discountText,
                            onValueChange = {
                                discountText = it.filter { c -> c.isDigit() || c == '.' }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text(if (discountType == "%") "Discount (%)" else "Discount (₹)")
                            },
                            singleLine = true
                        )

                        Spacer(Modifier.height(8.dp))
                        ProfileInfoRow("Discount", "₹%.2f".format(discountAmount))
                        ProfileInfoRow("Total Payable", "₹%.2f".format(total))

                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = paymentText,
                            onValueChange = {
                                paymentText = it.filter { c -> c.isDigit() || c == '.' }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Initial Payment (₹)") },
                            singleLine = true
                        )

                        Spacer(Modifier.height(10.dp))
                        Box {
                            OutlinedButton(
                                onClick = { paymentExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Payment Method: $paymentMethod")
                            }
                            DropdownMenu(
                                expanded = paymentExpanded,
                                onDismissRequest = { paymentExpanded = false }
                            ) {
                                listOf("Cash", "UPI", "Card", "Bank Transfer").forEach { method ->
                                    DropdownMenuItem(
                                        text = { Text(method) },
                                        onClick = {
                                            paymentMethod = method
                                            paymentExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        ProfileInfoRow(
                            "Balance",
                            "₹%.2f".format(
                                (total - initialPayment.coerceAtMost(total)).coerceAtLeast(
                                    0.0
                                )
                            )
                        )
                    }

                    if (errorMessage.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = plans.isNotEmpty(),
                    onClick = {
                        try {
                            val planId = selectedPlanId
                            if (planId == null) {
                                onError("Please select a membership plan.")
                                return@Button
                            }

                            val dbSql = db.writableDatabase

                            var months = 0
                            var planPrice = 0.0
                            var planActive = false
                            var isTrial = false

                            val c = dbSql.rawQuery(
                                "SELECT months,price,active,is_trial FROM membership_plans WHERE id=?",
                                arrayOf(planId.toString())
                            )
                            c.use {
                                if (!it.moveToFirst()) {
                                    onError("Plan not found.")
                                    return@Button
                                }
                                months = it.getInt(0)
                                planPrice = it.getDouble(1)
                                planActive = it.getInt(2) == 1
                                isTrial = it.getInt(3) == 1
                            }

                            if (!planActive || isTrial || months < 1) {
                                onError("Please select an active regular membership plan.")
                                return@Button
                            }

                            val discount = if (discountType == "%") {
                                planPrice * (discountValue.coerceIn(0.0, 100.0) / 100.0)
                            } else {
                                discountValue.coerceIn(0.0, planPrice)
                            }
                            val payable = (planPrice - discount).coerceAtLeast(0.0)
                            val paid = initialPayment.coerceAtMost(payable)
                            val balance = (payable - paid).coerceAtLeast(0.0)

                            val start = nextDay(activeEndDate)
                            val end = addMonths(start, months)

                            dbSql.beginTransaction()
                            try {
                                val membershipValues = android.content.ContentValues().apply {
                                    put(
                                        "member_id", dbSql.rawQuery(
                                            "SELECT id FROM members WHERE member_code=? LIMIT 1",
                                            arrayOf(memberCode)
                                        ).use { mc ->
                                            if (mc.moveToFirst()) mc.getLong(0) else 0L
                                        })
                                    put("plan_id", planId)
                                    put("start_date", start)
                                    put("end_date", end)
                                    put("plan_amount", planPrice)
                                    put("discount_type", discountType)
                                    put("discount_value", discountValue)
                                    put("discount_amount", discount)
                                    put("total_amount", payable)
                                    put("paid_amount", paid)
                                    put("balance_amount", balance)
                                    put("status", if (balance <= 0.005) "Active" else "Pending")
                                    put("frozen", 0)
                                    put("is_trial", 0)
                                    put(
                                        "created_at",
                                        java.text.SimpleDateFormat(
                                            "yyyy-MM-dd HH:mm:ss",
                                            java.util.Locale.US
                                        ).format(java.util.Date())
                                    )
                                }

                                val membershipId = dbSql.insertOrThrow(
                                    "memberships",
                                    null,
                                    membershipValues
                                )

                                if (paid > 0.0) {
                                    val paymentValues = android.content.ContentValues().apply {
                                        put("member_id", membershipValues.getAsLong("member_id"))
                                        put("membership_id", membershipId)
                                        put("amount", paid)
                                        put("method", paymentMethod)
                                        put("reference", "")
                                        put("notes", "Renewal payment")
                                        put(
                                            "paid_at",
                                            java.text.SimpleDateFormat(
                                                "yyyy-MM-dd HH:mm:ss",
                                                java.util.Locale.US
                                            ).format(java.util.Date())
                                        )
                                    }
                                    dbSql.insertOrThrow("payments", null, paymentValues)
                                }

                                dbSql.setTransactionSuccessful()
                            } finally {
                                dbSql.endTransaction()
                            }

                            onSaved()
                        } catch (e: Exception) {
                            onError(e.message ?: "Unable to renew membership.")
                        }
                    }
                ) {
                    Text("RENEW")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss) { Text("CANCEL") }
            }
        )
    }

    @Composable
    fun PayMembershipBalanceDialog(
        db: DatabaseHelper,
        memberCode: String,
        membershipId: Long,
        balanceAmount: Double,
        errorMessage: String,
        onError: (String) -> Unit,
        onDismiss: () -> Unit,
        onSaved: () -> Unit
    ) {
        var paymentText by rememberSaveable { mutableStateOf("%.2f".format(balanceAmount)) }
        var paymentMethod by rememberSaveable { mutableStateOf("Cash") }
        var paymentExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Pay Membership Balance") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    ProfileInfoRow("Outstanding Balance", "₹%.2f".format(balanceAmount))
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = paymentText,
                        onValueChange = {
                            paymentText = it.filter { c -> c.isDigit() || c == '.' }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Payment Amount (₹)") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    Box {
                        OutlinedButton(
                            onClick = { paymentExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Payment Method: $paymentMethod")
                        }
                        DropdownMenu(
                            expanded = paymentExpanded,
                            onDismissRequest = { paymentExpanded = false }) {
                            listOf("Cash", "UPI", "Card", "Bank Transfer").forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method) },
                                    onClick = { paymentMethod = method; paymentExpanded = false }
                                )
                            }
                        }
                    }
                    if (errorMessage.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    try {
                        val amount = paymentText.toDoubleOrNull() ?: 0.0
                        if (amount <= 0.0) {
                            onError("Enter a payment amount."); return@Button
                        }
                        if (amount > balanceAmount + 0.005) {
                            onError("Payment cannot exceed the outstanding balance."); return@Button
                        }
                        db.addPayment(
                            memberCode,
                            amount,
                            paymentMethod,
                            "",
                            "Membership balance payment"
                        )
                        onSaved()
                    } catch (e: Exception) {
                        onError(e.message ?: "Unable to record balance payment.")
                    }
                }) { Text("PAY") }
            },
            dismissButton = { OutlinedButton(onClick = onDismiss) { Text("CANCEL") } }
        )
    }

    @Composable
    fun MemberProfileWorkspace(
        db: DatabaseHelper,
        memberId: Long,
        name: String,
        memberCode: String,
        profileBitmap: Bitmap?,
        phone: String,
        email: String,
        gender: String,
        height: String,
        weight: String,
        dob: String,
        joiningDate: String,
        address: String,
        emergencyContact: String,
        emergencyPhone: String,
        notes: String,
        planName: String,
        startDate: String,
        endDate: String,
        planAmount: Double,
        discountAmount: Double,
        totalAmount: Double,
        paidAmount: Double,
        balanceAmount: Double,
        membershipStatus: String,
        isTrial: Boolean,
        currentAccessActive: Boolean,
        selectedSection: String,
        documents: List<Array<String>>,
        hasActiveRegularPlan: Boolean,
        hasPendingBalance: Boolean,
        message: String,
        onSectionSelected: (String) -> Unit,
        onAccessChange: (Boolean) -> Unit,
        onBack: () -> Unit,
        onAssign: () -> Unit,
        onRenew: () -> Unit,
        onPayBalance: () -> Unit,
        onEditMembership: () -> Unit,
        onEdit: () -> Unit,
        onAddDocument: () -> Unit,
        onViewDocument: (String) -> Unit,
        onDownloadDocument: (String, String) -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "MEMBER PROFILE",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                ProfileSectionCard(
                    key = "MEMBERSHIP",
                    title = "Membership",
                    subtitle = "Plan, dates & payment",
                    selected = selectedSection == "MEMBERSHIP",
                    onSelected = { onSectionSelected("MEMBERSHIP") }
                )
                Spacer(Modifier.height(10.dp))
                ProfileSectionCard(
                    key = "PERSONAL",
                    title = "Personal Information",
                    subtitle = "Contact & details",
                    selected = selectedSection == "PERSONAL",
                    onSelected = { onSectionSelected("PERSONAL") }
                )
                Spacer(Modifier.height(10.dp))
                ProfileSectionCard(
                    key = "DOCUMENTS",
                    title = "Documents",
                    subtitle = "Government attachments",
                    selected = selectedSection == "DOCUMENTS",
                    onSelected = { onSectionSelected("DOCUMENTS") }
                )
                Spacer(Modifier.height(12.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Gym Access", fontWeight = FontWeight.Bold)
                            Text(
                                if (currentAccessActive) "Access enabled" else "Access disabled",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = currentAccessActive, onCheckedChange = onAccessChange)
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("←  BACK TO MEMBERS")
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                ProfileMemberSummaryHeader(
                    profileBitmap = profileBitmap,
                    name = name,
                    memberCode = memberCode,
                    phone = phone,
                    email = email,
                    joiningDate = joiningDate,
                    accessActive = currentAccessActive,
                    onBack = onBack,
                    onAccessChange = onAccessChange
                )

                Spacer(Modifier.height(14.dp))

                AnimatedContent(
                    targetState = selectedSection,
                    transitionSpec = {
                        fadeIn(tween(220)) + scaleIn(initialScale = 0.98f) togetherWith
                                fadeOut(tween(150))
                    },
                    label = "member_profile_section"
                ) { section ->
                    ProfileSelectedContent(
                        section,
                        profileBitmap,
                        name,
                        memberCode,
                        planName,
                        startDate,
                        endDate,
                        planAmount,
                        discountAmount,
                        totalAmount,
                        paidAmount,
                        balanceAmount,
                        membershipStatus,
                        isTrial,
                        phone,
                        email,
                        gender,
                        height,
                        weight,
                        dob,
                        joiningDate,
                        address,
                        emergencyContact,
                        emergencyPhone,
                        notes,
                        currentAccessActive,
                        documents,
                        hasActiveRegularPlan,
                        hasPendingBalance,
                        onAssign = onAssign,
                        onRenew = onRenew,
                        onPayBalance = onPayBalance,
                        onEditMembership = onEditMembership,
                        onEdit = onEdit,
                        onAddDocument = onAddDocument,
                        onViewDocument = onViewDocument,
                        onDownloadDocument = onDownloadDocument
                    )
                }
            }

            if (message.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    message,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    @Composable
    fun CheckInModeScreen(db: DatabaseHelper, onExit: () -> Unit) {
        val settings = db.getGymSettings()
        val gymName = settings["name"].orEmpty().ifBlank { "GYM MANAGEMENT" }
        val prefix = settings["prefix"].orEmpty().filter { it.isLetterOrDigit() }.uppercase()
            .ifBlank { "AC" }

        var screen by rememberSaveable { mutableStateOf("HOME") }
        var memberNumber by rememberSaveable { mutableStateOf("") }
        var selectedCode by rememberSaveable { mutableStateOf("") }
        var registeredCode by rememberSaveable { mutableStateOf("") }
        var message by rememberSaveable { mutableStateOf("") }
        var error by rememberSaveable { mutableStateOf("") }
        var showExitDialog by rememberSaveable { mutableStateOf(false) }
        var password by rememberSaveable { mutableStateOf("") }
        var showPassword by rememberSaveable { mutableStateOf(false) }
        var refresh by remember { mutableIntStateOf(0) }

        // Block the Android back action while Check-In Mode is active.
        androidx.activity.compose.BackHandler(enabled = true) { }

        fun goHome() {
            screen = "HOME"
            memberNumber = ""
            selectedCode = ""
            message = ""
            error = ""
        }

        fun loadMember() {
            if (memberNumber.isBlank()) {
                error = "Enter your Member ID number."
                return
            }
            val code = (prefix + memberNumber).uppercase()
            val c = db.readableDatabase.rawQuery(
                "SELECT id FROM members WHERE upper(member_code)=? AND active=1 LIMIT 1",
                arrayOf(code)
            )
            val exists = c.use { it.moveToFirst() }
            if (!exists) {
                error = "$code was not found. Please check your Member ID."
                return
            }
            selectedCode = code
            message = ""
            error = ""
            screen = "MEMBER"
        }

        fun pressKey(key: String) {
            when (key) {
                "CLEAR" -> memberNumber = ""
                "BACK" -> if (memberNumber.isNotEmpty()) memberNumber = memberNumber.dropLast(1)
                else -> if (memberNumber.length < 8) memberNumber += key
            }
            error = ""
            message = ""
        }

        if (screen == "REGISTER") {
            AddMemberScreen(
                db = db,
                requirePhoto = true,
                onBack = { screen = "HOME" },
                onSaved = {
                    val c = db.readableDatabase.rawQuery(
                        "SELECT member_code FROM members ORDER BY id DESC LIMIT 1", null
                    )
                    registeredCode = c.use { if (it.moveToFirst()) it.getString(0) else "" }
                    screen = "REGISTERED"
                }
            )
            return
        }

        if (screen == "REGISTERED") {
            Box(Modifier
                .fillMaxSize()
                .padding(24.dp), contentAlignment = Alignment.Center) {
                Card(
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = 650.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(
                        Modifier.padding(36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Text(
                            "REGISTRATION SUCCESSFUL",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Welcome to $gymName", fontSize = 19.sp)
                        Text(
                            "Your Member ID",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            registeredCode.ifBlank { "MEMBER ID" },
                            fontSize = 42.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Trial membership has been activated.", fontSize = 16.sp)
                        Button(
                            onClick = { goHome() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                        ) { Text("BACK TO HOME", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
            return
        }

        if (screen == "MEMBER") {
            CheckInMemberDetailsScreen(
                db = db,
                memberCode = selectedCode,
                refresh = refresh,
                onBack = { goHome() },
                onChanged = { refresh++ },
                onCheckInComplete = { goHome() }
            )
        } else {
            CheckInTabletHomeScreen(
                gymName = gymName,
                prefix = prefix,
                memberNumber = memberNumber,
                message = message,
                error = error,
                onKey = ::pressKey,
                onContinue = ::loadMember,
                onNewMember = { screen = "REGISTER" },
                onExit = { showExitDialog = true }
            )
        }

        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = { Text("Exit Check-In Mode") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Enter the Check-In Password to return to the management screen.")
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = if (showPassword) {
                                androidx.compose.ui.text.input.VisualTransformation.None
                            } else {
                                androidx.compose.ui.text.input.PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                TextButton(onClick = { showPassword = !showPassword }) {
                                    Text(if (showPassword) "HIDE" else "SHOW")
                                }
                            }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (password == CheckInModeConfig.password()) {
                            CheckInModeConfig.setEnabled(false)
                            exitCheckInKiosk()
                            showExitDialog = false
                            password = ""
                            onExit()
                        } else error = "Incorrect password."
                    }) { Text("EXIT") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showExitDialog = false
                    }) { Text("CANCEL") }
                }
            )
        }
    }

    @Composable
    fun CheckInTabletHomeScreen(
        gymName: String,
        prefix: String,
        memberNumber: String,
        message: String,
        error: String,
        onKey: (String) -> Unit,
        onContinue: () -> Unit,
        onNewMember: () -> Unit,
        onExit: () -> Unit
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF06111F))
        ) {
            val wide = maxWidth >= 720.dp
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("CLEAR", "0", "BACK")
            )

            // UX ONLY: all callbacks and existing keypad behaviour are unchanged.
            if (wide) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // BRAND / WELCOME PANEL
                    Card(
                        Modifier
                            .weight(0.92f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(34.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1728)),
                        elevation = CardDefaults.cardElevation(10.dp)
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            Image(
                                painter = painterResource(id = R.drawable.checkin_bg),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(Modifier
                                .fillMaxSize()
                                .background(Color(0xB906111F)))
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Transparent,
                                                Color(0xEE06111F)
                                            )
                                        )
                                    )
                            )
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .padding(34.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier
                                            .size(58.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(Color(0xFF2563EB)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "AC",
                                            color = Color.White,
                                            fontSize = 21.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Spacer(Modifier.width(13.dp))
                                    Column {
                                        Text(
                                            gymName,
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            "AESTHETIC CLUB • SELF CHECK-IN",
                                            color = Color(0xFF60A5FA),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.2.sp
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        Modifier
                                            .size(128.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x332563EB)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            Modifier
                                                .size(94.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF2563EB)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "✓",
                                                color = Color.White,
                                                fontSize = 54.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(22.dp))
                                    Text(
                                        "WELCOME",
                                        color = Color.White,
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 2.sp
                                    )
                                    Spacer(Modifier.height(7.dp))
                                    Text(
                                        "READY FOR YOUR WORKOUT?",
                                        color = Color(0xFFBFDBFE),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(Modifier.height(5.dp))
                                    Text(
                                        "Enter your Member ID to continue",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 13.sp
                                    )
                                }

                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .padding(15.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF22C55E))
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            "CHECK-IN DESK READY",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            "Fast • Simple • Secure",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // KIOSK / KEYPAD PANEL
                    Card(
                        Modifier
                            .weight(1.08f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(34.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        elevation = CardDefaults.cardElevation(10.dp)
                    ) {
                        Column(Modifier
                            .fillMaxSize()
                            .padding(24.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "CHECK IN",
                                        color = Color(0xFF0F172A),
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        "Enter your member number",
                                        color = Color(0xFF64748B),
                                        fontSize = 12.sp
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFDCFCE7)
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF16A34A))
                                        )
                                        Spacer(Modifier.width(7.dp))
                                        Text(
                                            "READY",
                                            color = Color(0xFF166534),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(13.dp))

                            Card(
                                Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1B31))
                            ) {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 15.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "MEMBER ID",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 2.sp
                                    )
                                    Spacer(Modifier.height(5.dp))
                                    Text(
                                        if (memberNumber.isBlank()) "$prefix • • • •" else "$prefix$memberNumber",
                                        color = Color.White,
                                        fontSize = 34.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 3.sp
                                    )
                                    Text(
                                        "Prefix: $prefix",
                                        color = Color(0xFF60A5FA),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))

                            Column(
                                Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                keys.forEach { row ->
                                    Row(
                                        Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        row.forEach { key ->
                                            val isClear = key == "CLEAR"
                                            val isBack = key == "BACK"
                                            val buttonColor = when {
                                                isClear -> Color(0xFFDC2626)
                                                isBack -> Color(0xFFF59E0B)
                                                else -> Color(0xFF1D4ED8)
                                            }
                                            Button(
                                                onClick = { onKey(key) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight(),
                                                shape = RoundedCornerShape(17.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                                            ) {
                                                Text(
                                                    if (isBack) "⌫" else key,
                                                    fontSize = if (key.length > 1) 12.sp else 25.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (error.isNotBlank() || message.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Surface(
                                    Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (error.isNotBlank()) Color(0xFFFFE4E6) else Color(
                                        0xFFDCFCE7
                                    )
                                ) {
                                    Text(
                                        if (error.isNotBlank()) error else message,
                                        Modifier
                                            .padding(9.dp)
                                            .fillMaxWidth(),
                                        color = if (error.isNotBlank()) Color(0xFFB91C1C) else Color(
                                            0xFF166534
                                        ),
                                        fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                            Spacer(Modifier.height(9.dp))
                            Button(
                                onClick = onContinue,
                                enabled = memberNumber.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(17.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFF16A34A
                                    )
                                )
                            ) {
                                Text(
                                    "CONTINUE  →",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(Modifier.height(7.dp))
                            OutlinedButton(
                                onClick = onNewMember,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(45.dp),
                                shape = RoundedCornerShape(15.dp)
                            ) {
                                Text(
                                    "＋ NEW MEMBER / GUEST",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1728))
                    ) {
                        Box(Modifier
                            .fillMaxWidth()
                            .height(190.dp)) {
                            Image(
                                painter = painterResource(id = R.drawable.checkin_bg),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(Modifier
                                .fillMaxSize()
                                .background(Color(0xCC06111F)))
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    gymName,
                                    color = Color.White,
                                    fontSize = 23.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "WELCOME",
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                                Text("SELF CHECK-IN • READY", color = Color(0xFF60A5FA), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))) {
                        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ENTER MEMBER ID", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.height(8.dp))
                            Text(if (memberNumber.isBlank()) "$prefix • • • •" else "$prefix$memberNumber", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1D4ED8), letterSpacing = 2.sp)
                            Spacer(Modifier.height(10.dp))
                            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                keys.forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    row.forEach { key ->
                                        val c = when (key) { "CLEAR" -> Color(0xFFDC2626); "BACK" -> Color(0xFFF59E0B); else -> Color(0xFF1D4ED8) }
                                        Button(onClick = { onKey(key) }, modifier = Modifier
                                            .weight(1f)
                                            .height(58.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = c)) {
                                            Text(if (key == "BACK") "⌫" else key, fontSize = if (key.length > 1) 12.sp else 22.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                } }
                            }
                            if (error.isNotBlank()) Text(error, Modifier.padding(top = 7.dp), color = Color(0xFFDC2626), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = onContinue, enabled = memberNumber.isNotBlank(), modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))) { Text("CONTINUE  →", fontWeight = FontWeight.ExtraBold) }
                            OutlinedButton(onClick = onNewMember, modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp), shape = RoundedCornerShape(14.dp)) { Text("＋ NEW MEMBER / GUEST", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }

            OutlinedButton(onClick = onExit,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)))
            {
                Text("EXIT CHECK-IN MODE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }


    @Composable
        fun CheckInMemberDetailsScreen(
        db: DatabaseHelper,
        memberCode: String,
        refresh: Int,
        onBack: () -> Unit,
        onChanged: () -> Unit,
        onCheckInComplete: () -> Unit
    ) {
        val row = remember(memberCode, refresh) {
            val result = mutableListOf<Array<String>>()

            val c = db.readableDatabase.rawQuery(
                """
            SELECT m.name,
                   m.phone,
                   COALESCE(m.photo_path,''),
                   COALESCE(mp.name,'No Plan'),
                   COALESCE(ms.end_date,''),
                   COALESCE(ms.balance_amount,0),
                   COALESCE(ms.is_trial,0),
                   COALESCE(ms.status,''),
                   EXISTS(
                       SELECT 1
                       FROM attendance a
                       WHERE a.member_id=m.id
                         AND a.check_out IS NULL
                   )
            FROM members m
            LEFT JOIN memberships ms
                ON ms.id=(
                    SELECT MAX(ms2.id)
                    FROM memberships ms2
                    WHERE ms2.member_id=m.id
                )
            LEFT JOIN membership_plans mp
                ON mp.id=ms.plan_id
            WHERE upper(m.member_code)=?
            LIMIT 1
            """.trimIndent(),
                arrayOf(memberCode)
            )

            c.use {
                if (it.moveToFirst()) {
                    result.add(
                        Array(it.columnCount) { i ->
                            it.getString(i) ?: ""
                        }
                    )
                }
            }

            result.firstOrNull()
        }

        if (row == null) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Member not found",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Button(onClick = onBack) {
                        Text("BACK TO HOME")
                    }
                }
            }

            return
        }

        val name = row[0]
        val photoPath = row[2]
        val plan = row[3]
        val expiry = row[4]
        val balance = row[5].toDoubleOrNull() ?: 0.0
        val isTrial = row[6] == "1"
        val status = row[7]
        val checkedIn = row[8] == "1"

        val remainingDays =
            if (isTrial || expiry.isBlank()) {
                0
            } else {
                runCatching {
                    val fmt = java.text.SimpleDateFormat(
                        "yyyy-MM-dd",
                        java.util.Locale.US
                    )

                    val expiryDate = fmt.parse(expiry)
                    val today = fmt.parse(
                        fmt.format(java.util.Date())
                    )

                    if (
                        expiryDate != null &&
                        today != null
                    ) {
                        (
                                (expiryDate.time - today.time) /
                                        (24L * 60L * 60L * 1000L)
                                ).toInt()
                    } else {
                        0
                    }
                }.getOrDefault(0)
            }

        // ---------------------------------------------------------
        // CLOCK IN SUCCESS
        // ---------------------------------------------------------

        var checkInDone by rememberSaveable(memberCode) {
            mutableStateOf(false)
        }

        // ---------------------------------------------------------
        // CLOCK OUT SUCCESS
        // ---------------------------------------------------------

        var checkOutDone by rememberSaveable(memberCode) {
            mutableStateOf(false)
        }

        var workedHours by rememberSaveable(memberCode) {
            mutableStateOf(0.0)
        }

        // ---------------------------------------------------------
        // CLOCK IN SUCCESS TIMER
        // ---------------------------------------------------------

        LaunchedEffect(checkInDone) {
            if (checkInDone) {
                delay(1500)
                onCheckInComplete()
            }
        }

        // ---------------------------------------------------------
        // CLOCK OUT SUCCESS TIMER
        // ---------------------------------------------------------

        LaunchedEffect(checkOutDone) {
            if (checkOutDone) {
                delay(2500)
                onCheckInComplete()
            }
        }

        val bg = when {
            checkOutDone -> Color(0xFF1D4ED8)
            checkInDone -> Color(0xFF047857)
            isTrial -> Color(0xFF991B1B)
            else -> Color(0xFF06111F)
        }

        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(bg)
        ) {

            val wide = maxWidth >= 720.dp

            Image(
                painter = painterResource(
                    id = R.drawable.checkin_bg
                ),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Color.Black.copy(
                            alpha = if (
                                checkInDone || checkOutDone
                            ) {
                                0.55f
                            } else {
                                0.62f
                            }
                        )
                    )
            )

            // =====================================================
            // CLOCK-IN SUCCESS SCREEN
            // =====================================================

            if (checkInDone) {

                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {

                    Card(
                        Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp),
                        shape = RoundedCornerShape(34.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF047857)
                        )
                    ) {

                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment =
                                Alignment.CenterHorizontally,
                            verticalArrangement =
                                Arrangement.spacedBy(12.dp)
                        ) {

                            Box(
                                Modifier
                                    .size(104.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Color.White.copy(
                                            alpha = 0.16f
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {

                                Text(
                                    "✓",
                                    color = Color.White,
                                    fontSize = 62.sp,
                                    fontWeight =
                                        FontWeight.ExtraBold
                                )
                            }

                            Text(
                                "CHECK-IN COMPLETE",
                                color = Color.White,
                                fontSize =
                                    if (wide) 32.sp else 26.sp,
                                fontWeight =
                                    FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )

                            Text(
                                name,
                                color = Color.White,
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                memberCode,
                                color = Color(0xFFD1FAE5),
                                fontSize = 18.sp,
                                fontWeight =
                                    FontWeight.ExtraBold,
                                letterSpacing = 1.5.sp
                            )

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(
                                    alpha = 0.12f
                                )
                            ) {

                                Text(
                                    CheckInModeConfig
                                        .successMessage()
                                        .ifBlank {
                                            "CHECK-IN DONE! HAVE A GREAT WORKOUT."
                                        },
                                    Modifier.padding(
                                        horizontal = 18.dp,
                                        vertical = 12.dp
                                    ),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight =
                                        FontWeight.Medium,
                                    textAlign =
                                        androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                return@BoxWithConstraints
            }

            // =====================================================
            // CLOCK-OUT SUCCESS SCREEN
            // =====================================================

            if (checkOutDone) {

                val totalMinutes =
                    (workedHours * 60.0)
                        .toInt()
                        .coerceAtLeast(0)

                val workedHoursPart =
                    totalMinutes / 60

                val workedMinutesPart =
                    totalMinutes % 60

                val workedText =
                    when {
                        workedHoursPart > 0 &&
                                workedMinutesPart > 0 ->
                            "$workedHoursPart hr $workedMinutesPart min"

                        workedHoursPart > 0 ->
                            "$workedHoursPart hr"

                        else ->
                            "$workedMinutesPart min"
                    }

                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {

                    Card(
                        Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp),
                        shape = RoundedCornerShape(34.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1D4ED8)
                        )
                    ) {

                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment =
                                Alignment.CenterHorizontally,
                            verticalArrangement =
                                Arrangement.spacedBy(12.dp)
                        ) {

                            Box(
                                Modifier
                                    .size(104.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Color.White.copy(
                                            alpha = 0.16f
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {

                                Text(
                                    "✓",
                                    color = Color.White,
                                    fontSize = 62.sp,
                                    fontWeight =
                                        FontWeight.ExtraBold
                                )
                            }

                            Text(
                                "CLOCK-OUT COMPLETE",
                                color = Color.White,
                                fontSize =
                                    if (wide) 32.sp else 26.sp,
                                fontWeight =
                                    FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )

                            Text(
                                name,
                                color = Color.White,
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                memberCode,
                                color = Color(0xFFBFDBFE),
                                fontSize = 18.sp,
                                fontWeight =
                                    FontWeight.ExtraBold,
                                letterSpacing = 1.5.sp
                            )

                            Spacer(
                                Modifier.height(8.dp)
                            )

                            Text(
                                "YOU WORKED FOR $workedText",
                                color = Color.White.copy(
                                    alpha = 0.85f
                                ),
                                fontSize = 14.sp,
                                fontWeight =
                                    FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )

                            Text(
                                workedText,
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight =
                                    FontWeight.ExtraBold
                            )

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(
                                    alpha = 0.12f
                                )
                            ) {

                                Text(
                                    "GREAT WORKOUT! HAVE A WONDERFUL DAY.",
                                    Modifier.padding(
                                        horizontal = 18.dp,
                                        vertical = 12.dp
                                    ),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight =
                                        FontWeight.Medium,
                                    textAlign =
                                        androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                return@BoxWithConstraints
            }

            // =====================================================
            // LANDSCAPE UI
            // =====================================================

            if (wide) {

                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    horizontalArrangement =
                        Arrangement.spacedBy(18.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    // PROFILE SIDE

                    Card(
                        Modifier
                            .weight(0.92f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(34.dp),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                if (isTrial)
                                    Color(0xFF991B1B)
                                else
                                    Color(0xFF0B1B31)
                        ),
                        elevation =
                            CardDefaults.cardElevation(10.dp)
                    ) {

                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(30.dp),
                            horizontalAlignment =
                                Alignment.CenterHorizontally,
                            verticalArrangement =
                                Arrangement.SpaceBetween
                        ) {

                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Surface(
                                    shape =
                                        RoundedCornerShape(13.dp),
                                    color =
                                        if (isTrial)
                                            Color(0x33FFFFFF)
                                        else
                                            Color(0xFF172F52)
                                ) {

                                    Text(
                                        if (isTrial)
                                            "TRIAL"
                                        else
                                            "MEMBER",
                                        Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 7.dp
                                        ),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight =
                                            FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Spacer(
                                    Modifier.weight(1f)
                                )

                                Text(
                                    if (checkedIn)
                                        "● CHECKED IN"
                                    else
                                        "● READY",
                                    color =
                                        if (checkedIn)
                                            Color(0xFF86EFAC)
                                        else
                                            Color(0xFF93C5FD),
                                    fontSize = 10.sp,
                                    fontWeight =
                                        FontWeight.ExtraBold
                                )
                            }

                            Column(
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {

                                if (photoPath.isNotBlank()) {

                                    val bitmap =
                                        remember(photoPath) {
                                            BitmapFactory
                                                .decodeFile(
                                                    photoPath
                                                )
                                        }

                                    if (bitmap != null) {

                                        Image(
                                            bitmap =
                                                bitmap
                                                    .asImageBitmap(),
                                            contentDescription =
                                                "Member photo",
                                            modifier =
                                                Modifier
                                                    .size(150.dp)
                                                    .clip(
                                                        CircleShape
                                                    ),
                                            contentScale =
                                                ContentScale.Crop
                                        )

                                    } else {

                                        Box(
                                            Modifier
                                                .size(150.dp)
                                                .clip(
                                                    CircleShape
                                                )
                                                .background(
                                                    Color(0xFF2563EB)
                                                ),
                                            contentAlignment =
                                                Alignment.Center
                                        ) {

                                            Text(
                                                name
                                                    .take(1)
                                                    .uppercase(),
                                                color = Color.White,
                                                fontSize = 58.sp,
                                                fontWeight =
                                                    FontWeight.ExtraBold
                                            )
                                        }
                                    }

                                } else {

                                    Box(
                                        Modifier
                                            .size(150.dp)
                                            .clip(
                                                CircleShape
                                            )
                                            .background(
                                                if (isTrial)
                                                    Color(0xFFDC2626)
                                                else
                                                    Color(0xFF2563EB)
                                            ),
                                        contentAlignment =
                                            Alignment.Center
                                    ) {

                                        Text(
                                            name
                                                .take(1)
                                                .uppercase(),
                                            color = Color.White,
                                            fontSize = 58.sp,
                                            fontWeight =
                                                FontWeight.ExtraBold
                                        )
                                    }
                                }

                                Spacer(
                                    Modifier.height(18.dp)
                                )

                                Text(
                                    name,
                                    color = Color.White,
                                    fontSize = 30.sp,
                                    fontWeight =
                                        FontWeight.ExtraBold,
                                    textAlign =
                                        androidx.compose.ui.text.style.TextAlign.Center
                                )

                                Spacer(
                                    Modifier.height(4.dp)
                                )

                                Text(
                                    memberCode,
                                    color =
                                        if (isTrial)
                                            Color(0xFFFECACA)
                                        else
                                            Color(0xFF93C5FD),
                                    fontSize = 18.sp,
                                    fontWeight =
                                        FontWeight.ExtraBold,
                                    letterSpacing = 2.sp
                                )

                                Spacer(
                                    Modifier.height(10.dp)
                                )

                                Text(
                                    plan,
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 15.sp,
                                    fontWeight =
                                        FontWeight.Medium
                                )
                            }

                            Text(
                                if (isTrial)
                                    "TRIAL MEMBERSHIP • SPECIAL CHECK-IN"
                                else
                                    "WELCOME BACK • HAVE A GREAT SESSION",
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.sp,
                                fontWeight =
                                    FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                textAlign =
                                    androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    // DETAILS + ACTIONS SIDE

                    Card(
                        Modifier
                            .weight(1.08f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(34.dp),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                Color(0xFFF8FAFC)
                        ),
                        elevation =
                            CardDefaults.cardElevation(10.dp)
                    ) {

                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        ) {

                            Text(
                                if (isTrial)
                                    "TRIAL ACCESS"
                                else
                                    "MEMBERSHIP STATUS",
                                color = Color(0xFF64748B),
                                fontSize = 10.sp,
                                fontWeight =
                                    FontWeight.ExtraBold,
                                letterSpacing = 1.5.sp
                            )

                            Text(
                                if (isTrial)
                                    "Welcome to your first workout"
                                else
                                    "You're all set to train",
                                color = Color(0xFF0F172A),
                                fontSize = 25.sp,
                                fontWeight =
                                    FontWeight.ExtraBold
                            )

                            Spacer(
                                Modifier.height(14.dp)
                            )

                            if (!isTrial) {

                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement =
                                        Arrangement.spacedBy(10.dp)
                                ) {

                                    InfoTile(
                                        "EXPIRY",
                                        expiry.ifBlank {
                                            "—"
                                        },
                                        Color(0xFF2563EB),
                                        Modifier.weight(1f)
                                    )

                                    InfoTile(
                                        "DAYS LEFT",
                                        remainingDays.toString(),
                                        if (
                                            remainingDays <= 10
                                        )
                                            Color(0xFFF59E0B)
                                        else
                                            Color(0xFF16A34A),
                                        Modifier.weight(1f)
                                    )

                                    InfoTile(
                                        "BALANCE",
                                        "₹${
                                            String.format(
                                                java.util.Locale.US,
                                                "%.2f",
                                                balance
                                            )
                                        }",
                                        if (balance > 0)
                                            Color(0xFFDC2626)
                                        else
                                            Color(0xFF16A34A),
                                        Modifier.weight(1f)
                                    )
                                }

                            } else {

                                Card(
                                    Modifier.fillMaxWidth(),
                                    shape =
                                        RoundedCornerShape(20.dp),
                                    colors =
                                        CardDefaults.cardColors(
                                            containerColor =
                                                Color(0xFFFFE4E6)
                                        )
                                ) {

                                    Column(
                                        Modifier.padding(17.dp)
                                    ) {

                                        Text(
                                            "TRIAL MEMBERSHIP",
                                            color =
                                                Color(0xFFB91C1C),
                                            fontSize = 11.sp,
                                            fontWeight =
                                                FontWeight.ExtraBold
                                        )

                                        Spacer(
                                            Modifier.height(4.dp)
                                        )

                                        Text(
                                            "Your trial access is active. Enjoy your workout!",
                                            color =
                                                Color(0xFF7F1D1D),
                                            fontSize = 13.sp,
                                            fontWeight =
                                                FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Spacer(
                                Modifier.height(12.dp)
                            )

                            Card(
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                shape =
                                    RoundedCornerShape(22.dp),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor =
                                            Color(0xFFF1F5F9)
                                    )
                            ) {

                                Column(
                                    Modifier
                                        .fillMaxSize()
                                        .padding(18.dp),
                                    verticalArrangement =
                                        Arrangement.SpaceBetween
                                ) {

                                    Column {

                                        Text(
                                            "ATTENDANCE",
                                            color =
                                                Color(0xFF64748B),
                                            fontSize = 10.sp,
                                            fontWeight =
                                                FontWeight.ExtraBold,
                                            letterSpacing = 1.3.sp
                                        )

                                        Spacer(
                                            Modifier.height(6.dp)
                                        )

                                        Text(
                                            if (checkedIn)
                                                "Member is currently inside the gym"
                                            else
                                                "Ready to record today's visit",
                                            color =
                                                Color(0xFF334155),
                                            fontSize = 14.sp,
                                            fontWeight =
                                                FontWeight.SemiBold
                                        )

                                        Spacer(
                                            Modifier.height(10.dp)
                                        )

                                        Text(
                                            CheckInModeConfig
                                                .successMessage()
                                                .ifBlank {
                                                    "CHECK-IN DONE! HAVE A GREAT WORKOUT."
                                                },
                                            Modifier.padding(
                                                horizontal = 18.dp,
                                                vertical = 12.dp
                                            ),
                                            color = Color.Black,
                                            fontSize = 25.sp,
                                            fontWeight =
                                                FontWeight.ExtraBold,
                                            textAlign =
                                                androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }

                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement =
                                            Arrangement.spacedBy(10.dp)
                                    ) {

                                        Button(
                                            onClick = {
                                                try {

                                                    val ok =
                                                        db.checkIn(
                                                            memberCode
                                                        )

                                                    if (ok) {
                                                        checkInDone = true
                                                        onChanged()
                                                    }

                                                } catch (_: Exception) {
                                                }
                                            },
                                            enabled = !checkedIn,
                                            modifier =
                                                Modifier
                                                    .weight(1f)
                                                    .height(54.dp),
                                            shape =
                                                RoundedCornerShape(
                                                    18.dp
                                                ),
                                            colors =
                                                ButtonDefaults
                                                    .buttonColors(
                                                        containerColor =
                                                            Color(0xFF16A34A)
                                                    )
                                        ) {

                                            Text(
                                                "✓  CLOCK IN",
                                                fontSize = 16.sp,
                                                fontWeight =
                                                    FontWeight.ExtraBold
                                            )
                                        }

                                        // =================================================
                                        // CORRECTED CLOCK OUT
                                        // =================================================

                                        OutlinedButton(
                                            onClick = {

                                                try {

                                                    val ok =
                                                        db.checkOut(
                                                            memberCode
                                                        )

                                                    if (ok) {

                                                        // Read the workout duration
                                                        // that was saved by DatabaseHelper.
                                                        val durationCursor =
                                                            db.readableDatabase.rawQuery(
                                                                """
                                                            SELECT COALESCE(workout_hours,0)
                                                            FROM attendance
                                                            WHERE member_id = (
                                                                SELECT id
                                                                FROM members
                                                                WHERE UPPER(member_code)=UPPER(?)
                                                                LIMIT 1
                                                            )
                                                            ORDER BY id DESC
                                                            LIMIT 1
                                                            """.trimIndent(),
                                                                arrayOf(memberCode)
                                                            )

                                                        durationCursor.use {

                                                            if (
                                                                it.moveToFirst()
                                                            ) {
                                                                workedHours =
                                                                    it.getDouble(0)
                                                            }
                                                        }

                                                        checkOutDone = true
                                                        onChanged()
                                                    }

                                                } catch (_: Exception) {
                                                }
                                            },
                                            enabled = checkedIn,
                                            modifier =
                                                Modifier
                                                    .weight(1f)
                                                    .height(54.dp),
                                            shape =
                                                RoundedCornerShape(
                                                    18.dp
                                                )
                                        ) {

                                            Text(
                                                "CLOCK OUT",
                                                fontSize = 15.sp,
                                                fontWeight =
                                                    FontWeight.ExtraBold
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = onBack,
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .height(48.dp),
                                        shape =
                                            RoundedCornerShape(15.dp)
                                    ) {

                                        Text(
                                            "← BACK TO CHECK-IN",
                                            fontSize = 12.sp,
                                            fontWeight =
                                                FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

            } else {

                // =====================================================
                // PORTRAIT UI
                // =====================================================

                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(
                            rememberScrollState()
                        )
                        .padding(12.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Card(
                        Modifier.fillMaxWidth(),
                        shape =
                            RoundedCornerShape(28.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    if (isTrial)
                                        Color(0xFF991B1B)
                                    else
                                        Color(0xFF0B1B31)
                            )
                    ) {

                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            if (photoPath.isNotBlank()) {

                                val bitmap =
                                    remember(photoPath) {
                                        BitmapFactory
                                            .decodeFile(
                                                photoPath
                                            )
                                    }

                                if (bitmap != null) {

                                    Image(
                                        bitmap =
                                            bitmap
                                                .asImageBitmap(),
                                        contentDescription =
                                            "Member photo",
                                        modifier =
                                            Modifier
                                                .size(105.dp)
                                                .clip(
                                                    CircleShape
                                                ),
                                        contentScale =
                                            ContentScale.Crop
                                    )
                                }
                            }

                            Text(
                                if (isTrial)
                                    "TRIAL MEMBER"
                                else
                                    "WELCOME BACK",
                                color = Color.White,
                                fontSize = 23.sp,
                                fontWeight =
                                    FontWeight.ExtraBold
                            )

                            Text(
                                name,
                                color = Color.White,
                                fontSize = 27.sp,
                                fontWeight =
                                    FontWeight.ExtraBold
                            )

                            Text(
                                memberCode,
                                color = Color(0xFF93C5FD),
                                fontSize = 17.sp,
                                fontWeight =
                                    FontWeight.ExtraBold,
                                letterSpacing = 1.5.sp
                            )

                            Text(
                                plan,
                                color = Color(0xFFE2E8F0),
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(
                        Modifier.height(10.dp)
                    )

                    if (!isTrial) {

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {

                            InfoTile(
                                "EXPIRY",
                                expiry.ifBlank {
                                    "—"
                                },
                                Color(0xFF2563EB),
                                Modifier.weight(1f)
                            )

                            InfoTile(
                                "DAYS",
                                remainingDays.toString(),
                                if (
                                    remainingDays <= 10
                                )
                                    Color(0xFFF59E0B)
                                else
                                    Color(0xFF16A34A),
                                Modifier.weight(1f)
                            )

                            InfoTile(
                                "BALANCE",
                                "₹${
                                    String.format(
                                        java.util.Locale.US,
                                        "%.2f",
                                        balance
                                    )
                                }",
                                if (balance > 0)
                                    Color(0xFFDC2626)
                                else
                                    Color(0xFF16A34A),
                                Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(
                        Modifier.height(10.dp)
                    )

                    Button(
                        onClick = {

                            try {

                                if (
                                    db.checkIn(
                                        memberCode
                                    )
                                ) {
                                    checkInDone = true
                                    onChanged()
                                }

                            } catch (_: Exception) {
                            }
                        },
                        enabled = !checkedIn,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(58.dp),
                        shape =
                            RoundedCornerShape(17.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    Color(0xFF16A34A)
                            )
                    ) {

                        Text(
                            "✓  CLOCK IN",
                            fontSize = 16.sp,
                            fontWeight =
                                FontWeight.ExtraBold
                        )
                    }

                    Spacer(
                        Modifier.height(7.dp)
                    )

                    // =================================================
                    // CORRECTED PORTRAIT CLOCK OUT
                    // =================================================

                    OutlinedButton(
                        onClick = {

                            try {

                                val ok =
                                    db.checkOut(
                                        memberCode
                                    )

                                if (ok) {

                                    val durationCursor =
                                        db.readableDatabase.rawQuery(
                                            """
                                        SELECT COALESCE(workout_hours,0)
                                        FROM attendance
                                        WHERE member_id = (
                                            SELECT id
                                            FROM members
                                            WHERE UPPER(member_code)=UPPER(?)
                                            LIMIT 1
                                        )
                                        ORDER BY id DESC
                                        LIMIT 1
                                        """.trimIndent(),
                                            arrayOf(memberCode)
                                        )

                                    durationCursor.use {

                                        if (
                                            it.moveToFirst()
                                        ) {
                                            workedHours =
                                                it.getDouble(0)
                                        }
                                    }

                                    checkOutDone = true
                                    onChanged()
                                }

                            } catch (_: Exception) {
                            }
                        },
                        enabled = checkedIn,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                        shape =
                            RoundedCornerShape(15.dp)
                    ) {

                        Text(
                            "CLOCK OUT",
                            fontWeight =
                                FontWeight.ExtraBold
                        )
                    }

                    Spacer(
                        Modifier.height(7.dp)
                    )

                    OutlinedButton(
                        onClick = onBack,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                        shape =
                            RoundedCornerShape(15.dp)
                    ) {

                        Text(
                            "← BACK TO CHECK-IN",
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun InfoTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
        Column(modifier
            .clip(RoundedCornerShape(18.dp))
            .background(accent.copy(alpha = 0.10f))
            .padding(12.dp)) {
            Text(label, color = accent, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = Color(0xFF0F172A), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        }
    }
    @Composable
    fun GymSettingsScreen(db: DatabaseHelper, onEnterCheckInMode: () -> Unit) {
        val settings = db.getGymSettings()

        var gymName by rememberSaveable {
            mutableStateOf(settings["name"].orEmpty())
        }
        var gymPhone by rememberSaveable {
            mutableStateOf(settings["phone"].orEmpty())
        }
        var gymAddress by rememberSaveable {
            mutableStateOf(settings["address"].orEmpty())
        }
        var prefix by rememberSaveable {
            mutableStateOf(settings["prefix"].orEmpty().ifBlank { "AC" })
        }

        var storageMode by rememberSaveable {
            mutableStateOf(DataStorageConfig.mode())
        }
        var serverUrl by rememberSaveable {
            mutableStateOf(DataStorageConfig.serverUrl())
        }
        var apiKey by rememberSaveable {
            mutableStateOf(DataStorageConfig.apiKey())
        }
        var serverFieldsExpanded by rememberSaveable {
            mutableStateOf(storageMode == DataStorageMode.SERVER)
        }
        var message by rememberSaveable { mutableStateOf("") }
        var error by rememberSaveable { mutableStateOf("") }

        var checkInPassword by rememberSaveable {
            mutableStateOf(CheckInModeConfig.password())
        }
        var showCheckInPassword by rememberSaveable { mutableStateOf(false) }
        var checkInSuccessMessage by rememberSaveable { mutableStateOf(CheckInModeConfig.successMessage()) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Manage your gym configuration",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "GYM DETAILS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = gymName,
                        onValueChange = { gymName = it; error = ""; message = "" },
                        label = { Text("Gym Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = gymPhone,
                        onValueChange = { gymPhone = it; message = "" },
                        label = { Text("Gym Phone") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = gymAddress,
                        onValueChange = { gymAddress = it; message = "" },
                        label = { Text("Gym Address") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = prefix,
                        onValueChange = {
                            prefix = it.filter { c -> c.isLetterOrDigit() }.uppercase()
                            error = ""
                            message = ""
                        },
                        label = { Text("Member ID Prefix *") },
                        supportingText = { Text("Example: AC → AC0001") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            when {
                                gymName.isBlank() -> error = "Gym Name is required."
                                prefix.isBlank() -> error = "Member ID Prefix is required."
                                else -> {
                                    try {
                                        db.saveGymSettings(
                                            gymName.trim(),
                                            gymPhone.trim(),
                                            gymAddress.trim(),
                                            prefix.trim().uppercase()
                                        )
                                        message = "Gym details saved successfully."
                                        error = ""
                                    } catch (e: Exception) {
                                        error = e.message ?: "Unable to save gym details."
                                        message = ""
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("SAVE GYM DETAILS")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "DATA STORAGE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        if (storageMode == DataStorageMode.LOCAL)
                            "Local storage is active. Data is stored on this device."
                        else
                            "Server storage is active. Data connection uses the server settings below.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = storageMode == DataStorageMode.LOCAL,
                            onClick = {
                                storageMode = DataStorageMode.LOCAL
                                serverFieldsExpanded = false
                                error = ""
                                message = ""
                            },
                            label = { Text("Local") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = storageMode == DataStorageMode.SERVER,
                            onClick = {
                                storageMode = DataStorageMode.SERVER
                                serverFieldsExpanded = true
                                error = ""
                                message = ""
                            },
                            label = { Text("Server") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    AnimatedVisibility(
                        visible = storageMode == DataStorageMode.SERVER && serverFieldsExpanded
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = serverUrl,
                                onValueChange = { serverUrl = it; error = ""; message = "" },
                                label = { Text("Server Address") },
                                placeholder = { Text("http://192.168.x.x:8765") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = apiKey,
                                onValueChange = { apiKey = it; error = ""; message = "" },
                                label = { Text("API Key") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            if (storageMode == DataStorageMode.LOCAL) {
                                DataStorageConfig.saveLocal()
                                message = "Local storage selected."
                                error = ""
                            } else if (serverUrl.isBlank()) {
                                error = "Server Address is required."
                                message = ""
                            } else if (apiKey.isBlank()) {
                                error = "API Key is required."
                                message = ""
                            } else {
                                DataStorageConfig.saveServer(serverUrl.trim(), apiKey.trim())
                                message = "Server storage settings saved."
                                error = ""
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("SAVE STORAGE SETTINGS")
                    }

                    if (storageMode == DataStorageMode.SERVER) {
                        TextButton(
                            onClick = { serverFieldsExpanded = !serverFieldsExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (serverFieldsExpanded) "HIDE SERVER SETTINGS" else "SHOW SERVER SETTINGS")
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("CHECK-IN MODE", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Use this device as a member self-service check-in screen.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = checkInPassword,
                        onValueChange = { checkInPassword = it; error = ""; message = "" },
                        label = { Text("Check-In Password") },
                        singleLine = true,
                        visualTransformation = if (showCheckInPassword) {
                            androidx.compose.ui.text.input.VisualTransformation.None
                        } else {
                            androidx.compose.ui.text.input.PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            TextButton(
                                onClick = { showCheckInPassword = !showCheckInPassword }
                            ) {
                                Text(if (showCheckInPassword) "HIDE" else "SHOW")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = checkInSuccessMessage,
                        onValueChange = { checkInSuccessMessage = it; error = ""; message = "" },
                        label = { Text("Check-In Success Message") },
                        placeholder = { Text("CHECK-IN DONE! HAVE A GREAT WORKOUT.") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = {
                        if (checkInPassword.length < 4) error = "Check-In Password must be at least 4 characters."
                        else if (checkInSuccessMessage.isBlank()) error = "Check-In Success Message is required."
                        else {
                            CheckInModeConfig.setPassword(checkInPassword)
                            CheckInModeConfig.setSuccessMessage(checkInSuccessMessage.trim())
                            CheckInModeConfig.setEnabled(true)
                            enterCheckInKiosk()
                            message = "Check-In settings saved."
                            error = ""
                            onEnterCheckInMode()
                        }
                    }, modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)) {
                        Text(if (CheckInModeConfig.isEnabled()) "UPDATE PASSWORD & ENTER MODE" else "SAVE PASSWORD & ENTER MODE")
                    }
                    OutlinedButton(onClick = { CheckInModeConfig.setEnabled(false); message = "Check-In Mode disabled."; error = "" }, modifier = Modifier.fillMaxWidth()) {
                        Text("DISABLE CHECK-IN MODE")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "CURRENT CONFIGURATION",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    ProfileInfoRow("Storage Mode", if (storageMode == DataStorageMode.LOCAL) "Local" else "Server")
                    ProfileInfoRow("Member Prefix", prefix.ifBlank { "-" })
                    if (storageMode == DataStorageMode.SERVER) {
                        ProfileInfoRow("Server", serverUrl.ifBlank { "Not configured" })
                    }
                    ProfileInfoRow("Gym ID", ServerConfig.gymId().ifBlank { "Local configuration" })
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "APP INFORMATION",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(10.dp))
                    ProfileInfoRow("Application", "Gym Management")
                    ProfileInfoRow("Platform", "Android")
                    ProfileInfoRow("Configuration", "Settings module")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Settings changes are saved without changing the other application modules.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (error.isNotBlank()) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            if (message.isNotBlank()) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }

}

@Composable
fun ProfileHeader(
    name: String,
    memberCode: String,
    accessActive: Boolean,
    onBack: () -> Unit,
    onAccessChange: (Boolean) -> Unit,
    compact: Boolean = false
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 12.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBack) { Text("←") }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(name, fontSize = if (compact) 21.sp else 24.sp, fontWeight = FontWeight.Bold)
                Text(memberCode, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!compact) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (accessActive) "ACTIVE" else "INACTIVE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Switch(checked = accessActive, onCheckedChange = onAccessChange)
                }
            }
        }
    }
}

@Composable
fun ProfilePhotoCard(bitmap: Bitmap?, name: String, memberCode: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Member Profile Photo",
                    modifier = Modifier
                        .size(145.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    Modifier
                        .size(145.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) { Text("NO PHOTO", fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(10.dp))
            Text(name, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1)
            Text(memberCode, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ProfileMemberSummaryHeader(
    profileBitmap: Bitmap?,
    name: String,
    memberCode: String,
    phone: String,
    email: String,
    joiningDate: String,
    accessActive: Boolean,
    onBack: () -> Unit,
    onAccessChange: (Boolean) -> Unit
) {
    val headerColor = Color(0xFF1565C0)
    val accentColor = if (accessActive) Color(0xFF16A34A) else Color(0xFFDC2626)
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = headerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(Modifier
            .fillMaxWidth()
            .padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onBack,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .55f))
                ) { Text("← BACK") }
                Spacer(Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(50), color = accentColor) {
                    Text(
                        if (accessActive) "● ACTIVE ACCESS" else "● ACCESS DISABLED",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (profileBitmap != null) {
                    Image(
                        bitmap = profileBitmap.asImageBitmap(),
                        contentDescription = "Member Profile Photo",
                        modifier = Modifier
                            .size(118.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier
                        .size(118.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
                        Text("NO PHOTO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(name.ifBlank { "Member" }, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 2)
                    Spacer(Modifier.height(4.dp))
                    Text("MEMBER ID  •  $memberCode", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = .85f))
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileMiniInfo("PHONE", phone.ifBlank { "-" }, Modifier.weight(1f), Color.White.copy(alpha = .14f), Color.White)
                        ProfileMiniInfo("SINCE", joiningDate.ifBlank { "-" }, Modifier.weight(1f), Color.White.copy(alpha = .14f), Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileMiniInfo(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(modifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = contentColor.copy(alpha = .72f))
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = contentColor, maxLines = 1)
        }
    }
}

@Composable
fun ProfileSectionCard(
    key: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelected: () -> Unit
) {
    val sectionColor = when (key) {
        "MEMBERSHIP" -> Color(0xFF7C3AED)
        "PERSONAL" -> Color(0xFF0284C7)
        else -> Color(0xFF0F9D8A)
    }
    Card(
        modifier = Modifier.width(220.dp),
        onClick = onSelected,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) sectionColor else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 5.dp else 2.dp)
    ) {
        Row(Modifier
            .fillMaxWidth()
            .padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(
                    if (selected) Color.White.copy(alpha = .18f) else sectionColor.copy(
                        alpha = .12f
                    )
                ), contentAlignment = Alignment.Center) {
                Text(when (key) { "MEMBERSHIP" -> "▣"; "PERSONAL" -> "●"; else -> "▤" }, fontSize = 21.sp, color = if (selected) Color.White else sectionColor, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 10.sp, color = if (selected) Color.White.copy(alpha = .78f) else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ProfileSelectedContent(
    section: String,
    profileBitmap: Bitmap?,
    name: String,
    memberCode: String,
    planName: String,
    startDate: String,
    endDate: String,
    planAmount: Double,
    discountAmount: Double,
    totalAmount: Double,
    paidAmount: Double,
    balanceAmount: Double,
    membershipStatus: String,
    isTrial: Boolean,
    phone: String,
    email: String,
    gender: String,
    height: String,
    weight: String,
    dob: String,
    joiningDate: String,
    address: String,
    emergencyContact: String,
    emergencyPhone: String,
    notes: String,
    accessActive: Boolean,
    documents: List<Array<String>>,
    hasActiveRegularPlan: Boolean,
    hasPendingBalance: Boolean,
    onAssign: () -> Unit,
    onRenew: () -> Unit,
    onPayBalance: () -> Unit,
    onEditMembership: () -> Unit,
    onEdit: () -> Unit,
    onAddDocument: () -> Unit,
    onViewDocument: (String) -> Unit,
    onDownloadDocument: (String, String) -> Unit
) {
    val sectionColor = when (section) {
        "MEMBERSHIP" -> Color(0xFF7C3AED)
        "PERSONAL" -> Color(0xFF0284C7)
        else -> Color(0xFF0F9D8A)
    }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(sectionColor), contentAlignment = Alignment.Center) {
                    Text(when(section){"MEMBERSHIP"->"▣";"PERSONAL"->"●";else->"▤"}, color=Color.White, fontSize=22.sp, fontWeight=FontWeight.ExtraBold)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(when(section){"MEMBERSHIP"->"Membership Details";"PERSONAL"->"Personal Information";else->"Government Documents"}, fontSize=22.sp, fontWeight=FontWeight.ExtraBold)
                    Text(when(section){"MEMBERSHIP"->"Plan, dates and payment summary";"PERSONAL"->"Contact and member details";else->"Upload and manage member documents"}, fontSize=11.sp, color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (section == "PERSONAL") OutlinedButton(onClick=onEdit) { Text("EDIT") }
                if (section == "DOCUMENTS") Button(onClick=onAddDocument) { Text("+ ADD DOCUMENT") }
            }
            Spacer(Modifier.height(18.dp))
            when (section) {
                "MEMBERSHIP" -> {
                    val statusColor = if (isTrial) Color(0xFFDC2626) else if (membershipStatus.equals("Active", true)) Color(0xFF16A34A) else Color(0xFFF59E0B)
                    Surface(shape=RoundedCornerShape(14.dp), color=statusColor.copy(alpha=.12f)) { Text(if(isTrial) "TRIAL MEMBERSHIP" else membershipStatus.uppercase(), color=statusColor, fontSize=11.sp, fontWeight=FontWeight.ExtraBold, modifier=Modifier.padding(horizontal=12.dp,vertical=8.dp)) }
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        ProfileMetricCard("PLAN", planName.ifBlank{"-"}, sectionColor, Modifier.weight(1f))
                        ProfileMetricCard("START", startDate.ifBlank{"-"}, Color(0xFF0284C7), Modifier.weight(1f))
                        ProfileMetricCard("EXPIRY", endDate.ifBlank{"-"}, Color(0xFFF59E0B), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        ProfileMetricCard("PLAN AMOUNT", "₹%.2f".format(planAmount), Color(0xFF2563EB), Modifier.weight(1f))
                        ProfileMetricCard("DISCOUNT", "₹%.2f".format(discountAmount), Color(0xFFDB2777), Modifier.weight(1f))
                        ProfileMetricCard("TOTAL", "₹%.2f".format(totalAmount), Color(0xFF7C3AED), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        ProfileMetricCard("PAID", "₹%.2f".format(paidAmount), Color(0xFF16A34A), Modifier.weight(1f))
                        ProfileMetricCard("BALANCE", "₹%.2f".format(balanceAmount), if(balanceAmount>0) Color(0xFFDC2626) else Color(0xFF16A34A), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(18.dp))
                    OutlinedButton(onClick=onEditMembership, modifier=Modifier.fillMaxWidth()) { Text("EDIT MEMBERSHIP") }
                    Spacer(Modifier.height(8.dp))
                    when {
                        hasPendingBalance -> Button(onClick=onPayBalance, modifier=Modifier.fillMaxWidth()) { Text("PAY BALANCE  •  ₹%.2f".format(balanceAmount)) }
                        hasActiveRegularPlan -> OutlinedButton(onClick=onRenew, modifier=Modifier.fillMaxWidth()) { Text("RENEW MEMBERSHIP") }
                        else -> Button(onClick=onAssign, modifier=Modifier.fillMaxWidth()) { Text("ASSIGN MEMBERSHIP") }
                    }
                }
                "PERSONAL" -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        ProfileMetricCard("PHONE", phone.ifBlank{"-"}, Color(0xFF0284C7), Modifier.weight(1f))
                        ProfileMetricCard("EMAIL", email.ifBlank{"-"}, Color(0xFF7C3AED), Modifier.weight(1f))
                        ProfileMetricCard("GENDER", gender.ifBlank{"-"}, Color(0xFFDB2777), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        ProfileMetricCard("HEIGHT", if (height.isBlank()) "-" else "$height cm", Color(0xFF0891B2), Modifier.weight(1f))
                        ProfileMetricCard("WEIGHT", if (weight.isBlank()) "-" else "$weight kg", Color(0xFFEA580C), Modifier.weight(1f))
                        Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(14.dp))
                    ProfileInfoRow("Name", name); ProfileInfoRow("Member ID", memberCode); ProfileInfoRow("Date of Birth", dob); ProfileInfoRow("Joining Date", joiningDate); ProfileInfoRow("Address", address)
                    Spacer(Modifier.height(14.dp))
                    Text("EMERGENCY CONTACT", fontSize=13.sp, fontWeight=FontWeight.ExtraBold, color=Color(0xFFDC2626))
                    ProfileInfoRow("Name", emergencyContact); ProfileInfoRow("Phone", emergencyPhone)
                    if(notes.isNotBlank()){ Spacer(Modifier.height(10.dp)); Text("NOTES", fontSize=13.sp, fontWeight=FontWeight.ExtraBold, color=Color(0xFFF59E0B)); Text(notes, fontSize=14.sp) }
                }
                "DOCUMENTS" -> {
                    if (documents.isEmpty()) Text("No government documents uploaded.", color=MaterialTheme.colorScheme.onSurfaceVariant)
                    else documents.forEach { doc ->
                        Card(Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp), shape=RoundedCornerShape(16.dp), colors=CardDefaults.cardColors(containerColor=Color(0xFF0F9D8A).copy(alpha=.08f))) {
                            Row(Modifier
                                .fillMaxWidth()
                                .padding(13.dp), verticalAlignment=Alignment.CenterVertically) {
                                Box(Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0F9D8A)), contentAlignment=Alignment.Center){Text("▤",color=Color.White,fontSize=20.sp)}
                                Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)){Text(doc[1],fontWeight=FontWeight.Bold);Text("Uploaded: ${doc[3]}",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant);Text("UPLOADED",fontSize=9.sp,fontWeight=FontWeight.ExtraBold,color=Color(0xFF0F9D8A))}
                                OutlinedButton(onClick={onViewDocument(doc[2])}){Text("VIEW")}; Spacer(Modifier.width(5.dp)); Button(onClick={onDownloadDocument(doc[2],doc[1])}){Text("DOWNLOAD")}
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileMetricCard(title: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(modifier, shape=RoundedCornerShape(16.dp), colors=CardDefaults.cardColors(containerColor=accent.copy(alpha=.10f))) {
        Column(Modifier.padding(13.dp)) {
            Text(title,fontSize=9.sp,fontWeight=FontWeight.ExtraBold,color=accent)
            Spacer(Modifier.height(5.dp)); Text(value,fontSize=15.sp,fontWeight=FontWeight.ExtraBold,maxLines=2)
        }
    }
}

@Composable
fun ProfileInfoRow(title: String, value: String) {
    Row(Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp), verticalAlignment=Alignment.CenterVertically) {
        Text(title,Modifier.weight(1f),fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.ifBlank{"-"},fontSize=14.sp,fontWeight=FontWeight.Bold)
    }
}
@Composable
fun StandaloneReports(
    db: DatabaseHelper,
    refresh: Int
) {
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var period by rememberSaveable { mutableStateOf("TODAY") }
    var reportType by rememberSaveable { mutableStateOf("SUMMARY") }
    var customDate by rememberSaveable { mutableStateOf("") }

    // Attendance member search
    var attendanceMemberSearch by rememberSaveable { mutableStateOf("") }

    val context = LocalContext.current
    var pendingExportCsv by remember { mutableStateOf("") }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(
                        pendingExportCsv.toByteArray(Charsets.UTF_8)
                    )
                }
            } catch (_: Exception) {
            }
        }

        pendingExportCsv = ""
    }

    // ============================================================
    // DATE FILTER
    // ============================================================

    fun dateWhere(column: String): String = when (period) {

        "YESTERDAY" ->
            "date($column) = date('now','-1 day')"

        "THIS_WEEK" ->
            "date($column) >= date('now','-6 day') " +
                    "AND date($column) <= date('now')"

        "THIS_MONTH" ->
            "date($column) >= date('now','start of month') " +
                    "AND date($column) <= date('now')"

        "CUSTOM" ->
            if (customDate.isNotBlank()) {
                "date($column) = '$customDate'"
            } else {
                "date($column) = date('now')"
            }

        else ->
            "date($column) = date('now')"
    }

    fun dateLabel(): String = when (period) {

        "YESTERDAY" -> "Yesterday"

        "THIS_WEEK" -> "This Week"

        "THIS_MONTH" -> "This Month"

        "CUSTOM" ->
            if (customDate.isBlank()) {
                "Custom Date"
            } else {
                customDate
            }

        else -> "Today"
    }

    fun reportLabel(): String = when (reportType) {

        "MEMBERSHIP" ->
            "Membership Report"

        "PAYMENTS" ->
            "Payment / Revenue Report"

        "ATTENDANCE" ->
            "Attendance Report"

        "EXPENSE" ->
            "Expense / Profit Report"

        else ->
            "Summary Report"
    }

    // ============================================================
    // CUSTOM DATE PICKER
    // ============================================================

    fun openCustomDatePicker() {

        val cal = java.util.Calendar.getInstance()

        if (customDate.isNotBlank()) {
            try {
                val parsed = java.text.SimpleDateFormat(
                    "yyyy-MM-dd",
                    java.util.Locale.US
                ).parse(customDate)

                if (parsed != null) {
                    cal.time = parsed
                }

            } catch (_: Exception) {
            }
        }

        android.app.DatePickerDialog(
            context,
            { _, year, month, day ->

                customDate =
                    "%04d-%02d-%02d".format(
                        year,
                        month + 1,
                        day
                    )

                period = "CUSTOM"
            },

            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH),
            cal.get(java.util.Calendar.DAY_OF_MONTH)

        ).show()
    }

    // ============================================================
    // REFRESH
    // ============================================================

    val refreshKey = refresh

    // ============================================================
    // SUMMARY DATA
    // ============================================================

    val collection = db.scalarDouble(
        "SELECT COALESCE(SUM(amount),0) " +
                "FROM payments " +
                "WHERE ${dateWhere("paid_at")}"
    )

    val expenses = db.scalarDouble(
        "SELECT COALESCE(SUM(amount),0) " +
                "FROM expenses " +
                "WHERE ${dateWhere("expense_date")}"
    )

    val netIncome = collection - expenses

    val checkIns = db.scalarInt(
        "SELECT COUNT(*) " +
                "FROM attendance " +
                "WHERE ${dateWhere("check_in")}"
    )

    val newMembers = db.scalarInt(
        "SELECT COUNT(*) " +
                "FROM members " +
                "WHERE ${dateWhere("created_at")}"
    )

    val renewals = db.scalarInt(
        """
        SELECT COUNT(*)
        FROM payments
        WHERE ${dateWhere("paid_at")}
        AND lower(COALESCE(notes,'')) LIKE '%renew%'
        """.trimIndent()
    )

    // ============================================================
    // PENDING BALANCE
    // ============================================================

    val pendingBalance = db.scalarDouble(
        """
        SELECT COALESCE(SUM(balance_amount),0)
        FROM memberships ms
        WHERE ms.id = (
            SELECT MAX(ms2.id)
            FROM memberships ms2
            WHERE ms2.member_id = ms.member_id
        )
        AND balance_amount > 0.005
        """.trimIndent()
    )

    // ============================================================
    // MEMBER COUNTS
    // ============================================================

    val activeMembers = db.scalarInt(
        "SELECT COUNT(*) FROM members WHERE active=1"
    )

    val trialMembers = db.scalarInt(
        """
        SELECT COUNT(*)
        FROM memberships ms
        WHERE ms.is_trial=1
        AND ms.id=(
            SELECT MAX(ms2.id)
            FROM memberships ms2
            WHERE ms2.member_id=ms.member_id
        )
        """.trimIndent()
    )

    val expiringSoon = db.scalarInt(
        """
        SELECT COUNT(*)
        FROM memberships
        WHERE is_trial=0
        AND status='Active'
        AND date(end_date)
        BETWEEN date('now')
        AND date('now','+10 day')
        """.trimIndent()
    )

    // ============================================================
    // PAYMENTS
    // ============================================================

    val payments = mutableListOf<Array<String>>()

    db.readableDatabase.rawQuery(
        """
        SELECT
            m.member_code,
            m.name,
            COALESCE(mp.name,'-'),
            p.amount,
            p.paid_at,
            COALESCE(p.method,'-'),
            COALESCE(p.notes,'')
        FROM payments p
        JOIN members m
            ON m.id=p.member_id
        LEFT JOIN memberships ms
            ON ms.id=p.membership_id
        LEFT JOIN membership_plans mp
            ON mp.id=ms.plan_id
        WHERE ${dateWhere("p.paid_at")}
        ORDER BY p.id DESC
        LIMIT 30
        """.trimIndent(),
        null
    ).use { c ->

        while (c.moveToNext()) {

            payments += Array(c.columnCount) { i ->
                c.getString(i) ?: ""
            }
        }
    }

    // ============================================================
    // MEMBERSHIPS
    // ============================================================

    val memberships = mutableListOf<Array<String>>()

    db.readableDatabase.rawQuery(
        """
        SELECT
            m.member_code,
            m.name,
            COALESCE(mp.name,'-'),
            ms.start_date,
            ms.end_date,
            ms.status,
            ms.total_amount,
            ms.paid_amount,
            ms.balance_amount
        FROM memberships ms
        JOIN members m
            ON m.id=ms.member_id
        LEFT JOIN membership_plans mp
            ON mp.id=ms.plan_id
        WHERE ${dateWhere("ms.start_date")}
        ORDER BY ms.id DESC
        LIMIT 30
        """.trimIndent(),
        null
    ).use { c ->

        while (c.moveToNext()) {

            memberships += Array(c.columnCount) { i ->
                c.getString(i) ?: ""
            }
        }
    }

    // ============================================================
    // MEMBER SEARCH
    // ============================================================

    val searchText =
        attendanceMemberSearch
            .trim()
            .replace("'", "''")

    val attendanceMemberWhere =
        if (searchText.isBlank()) {
            ""
        } else {
            """
            AND (
                lower(m.name) LIKE lower('%$searchText%')
                OR lower(m.member_code) LIKE lower('%$searchText%')
            )
            """.trimIndent()
        }

    // ============================================================
    // ATTENDANCE DETAIL
    //
    // Columns:
    // 0 Member ID
    // 1 Member
    // 2 Plan
    // 3 Status
    // 4 Check In
    // 5 Check Out
    // 6 Login Hours
    // 7 Source
    // ============================================================

    val attendance = mutableListOf<Array<String>>()

    db.readableDatabase.rawQuery(
        """
        SELECT
            m.member_code,
            m.name,
            COALESCE(mp.name,'-'),
            COALESCE(ms.status,'-'),
            a.check_in,
            COALESCE(a.check_out,'OPEN'),

            CASE
                WHEN a.check_out IS NULL THEN
                    ROUND(
                        MAX(
                            0,
                            (julianday('now') - julianday(a.check_in)) * 24
                        ),
                        2
                    )
                ELSE
                    COALESCE(a.workout_hours,0)
            END,

            COALESCE(a.source,'Manual')

        FROM attendance a

        JOIN members m
            ON m.id=a.member_id

        LEFT JOIN memberships ms
            ON ms.id=(
                SELECT MAX(ms2.id)
                FROM memberships ms2
                WHERE ms2.member_id=m.id
            )

        LEFT JOIN membership_plans mp
            ON mp.id=ms.plan_id

        WHERE ${dateWhere("a.check_in")}
        $attendanceMemberWhere

        ORDER BY
            date(a.check_in) DESC,
            a.id DESC
        """.trimIndent(),
        null
    ).use { c ->

        while (c.moveToNext()) {

            attendance += Array(c.columnCount) { i ->
                c.getString(i) ?: ""
            }
        }
    }

    // ============================================================
    // DAILY ATTENDANCE
    //
    // One row per member per day.
    //
    // Columns:
    // 0 Date
    // 1 Member ID
    // 2 Member
    // 3 First Login
    // 4 Last Logout
    // 5 Total Login Hours
    // ============================================================

    val dailyAttendance = mutableListOf<Array<String>>()

    db.readableDatabase.rawQuery(
        """
        SELECT
            date(a.check_in) AS attendance_date,

            m.member_code,

            m.name,

            MIN(a.check_in) AS first_login,

            CASE
                WHEN SUM(
                    CASE
                        WHEN a.check_out IS NULL THEN 1
                        ELSE 0
                    END
                ) > 0
                THEN 'OPEN'
                ELSE MAX(a.check_out)
            END AS last_logout,

            ROUND(
                SUM(
                    CASE
                        WHEN a.check_out IS NULL THEN
                            MAX(
                                0,
                                (julianday('now') - julianday(a.check_in)) * 24
                            )
                        ELSE
                            COALESCE(a.workout_hours,0)
                    END
                ),
                2
            ) AS total_login_hours

        FROM attendance a

        JOIN members m
            ON m.id=a.member_id

        WHERE ${dateWhere("a.check_in")}
        $attendanceMemberWhere

        GROUP BY
            date(a.check_in),
            m.id,
            m.member_code,
            m.name

        ORDER BY
            date(a.check_in) DESC,
            m.name ASC
        """.trimIndent(),
        null
    ).use { c ->

        while (c.moveToNext()) {

            dailyAttendance += Array(c.columnCount) { i ->
                c.getString(i) ?: ""
            }
        }
    }

    // ============================================================
    // TOTAL LOGIN HOURS
    // ============================================================

    val totalLoginHours = db.scalarDouble(
        """
        SELECT COALESCE(
            SUM(
                CASE
                    WHEN a.check_out IS NULL THEN
                        MAX(
                            0,
                            (julianday('now') - julianday(a.check_in)) * 24
                        )
                    ELSE
                        COALESCE(a.workout_hours,0)
                END
            ),
            0
        )

        FROM attendance a

        JOIN members m
            ON m.id=a.member_id

        WHERE ${dateWhere("a.check_in")}
        $attendanceMemberWhere
        """.trimIndent()
    )

    // ============================================================
    // FORMAT HOURS
    // ============================================================

    fun formatHours(hours: Double): String {

        val totalMinutes =
            (hours * 60.0).toInt()

        val h =
            totalMinutes / 60

        val m =
            totalMinutes % 60

        return "${h}h ${m}m"
    }

    // ============================================================
    // CSV
    // ============================================================

    fun csvCell(value: String): String {

        val safe =
            value.replace("\"", "\"\"")

        return "\"$safe\""
    }

    fun buildCsv(): String {

        val sb = StringBuilder()

        sb.append(
            csvCell("Gym Management Report")
        ).append("\n")

        sb.append(
            csvCell("Report Type")
        ).append(",")

        sb.append(
            csvCell(reportLabel())
        ).append("\n")

        sb.append(
            csvCell("Period")
        ).append(",")

        sb.append(
            csvCell(dateLabel())
        ).append("\n\n")

        // ========================================================
        // METRICS
        // ========================================================

        sb.append("Metric,Value\n")

        sb.append(
            csvCell("Collection")
        ).append(",")

        sb.append(
            csvCell(
                "₹%.2f".format(collection)
            )
        ).append("\n")

        sb.append(
            csvCell("Expenses")
        ).append(",")

        sb.append(
            csvCell(
                "₹%.2f".format(expenses)
            )
        ).append("\n")

        sb.append(
            csvCell("Net Income")
        ).append(",")

        sb.append(
            csvCell(
                "₹%.2f".format(netIncome)
            )
        ).append("\n")

        sb.append(
            csvCell("Check-ins")
        ).append(",")
            .append(checkIns)
            .append("\n")

        sb.append(
            csvCell("New Members")
        ).append(",")
            .append(newMembers)
            .append("\n")

        sb.append(
            csvCell("Renewals")
        ).append(",")
            .append(renewals)
            .append("\n")

        sb.append(
            csvCell("Pending Balance")
        ).append(",")

        sb.append(
            csvCell(
                "₹%.2f".format(pendingBalance)
            )
        ).append("\n")

        sb.append(
            csvCell("Active Members")
        ).append(",")
            .append(activeMembers)
            .append("\n")

        sb.append(
            csvCell("Trial Members")
        ).append(",")
            .append(trialMembers)
            .append("\n")

        sb.append(
            csvCell("Expiring Within 10 Days")
        ).append(",")
            .append(expiringSoon)
            .append("\n")

        // ========================================================
        // ATTENDANCE TOTAL
        // ========================================================

        sb.append(
            csvCell("Total Login Hours")
        ).append(",")

        sb.append(
            csvCell(
                formatHours(totalLoginHours)
            )
        ).append("\n\n")

        // ========================================================
        // PAYMENTS
        // ========================================================

        if (
            reportType == "SUMMARY" ||
            reportType == "PAYMENTS"
        ) {

            sb.append("\nPayments\n")

            sb.append(
                "Member ID,Member,Plan,Amount,Date,Method,Type\n"
            )

            payments.forEach { r ->

                val type =
                    when {

                        (r.getOrNull(6) ?: "")
                            .contains("renew", true) ->
                            "Renewal"

                        (r.getOrNull(6) ?: "")
                            .contains("balance", true) ->
                            "Balance Payment"

                        else ->
                            "Membership Payment"
                    }

                listOf(
                    r.getOrNull(0) ?: "",
                    r.getOrNull(1) ?: "",
                    r.getOrNull(2) ?: "-",
                    r.getOrNull(3) ?: "0",
                    r.getOrNull(4) ?: "",
                    r.getOrNull(5) ?: "-",
                    type
                ).joinTo(sb, ",") {

                    csvCell(it)
                }

                sb.append("\n")
            }
        }

        // ========================================================
        // MEMBERSHIPS
        // ========================================================

        if (
            reportType == "SUMMARY" ||
            reportType == "MEMBERSHIP"
        ) {

            sb.append("\nMemberships\n")

            sb.append(
                "Member ID,Member,Plan,Start,Expiry,Status,Total,Paid,Balance\n"
            )

            memberships.forEach { r ->

                (0..8).joinTo(sb, ",") { idx ->
                    csvCell(
                        r.getOrNull(idx) ?: ""
                    )
                }

                sb.append("\n")
            }
        }

        // ========================================================
        // ATTENDANCE DETAIL CSV
        // ========================================================

        if (
            reportType == "SUMMARY" ||
            reportType == "ATTENDANCE"
        ) {

            sb.append("\nAttendance Detail\n")

            sb.append(
                "Member ID,Member,Plan,Status,Check In,Check Out,Login Hours,Source\n"
            )

            attendance.forEach { r ->

                listOf(
                    r.getOrNull(0) ?: "",
                    r.getOrNull(1) ?: "",
                    r.getOrNull(2) ?: "",
                    r.getOrNull(3) ?: "",
                    r.getOrNull(4) ?: "",
                    r.getOrNull(5) ?: "",
                    formatHours(
                        r.getOrNull(6)
                            ?.toDoubleOrNull()
                            ?: 0.0
                    ),
                    r.getOrNull(7) ?: ""
                ).joinTo(sb, ",") {

                    csvCell(it)
                }

                sb.append("\n")
            }

            // ====================================================
            // DAILY ATTENDANCE CSV
            // ====================================================

            sb.append("\nDaily Member Attendance\n")

            sb.append(
                "Date,Member ID,Member,First Login,Last Logout,Total Login Hours\n"
            )

            dailyAttendance.forEach { r ->

                listOf(
                    r.getOrNull(0) ?: "",
                    r.getOrNull(1) ?: "",
                    r.getOrNull(2) ?: "",
                    r.getOrNull(3) ?: "",
                    r.getOrNull(4) ?: "",
                    formatHours(
                        r.getOrNull(5)
                            ?.toDoubleOrNull()
                            ?: 0.0
                    )
                ).joinTo(sb, ",") {

                    csvCell(it)
                }

                sb.append("\n")
            }
        }

        return sb.toString()
    }

    // ============================================================
    // UI
    // ============================================================

    key(refreshKey) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(18.dp),

            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {

            // ====================================================
            // HEADER
            // ====================================================

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {

                Column(
                    Modifier.padding(22.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Column(
                            Modifier.weight(1f)
                        ) {

                            Text(
                                "Business Reports",
                                fontSize = 28.sp,
                                fontWeight =
                                    FontWeight.ExtraBold
                            )

                            Spacer(
                                Modifier.height(4.dp)
                            )

                            Text(
                                "${reportLabel()}  •  ${dateLabel()}",
                                fontSize = 13.sp,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                        }

                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {

                            OutlinedButton(

                                onClick = {

                                    pendingExportCsv =
                                        buildCsv()

                                    val safeType =
                                        reportType
                                            .lowercase()
                                            .replace(
                                                "/",
                                                "-"
                                            )

                                    exportCsvLauncher.launch(
                                        "gym_report_" +
                                                "${safeType}_" +
                                                dateLabel()
                                                    .replace(
                                                        " ",
                                                        "_"
                                                    ) +
                                                ".csv"
                                    )
                                },

                                shape =
                                    RoundedCornerShape(
                                        14.dp
                                    )
                            ) {

                                Text(
                                    "EXPORT CSV"
                                )
                            }

                            OutlinedButton(

                                onClick = {
                                    showFilters =
                                        !showFilters
                                },

                                shape =
                                    RoundedCornerShape(
                                        14.dp
                                    )
                            ) {

                                Text(
                                    if (showFilters)
                                        "▲ FILTER"
                                    else
                                        "▼ FILTER"
                                )
                            }
                        }
                    }

                    Spacer(
                        Modifier.height(18.dp)
                    )

                    Surface(
                        modifier =
                            Modifier.fillMaxWidth(),

                        shape =
                            RoundedCornerShape(16.dp),

                        tonalElevation = 2.dp
                    ) {

                        Row(
                            Modifier.padding(16.dp),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Text(
                                "REPORTING PERIOD",
                                fontSize = 10.sp,
                                fontWeight =
                                    FontWeight.Bold,

                                modifier =
                                    Modifier.weight(1f)
                            )

                            Text(
                                dateLabel(),
                                fontSize = 14.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ====================================================
            // FILTERS
            // ====================================================

            AnimatedVisibility(
                visible = showFilters,

                enter =
                    fadeIn(
                        tween(180)
                    ) +
                            scaleIn(
                                initialScale = 0.97f,
                                animationSpec =
                                    tween(180)
                            ),

                exit =
                    fadeOut(
                        tween(120)
                    ) +
                            scaleOut(
                                targetScale = 0.97f,
                                animationSpec =
                                    tween(120)
                            )
            ) {

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(20.dp)
                ) {

                    Column(
                        Modifier.padding(16.dp),

                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        Text(
                            "REPORT FILTERS",
                            fontSize = 11.sp,
                            fontWeight =
                                FontWeight.Bold
                        )

                        // =================================================
                        // DATE FILTER
                        // =================================================

                        LazyRow(
                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {

                            item {

                                FilterChip(
                                    period == "TODAY",
                                    {
                                        period =
                                            "TODAY"
                                    },

                                    label = {
                                        Text("Today")
                                    }
                                )
                            }

                            item {

                                FilterChip(
                                    period ==
                                            "YESTERDAY",

                                    {
                                        period =
                                            "YESTERDAY"
                                    },

                                    label = {
                                        Text("Yesterday")
                                    }
                                )
                            }

                            item {

                                FilterChip(
                                    period ==
                                            "THIS_WEEK",

                                    {
                                        period =
                                            "THIS_WEEK"
                                    },

                                    label = {
                                        Text("This Week")
                                    }
                                )
                            }

                            item {

                                FilterChip(
                                    period ==
                                            "THIS_MONTH",

                                    {
                                        period =
                                            "THIS_MONTH"
                                    },

                                    label = {
                                        Text("This Month")
                                    }
                                )
                            }

                            item {

                                FilterChip(

                                    period ==
                                            "CUSTOM",

                                    {
                                        openCustomDatePicker()
                                    },

                                    label = {

                                        Text(
                                            if (
                                                customDate
                                                    .isBlank()
                                            )
                                                "Custom Date 📅"
                                            else
                                                customDate
                                        )
                                    }
                                )
                            }
                        }

                        // =================================================
                        // REPORT TYPE
                        // =================================================

                        LazyRow(
                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {

                            val types =
                                listOf(

                                    "SUMMARY" to
                                            "Summary",

                                    "MEMBERSHIP" to
                                            "Membership",

                                    "PAYMENTS" to
                                            "Payments",

                                    "ATTENDANCE" to
                                            "Attendance",

                                    "EXPENSE" to
                                            "Expense / Profit"
                                )

                            items(types) { item ->

                                FilterChip(

                                    selected =
                                        reportType ==
                                                item.first,

                                    onClick = {
                                        reportType =
                                            item.first
                                    },

                                    label = {
                                        Text(
                                            item.second
                                        )
                                    }
                                )
                            }
                        }

                        // =================================================
                        // ATTENDANCE MEMBER SEARCH
                        // =================================================

                        if (
                            reportType ==
                            "ATTENDANCE" ||
                            reportType ==
                            "SUMMARY"
                        ) {

                            OutlinedTextField(

                                value =
                                    attendanceMemberSearch,

                                onValueChange = {
                                    attendanceMemberSearch =
                                        it
                                },

                                modifier =
                                    Modifier.fillMaxWidth(),

                                singleLine = true,

                                label = {
                                    Text(
                                        "Search Member Name / ID"
                                    )
                                },

                                placeholder = {
                                    Text(
                                        "Example: Arun or AC001"
                                    )
                                },

                                trailingIcon = {

                                    if (
                                        attendanceMemberSearch
                                            .isNotBlank()
                                    ) {

                                        TextButton(
                                            onClick = {
                                                attendanceMemberSearch =
                                                    ""
                                            }
                                        ) {
                                            Text("CLEAR")
                                        }
                                    }
                                }
                            )
                        }

                        // =================================================
                        // CLEAR ALL
                        // =================================================

                        OutlinedButton(

                            onClick = {

                                period =
                                    "TODAY"

                                reportType =
                                    "SUMMARY"

                                customDate =
                                    ""

                                attendanceMemberSearch =
                                    ""
                            }
                        ) {

                            Text(
                                "CLEAR FILTERS"
                            )
                        }
                    }
                }
            }

            // ====================================================
            // AT A GLANCE
            // ====================================================

            Text(
                "AT A GLANCE",
                fontSize = 11.sp,
                fontWeight =
                    FontWeight.Bold,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Row(
                Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                CreativeReportCard(
                    "COLLECTION",
                    "₹%.2f".format(collection),
                    dateLabel(),
                    Modifier.weight(1f)
                )

                CreativeReportCard(
                    "EXPENSES",
                    "₹%.2f".format(expenses),
                    dateLabel(),
                    Modifier.weight(1f)
                )

                CreativeReportCard(
                    "NET INCOME",
                    "₹%.2f".format(netIncome),

                    if (netIncome >= 0)
                        "Positive"
                    else
                        "Needs attention",

                    Modifier.weight(1f)
                )
            }

            Row(
                Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                CreativeReportSmallCard(
                    "CHECK-INS",
                    checkIns.toString(),
                    "Attendance",
                    Modifier.weight(1f)
                )

                CreativeReportSmallCard(
                    "NEW MEMBERS",
                    newMembers.toString(),
                    "Registrations",
                    Modifier.weight(1f)
                )

                CreativeReportSmallCard(
                    "RENEWALS",
                    renewals.toString(),
                    "Renewal payments",
                    Modifier.weight(1f)
                )

                CreativeReportSmallCard(
                    "PENDING",
                    "₹%.2f".format(
                        pendingBalance
                    ),
                    "Outstanding",
                    Modifier.weight(1f)
                )
            }

            Row(
                Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                CreativeReportSmallCard(
                    "ACTIVE",
                    activeMembers.toString(),
                    "Access enabled",
                    Modifier.weight(1f)
                )

                CreativeReportSmallCard(
                    "TRIAL",
                    trialMembers.toString(),
                    "Latest membership",
                    Modifier.weight(1f)
                )

                CreativeReportSmallCard(
                    "EXPIRING",
                    expiringSoon.toString(),
                    "Next 10 days",
                    Modifier.weight(1f)
                )
            }

            // ====================================================
            // ATTENDANCE TOTAL HOURS
            // ====================================================

            if (
                reportType == "ATTENDANCE" ||
                reportType == "SUMMARY"
            ) {

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(18.dp)
                ) {

                    Column(
                        Modifier.padding(18.dp),

                        verticalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        Text(
                            "ATTENDANCE",
                            fontSize = 11.sp,
                            fontWeight =
                                FontWeight.Bold,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .primary
                        )

                        Text(
                            formatHours(
                                totalLoginHours
                            ),

                            fontSize = 28.sp,

                            fontWeight =
                                FontWeight.ExtraBold
                        )

                        Text(
                            "Total login hours • ${dateLabel()}" +
                                    if (
                                        attendanceMemberSearch
                                            .isNotBlank()
                                    )
                                        " • $attendanceMemberSearch"
                                    else
                                        "",

                            fontSize = 13.sp,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }
            }

            // ====================================================
            // REPORT CONTENT
            // ====================================================

            when (reportType) {

                // =================================================
                // SUMMARY
                // =================================================

                "SUMMARY" -> {

                    CreativeReportSection(
                        "Recent Payments",
                        "${payments.size} records"
                    ) {

                        if (payments.isEmpty()) {

                            CreativeReportEmpty(
                                "No payments found for " +
                                        "${dateLabel().lowercase()}."
                            )

                        } else {

                            CreativeReportPayments(
                                payments
                            )
                        }
                    }

                    CreativeReportSection(
                        "Membership Activity",
                        "${memberships.size} records"
                    ) {

                        if (memberships.isEmpty()) {

                            CreativeReportEmpty(
                                "No membership activity found."
                            )

                        } else {

                            CreativeReportMemberships(
                                memberships
                            )
                        }
                    }

                    // =================================================
                    // DAILY ATTENDANCE
                    // =================================================

                    CreativeReportSection(
                        "Daily Member Attendance",
                        "${dailyAttendance.size} member-days"
                    ) {

                        if (dailyAttendance.isEmpty()) {

                            CreativeReportEmpty(
                                "No attendance records found."
                            )

                        } else {

                            Card(
                                modifier =
                                    Modifier.fillMaxWidth(),

                                shape =
                                    RoundedCornerShape(
                                        18.dp
                                    )
                            ) {

                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(
                                            rememberScrollState()
                                        )
                                        .padding(14.dp)
                                ) {

                                    Row(
                                        modifier =
                                            Modifier.width(
                                                900.dp
                                            )
                                    ) {

                                        Text(
                                            "DATE",
                                            Modifier.width(
                                                110.dp
                                            ),
                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        Text(
                                            "MEMBER ID",
                                            Modifier.width(
                                                110.dp
                                            ),
                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        Text(
                                            "MEMBER",
                                            Modifier.width(
                                                180.dp
                                            ),
                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        Text(
                                            "FIRST LOGIN",
                                            Modifier.width(
                                                150.dp
                                            ),
                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        Text(
                                            "LAST LOGOUT",
                                            Modifier.width(
                                                150.dp
                                            ),
                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        Text(
                                            "TOTAL HOURS",
                                            Modifier.width(
                                                150.dp
                                            ),
                                            fontWeight =
                                                FontWeight.Bold
                                        )
                                    }

                                    HorizontalDivider(
                                        Modifier.padding(
                                            vertical = 8.dp
                                        )
                                    )

                                    dailyAttendance.forEach { r ->

                                        Row(
                                            modifier =
                                                Modifier
                                                    .width(
                                                        900.dp
                                                    )
                                                    .padding(
                                                        vertical = 8.dp
                                                    )
                                        ) {

                                            Text(
                                                r.getOrNull(
                                                    0
                                                ) ?: "",
                                                Modifier.width(
                                                    110.dp
                                                )
                                            )

                                            Text(
                                                r.getOrNull(
                                                    1
                                                ) ?: "",
                                                Modifier.width(
                                                    110.dp
                                                )
                                            )

                                            Text(
                                                r.getOrNull(
                                                    2
                                                ) ?: "",
                                                Modifier.width(
                                                    180.dp
                                                )
                                            )

                                            Text(
                                                r.getOrNull(
                                                    3
                                                ) ?: "",
                                                Modifier.width(
                                                    150.dp
                                                )
                                            )

                                            Text(
                                                r.getOrNull(
                                                    4
                                                ) ?: "",
                                                Modifier.width(
                                                    150.dp
                                                )
                                            )

                                            Text(
                                                formatHours(
                                                    r.getOrNull(
                                                        5
                                                    )
                                                        ?.toDoubleOrNull()
                                                        ?: 0.0
                                                ),
                                                Modifier.width(
                                                    150.dp
                                                ),
                                                fontWeight =
                                                    FontWeight.Bold
                                            )
                                        }

                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }

                    // =================================================
                    // ATTENDANCE DETAIL
                    // =================================================

                    CreativeReportSection(
                        "Attendance Activity",
                        "${attendance.size} records"
                    ) {

                        if (attendance.isEmpty()) {

                            CreativeReportEmpty(
                                "No attendance records found."
                            )

                        } else {

                            Card(
                                modifier =
                                    Modifier.fillMaxWidth(),

                                shape =
                                    RoundedCornerShape(
                                        18.dp
                                    )
                            ) {

                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(
                                            rememberScrollState()
                                        )
                                        .padding(14.dp)
                                ) {

                                    Row(
                                        Modifier.width(
                                            1120.dp
                                        )
                                    ) {

                                        Text(
                                            "MEMBER ID",
                                            Modifier.width(
                                                110.dp
                                            ),
                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        Text(
                                            "MEMBER",
                                            Modifier.width(
                                                180.dp
                                            ),
                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        Text(
                                            "PLAN",
                                            Modifier.width(
                                                140.dp
                                            ),
                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        Text(
                                            "STATUS",
                                            Modifier.width(
                                                120.dp
                                            ),
                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        Text(
                                            "CHECK IN",
                                            Modifier.width(
                                                170.dp
                                            ),
                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        Text(
                                            "CHECK OUT",
                                            Modifier.width(
                                                170.dp
                                            ),
                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        Text(
                                            "LOGIN HOURS",
                                            Modifier.width(
                                                130.dp
                                            ),
                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        Text(
                                            "SOURCE",
                                            Modifier.width(
                                                120.dp
                                            ),
                                            fontWeight =
                                                FontWeight.Bold
                                        )
                                    }

                                    HorizontalDivider(
                                        Modifier.padding(
                                            vertical = 8.dp
                                        )
                                    )

                                    attendance.forEach { r ->

                                        Row(
                                            Modifier
                                                .width(
                                                    1000.dp
                                                )
                                                .padding(
                                                    vertical = 8.dp
                                                )
                                        ) {

                                            Text(
                                                r.getOrNull(
                                                    0
                                                ) ?: "",
                                                Modifier.width(
                                                    110.dp
                                                )
                                            )

                                            Text(
                                                r.getOrNull(
                                                    1
                                                ) ?: "",
                                                Modifier.width(
                                                    180.dp
                                                )
                                            )

                                            Text(
                                                r.getOrNull(
                                                    2
                                                ) ?: "",
                                                Modifier.width(
                                                    140.dp
                                                )
                                            )

                                            Text(
                                                r.getOrNull(
                                                    3
                                                ) ?: "",
                                                Modifier.width(
                                                    120.dp
                                                )
                                            )

                                            Text(
                                                r.getOrNull(
                                                    4
                                                ) ?: "",
                                                Modifier.width(
                                                    170.dp
                                                )
                                            )

                                            Text(
                                                r.getOrNull(
                                                    5
                                                ) ?: "",
                                                Modifier.width(
                                                    170.dp
                                                )
                                            )

                                            Text(
                                                formatHours(
                                                    r.getOrNull(
                                                        6
                                                    )
                                                        ?.toDoubleOrNull()
                                                        ?: 0.0
                                                ),
                                                Modifier.width(
                                                    130.dp
                                                ),
                                                fontWeight =
                                                    FontWeight.Bold
                                            )

                                            Text(
                                                r.getOrNull(
                                                    7
                                                ) ?: "",
                                                Modifier.width(
                                                    120.dp
                                                )
                                            )
                                        }

                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }
                }

                // =================================================
                // MEMBERSHIP
                // =================================================

                "MEMBERSHIP" -> {

                    CreativeReportSection(
                        "Membership Report",
                        "${memberships.size} records"
                    ) {

                        if (memberships.isEmpty()) {

                            CreativeReportEmpty(
                                "No membership records found."
                            )

                        } else {

                            CreativeReportMemberships(
                                memberships
                            )
                        }
                    }
                }

                // =================================================
                // PAYMENTS
                // =================================================

                "PAYMENTS" -> {

                    CreativeReportSection(
                        "Payment / Revenue Report",
                        "${payments.size} records"
                    ) {

                        if (payments.isEmpty()) {

                            CreativeReportEmpty(
                                "No payments found."
                            )

                        } else {

                            CreativeReportPayments(
                                payments
                            )
                        }
                    }
                }

                // =================================================
                // ATTENDANCE
                // =================================================

                "ATTENDANCE" -> {

                    // Daily summary

                    CreativeReportSection(
                        "Daily Member Attendance",
                        "${dailyAttendance.size} member-days"
                    ) {

                        if (dailyAttendance.isEmpty()) {

                            CreativeReportEmpty(
                                "No attendance records found."
                            )

                        } else {

                            Card(
                                modifier =
                                    Modifier.fillMaxWidth(),

                                shape =
                                    RoundedCornerShape(
                                        18.dp
                                    )
                            ) {

                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(
                                            rememberScrollState()
                                        )
                                        .padding(14.dp)
                                ) {

                                    Row(
                                        Modifier.width(
                                            900.dp
                                        )
                                    ) {

                                        Text(
                                            "DATE",
                                            Modifier.width(
                                                110.dp
                                            ),
                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        Text(
                                            "MEMBER ID",
                                            Modifier.width(
                                                110.dp
                                            ),
                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        Text(
                                            "MEMBER",
                                            Modifier.width(
                                                180.dp
                                            ),
                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        Text(
                                            "FIRST LOGIN",
                                            Modifier.width(
                                                150.dp
                                            ),
                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        Text(
                                            "LAST LOGOUT",
                                            Modifier.width(
                                                150.dp
                                            ),
                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        Text(
                                            "TOTAL HOURS",
                                            Modifier.width(
                                                150.dp
                                            ),
                                            fontWeight =
                                                FontWeight.Bold
                                        )
                                    }

                                    HorizontalDivider(
                                        Modifier.padding(
                                            vertical = 8.dp
                                        )
                                    )

                                    dailyAttendance.forEach { r ->

                                        Row(
                                            Modifier
                                                .width(
                                                    900.dp
                                                )
                                                .padding(
                                                    vertical = 8.dp
                                                )
                                        ) {

                                            Text(
                                                r.getOrNull(
                                                    0
                                                ) ?: "",
                                                Modifier.width(
                                                    110.dp
                                                )
                                            )

                                            Text(
                                                r.getOrNull(
                                                    1
                                                ) ?: "",
                                                Modifier.width(
                                                    110.dp
                                                )
                                            )

                                            Text(
                                                r.getOrNull(
                                                    2
                                                ) ?: "",
                                                Modifier.width(
                                                    180.dp
                                                )
                                            )

                                            Text(
                                                r.getOrNull(
                                                    3
                                                ) ?: "",
                                                Modifier.width(
                                                    150.dp
                                                )
                                            )

                                            Text(
                                                r.getOrNull(
                                                    4
                                                ) ?: "",
                                                Modifier.width(
                                                    150.dp
                                                )
                                            )

                                            Text(
                                                formatHours(
                                                    r.getOrNull(
                                                        5
                                                    )
                                                        ?.toDoubleOrNull()
                                                        ?: 0.0
                                                ),
                                                Modifier.width(
                                                    150.dp
                                                ),
                                                fontWeight =
                                                    FontWeight.Bold
                                            )
                                        }

                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }

                    // Detailed attendance

                    CreativeReportSection(
                        "Attendance Detail",
                        "${attendance.size} records"
                    ) {

                        if (attendance.isEmpty()) {

                            CreativeReportEmpty(
                                "No attendance records found."
                            )

                        } else {

                            Card(
                                modifier =
                                    Modifier.fillMaxWidth(),

                                shape =
                                    RoundedCornerShape(
                                        18.dp
                                    )
                            ) {

                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(
                                            rememberScrollState()
                                        )
                                        .padding(14.dp)
                                ) {

                                    Row(
                                        Modifier.width(
                                            1000.dp
                                        )
                                    ) {

                                        Text(
                                            "MEMBER ID",
                                            Modifier.width(
                                                110.dp
                                            ),
                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        Text(
                                            "MEMBER",
                                            Modifier.width(
                                                180.dp
                                            ),
                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        Text(
                                            "PLAN",
                                            Modifier.width(
                                                140.dp
                                            ),
                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        Text(
                                            "STATUS",
                                            Modifier.width(
                                                120.dp
                                            ),
                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        Text(
                                            "CHECK IN",
                                            Modifier.width(
                                                170.dp
                                            ),
                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        Text(
                                            "CHECK OUT",
                                            Modifier.width(
                                                170.dp
                                            ),
                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        Text(
                                            "LOGIN HOURS",
                                            Modifier.width(
                                                130.dp
                                            ),
                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        Text(
                                            "SOURCE",
                                            Modifier.width(
                                                120.dp
                                            ),
                                            fontWeight =
                                                FontWeight.Bold
                                        )
                                    }

                                    HorizontalDivider(
                                        Modifier.padding(
                                            vertical = 8.dp
                                        )
                                    )

                                    attendance.forEach { r ->

                                        Row(
                                            Modifier
                                                .width(
                                                    1000.dp
                                                )
                                                .padding(
                                                    vertical = 8.dp
                                                )
                                        ) {

                                            Text(
                                                r.getOrNull(
                                                    0
                                                ) ?: "",
                                                Modifier.width(
                                                    110.dp
                                                )
                                            )

                                            Text(
                                                r.getOrNull(
                                                    1
                                                ) ?: "",
                                                Modifier.width(
                                                    180.dp
                                                )
                                            )

                                            Text(
                                                r.getOrNull(
                                                    2
                                                ) ?: "",
                                                Modifier.width(
                                                    140.dp
                                                )
                                            )

                                            Text(
                                                r.getOrNull(
                                                    3
                                                ) ?: "",
                                                Modifier.width(
                                                    120.dp
                                                )
                                            )

                                            Text(
                                                r.getOrNull(
                                                    4
                                                ) ?: "",
                                                Modifier.width(
                                                    170.dp
                                                )
                                            )

                                            Text(
                                                r.getOrNull(
                                                    5
                                                ) ?: "",
                                                Modifier.width(
                                                    170.dp
                                                )
                                            )

                                            Text(
                                                formatHours(
                                                    r.getOrNull(
                                                        6
                                                    )
                                                        ?.toDoubleOrNull()
                                                        ?: 0.0
                                                ),
                                                Modifier.width(
                                                    130.dp
                                                ),
                                                fontWeight =
                                                    FontWeight.Bold
                                            )

                                            Text(
                                                r.getOrNull(
                                                    7
                                                ) ?: "",
                                                Modifier.width(
                                                    120.dp
                                                )
                                            )
                                        }

                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }
                }

                // =================================================
                // EXPENSE
                // =================================================

                "EXPENSE" -> {

                    CreativeReportSection(
                        "Expense / Profit",
                        dateLabel()
                    ) {

                        Card(
                            Modifier.fillMaxWidth(),
                            shape =
                                RoundedCornerShape(
                                    18.dp
                                )
                        ) {

                            Column(
                                Modifier.padding(
                                    18.dp
                                ),

                                verticalArrangement =
                                    Arrangement.spacedBy(
                                        12.dp
                                    )
                            ) {

                                CreativeReportLine(
                                    "Collection",
                                    "₹%.2f".format(
                                        collection
                                    )
                                )

                                CreativeReportLine(
                                    "Expenses",
                                    "₹%.2f".format(
                                        expenses
                                    )
                                )

                                HorizontalDivider()

                                CreativeReportLine(
                                    "Net Income",
                                    "₹%.2f".format(
                                        netIncome
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(
                Modifier.height(8.dp)
            )
        }
    }
}

@Composable
private fun CreativeReportCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(17.dp)) {
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CreativeReportSmallCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(13.dp)) {
            Text(title, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(value, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                subtitle,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CreativeReportSection(
    title: String,
    detail: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(detail, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        content()
    }
}

@Composable
private fun CreativeReportEmpty(text: String) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text,
            Modifier.padding(18.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CreativeReportLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), fontWeight = FontWeight.Medium)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CreativeReportPayments(rows: List<Array<String>>) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            Column(Modifier.width(820.dp)) {
                Row(Modifier.padding(13.dp)) {
                    CreativeReportHeader("MEMBER", 200.dp)
                    CreativeReportHeader("PLAN", 145.dp)
                    CreativeReportHeader("AMOUNT", 100.dp)
                    CreativeReportHeader("DATE", 145.dp)
                    CreativeReportHeader("METHOD", 110.dp)
                    CreativeReportHeader("TYPE", 120.dp)
                }
                HorizontalDivider()
                rows.take(30).forEach { r ->
                    Row(
                        Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        CreativeReportBody("${r.getOrNull(0)} • ${r.getOrNull(1)}", 200.dp)
                        CreativeReportBody(r.getOrNull(2) ?: "-", 145.dp)
                        CreativeReportBody("₹%.2f".format(r.getOrNull(3)?.toDoubleOrNull() ?: 0.0), 100.dp)
                        CreativeReportBody(r.getOrNull(4) ?: "-", 145.dp)
                        CreativeReportBody(r.getOrNull(5) ?: "-", 110.dp)
                        CreativeReportBody(
                            when {
                                (r.getOrNull(6) ?: "").contains("renew", true) -> "Renewal"
                                (r.getOrNull(6) ?: "").contains("balance", true) -> "Balance"
                                else -> "Membership"
                            },
                            120.dp
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun CreativeReportMemberships(rows: List<Array<String>>) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            Column(Modifier.width(950.dp)) {
                Row(Modifier.padding(13.dp)) {
                    CreativeReportHeader("MEMBER", 180.dp)
                    CreativeReportHeader("PLAN", 140.dp)
                    CreativeReportHeader("START", 120.dp)
                    CreativeReportHeader("EXPIRY", 120.dp)
                    CreativeReportHeader("STATUS", 110.dp)
                    CreativeReportHeader("TOTAL", 100.dp)
                    CreativeReportHeader("PAID", 100.dp)
                    CreativeReportHeader("BALANCE", 110.dp)
                }
                HorizontalDivider()
                rows.take(30).forEach { r ->
                    Row(Modifier.padding(horizontal = 13.dp, vertical = 11.dp)) {
                        CreativeReportBody("${r.getOrNull(0)} • ${r.getOrNull(1)}", 180.dp)
                        CreativeReportBody(r.getOrNull(2) ?: "-", 140.dp)
                        CreativeReportBody(r.getOrNull(3) ?: "-", 120.dp)
                        CreativeReportBody(r.getOrNull(4) ?: "-", 120.dp)
                        CreativeReportBody(r.getOrNull(5) ?: "-", 110.dp)
                        CreativeReportBody("₹%.2f".format(r.getOrNull(6)?.toDoubleOrNull() ?: 0.0), 100.dp)
                        CreativeReportBody("₹%.2f".format(r.getOrNull(7)?.toDoubleOrNull() ?: 0.0), 100.dp)
                        CreativeReportBody("₹%.2f".format(r.getOrNull(8)?.toDoubleOrNull() ?: 0.0), 110.dp)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun CreativeReportAttendance(rows: List<Array<String>>) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            Column(Modifier.width(850.dp)) {
                Row(Modifier.padding(13.dp)) {
                    CreativeReportHeader("MEMBER", 190.dp)
                    CreativeReportHeader("PLAN", 145.dp)
                    CreativeReportHeader("STATUS", 110.dp)
                    CreativeReportHeader("CHECK IN", 145.dp)
                    CreativeReportHeader("CHECK OUT", 145.dp)
                    CreativeReportHeader("SOURCE", 115.dp)
                }
                HorizontalDivider()
                rows.take(30).forEach { r ->
                    val status = r.getOrNull(3) ?: "-"
                    val rowBackground = when {
                        status.equals("Trial", true) -> MaterialTheme.colorScheme.tertiaryContainer
                        status.equals("Pending", true) -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    }
                    Surface(color = rowBackground) {
                        Row(Modifier.padding(horizontal = 13.dp, vertical = 11.dp)) {
                            CreativeReportBody("${r.getOrNull(0)} • ${r.getOrNull(1)}", 190.dp)
                            CreativeReportBody(r.getOrNull(2) ?: "-", 145.dp)
                            CreativeReportBody(status, 110.dp)
                            CreativeReportBody(r.getOrNull(4) ?: "-", 145.dp)
                            CreativeReportBody(r.getOrNull(5) ?: "-", 145.dp)
                            CreativeReportBody(r.getOrNull(6) ?: "-", 115.dp)
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun CreativeReportHeader(text: String, width: Dp) {
    Text(
        text,
        Modifier.width(width),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun CreativeReportBody(text: String, width: Dp) {
    Text(
        text,
        Modifier.width(width),
        fontSize = 12.sp
    )
}


/* =========================================================
   AUTOMATIC BACKUP
   - Daily local backup at 11:00 PM (inexact Android alarm)
   - Keeps the latest 7 backup files
   - Includes SQLite database + member profile photos
   ========================================================= */

object AutoBackupManager {
    private const val PREFS = "gym_auto_backup"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_LAST_BACKUP = "last_backup"
    private const val ACTION_DAILY_BACKUP = "com.gymmanagement.gymmanagement.ACTION_DAILY_BACKUP"
    private const val BACKUP_FOLDER = "GymManagementBackups"
    private const val MAX_BACKUPS = 7

    fun ensureScheduled(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_ENABLED)) {
            prefs.edit().putBoolean(KEY_ENABLED, true).apply()
        }
        if (prefs.getBoolean(KEY_ENABLED, true)) {
            schedule(context)
        }
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) schedule(context) else cancel(context)
    }

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, true)

    fun lastBackup(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_BACKUP, "Never") ?: "Never"

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AutoBackupReceiver::class.java).apply {
            action = ACTION_DAILY_BACKUP
        }
        return PendingIntent.getBroadcast(
            context,
            4107,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun schedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent(context)
        )
    }

    private fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context))
    }

    fun createBackup(context: Context): Result<String> = runCatching {
        val root = getBackupDirectory(context)
        if (!root.exists()) root.mkdirs()

        val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val zipFile = File(root, "GymBackup_$stamp.zip")

        val dbHelper = DatabaseHelper(context)
        val dbFile = context.getDatabasePath("gym_management.db")

        // Flush WAL changes before copying the SQLite database.
        dbHelper.writableDatabase.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }

        ZipOutputStream(zipFile.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("gym_management.db"))
            FileInputStream(dbFile).use { input -> input.copyTo(zip) }
            zip.closeEntry()

            val cursor = dbHelper.readableDatabase.rawQuery(
                "SELECT photo_path FROM members WHERE photo_path IS NOT NULL AND photo_path <> ''",
                null
            )
            cursor.use {
                val seen = mutableSetOf<String>()
                while (it.moveToNext()) {
                    val path = it.getString(0) ?: continue
                    val photo = File(path)
                    if (!photo.exists() || !photo.isFile) continue
                    val name = photo.name
                    if (!seen.add(name)) continue
                    zip.putNextEntry(ZipEntry("member_photos/$name"))
                    FileInputStream(photo).use { input -> input.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }

        dbHelper.close()

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(
                KEY_LAST_BACKUP,
                java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
                    .format(java.util.Date())
            )
            .apply()

        cleanupOldBackups(root)
        zipFile.absolutePath
    }

    private fun getBackupDirectory(context: Context): File {
        // Android 10+ allows writing app-created files to Downloads through MediaStore,
        // but using app-specific Documents is more reliable for automatic background work.
        // The backup remains accessible through Android's file manager and can be exported later.
        val base = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        return File(base, BACKUP_FOLDER)
    }

    private fun cleanupOldBackups(root: File) {
        val backups = root.listFiles { file ->
            file.isFile && file.name.startsWith("GymBackup_") && file.name.endsWith(".zip")
        }?.sortedByDescending { it.lastModified() }.orEmpty()

        backups.drop(MAX_BACKUPS).forEach { it.delete() }
    }
}

class AutoBackupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            AutoBackupManager.ensureScheduled(context)
            return
        }
        if (intent?.action == "com.gymmanagement.gymmanagement.ACTION_DAILY_BACKUP") {
            if (!AutoBackupManager.isEnabled(context)) return
            val pending = goAsync()
            Thread {
                try {
                    AutoBackupManager.createBackup(context)
                } finally {
                    pending.finish()
                }
            }.start()
        }
    }
}

@Composable
fun BackupRestoreScreen(db: DatabaseHelper) {
    val context = LocalContext.current
    var enabled by rememberSaveable { mutableStateOf(AutoBackupManager.isEnabled(context)) }
    var lastBackup by rememberSaveable { mutableStateOf(AutoBackupManager.lastBackup(context)) }
    var message by rememberSaveable { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var selectedBackupUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedBackupUri = uri
            showRestoreConfirm = true
        }
    }

    LaunchedEffect(Unit) {
        AutoBackupManager.ensureScheduled(context)
    }

    fun restartApp() {
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        if (launchIntent != null) {
            context.startActivity(launchIntent)
        }
        if (context is Activity) {
            context.finishAffinity()
        }
    }

    fun restoreBackup(uri: android.net.Uri) {
        working = true
        message = "Restoring backup..."

        Thread {
            var success = false
            var errorText = "Unknown error"
            try {
                val tempZip = File(context.cacheDir, "restore_backup_${System.currentTimeMillis()}.zip")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempZip).use { output -> input.copyTo(output) }
                } ?: throw IllegalStateException("Unable to read the selected backup file.")

                var databaseEntryFound = false
                val tempDb = File(context.cacheDir, "restored_gym_management.db")
                val photoEntries = mutableListOf<Pair<String, ByteArray>>()

                java.util.zip.ZipInputStream(tempZip.inputStream().buffered()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory && entry.name == "gym_management.db") {
                            FileOutputStream(tempDb).use { output -> zip.copyTo(output) }
                            databaseEntryFound = true
                        } else if (!entry.isDirectory && entry.name.startsWith("member_photos/")) {
                            val fileName = File(entry.name).name
                            if (fileName.isNotBlank()) {
                                photoEntries.add(fileName to zip.readBytes())
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }

                if (!databaseEntryFound || !tempDb.exists() || tempDb.length() == 0L) {
                    throw IllegalStateException("Invalid backup: gym_management.db was not found.")
                }

                // Close the app's active database before replacing the SQLite file.
                db.close()

                val dbFile = context.getDatabasePath("gym_management.db")
                dbFile.parentFile?.mkdirs()
                File(dbFile.path + "-wal").delete()
                File(dbFile.path + "-shm").delete()

                tempDb.copyTo(dbFile, overwrite = true)

                // Restore member photos to the same app-private directory used during registration.
                photoEntries.forEach { (fileName, bytes) ->
                    File(context.filesDir, fileName).writeBytes(bytes)
                }

                tempDb.delete()
                tempZip.delete()
                success = true
            } catch (e: Exception) {
                errorText = e.message ?: e.javaClass.simpleName
            }

            android.os.Handler(context.mainLooper).post {
                working = false
                if (success) {
                    message = "Restore successful. Restarting app..."
                    android.os.Handler(context.mainLooper).postDelayed(
                        { restartApp() },
                        1200L
                    )
                } else {
                    message = "Restore failed: $errorText"
                }
            }
        }.start()
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = {
                if (!working) {
                    showRestoreConfirm = false
                    selectedBackupUri = null
                }
            },
            title = { Text("Restore Backup?") },
            text = {
                Text(
                    "This will replace the current gym database with the selected backup. " +
                            "Member photos included in the backup will also be restored. Continue?"
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !working && selectedBackupUri != null,
                    onClick = {
                        val uri = selectedBackupUri ?: return@TextButton
                        showRestoreConfirm = false
                        restoreBackup(uri)
                    }
                ) {
                    Text("RESTORE")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !working,
                    onClick = {
                        showRestoreConfirm = false
                        selectedBackupUri = null
                    }
                ) {
                    Text("CANCEL")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("BACKUP & RESTORE", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "Your gym data is automatically backed up every day.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Automatic Backup", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (enabled) "Daily at 11:00 PM" else "Automatic backup is OFF",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            enabled = it
                            AutoBackupManager.setEnabled(context, it)
                            message = if (it) "Automatic backup enabled." else "Automatic backup disabled."
                        }
                    )
                }

                HorizontalDivider()

                Row(Modifier.fillMaxWidth()) {
                    Text("Last Backup", Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    Text(lastBackup, fontWeight = FontWeight.Bold)
                }

                Row(Modifier.fillMaxWidth()) {
                    Text("Backup Retention", Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    Text("Last 7 backups", fontWeight = FontWeight.Bold)
                }

                Row(Modifier.fillMaxWidth()) {
                    Text("Contents", Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    Text("Database + Photos", fontWeight = FontWeight.Bold)
                }
            }
        }

        Button(
            onClick = {
                if (working) return@Button
                working = true
                message = "Creating backup..."
                Thread {
                    val result = AutoBackupManager.createBackup(context)
                    android.os.Handler(context.mainLooper).post {
                        working = false
                        result.onSuccess {
                            lastBackup = AutoBackupManager.lastBackup(context)
                            message = "Backup created successfully."
                        }.onFailure {
                            message = "Backup failed: ${it.message ?: "Unknown error"}"
                        }
                    }
                }.start()
            },
            enabled = !working,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(if (working) "CREATING BACKUP..." else "BACKUP NOW", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = {
                if (!working) {
                    restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                }
            },
            enabled = !working,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("RESTORE BACKUP", fontWeight = FontWeight.Bold)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Backup Location", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(
                    "App Documents / GymManagementBackups",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (message.isNotBlank()) {
            Text(message, fontWeight = FontWeight.Medium)
        }
    }
}
class AutoClockOutWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = DatabaseHelper(applicationContext)

            // Automatically clock out members who have exceeded 12 hours
            db.autoClockOutAfter6Hours()

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
fun scheduleAutoClockOut(context: Context) {

    val request = PeriodicWorkRequestBuilder<AutoClockOutWorker>(
        15,
        TimeUnit.MINUTES
    ).build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "AUTO_CLOCK_OUT",
        ExistingPeriodicWorkPolicy.KEEP,
        request
    )
}


@Composable
fun SwipeLeftMemberCard(
    onSwipedLeft: () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var cardWidth by remember { mutableIntStateOf(1) }
    val animatedOffset = remember { Animatable(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { cardWidth = it.width }
            .graphicsLayer {
                translationX = animatedOffset.value
                val progress = (-animatedOffset.value / cardWidth.toFloat())
                    .coerceIn(0f, 1f)
                rotationZ = -progress * 4f
                scaleX = 1f - progress * 0.10f
                scaleY = 1f - progress * 0.10f
                alpha = 1f - progress * 0.15f
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset = (dragOffset + dragAmount).coerceAtMost(0f)
                        scope.launch { animatedOffset.snapTo(dragOffset) }
                    },
                    onDragEnd = {
                        scope.launch {
                            val threshold = cardWidth * 0.25f
                            if (dragOffset <= -threshold) {
                                animatedOffset.animateTo(
                                    -cardWidth.toFloat(),
                                    animationSpec = tween(250)
                                )
                                onSwipedLeft()
                            } else {
                                animatedOffset.animateTo(0f, animationSpec = tween(250))
                                dragOffset = 0f
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            animatedOffset.animateTo(0f, animationSpec = tween(250))
                            dragOffset = 0f
                        }
                    }
                )
            }
    ) {
        content()
    }
}



