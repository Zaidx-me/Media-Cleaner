package com.zaidxme.whatsappcleaner

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zaidxme.whatsappcleaner.data.FileRepository
import com.zaidxme.whatsappcleaner.data.StoreData
import com.zaidxme.whatsappcleaner.model.ListDirectory
import com.zaidxme.whatsappcleaner.model.ListFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

class MainViewModel(private val application: Application) : AndroidViewModel(application) {
    private val _fileList = MutableStateFlow<List<ListFile>>(emptyList())
    val fileList: StateFlow<List<ListFile>> = _fileList.asStateFlow()

    private val _receivedStack = MutableStateFlow<List<ListFile>>(emptyList())
    val receivedStack: StateFlow<List<ListFile>> = _receivedStack.asStateFlow()

    private val _trashFileList = MutableStateFlow<List<ListFile>>(emptyList())
    val trashFileList: StateFlow<List<ListFile>> = _trashFileList.asStateFlow()

    private val _sentList = MutableStateFlow<List<ListFile>>(emptyList())
    val sentList: StateFlow<List<ListFile>> = _sentList.asStateFlow()

    private val _sentStack = MutableStateFlow<List<ListFile>>(emptyList())
    val sentStack: StateFlow<List<ListFile>> = _sentStack.asStateFlow()

    private val _trashSentList = MutableStateFlow<List<ListFile>>(emptyList())
    val trashSentList: StateFlow<List<ListFile>> = _trashSentList.asStateFlow()

    private val _privateList = MutableStateFlow<List<ListFile>>(emptyList())
    val privateList: StateFlow<List<ListFile>> = _privateList.asStateFlow()

    private val _privateStack = MutableStateFlow<List<ListFile>>(emptyList())
    val privateStack: StateFlow<List<ListFile>> = _privateStack.asStateFlow()

    private val _trashPrivateList = MutableStateFlow<List<ListFile>>(emptyList())
    val trashPrivateList: StateFlow<List<ListFile>> = _trashPrivateList.asStateFlow()

    private val _isInProgress = MutableStateFlow(false)
    val isInProgress: StateFlow<Boolean> = _isInProgress.asStateFlow()

    private val _directories = MutableStateFlow<List<String>>(emptyList())
    val directories: StateFlow<List<String>> = _directories

    private val _homeUri = MutableStateFlow<String?>("")
    val homeUri: StateFlow<String?> = _homeUri.asStateFlow()

    private val _fileReloadTrigger = MutableStateFlow(false)
    val fileReloadTrigger: StateFlow<Boolean> = _fileReloadTrigger.asStateFlow()

    private val storeData = StoreData(application.applicationContext)

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _directoryItem =
        MutableStateFlow<ViewState<Pair<String, List<ListDirectory>>>>(ViewState.Loading)
    val directoryItem: StateFlow<ViewState<Pair<String, List<ListDirectory>>>> =
        _directoryItem.asStateFlow()

