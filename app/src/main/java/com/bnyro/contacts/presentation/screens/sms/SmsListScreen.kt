package com.bnyro.contacts.presentation.screens.sms

import android.annotation.SuppressLint
import android.os.Build
import android.provider.BlockedNumberContract
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.FrontHand
import androidx.compose.material.icons.rounded.Handshake
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bnyro.contacts.R
import com.bnyro.contacts.domain.model.ContactData
import com.bnyro.contacts.domain.model.SmsThread
import com.bnyro.contacts.navigation.NavRoutes
import com.bnyro.contacts.presentation.components.ClickableIcon
import com.bnyro.contacts.presentation.components.NothingHere
import com.bnyro.contacts.presentation.components.TopBarMoreMenu
import com.bnyro.contacts.presentation.features.NumberPickerDialog
import com.bnyro.contacts.presentation.screens.calllog.SheetSettingItem
import com.bnyro.contacts.presentation.screens.contacts.model.ContactsModel
import com.bnyro.contacts.presentation.screens.sms.components.SmsSearchScreen
import com.bnyro.contacts.presentation.screens.sms.components.SmsThreadItem
import com.bnyro.contacts.presentation.screens.sms.model.SmsModel
import com.bnyro.contacts.util.IntentHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsListScreen(
    smsModel: SmsModel,
    contactsModel: ContactsModel,
    scrollConnection: NestedScrollConnection?,
    onNavigate: (NavRoutes) -> Unit,
    onClickMessage: (address: String, contactData: ContactData?) -> Unit
) {
    var showNumberPicker by remember {
        mutableStateOf(false)
    }
    var showSearch by remember {
        mutableStateOf(false)
    }

    var selectedThread by remember {
        mutableStateOf<SmsThread?>(null)
    }
    LaunchedEffect(Unit) {
        smsModel.initialAddressAndBody?.let {
            onClickMessage(it.first, null)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.messages))
                },
                actions = {
                    ClickableIcon(
                        icon = Icons.Default.Search,
                        contentDescription = R.string.search
                    ) {
                        showSearch = true
                    }
                    TopBarMoreMenu(options = listOf(
                        stringResource(R.string.settings),
                        stringResource(R.string.about)
                    ),
                        onOptionClick = { index ->
                            when (index) {
                                0 -> {
                                    onNavigate.invoke(NavRoutes.Settings)
                                }

                                1 -> {
                                    onNavigate.invoke(NavRoutes.About)
                                }
                            }
                        })
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showNumberPicker = true
                }
            ) {
                Icon(Icons.Default.Edit, null)
            }
        }) { pv ->
        val smsList by smsModel.smsList.collectAsState()
        if (smsList.isNotEmpty()) {
            val threadList = smsList.groupBy { it.threadId }
                .map { (threadId, smsList) ->
                    val address = smsList.first().address
                    SmsThread(
                        threadId = threadId,
                        contactData = contactsModel.getContactByNumber(address),
                        address = address,
                        smsList = smsList
                    )
                }
                .sortedBy { thread -> thread.smsList.maxOf { it.timestamp } }
                .reversed()

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pv)
                    .let { modifier ->
                        scrollConnection?.let { modifier.nestedScroll(it) } ?: modifier
                    }
            ) {
                items(threadList) { thread ->
                    SmsThreadItem(smsModel, thread, onClick = onClickMessage, onLongClick = {
                        selectedThread = thread
                    })
                }
            }
            if (showSearch) {
                SmsSearchScreen(smsModel, threadList, { showSearch = false }, onClickMessage)
            }
        } else {
            Column(Modifier.padding(pv)) {
                NothingHere()
            }
        }

        if (showNumberPicker) {
            NumberPickerDialog(
                contactsModel,
                onDismissRequest = { showNumberPicker = false },
                onNumberSelect = onClickMessage
            )
        }
    }
    selectedThread?.let { thread ->
        SMSThreadOptionsSheet(onDismissRequest = { selectedThread = null }, thread = thread)
    }
}

@SuppressLint("NewApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SMSThreadOptionsSheet(
    onDismissRequest: () -> Unit,
    thread: SmsThread
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
            isBlocked = BlockedNumberContract.isBlocked(context, thread.address)
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
                if (thread.contactData?.thumbnail != null) {
                    Image(
                        modifier = Modifier
                            .fillMaxSize(),
                        bitmap = thread.contactData.thumbnail!!.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        modifier = Modifier.fillMaxSize(),
                        painter = painterResource(id = R.drawable.ic_person),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(8.dp)
            ) {
                Text(
                    thread.contactData?.displayName ?: thread.address,
                    style = MaterialTheme.typography.titleLarge,
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
                            BlockedNumberContract.unblock(context, thread.address)
                            onDismissRequest()
                        }
                    )
                } else {
                    SheetSettingItem(
                        icon = Icons.Rounded.FrontHand,
                        description = R.string.block_number,
                        onClick = {
                            IntentHelper.blockNumberOrAddress(context, thread.address)
                            onDismissRequest()
                        }
                    )
                }
            }
        }
    }
}