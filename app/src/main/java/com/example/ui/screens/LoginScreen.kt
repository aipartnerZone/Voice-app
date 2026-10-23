package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.BrandRoyalBlue
import com.example.ui.theme.IceWhiteBackground
import com.example.ui.theme.NavyTextPrimary
import com.example.ui.theme.NavyTextSecondary
import com.example.ui.theme.PureWhiteSurface
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SoftSkyBlueCard

@Composable
fun LoginScreen(
    onContinueSuccess: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeInputMode by remember { mutableStateOf<String?>(null) } // "PHONE" or "EMAIL"
    var inputFieldValue by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF3F9FF),
                        Color(0xFFFFFFFF),
                        Color(0xFFEAF5FF)
                    )
                )
            )
            .testTag("login_screen")
    ) {
        // Soft sky-blue decorative wave at the bottom matching the mockup
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .align(Alignment.BottomCenter)
        ) {
            val w = size.width
            val h = size.height
            val wavePath1 = Path().apply {
                moveTo(0f, h * 0.45f)
                cubicTo(w * 0.25f, h * 0.15f, w * 0.65f, h * 0.75f, w, h * 0.35f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(wavePath1, color = Color(0xFFD4ECFF))

            val wavePath2 = Path().apply {
                moveTo(0f, h * 0.68f)
                cubicTo(w * 0.35f, h * 0.95f, w * 0.75f, h * 0.40f, w, h * 0.65f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(wavePath2, color = Color(0xFFBBE1FE))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 460.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // 3D Munna Robot Hero Illustration with "AI का अपना दोस्त Munna"
                Image(
                    painter = painterResource(id = R.drawable.img_munna_robot),
                    contentDescription = "Munna AI 3D Robot Friend",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(215.dp)
                        .clip(RoundedCornerShape(24.dp))
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Title: "Munna AI"
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = NavyTextPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        ) {
                            append("Munna ")
                        }
                        withStyle(
                            SpanStyle(
                                color = BrandRoyalBlue,
                                fontWeight = FontWeight.ExtraBold
                            )
                        ) {
                            append("AI")
                        }
                    },
                    fontSize = 36.sp,
                    lineHeight = 42.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "आपका अपना AI दोस्त",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = NavyTextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "पढ़ाई, काम, जानकारी, एंटरटेनमेंट...\nअब सब कुछ एक ही जगह!",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    color = NavyTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(22.dp))

                // 1. Google से जारी रखें
                LoginOptionPillButton(
                    text = "Google से जारी रखें",
                    containerColor = PureWhiteSurface,
                    borderColor = SoftBlueBorder,
                    leadingContent = {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = "Google",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onClick = { onContinueSuccess("Google यूज़र") },
                    modifier = Modifier.testTag("login_google_button")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. मोबाइल नंबर से जारी रखें
                LoginOptionPillButton(
                    text = "मोबाइल नंबर से जारी रखें",
                    containerColor = PureWhiteSurface,
                    borderColor = SoftBlueBorder,
                    leadingContent = {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BrandRoyalBlue,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Mobile Number",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        inputFieldValue = ""
                        activeInputMode = "PHONE"
                    },
                    modifier = Modifier.testTag("login_phone_button")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. ईमेल से जारी रखें
                LoginOptionPillButton(
                    text = "ईमेल से जारी रखें",
                    containerColor = PureWhiteSurface,
                    borderColor = SoftBlueBorder,
                    leadingContent = {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BrandRoyalBlue,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Email",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        inputFieldValue = ""
                        activeInputMode = "EMAIL"
                    },
                    modifier = Modifier.testTag("login_email_button")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // "—— या ——" Divider
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = SoftBlueBorder
                    )
                    Text(
                        text = "या",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NavyTextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = SoftBlueBorder
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. बिना लॉगिन जारी रखें (Guest Mode)
                LoginOptionPillButton(
                    text = "बिना लॉगिन जारी रखें",
                    containerColor = SoftSkyBlueCard,
                    borderColor = Color(0xFFB8DCFC),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.PersonOutline,
                            contentDescription = "Guest Mode",
                            tint = BrandRoyalBlue,
                            modifier = Modifier.size(26.dp)
                        )
                    },
                    onClick = { onContinueSuccess("दोस्त (Guest)") },
                    modifier = Modifier.testTag("login_guest_button")
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "(Guest Mode)",
                    style = MaterialTheme.typography.labelLarge,
                    color = NavyTextSecondary
                )
            }

            // Footer: Terms & Privacy Policy
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 18.dp, bottom = 12.dp)
            ) {
                Text(
                    text = buildAnnotatedString {
                        append("जारी रखकर आप हमारी\n")
                        withStyle(
                            SpanStyle(
                                color = BrandRoyalBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        ) {
                            append("Terms & Privacy Policy")
                        }
                        append(" से सहमत होते हैं।")
                    },
                    style = MaterialTheme.typography.labelMedium.copy(lineHeight = 18.sp),
                    color = NavyTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (activeInputMode != null) {
        val isPhone = activeInputMode == "PHONE"
        AlertDialog(
            onDismissRequest = { activeInputMode = null },
            containerColor = PureWhiteSurface,
            title = {
                Text(
                    text = if (isPhone) "मोबाइल नंबर से लॉगिन" else "ईमेल से लॉगिन",
                    color = NavyTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isPhone) "अपना 10 अंकों का मोबाइल नंबर दर्ज करें:" else "अपनी ईमेल आईडी दर्ज करें:",
                        color = NavyTextSecondary
                    )
                    OutlinedTextField(
                        value = inputFieldValue,
                        onValueChange = { inputFieldValue = it },
                        singleLine = true,
                        placeholder = {
                            Text(if (isPhone) "9876543210" else "name@example.com")
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val label = inputFieldValue.trim().ifEmpty {
                            if (isPhone) "मोबाइल यूज़र" else "ईमेल यूज़र"
                        }
                        activeInputMode = null
                        onContinueSuccess(label)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandRoyalBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text("जारी रखें")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeInputMode = null }) {
                    Text("रद्द करें", color = NavyTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun LoginOptionPillButton(
    text: String,
    containerColor: Color,
    borderColor: Color,
    leadingContent: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 3.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                leadingContent()
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = NavyTextPrimary
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = BrandRoyalBlue,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
