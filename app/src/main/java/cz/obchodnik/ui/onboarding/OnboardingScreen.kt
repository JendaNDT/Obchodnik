package cz.obchodnik.ui.onboarding

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.obchodnik.domain.model.Asset
import cz.obchodnik.domain.model.StaticAssetCatalog
import cz.obchodnik.ui.components.AssetIcon
import cz.obchodnik.ui.components.ObchodnikCard
import cz.obchodnik.ui.theme.JetBrainsMono
import cz.obchodnik.ui.theme.Obchodnik

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onToggleAsset: (String) -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onRequestNotificationsPermission: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Obchodnik.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg)
            .padding(horizontal = 24.dp)
            .padding(top = 56.dp, bottom = 24.dp),
    ) {
        // Step Stepper Indicator
        StepIndicator(currentStep = state.step, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(32.dp))

        // Content Area (scrollable to handle smaller screens)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (state.step) {
                1 -> WelcomeStep()
                2 -> WatchlistStep(selectedAssetIds = state.selectedAssetIds, onToggleAsset = onToggleAsset)
                3 -> NotificationsStep(onRequestPermission = onRequestNotificationsPermission)
                4 -> FinishStep(selectedCount = state.selectedAssetIds.size)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Bottom Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.step > 1) {
                Text(
                    text = "Zpět",
                    color = c.text2,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .border(BorderStroke(1.dp, c.borderStrong), RoundedCornerShape(Obchodnik.radii.radius))
                        .clickable { onPreviousStep() }
                        .padding(vertical = 14.dp)
                )
            }

            val isLastStep = state.step == 4
            Text(
                text = if (isLastStep) "Začít sledovat" else "Pokračovat",
                color = c.onAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(if (state.step > 1) 2f else 1f)
                    .background(c.accent, RoundedCornerShape(Obchodnik.radii.radius))
                    .clickable {
                        if (isLastStep) onComplete() else onNextStep()
                    }
                    .padding(vertical = 14.dp)
            )
        }
    }
}

@Composable
private fun StepIndicator(currentStep: Int, modifier: Modifier = Modifier) {
    val c = Obchodnik.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..4) {
            val active = i == currentStep
            val passed = i < currentStep
            Box(
                modifier = Modifier
                    .size(if (active) 12.dp else 8.dp)
                    .background(
                        color = if (active || passed) c.accent else c.borderStrong,
                        shape = CircleShape
                    )
            )
            if (i < 4) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(2.dp)
                        .background(if (passed) c.accent else c.borderStrong)
                )
                Spacer(Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    val c = Obchodnik.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(c.accent.copy(alpha = 0.15f), CircleShape)
                .border(1.dp, c.accent.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.TrendingUp,
                contentDescription = null,
                tint = c.accent,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Vítejte v Obchodníkovi",
            color = c.text,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Moderní terminal-style nástroj pro sledování kryptoměn, tokenizovaných kovů, reálných komodit a akciových indexů.",
            color = c.text3,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun WatchlistStep(selectedAssetIds: Set<String>, onToggleAsset: (String) -> Unit) {
    val c = Obchodnik.colors
    val popularAssets = StaticAssetCatalog.assets.filter {
        it.symbol in setOf("BTC", "ETH", "SOL", "PAXG", "SPY", "WTI")
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Co chcete sledovat?",
            color = c.text,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Vyberte aktiva pro počáteční watchlist. Další můžete vyhledat a přidat později.",
            color = c.text3,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            popularAssets.forEach { asset ->
                val isSelected = selectedAssetIds.contains(asset.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.surface, RoundedCornerShape(Obchodnik.radii.radius))
                        .border(
                            BorderStroke(1.dp, if (isSelected) c.accent else c.border),
                            RoundedCornerShape(Obchodnik.radii.radius)
                        )
                        .clickable { onToggleAsset(asset.id) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssetIcon(symbol = asset.symbol, colorHex = asset.colorHex)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = asset.symbol,
                            color = c.text,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = asset.name,
                            color = c.text3,
                            fontSize = 12.sp
                        )
                    }
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleAsset(asset.id) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = c.accent,
                            uncheckedColor = c.text3,
                            checkmarkColor = c.onAccent
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationsStep(onRequestPermission: () -> Unit) {
    val c = Obchodnik.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(c.accent.copy(alpha = 0.15f), CircleShape)
                .border(1.dp, c.accent.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Notifications,
                contentDescription = null,
                tint = c.accent,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Zůstaňte v obraze",
            color = c.text,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Aplikace může běžet na pozadí a hlídat vaše cenové alerty. Povolte prosím zasílání oznámení, abychom vás mohli okamžitě informovat.",
            color = c.text3,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(Modifier.height(24.dp))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Text(
                text = "Povolit upozornění",
                color = c.onAccent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(c.accent, RoundedCornerShape(Obchodnik.radii.chip))
                    .clickable { onRequestPermission() }
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun FinishStep(selectedCount: Int) {
    val c = Obchodnik.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(c.up.copy(alpha = 0.15f), CircleShape)
                .border(1.dp, c.up.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = c.up,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Vše je připraveno!",
            color = c.text,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Založili jsme váš watchlist s $selectedCount sledovanými aktivy. Nyní můžete prozkoumat trh, nastavit své nákupní pozice nebo sledovat index Fear & Greed.",
            color = c.text3,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}
