package com.auracle.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font as GoogleFontResource
import com.auracle.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val plusJakartaSansFont = GoogleFont("Plus Jakarta Sans")

val PlusJakartaSans = FontFamily(
    GoogleFontResource(googleFont = plusJakartaSansFont, fontProvider = provider)
)

val MomoSignature = FontFamily(
    Font(R.font.momo_signature, FontWeight.Normal)
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    displayLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
