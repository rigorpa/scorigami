package com.scorigami.app.ui.history

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.scorigami.app.R
import com.scorigami.app.ui.round.StatTotalsLine
import com.scorigami.app.ui.round.StatUnderlines
import com.scorigami.app.ui.round.formatVsPar
import com.scorigami.app.ui.theme.ContentLightGrey
import com.scorigami.app.ui.theme.ContentWhite
import com.scorigami.app.ui.theme.HistoryGradientEnd
import com.scorigami.app.ui.theme.HistoryGradientStart
import com.scorigami.app.ui.theme.ScoreUnderParColor
import com.scorigami.app.ui.theme.ScaleGrey1
import com.scorigami.app.ui.theme.ScreenBackground
import com.scorigami.app.viewmodel.RoundDetailState
import com.scorigami.shared.db.entity.HoleEntity
import com.scorigami.shared.db.entity.PlayerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Preview-and-share dialog: shows the branded [ShareScorecardCard] and, on Share, captures it
 * to a PNG (via a [GraphicsLayer] recording of the card's draw pass) and fires the system
 * share sheet. The capture modifier lives on the card itself — not the scroll container — so
 * the full card is recorded even when the preview viewport clips it.
 */
@Composable
internal fun ShareRoundDialog(
    detail: RoundDetailState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    var isSharing by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isSharing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ScaleGrey1,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 32.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    ShareScorecardCard(
                        detail = detail,
                        modifier = Modifier.drawWithContent {
                            graphicsLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            drawLayer(graphicsLayer)
                        }
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSharing) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                if (graphicsLayer.size.width == 0 || graphicsLayer.size.height == 0) return@launch
                                isSharing = true
                                try {
                                    sharePng(context, graphicsLayer, detail.courseName, detail.date)
                                    onDismiss()
                                } finally {
                                    isSharing = false
                                }
                            }
                        },
                        enabled = !isSharing
                    ) {
                        if (isSharing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Share")
                        }
                    }
                }
            }
        }
    }
}

private suspend fun sharePng(
    context: Context,
    graphicsLayer: GraphicsLayer,
    courseName: String,
    date: String
) {
    val imageBitmap = graphicsLayer.toImageBitmap()
    val uri = withContext(Dispatchers.IO) {
        var bmp = imageBitmap.asAndroidBitmap()
        if (bmp.config == Bitmap.Config.HARDWARE) {
            bmp = bmp.copy(Bitmap.Config.ARGB_8888, false)
        }
        val dir = File(context.cacheDir, "shared_rounds").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val safeName = "$courseName $date".replace(Regex("""[/\\:*?"<>|]"""), "_")
        val file = File(dir, "$safeName.png")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "$courseName — Scorigami Round")
        clipData = ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share round"))
}

/**
 * The branded scorecard rendered to the shared PNG. Fixed 340.dp width for consistent output
 * across devices; the root paints [ScreenBackground] edge to edge so the PNG is fully opaque.
 */
@Composable
internal fun ShareScorecardCard(
    detail: RoundDetailState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(340.dp)
            .background(ScreenBackground)
    ) {
        // Header — matches the History screen's gradient identity
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(HistoryGradientStart, HistoryGradientEnd)))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "Scorigami",
                    style = MaterialTheme.typography.labelMedium,
                    color = ContentWhite.copy(alpha = 0.8f)
                )
                Text(
                    detail.courseName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ContentWhite,
                    maxLines = 1
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                buildString {
                    if (detail.date.isNotEmpty()) append("Played on ${detail.date}  ·  ")
                    append("${detail.holes.size} holes · Par ${detail.holes.sumOf { it.par }}")
                },
                style = MaterialTheme.typography.labelSmall,
                color = ContentLightGrey
            )
            Spacer(Modifier.height(10.dp))

            detail.players.forEach { player ->
                SharePlayerBlock(
                    player = player,
                    holes = detail.holes,
                    scores = detail.scores,
                    obCounts = detail.obCounts,
                    c1xCounts = detail.c1xCounts
                )
                Spacer(Modifier.height(8.dp))
            }

            Text(
                "Shared from Scorigami",
                style = MaterialTheme.typography.labelSmall,
                color = ContentLightGrey.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SharePlayerBlock(
    player: PlayerEntity,
    holes: List<HoleEntity>,
    scores: Map<Pair<Long, Int>, Int>,
    obCounts: Map<Pair<Long, Int>, Int>,
    c1xCounts: Map<Pair<Long, Int>, Int>
) {
    val totalThrows = scores.entries.filter { it.key.first == player.id }.sumOf { it.value }
    val totalPar = holes.filter { scores[Pair(player.id, it.number)] != null }.sumOf { it.par }
    val vsPar = totalThrows - totalPar
    val totalOb = obCounts.entries.filter { it.key.first == player.id }.sumOf { it.value }
    val totalC1x = c1xCounts.entries.filter { it.key.first == player.id }.sumOf { it.value }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ScaleGrey1)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                player.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = ContentWhite,
                maxLines = 1
            )
            Text(
                formatVsPar(vsPar),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = shareVsParColor(vsPar)
            )
        }
        Spacer(Modifier.height(6.dp))
        holes.chunked(9).forEach { group ->
            Row(modifier = Modifier.fillMaxWidth()) {
                group.forEach { hole ->
                    val throws = scores[Pair(player.id, hole.number)]
                    val diff = throws?.minus(hole.par)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "${hole.number}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ContentWhite
                        )
                        Text(
                            text = throws?.toString() ?: "—",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = diff?.let { shareVsParColor(it) } ?: ContentLightGrey,
                            textAlign = TextAlign.Center
                        )
                        StatUnderlines(
                            hasOb = (obCounts[Pair(player.id, hole.number)] ?: 0) > 0,
                            hasC1x = (c1xCounts[Pair(player.id, hole.number)] ?: 0) > 0
                        )
                    }
                }
                // Pad short rows so a trailing 9-hole group keeps cell widths consistent
                repeat(9 - group.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(3.dp))
        }
        StatTotalsLine(totalOb, totalC1x, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * Share-card vs-par colors: green under par (the theme's primary — which ScoreFormat's
 * vsParColor uses — is a light blue, wrong for the exported image).
 */
@Composable
private fun shareVsParColor(vsPar: Int) = when {
    vsPar < 0 -> ScoreUnderParColor
    vsPar == 0 -> ContentWhite
    else -> MaterialTheme.colorScheme.error
}
