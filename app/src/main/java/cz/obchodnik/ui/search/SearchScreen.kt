package cz.obchodnik.ui.search

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.obchodnik.domain.AssetType
import cz.obchodnik.domain.model.Asset
import cz.obchodnik.ui.components.AssetIcon
import cz.obchodnik.ui.theme.JetBrainsMono
import cz.obchodnik.ui.theme.Obchodnik

@Composable
fun SearchScreen(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onToggleWatch: (Asset) -> Unit,
    onOpenAsset: (Asset) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Obchodnik.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        SearchTopBar(
            query = state.query,
            onQueryChange = onQueryChange,
            onBack = onBack,
        )
        if (state.isSearching) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = c.accent,
                trackColor = c.surface,
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
        ) {
            if (state.errorMessage != null) {
                item {
                    Text(
                        text = state.errorMessage,
                        color = c.down,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
            if (state.remoteResults.isNotEmpty()) {
                item { SectionLabel("Výsledky CoinGecko") }
                items(
                    items = state.remoteResults,
                    key = { "remote-${it.id}" },
                ) { asset ->
                    SearchAssetRow(
                        asset = asset,
                        isWatched = asset.id in state.watchlistIds,
                        onToggleWatch = onToggleWatch,
                        onOpenAsset = onOpenAsset,
                    )
                }
            }
            if (state.catalogResults.isNotEmpty()) {
                item {
                    SectionLabel(if (state.query.isBlank()) "Doporučené" else "Katalog")
                }
                items(
                    items = state.catalogResults,
                    key = { "catalog-${it.id}" },
                ) { asset ->
                    SearchAssetRow(
                        asset = asset,
                        isWatched = asset.id in state.watchlistIds,
                        onToggleWatch = onToggleWatch,
                        onOpenAsset = onOpenAsset,
                    )
                }
            }
            if (!state.isSearching && state.remoteResults.isEmpty() && state.catalogResults.isEmpty()) {
                item {
                    EmptySearchState(
                        query = state.query,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp, vertical = 70.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
) {
    val c = Obchodnik.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.bg)
            .padding(start = 4.dp, end = 16.dp, top = 44.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět", tint = c.text)
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text("Hledat aktivum, ticker…", color = c.text3) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = c.text3) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Vymazat", tint = c.text3)
                    }
                }
            },
            shape = RoundedCornerShape(Obchodnik.radii.chip),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = c.text,
                unfocusedTextColor = c.text,
                focusedContainerColor = c.surface,
                unfocusedContainerColor = c.surface,
                focusedBorderColor = c.borderStrong,
                unfocusedBorderColor = c.border,
                cursorColor = c.accent,
            ),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = Obchodnik.colors.text3,
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 7.dp),
    )
}

@Composable
private fun SearchAssetRow(
    asset: Asset,
    isWatched: Boolean,
    onToggleWatch: (Asset) -> Unit,
    onOpenAsset: (Asset) -> Unit,
) {
    val c = Obchodnik.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenAsset(asset) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AssetIcon(symbol = asset.symbol, colorHex = asset.colorHex, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = asset.symbol,
                    color = c.text,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Text(
                    text = asset.type.label,
                    color = c.text3,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
            Text(
                text = asset.name,
                color = c.text3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        IconButton(onClick = { onToggleWatch(asset) }) {
            Icon(
                imageVector = if (isWatched) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = if (isWatched) "Odebrat z watchlistu" else "Přidat do watchlistu",
                tint = if (isWatched) c.accent else c.text3,
            )
        }
    }
}

@Composable
private fun EmptySearchState(
    query: String,
    modifier: Modifier = Modifier,
) {
    val c = Obchodnik.colors
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = c.text3, modifier = Modifier.size(42.dp))
            Spacer(Modifier.height(14.dp))
            Text(text = "Nic nenalezeno", color = c.text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(text = "Pro „$query“ nejsou žádné výsledky.", color = c.text3, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

private val AssetType.label: String
    get() = when (this) {
        AssetType.CRYPTO -> "Krypto"
        AssetType.METAL -> "Kovy"
        AssetType.COMMODITY -> "Komodita"
        AssetType.INDEX -> "Index"
    }
