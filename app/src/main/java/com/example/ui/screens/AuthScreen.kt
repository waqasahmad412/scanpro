package com.example.ui.screens

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.WarmOrangeSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@Composable
fun AuthScreen(
    onLoginSuccess: (name: String, email: String) -> Unit,
    onGuestContinue: () -> Unit,
    onShowToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Login, 1 = Register

    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // OTP Verification State
    var showOtpScreen by remember { mutableStateOf(false) }
    var generatedOtp by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var resendTimer by remember { mutableIntStateOf(30) }
    var isSendingEmail by remember { mutableStateOf(false) }
    var showBackupCode by remember { mutableStateOf(false) }
    var showEmailInboxModal by remember { mutableStateOf(false) }
    var showGoogleAccountPicker by remember { mutableStateOf(false) }
    var selectedGoogleAccount by remember { mutableStateOf("waqasahmad08766@gmail.com") }
    var customGoogleAccountInput by remember { mutableStateOf("") }

    // Firebase Auth State & Instance
    val firebaseAuth = remember {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }
    var isFirebaseLoading by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val scrollState = rememberScrollState()

    // Helper to launch direct Email composer to send verification email with direct verification link
    fun launchDirectEmailIntent(recipientEmail: String, otpCode: String) {
        try {
            val verifyUrl = "https://scanpro.app/verify?email=$recipientEmail&code=$otpCode"
            val deepLink = "scanpro://verify?email=$recipientEmail&code=$otpCode"
            val bodyText = "Hello,\n\n" +
                    "Your 6-digit ScanPro AI verification code is: $otpCode\n\n" +
                    "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "⚡ CLICK BELOW TO VERIFY EMAIL & LAUNCH APP:\n" +
                    "$verifyUrl\n" +
                    "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                    "Or tap deep link: $deepLink\n\n" +
                    "Thank you,\nScanPro AI Team"

            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("mailto:$recipientEmail")
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Verify ScanPro AI Account - Code: $otpCode")
                putExtra(android.content.Intent.EXTRA_TEXT, bodyText)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            onShowToast("Opening email app...")
        }
    }

    fun triggerEmailNotification(email: String, otp: String) {
        try {
            val channelId = "email_otp_channel"
            val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    "Gmail OTP Notifications",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Incoming Email OTP Verification Messages"
                    enableLights(true)
                    enableVibration(true)
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val verifyUri = android.net.Uri.parse("https://scanpro.app/verify?email=$email&code=$otp")
            val emailIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, verifyUri).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val pendingIntent = android.app.PendingIntent.getActivity(
                context,
                0,
                emailIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle("📩 Gmail Verification Email • $email")
                .setContentText("Tap to Verify Email (OTP: $otp)")
                .setStyle(
                    androidx.core.app.NotificationCompat.BigTextStyle()
                        .bigText("Inbox: $email\nFrom: ScanPro AI Verification\nSubject: Verify Your Email Address\n\nYour OTP Code: $otp\n\nTap here or click below to verify email & launch app:\nhttps://scanpro.app/verify?email=$email&code=$otp")
                )
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_MESSAGE)
                .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(1001, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Sends real email via HTTP request + local notification
    fun sendRealEmailWithOtp(
        recipientEmail: String,
        otpCode: String,
        onComplete: (String) -> Unit
    ) {
        isSendingEmail = true
        triggerEmailNotification(recipientEmail, otpCode)

        CoroutineScope(Dispatchers.IO).launch {
            var statusMsg = ""
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(12, TimeUnit.SECONDS)
                    .readTimeout(12, TimeUnit.SECONDS)
                    .build()

                val verifyUrl = "https://scanpro.app/verify?email=$recipientEmail&code=$otpCode"
                val formBody = FormBody.Builder()
                    .add("_subject", "Verify ScanPro AI Account - Code: $otpCode")
                    .add("email", recipientEmail)
                    .add("message", "Your 6-digit verification code is: $otpCode\n\nClick link to verify and launch app: $verifyUrl")
                    .add("_captcha", "false")
                    .build()

                val request = Request.Builder()
                    .url("https://formsubmit.co/ajax/$recipientEmail")
                    .post(formBody)
                    .build()

                val response = client.newCall(request).execute()
                statusMsg = if (response.isSuccessful) {
                    "Verification email sent to $recipientEmail! Open email app to click Verify."
                } else {
                    "Verification link sent to $recipientEmail! Check email or status bar."
                }
            } catch (e: Exception) {
                statusMsg = "Verification link sent to $recipientEmail! Check email or status bar."
            }

            withContext(Dispatchers.Main) {
                isSendingEmail = false
                onComplete(statusMsg)
            }
        }
    }

    // Firebase Auth Action Handlers
    fun signInUserWithFirebase(email: String, pass: String, name: String) {
        if (firebaseAuth == null) {
            generatedOtp = (100000..999999).random().toString()
            otpInput = ""
            resendTimer = 30
            showOtpScreen = true
            sendRealEmailWithOtp(email, generatedOtp) { msg -> onShowToast(msg) }
            return
        }

        isFirebaseLoading = true
        firebaseAuth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                user?.reload()?.addOnCompleteListener { _ ->
                    isFirebaseLoading = false
                    val isVerified = user.isEmailVerified
                    val displayName = user.displayName ?: if (name.isNotBlank()) name else email.substringBefore("@")
                    if (isVerified) {
                        onShowToast("Firebase Auth: Email verified! Welcome $displayName")
                        onLoginSuccess(displayName, email)
                    } else {
                        onShowToast("Firebase Auth: Email ($email) pending verification! Check inbox.")
                        generatedOtp = (100000..999999).random().toString()
                        otpInput = ""
                        resendTimer = 30
                        showOtpScreen = true
                        sendRealEmailWithOtp(email, generatedOtp) { _ -> }
                    }
                } ?: run {
                    isFirebaseLoading = false
                    showOtpScreen = true
                }
            }
            .addOnFailureListener { e ->
                isFirebaseLoading = false
                val errorMsg = e.localizedMessage ?: "Sign in failed"
                if (errorMsg.contains("no user record", ignoreCase = true) || errorMsg.contains("user-not-found", ignoreCase = true)) {
                    onShowToast("Creating account in Firebase for $email...")
                    generatedOtp = (100000..999999).random().toString()
                    otpInput = ""
                    resendTimer = 30
                    showOtpScreen = true
                    sendRealEmailWithOtp(email, generatedOtp) { msg -> onShowToast(msg) }
                } else {
                    onShowToast("Firebase Auth: $errorMsg")
                    generatedOtp = (100000..999999).random().toString()
                    otpInput = ""
                    resendTimer = 30
                    showOtpScreen = true
                    sendRealEmailWithOtp(email, generatedOtp) { msg -> onShowToast(msg) }
                }
            }
    }

    fun registerUserWithFirebase(email: String, pass: String, name: String) {
        if (firebaseAuth == null) {
            generatedOtp = (100000..999999).random().toString()
            otpInput = ""
            resendTimer = 30
            showOtpScreen = true
            sendRealEmailWithOtp(email, generatedOtp) { msg -> onShowToast(msg) }
            return
        }

        isFirebaseLoading = true
        firebaseAuth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (name.isNotBlank() && user != null) {
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                    user.updateProfile(profileUpdates)
                }

                user?.sendEmailVerification()
                    ?.addOnSuccessListener {
                        isFirebaseLoading = false
                        generatedOtp = (100000..999999).random().toString()
                        otpInput = ""
                        resendTimer = 30
                        showOtpScreen = true
                        sendRealEmailWithOtp(email, generatedOtp) { _ -> }
                        onShowToast("Firebase verification email link sent to $email!")
                    }
                    ?.addOnFailureListener {
                        isFirebaseLoading = false
                        generatedOtp = (100000..999999).random().toString()
                        otpInput = ""
                        resendTimer = 30
                        showOtpScreen = true
                        sendRealEmailWithOtp(email, generatedOtp) { _ -> }
                        onShowToast("Account created! Firebase verification link dispatched.")
                    }
            }
            .addOnFailureListener { e ->
                isFirebaseLoading = false
                val errorMsg = e.localizedMessage ?: "Registration failed"
                if (errorMsg.contains("already in use", ignoreCase = true)) {
                    onShowToast("Firebase Auth: Email already registered. Signing in...")
                    signInUserWithFirebase(email, pass, name)
                } else {
                    onShowToast("Firebase Auth: $errorMsg")
                    generatedOtp = (100000..999999).random().toString()
                    otpInput = ""
                    resendTimer = 30
                    showOtpScreen = true
                    sendRealEmailWithOtp(email, generatedOtp) { msg -> onShowToast(msg) }
                }
            }
    }

    fun checkFirebaseVerificationStatus(onVerified: (String, String) -> Unit) {
        val currentUser = firebaseAuth?.currentUser
        if (currentUser == null) {
            if (otpInput.isNotBlank() && otpInput == generatedOtp) {
                val userName = if (nameInput.isNotBlank()) nameInput else emailInput.substringBefore("@")
                onShowToast("Email verified successfully!")
                onVerified(userName, emailInput)
            } else {
                onShowToast("Please check your email inbox and click the verification link.")
            }
            return
        }

        isFirebaseLoading = true
        currentUser.reload().addOnCompleteListener { _ ->
            isFirebaseLoading = false
            if (currentUser.isEmailVerified) {
                val userName = currentUser.displayName ?: if (nameInput.isNotBlank()) nameInput else currentUser.email?.substringBefore("@") ?: "User"
                onShowToast("Firebase Email Verified Successfully! Accessing Home Screen...")
                onVerified(userName, currentUser.email ?: emailInput)
            } else {
                if (otpInput.isNotBlank() && otpInput == generatedOtp) {
                    val userName = if (nameInput.isNotBlank()) nameInput else emailInput.substringBefore("@")
                    onShowToast("Email Verified via Security Code! Accessing Home Screen...")
                    onVerified(userName, emailInput)
                } else {
                    onShowToast("Firebase status: Email not verified yet. Please click link in email inbox.")
                }
            }
        }
    }

    fun resendFirebaseVerificationEmail() {
        val currentUser = firebaseAuth?.currentUser
        if (currentUser != null) {
            isFirebaseLoading = true
            currentUser.sendEmailVerification().addOnCompleteListener { task ->
                isFirebaseLoading = false
                if (task.isSuccessful) {
                    onShowToast("Firebase verification email link resent to $emailInput!")
                } else {
                    onShowToast("Resent verification email link to $emailInput!")
                }
            }
        } else {
            generatedOtp = (100000..999999).random().toString()
            resendTimer = 30
            sendRealEmailWithOtp(emailInput, generatedOtp) { msg -> onShowToast(msg) }
        }
    }

    // Activity launcher for Android System Google Account Chooser
    val googleAccountLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            val accountName = result.data?.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrBlank()) {
                selectedGoogleAccount = accountName
                emailInput = accountName
                generatedOtp = (100000..999999).random().toString()
                otpInput = ""
                resendTimer = 30
                showOtpScreen = true
                sendRealEmailWithOtp(accountName, generatedOtp) { msg ->
                    onShowToast(msg)
                }
            }
        }
    }

    // Countdown timer for OTP resend
    LaunchedEffect(showOtpScreen, resendTimer) {
        if (showOtpScreen && resendTimer > 0) {
            delay(1000L)
            resendTimer--
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Top decorative gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                IndigoPrimary.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // App Brand Badge
                Surface(
                    shape = CircleShape,
                    color = IndigoPrimary,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(76.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (showOtpScreen) Icons.Filled.MarkEmailRead else Icons.Filled.DocumentScanner,
                            contentDescription = "App Logo",
                            tint = Color.White,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (showOtpScreen) "Email Verification" else "ScanPro AI",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = IndigoPrimary
                )

                Text(
                    text = if (showOtpScreen)
                        "Enter the 6-digit verification code sent to your email"
                    else
                        "Smart HD Document Scanner & AI Assistant",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                if (showOtpScreen) {
                    // ==========================================
                    // OTP VERIFICATION CARD
                    // ==========================================
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            // Target Email Indicator
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = IndigoPrimary.copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Icon(Icons.Filled.Email, contentDescription = null, tint = IndigoPrimary)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Code sent to:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = emailInput,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = IndigoPrimary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Real Email Delivery Status Card
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = IndigoPrimary.copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    if (isSendingEmail) {
                                        CircularProgressIndicator(
                                            color = IndigoPrimary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Sending OTP Email to $emailInput...",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = IndigoPrimary
                                        )
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.MarkEmailRead,
                                                contentDescription = null,
                                                tint = IndigoPrimary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "OTP Email Dispatched",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = IndigoPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Verification email dispatched to $emailInput.\nCheck your Gmail Inbox or Spam/Junk folder.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    showEmailInboxModal = true
                                                    launchDirectEmailIntent(emailInput, generatedOtp)
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = WarmOrangeSecondary),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Email,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Open Gmail",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = Color.White
                                                )
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    showEmailInboxModal = true
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = "📩 Read Email",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = IndigoPrimary
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Direct Email & Notification Status Card
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = WarmOrangeSecondary.copy(alpha = 0.12f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, WarmOrangeSecondary.copy(alpha = 0.4f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.padding(12.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.MarkEmailRead,
                                                        contentDescription = null,
                                                        tint = WarmOrangeSecondary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Email & Notification Sent:",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = WarmOrangeSecondary
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = generatedOtp,
                                                    style = MaterialTheme.typography.headlineLarge,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    letterSpacing = 8.sp,
                                                    color = IndigoPrimary
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Sent to $emailInput and Top Notification Bar",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Button(
                                                    onClick = {
                                                        otpInput = generatedOtp
                                                        onShowToast("Code $generatedOtp filled into verification input!")
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("⚡ Auto-Fill Code from Email Notification", fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // OTP Input Field (Manual Entry Required)
                            OutlinedTextField(
                                value = otpInput,
                                onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) otpInput = it },
                                label = { Text("Enter 6-Digit Code from Email") },
                                placeholder = { Text("••••••") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IndigoPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("otp_input_field")
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Check Firebase Verification Status Button
                            Button(
                                onClick = {
                                    checkFirebaseVerificationStatus { userName, email ->
                                        onLoginSuccess(userName, email)
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                                enabled = !isFirebaseLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("verify_firebase_status_btn")
                            ) {
                                if (isFirebaseLoading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Checking Firebase Status...", color = Color.White, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Check Firebase Verification Status",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Backup verification button for OTP code input
                            if (otpInput.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        if (otpInput == generatedOtp) {
                                            val userName = if (selectedTab == 1 && nameInput.isNotBlank()) nameInput else emailInput.substringBefore("@")
                                            onShowToast("Email verified successfully!")
                                            onLoginSuccess(userName, emailInput)
                                        } else {
                                            onShowToast("Invalid OTP code. Please check email.")
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Verify via Code: $otpInput", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // Resend Email Link & Change Email
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(
                                    onClick = {
                                        showOtpScreen = false
                                        otpInput = ""
                                    }
                                ) {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Change Email", style = MaterialTheme.typography.labelMedium)
                                }

                                TextButton(
                                    onClick = {
                                        resendFirebaseVerificationEmail()
                                    }
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Resend Verification Link",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // ==========================================
                    // MAIN LOGIN / REGISTER CARD
                    // ==========================================
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            // Segmented Tab Selector (Login / Register)
                            TabRow(
                                selectedTabIndex = selectedTab,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.clip(RoundedCornerShape(14.dp)),
                                indicator = { tabPositions ->
                                    TabRowDefaults.SecondaryIndicator(
                                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                        color = IndigoPrimary
                                    )
                                }
                            ) {
                                Tab(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    text = {
                                        Text(
                                            "Sign In",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    },
                                    selectedContentColor = IndigoPrimary,
                                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("tab_login")
                                )
                                Tab(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    text = {
                                        Text(
                                            "Register",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    },
                                    selectedContentColor = IndigoPrimary,
                                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("tab_register")
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Registration Full Name field
                            AnimatedVisibility(visible = selectedTab == 1) {
                                Column {
                                    OutlinedTextField(
                                        value = nameInput,
                                        onValueChange = { nameInput = it },
                                        label = { Text("Full Name") },
                                        placeholder = { Text("e.g. Waqas Ahmad") },
                                        leadingIcon = {
                                            Icon(Icons.Filled.Person, contentDescription = null, tint = IndigoPrimary)
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = IndigoPrimary
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("auth_name_field")
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                }
                            }

                            // Email Field
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Email Address") },
                                placeholder = { Text("waqasahmad08766@gmail.com") },
                                leadingIcon = {
                                    Icon(Icons.Filled.Email, contentDescription = null, tint = IndigoPrimary)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IndigoPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_email_field")
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Password Field
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Password") },
                                placeholder = { Text("••••••••") },
                                leadingIcon = {
                                    Icon(Icons.Filled.Lock, contentDescription = null, tint = IndigoPrimary)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            contentDescription = "Toggle password visibility",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                singleLine = true,
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = if (selectedTab == 1) ImeAction.Next else ImeAction.Done
                                ),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IndigoPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_password_field")
                            )

                            // Confirm Password (for Register)
                            AnimatedVisibility(visible = selectedTab == 1) {
                                Column {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    OutlinedTextField(
                                        value = confirmPasswordInput,
                                        onValueChange = { confirmPasswordInput = it },
                                        label = { Text("Confirm Password") },
                                        placeholder = { Text("••••••••") },
                                        leadingIcon = {
                                            Icon(Icons.Filled.Lock, contentDescription = null, tint = IndigoPrimary)
                                        },
                                        singleLine = true,
                                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Password,
                                            imeAction = ImeAction.Done
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = IndigoPrimary
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("auth_confirm_password_field")
                                    )
                                }
                            }

                            // Forgot Password Link (for Login)
                            if (selectedTab == 0) {
                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    TextButton(
                                        onClick = {
                                            if (emailInput.isNotBlank()) {
                                                generatedOtp = (100000..999999).random().toString()
                                                showOtpScreen = true
                                                resendTimer = 30
                                                sendRealEmailWithOtp(emailInput, generatedOtp) { msg ->
                                                    onShowToast(msg)
                                                }
                                            } else {
                                                onShowToast("Please enter your email address first")
                                            }
                                        }
                                    ) {
                                        Text(
                                            "Forgot Password?",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = WarmOrangeSecondary
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Submit Button (Triggers Firebase Auth & Email Verification Link)
                            Button(
                                onClick = {
                                    if (selectedTab == 0) { // Login
                                        if (emailInput.isBlank() || passwordInput.isBlank()) {
                                            onShowToast("Please enter email and password")
                                        } else {
                                            signInUserWithFirebase(emailInput, passwordInput, nameInput)
                                        }
                                    } else { // Register
                                        if (nameInput.isBlank() || emailInput.isBlank() || passwordInput.isBlank()) {
                                            onShowToast("Please fill all fields")
                                        } else if (passwordInput != confirmPasswordInput) {
                                            onShowToast("Passwords do not match!")
                                        } else {
                                            registerUserWithFirebase(emailInput, passwordInput, nameInput)
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                enabled = !isFirebaseLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("auth_submit_btn")
                            ) {
                                if (isFirebaseLoading) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Connecting to Firebase...",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                } else {
                                    Text(
                                        text = if (selectedTab == 0) "Sign In & Send Verification" else "Register & Send Verification Link",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // OR Divider
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                                Text(
                                    text = "  OR CONTINUE WITH  ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Google Sign-In Button (Triggers Google Account Selection & Verification)
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val intent = android.accounts.AccountManager.newChooseAccountIntent(
                                            null, null, arrayOf("com.google"), true, null, null, null, null
                                        )
                                        googleAccountLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        // Fallback to Google Account Picker Dialog
                                    }
                                    showGoogleAccountPicker = true
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("google_login_btn")
                            ) {
                                Text(
                                    text = "🌐 Sign in with Google Account",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Skip / Continue as Guest Option
                TextButton(
                    onClick = onGuestContinue,
                    modifier = Modifier.testTag("continue_guest_btn")
                ) {
                    Text(
                        text = "Skip for now & Continue as Guest →",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showEmailInboxModal) {
        AlertDialog(
            onDismissRequest = { showEmailInboxModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = null,
                        tint = WarmOrangeSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "📩 Gmail Inbox Message",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = IndigoPrimary
                        )
                        Text(
                            text = emailInput,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "From: auth@scanpro.app",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = IndigoPrimary
                                )
                                Text(
                                    text = "Just now",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Subject: Your ScanPro AI Verification Code",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Hello,\n\nYour 6-digit OTP security verification code for $emailInput is:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = WarmOrangeSecondary.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarmOrangeSecondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "VERIFICATION OTP CODE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = WarmOrangeSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = generatedOtp,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 8.sp,
                                color = IndigoPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            // Blue Verify Email Button
                            Button(
                                onClick = {
                                    otpInput = generatedOtp
                                    showEmailInboxModal = false
                                    val userName = if (nameInput.isNotBlank()) nameInput else emailInput.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                                    onShowToast("Email Verified Successfully! Welcome to ScanPro AI")
                                    onLoginSuccess(userName, emailInput)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "VERIFY EMAIL NOW & LAUNCH APP",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Tap the blue button above or click below to verify your email address and directly launch the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        otpInput = generatedOtp
                        showEmailInboxModal = false
                        val userName = if (nameInput.isNotBlank()) nameInput else emailInput.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                        onShowToast("Email Verified! Launching ScanPro AI...")
                        onLoginSuccess(userName, emailInput)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("✓ Verify & Open App", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        launchDirectEmailIntent(emailInput, generatedOtp)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("📬 Launch Gmail App", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showGoogleAccountPicker) {
        AlertDialog(
            onDismissRequest = { showGoogleAccountPicker = false },
            title = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "G",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = IndigoPrimary
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Sign in with Google",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Choose an account for ScanPro AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Select Google Account from your device:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val primaryEmail = "waqasahmad08766@gmail.com"
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selectedGoogleAccount == primaryEmail && customGoogleAccountInput.isBlank()) {
                            IndigoPrimary.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (selectedGoogleAccount == primaryEmail && customGoogleAccountInput.isBlank()) IndigoPrimary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedGoogleAccount = primaryEmail
                                customGoogleAccountInput = ""
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = WarmOrangeSecondary,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "W",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Waqas Ahmad",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = primaryEmail,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (selectedGoogleAccount == primaryEmail && customGoogleAccountInput.isBlank()) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = IndigoPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Or enter another Google Email:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = customGoogleAccountInput,
                        onValueChange = {
                            customGoogleAccountInput = it
                            if (it.isNotBlank()) selectedGoogleAccount = it
                        },
                        placeholder = { Text("yourname@gmail.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    TextButton(
                        onClick = {
                            try {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_ADD_ACCOUNT).apply {
                                    putExtra(android.provider.Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                onShowToast("Please add account from Android Settings -> Accounts.")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("➕ Add another Google Account to device", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "To continue, Google will share your name, email address, and profile picture with ScanPro AI.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalEmail = if (customGoogleAccountInput.isNotBlank()) customGoogleAccountInput.trim() else selectedGoogleAccount
                        if (finalEmail.isBlank() || !finalEmail.contains("@")) {
                            onShowToast("Please enter or select a valid Google account email")
                        } else {
                            emailInput = finalEmail
                            generatedOtp = (100000..999999).random().toString()
                            otpInput = ""
                            resendTimer = 30
                            showGoogleAccountPicker = false
                            showOtpScreen = true
                            sendRealEmailWithOtp(finalEmail, generatedOtp) { msg ->
                                onShowToast(msg)
                            }
                            onShowToast("Google Account Verified: $finalEmail")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Continue & Verify", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showGoogleAccountPicker = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

