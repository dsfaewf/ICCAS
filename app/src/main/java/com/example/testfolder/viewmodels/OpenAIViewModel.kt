package com.example.testfolder.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class OpenAIViewModel : ViewModel() {
    private val _apiKey = MutableLiveData<String>()
    val apiKey: LiveData<String> get() = _apiKey
    private val _gotResponseForBlankLiveData = MutableLiveData<Boolean>()
    val gotResponseForBlankLiveData: LiveData<Boolean> get() = _gotResponseForBlankLiveData

    fun setApiKey(key: String) {
        // setValue()를 사용하여 값 셋팅
        _apiKey.value = key
    }

    fun setGotResponseForBlankLiveData(gotResponse: Boolean) {
        // setValue()를 사용하여 값 셋팅
        _gotResponseForBlankLiveData.postValue(gotResponse)
    }
}