package com.bnyro.contacts.presentation.screens.calllog

import android.annotation.SuppressLint
import android.os.Build
import android.provider.BlockedNumberContract
import android.text.format.DateUtils
import android.view.SoundEffectConstants
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.rounded.FrontHand
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bnyro.contacts.R
import com.bnyro.contacts.domain.model.CallLogEntry
import com.bnyro.contacts.presentation.components.ClickableIcon
import com.bnyro.contacts.presentation.components.NothingHere
import com.bnyro.contacts.presentation.features.ConfirmationDialog
import com.bnyro.contacts.presentation.screens.calllog.components.NumberInput
import com.bnyro.contacts.presentation.screens.calllog.components.PhoneNumberDisplay
import com.bnyro.contacts.presentation.screens.calllog.model.CallModel
import com.bnyro.contacts.presentation.screens.contacts.model.ContactsModel
import com.bnyro.contacts.presentation.screens.editor.components.ContactIconPlaceholder
import com.bnyro.contacts.presentation.screens.settings.model.ThemeModel
import com.bnyro.contacts.util.IntentHelper


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CallLogsScreen(
    contactsModel: ContactsModel,
    themeModel: ThemeModel
) {
    val callModel: CallModel = viewModel()
    val context = LocalContext.current

    var showNumberPicker by remember {
        mutableStateOf(callModel.initialPhoneNumber != null)
    }

    var selectedCallLog by remember {
        mutableStateOf<CallLogEntry?>(null)
    }

    val callLog = callModel.callLogs
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNumberPicker = true }
            ) {
                Icon(Icons.Default.Dialpad, contentDescription = null)
            }
        }, topBar = {
            TopAppBar(title = {
                Text(text = stringResource(id = R.string.recent_calls))
            }, actions = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Box {
                        var showMore by remember { mutableStateOf(false) }
                        ClickableIcon(
                            icon = Icons.Rounded.MoreVert,
                            contentDescription = R.string.more
                        ) {
                            showMore = !showMore
                        }
                        DropdownMenu(
                            expanded = showMore,
                            onDismissRequest = {
                                showMore = false
                            }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.block_numbers)
                                    )
                                },
                                onClick = {
                                    IntentHelper.openBlockedNumberManager(context)
                                }
                            )
                        }
                    }
                }
            })
        }
    ) { pV ->
        if (callLog.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.padding(pV)
            ) {
                items(callLog) {
                    val contact = remember {
                        contactsModel.getContactByNumber(it.phoneNumber)
                    }
                    var showCallDialog by remember {
                        mutableStateOf(false)
                    }
                    val shape = RoundedCornerShape(20.dp)

                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .combinedClickable(onClick = {
                                if (it.phoneNumber.isNotBlank()) showCallDialog = true
                            }, onLongClick = {
                                selectedCallLog = it
                            }),
                        shape = shape
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ContactIconPlaceholder(
                                themeModel = themeModel,
                                firstChar = contact?.displayName?.firstOrNull()
                            )

                            Spacer(modifier = Modifier.width(20.dp))

                            Column {
                                Text(text = contact?.displayName ?: it.phoneNumber, maxLines = 1)
                                Text(
                                    text = DateUtils.getRelativeTimeSpanString(it.time)?.toString()
                                        .orEmpty()
                                )
                            }
                        }
                    }

                    if (showCallDialog) {
                        ConfirmationDialog(
                            onDismissRequest = { showCallDialog = false },
                            title = stringResource(id = R.string.dial),
                            text = stringResource(
                                id = R.string.confirm_start_call,
                                contact?.displayName ?: it.phoneNumber
                            )
                        ) {
                            callModel.callNumber(it.phoneNumber)
                        }
                    }
                }
            }
        } else {
            NothingHere()
        }
    }

    if (showNumberPicker) {
        val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(onDismissRequest = { showNumberPicker = false }, sheetState = state) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val contacts by callModel.contacts.collectAsState()
                PhoneNumberDisplay(
                    displayText = callModel.numberToCall,
                    contacts = contacts,
                    onClickContact = callModel::setPhoneNumberContact
                )
                NumberInput(
                    onNumberInput = callModel::onNumberInput,
                    onDelete = callModel::onBackSpace,
                    onDial = {
                        callModel.callNumber()
                    },
                    subscriptions = callModel.subscriptions,
                    onSubscriptionIndexChange = callModel::onSubscriptionIndexChange
                )
            }
        }
    }

    selectedCallLog?.let {
        CallLogOptionsSheet(
            onDismissRequest = { selectedCallLog = null },
            log = it
        )
    }

    LaunchedEffect(Unit) {
        callModel.requestDefaultDialerApp(context)
    }
}

@SuppressLint("NewApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallLogOptionsSheet(
    onDismissRequest: () -> Unit,
    log: CallLogEntry
) {
    val songSettingsSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val context = LocalContext.current
    var isBlocked by remember {
        mutableStateOf<Boolean?>(null)
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (!BlockedNumberContract.canCurrentUserBlockNumbers(context)) return@LaunchedEffect
            isBlocked = BlockedNumberContract.isBlocked(context, log.phoneNumber)
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = songSettingsSheetState,
        windowInsets = WindowInsets.systemBars,
        dragHandle = null
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(id = R.drawable.ic_person),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(8.dp)
            ) {
                Text(
                    log.phoneNumber,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    DateUtils.getRelativeTimeSpanString(log.time)?.toString()
                        .orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Column(
            Modifier
                .padding(horizontal = 8.dp, vertical = 16.dp)
        ) {
            isBlocked?.let { isBlocked ->
                if (isBlocked) {
                    SheetSettingItem(
                        icon = Icons.Rounded.Handshake,
                        description = R.string.unblock_number,
                        onClick = {
                            BlockedNumberContract.unblock(context, log.phoneNumber)
                            onDismissRequest()
                        }
                    )
                } else {
                    SheetSettingItem(
                        icon = Icons.Rounded.FrontHand,
                        description = R.string.block_number,
                        onClick = {
                            IntentHelper.blockNumberOrAddress(context, log.phoneNumber)
                            onDismissRequest()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SheetSettingItem(icon: ImageVector, @StringRes description: Int, onClick: () -> Unit) {
    val view = LocalView.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                view.playSoundEffect(SoundEffectConstants.CLICK)
                onClick()
            }
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(Modifier.width(16.dp))
        Text(text = stringResource(id = description))
    }
}