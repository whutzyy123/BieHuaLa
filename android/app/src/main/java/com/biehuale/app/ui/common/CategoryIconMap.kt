package com.biehuale.app.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.biehuale.app.ui.icons.BhlIcons

/**
 * 类别 / 账户 icon key → ImageVector（种子与 IconColorPresets 共用）。
 */
object CategoryIconMap {

    fun iconFor(key: String?): ImageVector {
        if (key.isNullOrBlank()) return BhlIcons.Category
        return when (key.lowercase()) {
            "restaurant", "local_cafe", "cafe" -> Icons.Outlined.Restaurant
            "directions_bus", "bus", "directions_car" -> Icons.Outlined.DirectionsBus
            "shopping_cart", "shopping" -> Icons.Outlined.ShoppingCart
            "home", "house" -> Icons.Outlined.Home
            "sports_esports", "sports", "game" -> Icons.Outlined.SportsEsports
            "medical_services", "medical", "health" -> Icons.Outlined.MedicalServices
            "school", "education" -> Icons.Outlined.School
            "phone", "call", "phone_android" -> Icons.Outlined.Phone
            "spa", "face" -> Icons.Outlined.Spa
            "more_horiz", "more", "category" -> Icons.Outlined.MoreHoriz
            "payments", "payment", "attach_money" -> Icons.Outlined.Payments
            "redeem", "card_giftcard" -> Icons.Outlined.Redeem
            "trending_up", "show_chart" -> Icons.Outlined.TrendingUp
            "savings", "piggy" -> Icons.Outlined.Savings
            "cash", "money" -> Icons.Outlined.Payments
            "wallet" -> Icons.Outlined.AccountBalanceWallet
            "wechat", "alipay" -> Icons.Outlined.AccountBalanceWallet
            "bank", "account_balance" -> Icons.Outlined.AccountBalance
            "card", "credit_card" -> Icons.Outlined.CreditCard
            "local_cafe_alt" -> Icons.Outlined.LocalCafe
            else -> Icons.Outlined.Category
        }
    }

    /** 色圆底：过淡则略加深，保证图标可读 */
    fun circleColor(hex: String?, fallback: Color): Color {
        val raw = parseColorOrDefault(hex, fallback)
        val luminance = 0.299f * raw.red + 0.587f * raw.green + 0.114f * raw.blue
        return if (luminance > 0.78f) {
            Color(
                red = (raw.red * 0.82f).coerceIn(0f, 1f),
                green = (raw.green * 0.82f).coerceIn(0f, 1f),
                blue = (raw.blue * 0.82f).coerceIn(0f, 1f),
                alpha = 1f
            )
        } else raw
    }
}
