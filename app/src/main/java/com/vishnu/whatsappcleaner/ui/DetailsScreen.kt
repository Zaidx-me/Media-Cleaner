package com.zaidxme.whatsappcleaner.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.zaidxme.whatsappcleaner.Constants
import com.zaidxme.whatsappcleaner.MainViewModel
import com.zaidxme.whatsappcleaner.R
import com.zaidxme.whatsappcleaner.Target
import com.zaidxme.whatsappcleaner.model.ListDirectory
import com.zaidxme.whatsappcleaner.model.ListFile
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.text.DateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(navController: NavHostController, viewModel: MainViewModel) {
    val listDirectory = navController.previousBackStackEntry?.savedStateHandle?.get<ListDirectory>(
        Constants.DETAILS_LIST_ITEM
    )

    if (listDirectory == null) return Surface {}

    val coroutineScope = rememberCoroutineScope()

    val fileList by viewModel.fileList.collectAsState()
    val sentList by viewModel.sentList.collectAsState()
    val privateList by viewModel.privateList.collectAsState()
    val receivedStack by viewModel.receivedStack.collectAsState()
    val sentStack by viewModel.sentStack.collectAsState()
    val privateStack by viewModel.privateStack.collectAsState()
    val trashFileList by viewModel.trashFileList.collectAsState()
    val trashSentList by viewModel.trashSentList.collectAsState()
    val trashPrivateList by viewModel.trashPrivateList.collectAsState()
    val isInProgress by viewModel.isInProgress.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val fileReloadTrigger by viewModel.fileReloadTrigger.collectAsState()

    var selectedItems = remember { mutableStateListOf<ListFile>() }
    var sortBy = remember { mutableStateOf("Date") }
    var isSortDescending = remember { mutableStateOf(true) }

    val dateRangePickerState = rememberDateRangePickerState()

    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var isAllSelected by remember { mutableStateOf(false) }
    var showTrashBin by remember { mutableStateOf(false) }

    val tabs = listOf("Received", "Sent", "Private")

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = {
            if (listDirectory.hasSent) {
                if (listDirectory.hasPrivate) 3
                else 2
            } else 1
        }
    )

    val gridStates = remember {
        List(3) { LazyGridState() }
    }
    val showHeader by remember {
        derivedStateOf {
            if (isGridView) {
                val state = gridStates[pagerState.currentPage]
                state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0
            } else {
                true
            }
        }
    }

    LaunchedEffect(
        fileReloadTrigger,
        sortBy.value,
        isSortDescending.value,
        dateRangePickerState.selectedStartDateMillis,
        dateRangePickerState.selectedEndDateMillis
    ) {
        viewModel.getFileList(
            Target.Received,
            listDirectory.path,
            sortBy.value,
            isSortDescending.value,
            dateRangePickerState.selectedStartDateMillis,
            dateRangePickerState.selectedEndDateMillis
        )

        if (listDirectory.hasSent) {
            viewModel.getFileList(
                Target.Sent,
                "${listDirectory.path}/Sent",
                sortBy.value,
                isSortDescending.value,
                dateRangePickerState.selectedStartDateMillis,
                dateRangePickerState.selectedEndDateMillis
            )
        }

        if (listDirectory.hasPrivate) {
            viewModel.getFileList(
                Target.Private,
                "${listDirectory.path}/Private",
                sortBy.value,
                isSortDescending.value,
                dateRangePickerState.selectedStartDateMillis,
                dateRangePickerState.selectedEndDateMillis
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearFileListStates()
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (selectedItems.isNotEmpty() || isAllSelected) {
            selectedItems.clear()
            isAllSelected = false
        }
    }

    LaunchedEffect(isGridView) {
        if (!isGridView) {
            selectedItems.clear()
            isAllSelected = false
        }
    }

    Scaffold(
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        topBar = {
            DetailScreenTopBar(
                title = listDirectory.name,
                toggleGridView = {
                    viewModel.toggleViewType()
                },
                isGridView = isGridView,
                isTrashMode = showTrashBin,
                onTrashToggle = {
                    showTrashBin = !showTrashBin
                },
                onSortClick = {
                    showSortDialog = true
                    dateRangePickerState.setSelection(null, null)
                    selectedItems.clear()
                    isAllSelected = false
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isInProgress) LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(8.dp),
            )

            AnimatedVisibility(
                visible = showHeader,
                enter = fadeIn(animationSpec = tween(200)) +
                    slideInVertically(
                        animationSpec = tween(200),
                        initialOffsetY = { it / 8 }
                    ),
                exit = fadeOut(animationSpec = tween(150)) +
                    slideOutVertically(
                        animationSpec = tween(150),
                        targetOffsetY = { it / 8 }
                    )
            ) {
                Banner(
                    Modifier.padding(16.dp),
                    buildAnnotatedString {
                        val size = listDirectory.size
                        val parts = size.split(" ")
                        if (parts.size == 2) {
                            withStyle(SpanStyle(fontSize = 24.sp)) { append(parts[0]) }
                            withStyle(SpanStyle(fontSize = 18.sp)) { append(" ${parts[1]}") }
                        } else {
                            withStyle(SpanStyle(fontSize = 24.sp)) { append(size) }
                        }
                    }
                )
            }

            if (listDirectory.hasSent || listDirectory.hasPrivate) {
                CustomTabLayout(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    selectedItemIndex = pagerState.currentPage,
                    items = tabs,
                    onTabSelected = { index ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val list = when (page) {
                    0 -> fileList
                    1 -> sentList
                    else -> privateList
                }

                val swipeStack = when (page) {
                    0 -> receivedStack
                    1 -> sentStack
                    else -> privateStack
                }

                val trashList = when (page) {
                    0 -> trashFileList
                    1 -> trashSentList
                    else -> trashPrivateList
                }
                            var showTrashRestoreAllConfirm by remember { mutableStateOf(false) }
                            var showTrashDeleteAllConfirm by remember { mutableStateOf(false) }

                val target = when (page) {
                    0 -> Target.Received
                    1 -> Target.Sent
                    else -> Target.Private
                }

                Column(
                    Modifier
                        .fillMaxSize()
                ) {
                    if (!showTrashBin && isGridView && list.isNotEmpty()) {
                        IconButton(
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(8.dp)
                                .size(32.dp),
                            onClick = {
                                isAllSelected = !isAllSelected
                                if (isAllSelected) selectedItems.addAll(list)
                                else selectedItems.clear()
                            }
                        ) {
                            Icon(
                                modifier = Modifier.size(32.dp),
                                painter = painterResource(id = if (isAllSelected) R.drawable.check_circle_filled else R.drawable.check_circle),
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = "select all"
                            )
                        }
                    }

                    if (showTrashBin && trashList.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = { showTrashRestoreAllConfirm = true }) {
                                Text(text = "Restore All")
                            }

                            TextButton(onClick = { showTrashDeleteAllConfirm = true }) {
                                Text(text = "Delete All")
                            }
                        }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            items(trashList) { trashFile ->
                                TrashFileRow(
                                    file = trashFile,
                                    onRestore = {
                                        viewModel.restoreFromTrash(target, trashFile)
                                    },
                                    onDeletePermanently = {
                                        viewModel.deleteFromTrash(target, trashFile)
                                    }
                                )
                            }
                        }
                        if (showTrashRestoreAllConfirm) {
                            ConfirmationDialog(
                                onDismissRequest = { showTrashRestoreAllConfirm = false },
                                onConfirmation = {
                                    viewModel.restoreAllTrash(target)
                                    showTrashRestoreAllConfirm = false
                                },
                                list = trashList,
                                navController = navController
                            )
                        }

                        if (showTrashDeleteAllConfirm) {
                            ConfirmationDialog(
                                onDismissRequest = { showTrashDeleteAllConfirm = false },
                                onConfirmation = {
                                    viewModel.deleteAllTrash(target)
                                    showTrashDeleteAllConfirm = false
                                },
                                list = trashList,
                                navController = navController
                            )
                        }
                    } else if (!showTrashBin && isGridView && list.isNotEmpty()) {
                        if (isGridView) {
                            LazyVerticalGrid(
                                state = gridStates[page],
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                columns = GridCells.Fixed(3)
                            ) {
                                items(list) {
                                    ItemGridCard(it, navController, selectedItems.contains(it)) {
                                        if (selectedItems.contains(it)) selectedItems.remove(it)
                                        else selectedItems.add(it)
                                    }
                                }
                            }
                        }
                    } else if (!showTrashBin && !isGridView && swipeStack.isNotEmpty()) {
                        SwipeableCardStack(
                            files = swipeStack,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            onKeepTop = {
                                viewModel.keepTopFile(target)
                            },
                            onDeleteTop = {
                                viewModel.deleteTopFile(target)
                            },
                            onOpenFile = {
                                openFile(navController.context, it)
                            }
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                modifier = Modifier
                                    .fillMaxSize(0.4f)
                                    .padding(8.dp),
                                painter = painterResource(id = R.drawable.clean),
                                contentDescription = "empty",
                                tint = MaterialTheme.colorScheme.secondaryContainer
                            )
                            Text(
                                text = if (showTrashBin) "Trash bin is empty" else "Nothing to clean",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (!showTrashBin && isGridView) {
                CleanUpButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    selectedItems = selectedItems,
                    onShowDialog = { showConfirmationDialog = true }
                )
            } else if (!showTrashBin) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    text = "Swipe left to delete and right to keep.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    text = "Restore items or permanently delete them from Trash Bin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showSortDialog) {
        SortDialog(
            navController,
            onDismissRequest = {
                showSortDialog = false
            },
            sortBy,
            isSortDescending,
            dateRangePickerState
        )
    }

    if (showConfirmationDialog) {
        ConfirmationDialog(
            onDismissRequest = {
                showConfirmationDialog = false
                isAllSelected = false
                selectedItems.clear()
            },
            onConfirmation = {
                viewModel.delete(selectedItems.toList())
                showConfirmationDialog = false
                selectedItems.clear()
            },
            selectedItems,
            navController
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreenTopBar(
    modifier: Modifier = Modifier,
    title: String = "",
    toggleGridView: () -> Unit,
    isGridView: Boolean,
    isTrashMode: Boolean,
    onTrashToggle: () -> Unit,
    onSortClick: () -> Unit
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Title(text = title, modifier = Modifier)
        },
        actions = {
            IconButton(
                modifier = Modifier
                    .size(32.dp),
                onClick = {
                    toggleGridView()
                }
            ) {
                Icon(
                    modifier = Modifier
                        .size(32.dp),
                    painter =
                    if (isGridView)
                        painterResource(id = R.drawable.ic_view_list)
                    else
                        painterResource(id = R.drawable.ic_grid_view),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = "grid list view",
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(
                modifier = Modifier
                    .size(32.dp),
                onClick = { onTrashToggle() }
            ) {
                Icon(
                    modifier = Modifier.size(32.dp),
                    painter = painterResource(
                        id = if (isTrashMode) R.drawable.recycling_round else R.drawable.recycling
                    ),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = "trash bin",
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(
                modifier = Modifier
                    .size(32.dp),
                onClick = { onSortClick() }
            ) {
                Icon(
                    modifier = Modifier.size(32.dp),
                    painter = painterResource(id = R.drawable.ic_sort),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = "sort",
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
    )
}

@Composable
fun TrashFileRow(
    file: ListFile,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = file.name.removeSuffix(".trash"),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1
            )

            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = "${file.size} • ${DateFormat.getDateInstance().format(file.lastModified())}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onRestore) {
                    Text(text = "Restore")
                }

                Spacer(modifier = Modifier.width(8.dp))

                TextButton(onClick = onDeletePermanently) {
                    Text(text = "Delete Permanently")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortDialog(
    navController: NavHostController,
    onDismissRequest: () -> Unit,
    sortBy: MutableState<String>,
    isSortDescending: MutableState<Boolean>,
    dateRangePickerState: DateRangePickerState,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
            decorFitsSystemWindows = true
        ),
    ) {
        var isDescending by remember { mutableStateOf(isSortDescending) }
        var selectedItem by remember { mutableStateOf(sortBy) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 64.dp, horizontal = 32.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp),
            ) {
                Text(
                    modifier = Modifier
                        .wrapContentHeight()
                        .padding(8.dp),
                    text = "Sort Criteria",
                    style = MaterialTheme.typography.headlineLarge,
                )

                listOf(
                    "Date",
                    "Size",
                    "Name",
                ).forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedItem.value = item
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = sortBy.value == item,
                            onClick = {
                                selectedItem.value = item
                            },
                            enabled = true,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(text = item, modifier = Modifier.padding(start = 8.dp))
                    }
                }

                if (showDatePicker) DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDatePicker = false
                            }
                        ) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showDatePicker = false
                            }
                        ) { Text("Cancel") }
                    }
                ) {
                    DateRangePicker(state = dateRangePickerState)
                }

                Row(
                    modifier = Modifier
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (selectedItem.value) {
                        "Date" -> {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .pointerInput(Unit) {
                                        awaitEachGesture {
                                            awaitFirstDown(pass = PointerEventPass.Initial)
                                            val upEvent =
                                                waitForUpOrCancellation(pass = PointerEventPass.Initial)
                                            if (upEvent != null) {
                                                showDatePicker = true
                                            }
                                        }
                                    },
                                readOnly = true,
                                value = if (dateRangePickerState.selectedEndDateMillis != null)
                                    DateFormat.getDateInstance()
                                        .format(dateRangePickerState.selectedStartDateMillis)
                                else
                                    "N/A",
                                onValueChange = {},
                                label = { Text("From Date") },
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .pointerInput(Unit) {
                                        awaitEachGesture {
                                            awaitFirstDown(pass = PointerEventPass.Initial)
                                            val upEvent =
                                                waitForUpOrCancellation(pass = PointerEventPass.Initial)
                                            if (upEvent != null) {
                                                showDatePicker = true
                                            }
                                        }
                                    },
                                readOnly = true,
                                value = if (dateRangePickerState.selectedEndDateMillis != null)
                                    DateFormat.getDateInstance()
                                        .format(dateRangePickerState.selectedEndDateMillis)
                                else
                                    "N/A",
                                onValueChange = {},
                                label = { Text("To Date") },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = isDescending.value,
                        onCheckedChange = { isDescending.value = it }
                    )

                    Text(text = "Descending", modifier = Modifier.padding(start = 8.dp))
                }

                TextButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(4.dp),
                    onClick = {
                        sortBy.value = selectedItem.value
                        isSortDescending.value = isDescending.value
                        onDismissRequest()
                    }
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 1.sp
                                )
                            ) {
                                append("Apply")
                            }
                        },
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun ConfirmationDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    list: List<ListFile>,
    navController: NavHostController
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
            decorFitsSystemWindows = true
        ),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 64.dp, horizontal = 32.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.wrapContentHeight(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Column(
                        Modifier
                            .weight(0.6f)
                            .fillMaxWidth()
                    ) {
                        Text(
                            modifier = Modifier
                                .wrapContentHeight()
                                .padding(vertical = 4.dp)
                                .align(Alignment.Start),
                            text = "Confirm Cleanup",
                            style = MaterialTheme.typography.titleLarge,
                        )

                        Text(
                            modifier = Modifier
                                .wrapContentHeight()
                                .padding(vertical = 2.dp)
                                .align(Alignment.Start),
                            text = "The following files will be deleted.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    TextButton(
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 16.dp),
                        onClick = onConfirmation,
                        content = {
                            Text(
                                text = "Confirm",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        },
                    )
                }

                // todo: no preview & replace it with count + red colored CTA
                LazyVerticalGrid(
                    modifier = Modifier
                        .wrapContentHeight(),
                    columns = GridCells.Fixed(3),
                ) {
                    items(list) { ItemGridCard(it, navController, selectionEnabled = false) {} }
                }
            }
        }
    }
}
