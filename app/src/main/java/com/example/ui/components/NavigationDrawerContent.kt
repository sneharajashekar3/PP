package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ConversationEntity
import com.example.ui.AppDestination

@Composable
fun PagePilotNavigationDrawerContent(
    currentDestination: AppDestination,
    conversations: List<ConversationEntity>,
    activeConversationId: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNewChat: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onRenameConversation: (String, String) -> Unit,
    onTogglePinConversation: (ConversationEntity) -> Unit,
    onToggleArchiveConversation: (ConversationEntity) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onExportConversation: (String) -> Unit,
    onNavigateTo: (AppDestination) -> Unit,
    onShowMilestone3Dialog: (String) -> Unit
) {
    var showArchivedSection by remember { mutableStateOf(false) }
    var renameDialogTarget by remember { mutableStateOf<ConversationEntity?>(null) }
    var renameText by remember { mutableStateOf("") }

    val filteredConversations = remember(conversations, searchQuery) {
        if (searchQuery.isBlank()) {
            conversations
        } else {
            val q = searchQuery.trim().lowercase()
            conversations.filter {
                it.title.lowercase().contains(q) || it.lastMessagePreview.lowercase().contains(q)
            }
        }
    }

    val pinnedChats = remember(filteredConversations) {
        filteredConversations.filter { it.isPinned && !it.isArchived }
    }
    val recentChats = remember(filteredConversations) {
        filteredConversations.filter { !it.isPinned && !it.isArchived }
    }
    val archivedChats = remember(filteredConversations) {
        filteredConversations.filter { it.isArchived }
    }

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Brand Header + New Chat Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PagePilotLogo(size = 34.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PagePilot",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Offline Custom LLM • 8GB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onNewChat)
                        .testTag("drawer_new_chat_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Chat",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "New Chat",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search Conversations Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = "Search conversations…",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search conversations",
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("drawer_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Scrollable Conversation Groups & System Destinations
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (pinnedChats.isNotEmpty()) {
                    item {
                        DrawerSectionHeader("PINNED CHATS (${pinnedChats.size})")
                    }
                    items(pinnedChats, key = { "pin_${it.id}" }) { conv ->
                        ConversationDrawerRow(
                            conversation = conv,
                            isSelected = currentDestination == AppDestination.HOME_OR_CHAT && activeConversationId == conv.id,
                            onSelect = { onSelectConversation(conv.id) },
                            onRename = {
                                renameDialogTarget = conv
                                renameText = conv.title
                            },
                            onTogglePin = { onTogglePinConversation(conv) },
                            onToggleArchive = { onToggleArchiveConversation(conv) },
                            onExport = { onExportConversation(conv.id) },
                            onDelete = { onDeleteConversation(conv.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(6.dp)) }
                }

                item {
                    DrawerSectionHeader("RECENT CHATS (${recentChats.size})")
                }
                if (recentChats.isEmpty()) {
                    item {
                        Text(
                            text = if (searchQuery.isBlank()) "No recent chats yet. Start a conversation." else "No matching conversations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    items(recentChats, key = { "rec_${it.id}" }) { conv ->
                        ConversationDrawerRow(
                            conversation = conv,
                            isSelected = currentDestination == AppDestination.HOME_OR_CHAT && activeConversationId == conv.id,
                            onSelect = { onSelectConversation(conv.id) },
                            onRename = {
                                renameDialogTarget = conv
                                renameText = conv.title
                            },
                            onTogglePin = { onTogglePinConversation(conv) },
                            onToggleArchive = { onToggleArchiveConversation(conv) },
                            onExport = { onExportConversation(conv.id) },
                            onDelete = { onDeleteConversation(conv.id) }
                        )
                    }
                }

                // Archived Chats Toggle Section
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showArchivedSection = !showArchivedSection }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = "Archived chats",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Archived Chats (${archivedChats.size})",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = if (showArchivedSection) "Hide" else "Show",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (showArchivedSection && archivedChats.isNotEmpty()) {
                    items(archivedChats, key = { "arc_${it.id}" }) { conv ->
                        ConversationDrawerRow(
                            conversation = conv,
                            isSelected = currentDestination == AppDestination.HOME_OR_CHAT && activeConversationId == conv.id,
                            onSelect = { onSelectConversation(conv.id) },
                            onRename = {
                                renameDialogTarget = conv
                                renameText = conv.title
                            },
                            onTogglePin = { onTogglePinConversation(conv) },
                            onToggleArchive = { onToggleArchiveConversation(conv) },
                            onExport = { onExportConversation(conv.id) },
                            onDelete = { onDeleteConversation(conv.id) }
                        )
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DrawerSectionHeader("MODULAR WORKSPACE (MILESTONE 3)")
                }

                item {
                    DrawerMilestonePreviewItem(
                        icon = Icons.Default.FolderSpecial,
                        label = "Projects",
                        badge = "Milestone 3",
                        onClick = {
                            onShowMilestone3Dialog(
                                "Projects Architecture Prepared: Domain contract (ProjectWorkspaceContract) and Room foreign-key hooks are in place for Milestone 3."
                            )
                        }
                    )
                }

                item {
                    DrawerMilestonePreviewItem(
                        icon = Icons.Default.Psychology,
                        label = "User Memory",
                        badge = "Milestone 3",
                        onClick = {
                            onShowMilestone3Dialog(
                                "User Memory Architecture Prepared: Domain contract (UserMemoryContract) is defined for Milestone 3 local preference injection."
                            )
                        }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            // Primary System Navigation Items
            NavigationDrawerItem(
                label = { Text("Model Manager", style = MaterialTheme.typography.titleSmall) },
                icon = { Icon(Icons.Default.Memory, contentDescription = "Model Manager") },
                selected = currentDestination == AppDestination.MODEL_MANAGER,
                onClick = { onNavigateTo(AppDestination.MODEL_MANAGER) },
                modifier = Modifier.testTag("nav_model_manager"),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            NavigationDrawerItem(
                label = { Text("Model Architecture & Math", style = MaterialTheme.typography.titleSmall) },
                icon = { Icon(Icons.Default.Science, contentDescription = "Architecture & Math") },
                selected = currentDestination == AppDestination.ARCHITECTURE_SPEC,
                onClick = { onNavigateTo(AppDestination.ARCHITECTURE_SPEC) },
                modifier = Modifier.testTag("nav_architecture_spec"),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            NavigationDrawerItem(
                label = { Text("Settings", style = MaterialTheme.typography.titleSmall) },
                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                selected = currentDestination == AppDestination.SETTINGS,
                onClick = { onNavigateTo(AppDestination.SETTINGS) },
                modifier = Modifier.testTag("nav_settings"),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            NavigationDrawerItem(
                label = { Text("About & Milestones", style = MaterialTheme.typography.titleSmall) },
                icon = { Icon(Icons.Default.Info, contentDescription = "About") },
                selected = currentDestination == AppDestination.ABOUT,
                onClick = { onNavigateTo(AppDestination.ABOUT) },
                modifier = Modifier.testTag("nav_about"),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }

    if (renameDialogTarget != null) {
        val target = renameDialogTarget!!
        AlertDialog(
            onDismissRequest = { renameDialogTarget = null },
            title = { Text("Rename Conversation") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text("Conversation Title") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRenameConversation(target.id, renameText)
                        renameDialogTarget = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameDialogTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DrawerSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

@Composable
private fun DrawerMilestonePreviewItem(
    icon: ImageVector,
    label: String,
    badge: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun ConversationDrawerRow(
    conversation: ConversationEntity,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleArchive: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onSelect)
            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (conversation.isPinned) Icons.Default.PushPin else Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (conversation.lastMessagePreview.isNotBlank()) {
                Text(
                    text = conversation.lastMessagePreview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Conversation actions",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Rename") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onRename()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (conversation.isPinned) "Unpin" else "Pin") },
                    leadingIcon = { Icon(Icons.Outlined.PushPin, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onTogglePin()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (conversation.isArchived) "Unarchive" else "Archive") },
                    leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onToggleArchive()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Export Markdown") },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onExport()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}
