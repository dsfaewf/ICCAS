package com.example.testfolder.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ApiKeyViewModel : ViewModel() {
    private val _apiKey = MutableLiveData<String>()
    val apiKey: LiveData<String> get() = _apiKey

    fun setApiKey(key: String) {
        // setValue()를 사용하여 값 셋팅
        _apiKey.value = key
    }
}