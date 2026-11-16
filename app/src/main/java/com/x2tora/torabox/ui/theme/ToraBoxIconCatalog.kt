package com.x2tora.torabox.ui.theme

import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Alarm
import top.yukonga.miuix.kmp.icon.extended.BankCards
import top.yukonga.miuix.kmp.icon.extended.Carrier
import top.yukonga.miuix.kmp.icon.extended.CloudFill
import top.yukonga.miuix.kmp.icon.extended.Email
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Layers
import top.yukonga.miuix.kmp.icon.extended.Location
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Photos
import top.yukonga.miuix.kmp.icon.extended.Promotions
import top.yukonga.miuix.kmp.icon.extended.Scan
import top.yukonga.miuix.kmp.icon.extended.ScreenMirroring
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.icon.extended.Store
import top.yukonga.miuix.kmp.icon.extended.Tasks
import top.yukonga.miuix.kmp.icon.extended.Timer
import top.yukonga.miuix.kmp.icon.extended.WorldClock

data class ToraIconSpec(
    val id: String,
    val label: String,
    val icon: ImageVector
)

data class ToraIconCategory(
    val label: String,
    val items: List<ToraIconSpec>
)

object ToraBoxIconCatalog {

    val categories: List<ToraIconCategory> = listOf(
        ToraIconCategory(
            label = "택배·물류",
            items = listOf(
                ToraIconSpec("box", "일반 택배", MiuixIcons.Layers),
                ToraIconSpec("truck", "간선·차량", MiuixIcons.Send),
                ToraIconSpec("cargo", "화물·중량", MiuixIcons.Carrier),
                ToraIconSpec("mail", "우편·등기", MiuixIcons.Email),
                ToraIconSpec("globe", "국제·직구", MiuixIcons.WorldClock),
                ToraIconSpec("bolt", "당일·새벽", MiuixIcons.Timer),
                ToraIconSpec("plane", "특송·항공", MiuixIcons.CloudFill),
                ToraIconSpec("scan", "송장·바코드", MiuixIcons.Scan)
            )
        ),
        ToraIconCategory(
            label = "물품·쇼핑",
            items = listOf(
                ToraIconSpec("store", "스토어·편의점", MiuixIcons.Store),
                ToraIconSpec("promotions", "쇼핑·선물", MiuixIcons.Promotions),
                ToraIconSpec("screen_mirroring", "전자기기", MiuixIcons.ScreenMirroring),
                ToraIconSpec("notes", "도서·문구", MiuixIcons.Notes),
                ToraIconSpec("favorites", "생활·뷰티", MiuixIcons.Favorites),
                ToraIconSpec("bank_cards", "중고거래", MiuixIcons.BankCards),
                ToraIconSpec("photos", "패션·잡화", MiuixIcons.Photos),
                ToraIconSpec("folder", "서류·계약", MiuixIcons.Folder)
            )
        ),
        ToraIconCategory(
            label = "수령·위치",
            items = listOf(
                ToraIconSpec("home", "집·문앞", MiuixIcons.Home),
                ToraIconSpec("location", "픽업·거점", MiuixIcons.Location),
                ToraIconSpec("tasks", "체크·검수", MiuixIcons.Tasks),
                ToraIconSpec("alarm", "도착 알림", MiuixIcons.Alarm)
            )
        )
    )

    val allSpecs: List<ToraIconSpec> = categories.flatMap { it.items }
    private val byId: Map<String, ToraIconSpec> = allSpecs.associateBy { it.id }

    fun normalizeId(key: String): String {
        val cleaned = key.trim().removePrefix("icon:")
        return if (byId.containsKey(cleaned)) cleaned else "box"
    }

    fun resolveIcon(key: String, fallback: ImageVector = MiuixIcons.Layers): ImageVector =
        byId[key.trim()]?.icon ?: fallback

    fun resolveLabel(key: String): String =
        byId[key.trim()]?.label.orEmpty()
}
