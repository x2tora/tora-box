package com.x2tora.torabox.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.x2tora.torabox.MainActivity
import com.x2tora.torabox.data.ToraBoxRepository
import com.x2tora.torabox.domain.KoreanCarrierCatalog
import com.x2tora.torabox.model.ShipmentRecord
import com.x2tora.torabox.model.StandardDeliveryPhase
import com.x2tora.torabox.notification.NotificationHelper
import com.x2tora.torabox.worker.ShipmentSyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ToraBoxGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = ToraBoxRepository(context).load()
        val activeShipments = state.shipments
            .filter { !it.isArchived }
            .sortedWith(
                compareByDescending<ShipmentRecord> { it.isPinned }
                    .thenBy { it.currentPhase == StandardDeliveryPhase.DELIVERED }
                    .thenByDescending { it.currentPhase == StandardDeliveryPhase.OUT_FOR_DELIVERY }
                    .thenByDescending { maxOf(it.lastUpdatedEpochMs, it.createdAtEpochMs) }
            )

        provideContent {
            ToraBoxWidgetContent(
                context = context,
                activeShipments = activeShipments
            )
        }
    }
}

@Composable
private fun ToraBoxWidgetContent(
    context: Context,
    activeShipments: List<ShipmentRecord>
) {
    val bgSurface = ColorProvider(day = Color(0xFFF7F8FA), night = Color(0xFF171A21))
    val cardSurface = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF212631))
    val textPrimary = ColorProvider(day = Color(0xFF141820), night = Color(0xFFF3F5F8))
    val textSecondary = ColorProvider(day = Color(0xFF5D6676), night = Color(0xFFA0AAB8))
    val accentBlue = ColorProvider(day = Color(0xFF2B76FF), night = Color(0xFF5A97FF))
    val pillBg = ColorProvider(day = Color(0xFFE8F0FF), night = Color(0xFF24334D))

    val inProgressCount = activeShipments.count { it.currentPhase != StandardDeliveryPhase.DELIVERED }
    val outForDeliveryCount = activeShipments.count { it.currentPhase == StandardDeliveryPhase.OUT_FOR_DELIVERY }
    val launchHomeIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgSurface)
            .cornerRadius(20.dp)
            .padding(14.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(actionStartActivity(launchHomeIntent))
            ) {
                Text(
                    text = "토라박스",
                    style = TextStyle(
                        color = textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                val subtitle = when {
                    outForDeliveryCount > 0 -> "진행 ${inProgressCount}건 · 오늘 출발 ${outForDeliveryCount}건"
                    inProgressCount > 0 -> "진행 중인 택배 ${inProgressCount}건"
                    else -> "등록된 배송 ${activeShipments.size}건"
                }
                Text(
                    text = subtitle,
                    style = TextStyle(
                        color = if (outForDeliveryCount > 0) accentBlue else textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Box(
                modifier = GlanceModifier
                    .background(pillBg)
                    .cornerRadius(12.dp)
                    .clickable(actionRunCallback<RefreshWidgetAction>())
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "새로고침",
                    style = TextStyle(
                        color = accentBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(10.dp))

        if (activeShipments.isEmpty()) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(cardSurface)
                    .cornerRadius(14.dp)
                    .clickable(actionStartActivity(launchHomeIntent))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "진행 중인 택배가 없습니다. 탭하여 송장을 등록하세요.",
                    style = TextStyle(color = textSecondary, fontSize = 12.sp)
                )
            }
        } else {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                activeShipments.take(3).forEachIndexed { index, record ->
                    if (index > 0) {
                        Spacer(modifier = GlanceModifier.height(6.dp))
                    }
                    val carrierShort = KoreanCarrierCatalog.findById(record.carrierId)?.shortName ?: record.carrierId
                    val detailIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra(NotificationHelper.EXTRA_SHIPMENT_ID, record.id)
                    }
                    val phaseColor = when (record.currentPhase) {
                        StandardDeliveryPhase.OUT_FOR_DELIVERY -> ColorProvider(Color(0xFF10B981), Color(0xFF34D399))
                        StandardDeliveryPhase.DELIVERED -> ColorProvider(Color(0xFF6B7280), Color(0xFF9CA3AF))
                        StandardDeliveryPhase.EXCEPTION -> ColorProvider(Color(0xFFEF4444), Color(0xFFF87171))
                        else -> accentBlue
                    }

                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(cardSurface)
                            .cornerRadius(12.dp)
                            .clickable(actionStartActivity(detailIntent))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = record.displayTitle(carrierShort),
                                maxLines = 1,
                                style = TextStyle(
                                    color = textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "$carrierShort · ${record.waybillNumber}",
                                maxLines = 1,
                                style = TextStyle(
                                    color = textSecondary,
                                    fontSize = 10.sp
                                )
                            )
                        }
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(
                            text = record.currentPhase.label,
                            style = TextStyle(
                                color = phaseColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        ShipmentSyncScheduler.triggerImmediateSync(context)
        ToraBoxGlanceWidget().update(context, glanceId)
    }
}

class ToraBoxWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ToraBoxGlanceWidget()
}

class ToraBoxCompactWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = ToraBoxRepository(context).load()
        val active = state.shipments.filter { !it.isArchived }
        val inProgress = active.count { it.currentPhase != StandardDeliveryPhase.DELIVERED }
        val outForDelivery = active.count { it.currentPhase == StandardDeliveryPhase.OUT_FOR_DELIVERY }
        val anomalyCount = active.count { it.anomalyType != null }

        provideContent {
            val bgSurface = ColorProvider(day = Color(0xFFF7F8FA), night = Color(0xFF171A21))
            val textPrimary = ColorProvider(day = Color(0xFF141820), night = Color(0xFFF3F5F8))
            val textSecondary = ColorProvider(day = Color(0xFF5D6676), night = Color(0xFFA0AAB8))
            val accentBlue = ColorProvider(day = Color(0xFF2B76FF), night = Color(0xFF5A97FF))
            val warnRed = ColorProvider(day = Color(0xFFDC2626), night = Color(0xFFF87171))
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(bgSurface)
                    .cornerRadius(18.dp)
                    .clickable(actionStartActivity(launchIntent))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "토라박스",
                        style = TextStyle(color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = when {
                            anomalyCount > 0 -> "확인 필요 ${anomalyCount}건"
                            outForDelivery > 0 -> "오늘 출발 ${outForDelivery}건"
                            else -> "진행 중 ${inProgress}건"
                        },
                        style = TextStyle(
                            color = when {
                                anomalyCount > 0 -> warnRed
                                outForDelivery > 0 -> accentBlue
                                else -> textSecondary
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                Text(
                    text = inProgress.toString(),
                    style = TextStyle(
                        color = accentBlue,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

class ToraBoxCompactWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ToraBoxCompactWidget()
}

class ToraBoxFocusWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = ToraBoxRepository(context).load()
        val focusRecord = state.shipments
            .filter { !it.isArchived }
            .sortedWith(
                compareByDescending<ShipmentRecord> { it.isPinned }
                    .thenBy { it.currentPhase == StandardDeliveryPhase.DELIVERED }
                    .thenByDescending { it.currentPhase == StandardDeliveryPhase.OUT_FOR_DELIVERY }
                    .thenByDescending { maxOf(it.lastUpdatedEpochMs, it.createdAtEpochMs) }
            )
            .firstOrNull()

        provideContent {
            val bgSurface = ColorProvider(day = Color(0xFFF7F8FA), night = Color(0xFF171A21))
            val cardSurface = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF212631))
            val textPrimary = ColorProvider(day = Color(0xFF141820), night = Color(0xFFF3F5F8))
            val textSecondary = ColorProvider(day = Color(0xFF5D6676), night = Color(0xFFA0AAB8))
            val accentBlue = ColorProvider(day = Color(0xFF2B76FF), night = Color(0xFF5A97FF))
            val pillBg = ColorProvider(day = Color(0xFFE8F0FF), night = Color(0xFF24334D))
            val launchHomeIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(bgSurface)
                    .cornerRadius(22.dp)
                    .padding(14.dp)
            ) {
                if (focusRecord == null) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(cardSurface)
                            .cornerRadius(16.dp)
                            .clickable(actionStartActivity(launchHomeIntent))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "고정된 배송이 없습니다. 탭하여 송장을 추가하세요.",
                            style = TextStyle(color = textSecondary, fontSize = 12.sp)
                        )
                    }
                } else {
                    val carrierShort = KoreanCarrierCatalog.findById(focusRecord.carrierId)?.shortName ?: focusRecord.carrierId
                    val detailIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra(NotificationHelper.EXTRA_SHIPMENT_ID, focusRecord.id)
                    }
                    val latestEvent = focusRecord.events.firstOrNull()

                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .clickable(actionStartActivity(detailIntent)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = focusRecord.displayTitle(carrierShort),
                                maxLines = 1,
                                style = TextStyle(color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "$carrierShort · ${focusRecord.waybillNumber}",
                                maxLines = 1,
                                style = TextStyle(color = textSecondary, fontSize = 11.sp)
                            )
                        }
                        Box(
                            modifier = GlanceModifier
                                .background(pillBg)
                                .cornerRadius(12.dp)
                                .clickable(actionRunCallback<RefreshWidgetAction>())
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "새로고침",
                                style = TextStyle(color = accentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(10.dp))

                    // Step progress indicators (6 phases)
                    val activeStep = focusRecord.currentPhase.stepIndex.coerceIn(0, 5)
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        for (step in 0..5) {
                            if (step > 0) Spacer(modifier = GlanceModifier.width(4.dp))
                            val filled = step <= activeStep
                            Box(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .height(6.dp)
                                    .cornerRadius(3.dp)
                                    .background(if (filled) accentBlue else cardSurface)
                            ) {}
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(10.dp))

                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(cardSurface)
                            .cornerRadius(14.dp)
                            .clickable(actionStartActivity(detailIntent))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = buildString {
                                append(focusRecord.currentPhase.label)
                                if (focusRecord.estimatedDeliveryLabel.isNotBlank()) {
                                    append(" · ").append(focusRecord.estimatedDeliveryLabel)
                                }
                            },
                            style = TextStyle(color = accentBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = latestEvent?.let { ev ->
                                listOf(ev.location, ev.description).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { ev.statusName }
                            } ?: focusRecord.currentStatusLabel,
                            maxLines = 2,
                            style = TextStyle(color = textPrimary, fontSize = 11.sp)
                        )
                        if (latestEvent?.displayTime?.isNotBlank() == true) {
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            Text(
                                text = latestEvent.displayTime,
                                style = TextStyle(color = textSecondary, fontSize = 10.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}

class ToraBoxFocusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ToraBoxFocusWidget()
}

object ToraBoxWidgetUpdater {
    fun updateAllAsync(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            updateAll(appContext)
        }
    }

    suspend fun updateAll(context: Context) {
        val appContext = context.applicationContext
        runCatching {
            val manager = GlanceAppWidgetManager(appContext)
            manager.getGlanceIds(ToraBoxGlanceWidget::class.java).forEach { id ->
                ToraBoxGlanceWidget().update(appContext, id)
            }
            manager.getGlanceIds(ToraBoxCompactWidget::class.java).forEach { id ->
                ToraBoxCompactWidget().update(appContext, id)
            }
            manager.getGlanceIds(ToraBoxFocusWidget::class.java).forEach { id ->
                ToraBoxFocusWidget().update(appContext, id)
            }
        }
    }
}
