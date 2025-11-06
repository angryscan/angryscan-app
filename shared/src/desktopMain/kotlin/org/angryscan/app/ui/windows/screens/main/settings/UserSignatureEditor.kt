package org.angryscan.app.ui.windows.screens.main.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberDialogState
import org.jetbrains.compose.resources.stringResource
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.Matcher_UserSignature_Name
import org.angryscan.app.resources.Matcher_UserSignature_Signature
import org.angryscan.app.resources.Matcher_UserSignature_Title
import org.angryscan.common.matchers.UserSignature
import org.angryscan.app.ui.windows.components.DesktopWindowShapes
import org.angryscan.app.ui.windows.components.TitleBar

@Composable
fun UserSignatureEditor(
    userSignature: UserSignature? = null,
    onCloseRequest: () -> Unit,
    onSaveRequest: (UserSignature) -> Unit
) {
    val state = rememberDialogState(
        width = 450.dp,
        height = 500.dp
    )

    var name by remember { mutableStateOf(userSignature?.name ?: "") }
    var signature by remember { mutableStateOf("") }

    val signaturesList = remember { userSignature?.searchSignatures?.toMutableStateList() ?: mutableStateListOf() }

    DialogWindow(
        onCloseRequest = onCloseRequest,
        state = state,
        transparent = true,
        undecorated = true,
        resizable = false
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .clip(DesktopWindowShapes())
                .border(
                    width = 1.dp,
                    shape = DesktopWindowShapes(),
                    color = MaterialTheme.colorScheme.primary
                ),
            floatingActionButton = {
                if (name.isNotEmpty() && signaturesList.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = {
                            onSaveRequest(
                                UserSignature(
                                    name = name,
                                    searchSignatures = signaturesList
                                )
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = if(userSignature != null) Icons.Outlined.Save else Icons.Outlined.AddCircle,
                            contentDescription = null,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TitleBar(
                    windowPlacement = WindowPlacement.Floating
                ) {
                    Box(
                        modifier = Modifier
                            .height(54.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                            .padding(top = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(Res.string.Matcher_UserSignature_Title),
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = onCloseRequest
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                        },
                        placeholder = { Text(text = stringResource(Res.string.Matcher_UserSignature_Name)) },
                        modifier = Modifier.width(350.dp),
                        enabled = userSignature == null
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = signature,
                            onValueChange = { signature = it },
                            placeholder = { Text(text = stringResource(Res.string.Matcher_UserSignature_Signature)) },
                            modifier = Modifier.width(350.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surface.copy(0.5f))
                                .clickable {
                                    if (!signaturesList.contains(signature))
                                        signaturesList.add(signature)
                                    signature = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AddCircle,
                                contentDescription = null
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        val scrollState = rememberLazyListState()
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .animateContentSize()
                                .padding(end = if(scrollState.canScrollForward || scrollState.canScrollBackward) 30.dp else 0.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            state = scrollState
                        ) {
                            items(signaturesList) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.small)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = it)
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(MaterialTheme.shapes.medium)
                                            .clickable {
                                                signaturesList.remove(it)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                        }

                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(scrollState),
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .width(10.dp)
                                .align(Alignment.CenterEnd),
                            style = LocalScrollbarStyle.current.copy(
                                hoverColor = MaterialTheme.colorScheme.primary,
                                unhoverColor = MaterialTheme.colorScheme.secondary
                            )
                        )
                    }
                }
            }
        }
    }
}
