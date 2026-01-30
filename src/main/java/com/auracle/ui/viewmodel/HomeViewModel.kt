package com.auracle.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.auracle.data.Audiobook
import com.auracle.data.AudiobookRepository
import com.auracle.data.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AudiobookRepository(application)
    private val preferenceManager = PreferenceManager(application)
    
    fun getBookProgress(id: String) = preferenceManager.getBookProgress(id)
    
    private val _audiobooks = MutableStateFlow<List<Audiobook>>(emptyList())
    val audiobooks: StateFlow<List<Audiobook>> = _audiobooks.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun loadAudiobooks(folderUri: Uri, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (forceRefresh) {
                _isRefreshing.value = true
            } else {
                _isLoading.value = _audiobooks.value.isEmpty()
            }
            
            repository.getAudiobooks(folderUri, forceRefresh).collect { list ->
                _audiobooks.value = list
                _isLoading.value = false
                _isRefreshing.value = false
            }
        }
    }
}
