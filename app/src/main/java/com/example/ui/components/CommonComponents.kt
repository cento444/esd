package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.Screen
import com.example.ui.theme.*

@Composable
fun AppTopHeader(
    title: String = "",
    showMenu: Boolean = true,
    showBack: Boolean = false,
    showLogo: Boolean = false,
    onMenuClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (showBack) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (showMenu) {
                        IconButton(
                            onClick = onMenuClick,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menú",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (showLogo) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_agro_work),
                            contentDescription = "Logo Agro Work",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    if (title.isNotEmpty()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 20.sp
                            ),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    content = actions
                )
            }
        }
    }
    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun AppBottomNavBar(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp
    ) {
        Column {
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    label = "Inicio",
                    icon = Icons.Default.Home,
                    selected = activeTab == "inicio",
                    onClick = { onTabSelected("inicio") }
                )
                BottomNavItem(
                    label = "Partes",
                    icon = Icons.Default.Assignment,
                    selected = activeTab == "partes",
                    onClick = { onTabSelected("partes") }
                )
                BottomNavItem(
                    label = "Tareas",
                    icon = Icons.Default.TaskAlt,
                    selected = activeTab == "tareas",
                    onClick = { onTabSelected("tareas") }
                )
                BottomNavItem(
                    label = "Finanzas",
                    icon = Icons.Default.Payments,
                    selected = activeTab == "finanzas",
                    onClick = { onTabSelected("finanzas") }
                )
                BottomNavItem(
                    label = "Socios",
                    icon = Icons.Default.Group,
                    selected = activeTab == "socios",
                    onClick = { onTabSelected("socios") }
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .then(
                if (selected) {
                    Modifier
                        .background(PrimaryGreenDark, shape = RoundedCornerShape(16.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                } else Modifier
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun appOutlinedTextFieldColors(
    containerColor: Color = SurfaceWhite,
    focusedBorderColor: Color = PrimaryGreen,
    unfocusedBorderColor: Color = OutlineVariant
): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = OnSurface,
        unfocusedTextColor = OnSurface,
        disabledTextColor = OnSurface,
        focusedContainerColor = containerColor,
        unfocusedContainerColor = containerColor,
        disabledContainerColor = containerColor,
        focusedBorderColor = focusedBorderColor,
        unfocusedBorderColor = unfocusedBorderColor,
        disabledBorderColor = unfocusedBorderColor.copy(alpha = 0.5f),
        cursorColor = PrimaryGreen,
        focusedLeadingIconColor = OnSurfaceVariant,
        unfocusedLeadingIconColor = OnSurfaceVariant,
        disabledLeadingIconColor = OnSurfaceVariant.copy(alpha = 0.6f),
        focusedTrailingIconColor = OnSurfaceVariant,
        unfocusedTrailingIconColor = OnSurfaceVariant,
        disabledTrailingIconColor = OnSurfaceVariant.copy(alpha = 0.6f),
        focusedPlaceholderColor = OnSurfaceVariant.copy(alpha = 0.6f),
        unfocusedPlaceholderColor = OnSurfaceVariant.copy(alpha = 0.6f),
        disabledPlaceholderColor = OnSurfaceVariant.copy(alpha = 0.4f),
        focusedLabelColor = PrimaryGreen,
        unfocusedLabelColor = OnSurfaceVariant
    )
}

