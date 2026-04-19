package com.waju.factory.digitalnote.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waju.factory.digitalnote.ui.theme.TextSecondary

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    var syncEnabled by rememberSaveable { mutableStateOf(true) }
    var pressure by rememberSaveable { mutableStateOf(0.85f) }
    var palmRejection by rememberSaveable { mutableStateOf(false) }
    var darkMode by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionTitle("ACCOUNT") }
        item {
            SettingItemCard(
                title = "Profile Information",
                subtitle = "Update your name, email and avatar"
            )
        }
        item {
            SettingItemCard(
                title = "Password & Security",
                subtitle = "Two-factor authentication and history"
            )
        }

        item { SectionTitle("SYNC") }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cloud Synchronization", fontWeight = FontWeight.SemiBold)
                        Text("Keep all your notes across devices", color = TextSecondary)
                    }
                    Switch(checked = syncEnabled, onCheckedChange = { syncEnabled = it })
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Pressure Sensitivity", modifier = Modifier.weight(1f))
                        Text("${(pressure * 100).toInt()}%", color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(value = pressure, onValueChange = { pressure = it })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Palm Rejection", modifier = Modifier.weight(1f))
                        Switch(checked = palmRejection, onCheckedChange = { palmRejection = it })
                    }
                }
            }
        }

        item { SectionTitle("APPEARANCE") }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppearanceCard(
                    title = "Light Mode",
                    selected = !darkMode,
                    modifier = Modifier.weight(1f),
                    onClick = { darkMode = false }
                )
                AppearanceCard(
                    title = "Dark Mode",
                    selected = darkMode,
                    modifier = Modifier.weight(1f),
                    onClick = { darkMode = true }
                )
            }
        }

        item {
            OutlinedTextField(
                value = "Sign Out",
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                readOnly = true,
                singleLine = true
            )
        }

        item {
            Text(
                text = "Version 2.4.0 (2024)",
                color = TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = TextSecondary,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp)
    )
}

@Composable
private fun SettingItemCard(title: String, subtitle: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = TextSecondary)
            }
            Icon(Icons.Outlined.MoreVert, contentDescription = null, tint = TextSecondary)
        }
    }
}

@Composable
private fun AppearanceCard(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .background(if (title == "Dark Mode") Color(0xFF0B1734) else Color(0xFFE5E7EB))
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(title)
        }
    }
}

