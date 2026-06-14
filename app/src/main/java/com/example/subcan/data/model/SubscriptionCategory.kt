package com.example.subcan.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class SubscriptionCategory(val label: String, val icon: ImageVector, val color: Color) {
    VIDEO("動画", Icons.Filled.VideoLibrary, Color(0xFFE53935)),
    MUSIC("音楽", Icons.Filled.MusicNote, Color(0xFF8E24AA)),
    BOOKS_AUDIO("本・オーディオ", Icons.AutoMirrored.Filled.MenuBook, Color(0xFF6D4C41)),
    GAMING("ゲーム", Icons.Filled.SportsEsports, Color(0xFF43A047)),

    CLOUD("クラウド", Icons.Filled.Cloud, Color(0xFF1E88E5)),
    PRODUCTIVITY("仕事効率化", Icons.Filled.Work, Color(0xFFFB8C00)),
    AI_DEV("AI・開発", Icons.Filled.Terminal, Color(0xFF5E35B1)),

    NEWS("ニュース", Icons.Filled.Newspaper, Color(0xFF00ACC1)),
    EDUCATION("教育", Icons.Filled.School, Color(0xFF3949AB)),

    FITNESS("フィットネス", Icons.Filled.FitnessCenter, Color(0xFFD81B60)),
    FOOD("フード・デリバリー", Icons.Filled.Restaurant, Color(0xFFFF7043)),
    SHOPPING("ショッピング", Icons.Filled.ShoppingCart, Color(0xFF7CB342)),
    TRANSPORT("交通・移動", Icons.Filled.DirectionsCar, Color(0xFF00897B)),

    FINANCE("金融・家計", Icons.Filled.AccountBalanceWallet, Color(0xFF546E7A)),
    COMMUNICATION("通信・VPN", Icons.Filled.Wifi, Color(0xFF039BE5)),
    LIFESTYLE("ライフスタイル", Icons.Filled.Spa, Color(0xFF9CCC65)),

    OTHER("その他", Icons.Filled.Category, Color(0xFF757575))
}
