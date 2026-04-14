package com.open.kahf

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DnsStatusCard(viewModel: MainViewModel) {
    val isDnsActive by viewModel.isDnsActive.collectAsState()
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDnsActive) Color(0xFFF0F9F0) else Color(0xFFFFF5F5)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (isDnsActive) Color(0xFF4CAF50) else Color(0xFFF44336))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "DNS Status: ${if (isDnsActive) "Active" else "Inactive"}",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1C1E),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.checkDnsStatus(context) }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF4A2C7E))
            }
        }
    }
}

@Composable
fun BlocklistChecker(viewModel: MainViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val blocklistResult by viewModel.blocklistResult.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    Column {
        Text(
            text = "Blocklist Checker",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1C1E)
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search URL to check...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF1F3F4),
                unfocusedContainerColor = Color(0xFFF1F3F4),
                disabledContainerColor = Color(0xFFF1F3F4),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.searchHost() })
        )

        if (isSearching) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
        } else if (blocklistResult != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .animateContentSize(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (blocklistResult == true) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                )
            ) {
                Text(
                    text = if (blocklistResult == true) "Blocked" else "Accessible",
                    modifier = Modifier.padding(16.dp),
                    color = if (blocklistResult == true) Color.Red else Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