    fun setHomeUri(homePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            storeData.set(
                Constants.WHATSAPP_HOME_URI,
                homePath,
            )
        }
    }

    fun toggleViewType() {
        _isGridView.value = !_isGridView.value
    }

    fun getHomeUri() {
        viewModelScope.launch(Dispatchers.Default) {
            _homeUri.value = storeData.get(Constants.WHATSAPP_HOME_URI)
        }
    }

    fun getDirectoryList() {
        Log.i("vishnu", "getDirectoryList() called")

        viewModelScope.launch(Dispatchers.Default) {
            storeData.get(Constants.WHATSAPP_HOME_URI)
                ?.let { homeUri ->
                    val pair = FileRepository.getDirectoryList(
                        application,
                        homeUri
                    )
                    Log.e("vishnu", "getDirectoryList: $pair")
                    _directoryItem.value = ViewState.Success(pair)
                }
        }
    }

    fun getFileList(
        target: Target,
        path: String,
        sortBy: String,
        isSortDescending: Boolean,
        filterStartDate: Long?,
        filterEndDate: Long?
    ) {
        Log.i("vishnu", "getFileList: $path")

        _isInProgress.value = true
        viewModelScope.launch(Dispatchers.Default) {
            val fileList = FileRepository.getFileList(application, path)
            val trashList = FileRepository.getTrashFileList(application, path)
            _isInProgress.value = false

            fileList.sortWith(
                when {
                    sortBy.contains("Name") -> compareBy { it.name }
                    sortBy.contains("Size") -> compareBy { it.length() }
                    else -> compareBy { it.lastModified() }
                }
            )

            if (
                sortBy.contains("Date") &&
                filterStartDate != null &&
                filterEndDate != null
            ) {
                val filteredList = fileList.filter {
                    val lastModified = Date(it.lastModified())
                    lastModified.after(Date(filterStartDate)) &&
                        lastModified.before(
                            Date(
                                filterEndDate
                            )
                        )
                }
                fileList.clear()
                fileList.addAll(filteredList)
            }

            if (isSortDescending) fileList.reverse()

            trashList.sortWith(compareByDescending { it.lastModified() })

            when (target) {
                Target.Received -> {
                    _fileList.value = fileList
                    _receivedStack.value = fileList
                    _trashFileList.value = trashList
                }

                Target.Sent -> {
                    _sentList.value = fileList
                    _sentStack.value = fileList
                    _trashSentList.value = trashList
                }

                Target.Private -> {
                    _privateList.value = fileList
                    _privateStack.value = fileList
                    _trashPrivateList.value = trashList
                }
            }
        }
    }

    fun keepTopFile(target: Target) {
        popTopFile(target)
    }

    fun deleteTopFile(target: Target) {
        val file = popTopFile(target) ?: return

        _isInProgress.value = true
        viewModelScope.launch(Dispatchers.IO) {
            FileRepository.moveToTrashFiles(listOf(file))
            _isInProgress.value = false
            _fileReloadTrigger.value = !_fileReloadTrigger.value
        }
    }

    fun restoreFromTrash(target: Target, file: ListFile) {
        _isInProgress.value = true
        viewModelScope.launch(Dispatchers.IO) {
            FileRepository.restoreFromTrashFiles(listOf(file))
            _isInProgress.value = false
            _fileReloadTrigger.value = !_fileReloadTrigger.value
        }
    }

    fun deleteFromTrash(target: Target, file: ListFile) {
        _isInProgress.value = true
        viewModelScope.launch(Dispatchers.IO) {
            FileRepository.deleteFiles(listOf(file))
            _isInProgress.value = false
            _fileReloadTrigger.value = !_fileReloadTrigger.value
        }
    }

    fun restoreAllTrash(target: Target) {
        val list = when (target) {
            Target.Received -> _trashFileList.value
            Target.Sent -> _trashSentList.value
            Target.Private -> _trashPrivateList.value
        }
        if (list.isEmpty()) return

        _isInProgress.value = true
        viewModelScope.launch(Dispatchers.IO) {
            FileRepository.restoreFromTrashFiles(list)
            _isInProgress.value = false
            _fileReloadTrigger.value = !_fileReloadTrigger.value
        }
    }

    fun deleteAllTrash(target: Target) {
        val list = when (target) {
            Target.Received -> _trashFileList.value
            Target.Sent -> _trashSentList.value
            Target.Private -> _trashPrivateList.value
        }
        if (list.isEmpty()) return

        _isInProgress.value = true
        viewModelScope.launch(Dispatchers.IO) {
            FileRepository.deleteFiles(list)
            _isInProgress.value = false
            _fileReloadTrigger.value = !_fileReloadTrigger.value
        }
    }

    private fun popTopFile(target: Target): ListFile? {
        val currentStack = when (target) {
            Target.Received -> _receivedStack.value
            Target.Sent -> _sentStack.value
            Target.Private -> _privateStack.value
        }

        val topFile = currentStack.firstOrNull() ?: return null

        when (target) {
            Target.Received -> {
                _receivedStack.value = _receivedStack.value.drop(1)
                _fileList.value = _fileList.value.filterNot { it.filePath == topFile.filePath }
            }

            Target.Sent -> {
                _sentStack.value = _sentStack.value.drop(1)
                _sentList.value = _sentList.value.filterNot { it.filePath == topFile.filePath }
            }

            Target.Private -> {
                _privateStack.value = _privateStack.value.drop(1)
                _privateList.value = _privateList.value.filterNot { it.filePath == topFile.filePath }
            }
        }

        return topFile
    }

    fun listDirectories(path: String) {
        Log.i("vishnu", "listDirectories: $path")

        viewModelScope.launch(Dispatchers.Default) {
            val dirList = FileRepository.getDirectoryList(path)
            _directories.value = dirList
        }
    }

    fun delete(fileList: List<ListFile>) {
        Log.i("vishnu", "delete() called with: fileList = $fileList")

        _isInProgress.value = true
        viewModelScope.launch(Dispatchers.IO) {
            FileRepository.deleteFiles(fileList)
            _isInProgress.value = false
            _fileReloadTrigger.value = !_fileReloadTrigger.value
        }
    }

    fun clearFileListStates() {
        _fileList.value = emptyList()
        _sentList.value = emptyList()
        _privateList.value = emptyList()
        _trashFileList.value = emptyList()
        _trashSentList.value = emptyList()
        _trashPrivateList.value = emptyList()
        _receivedStack.value = emptyList()
        _sentStack.value = emptyList()
        _privateStack.value = emptyList()
    }
}

sealed class Target {
    data object Received : Target()
    data object Sent : Target()
    data object Private : Target()
}

sealed class ViewState<out T> {
    data object Loading : ViewState<Nothing>()
    data class Success<T>(val data: T) : ViewState<T>()
    data class Error(val message: String) : ViewState<Nothing>()
}

class MainViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
