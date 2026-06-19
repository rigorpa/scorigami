package com.scorigami.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import com.scorigami.wear.ui.theme.HoleJumpSelectedColor
import com.scorigami.wear.ui.theme.IncompleteHoleDotColor
import com.scorigami.wear.ui.theme.WearButtonBackground

@Composable
internal fun WearHoleJumpGrid(
    totalHoles: Int,
    currentHole: Int,
    incompleteHoles: Set<Int>,
    onHoleSelected: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        (1..totalHoles).chunked(3).forEach { rowHoles ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowHoles.forEach { holeNum ->
                    val isCurrent = holeNum == currentHole
                    val incomplete = holeNum in incompleteHoles
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(
                                if (isCurrent) HoleJumpSelectedColor else WearButtonBackground,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onHoleSelected(holeNum) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$holeNum",
                            fontSize = 14.sp,
                            fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Normal,
                            color = Color.White
                        )
                        if (incomplete) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 3.dp, end = 3.dp)
                                    .size(5.dp)
                                    .background(IncompleteHoleDotColor, CircleShape)
                            )
                        }
                    }
                }
                repeat(3 - rowHoles.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
