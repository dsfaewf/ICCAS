package com.katzheimer.testfolder.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PhotoViewModel : ViewModel() {
    private val _urlLiveData = MutableLiveData<String>()
    val urlLiveData: LiveData<String> get() = _urlLiveData

    fun setUrlLiveData(url: String) {
        _urlLiveData.postValue(url)
    }
}