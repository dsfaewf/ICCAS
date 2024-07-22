package com.katzheimer.testfolder.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DiaryWriteViewModel : ViewModel() {
    private val _liveDataDate = MutableLiveData<String>()
    val liveDataDate: LiveData<String> get() = _liveDataDate
    private val _buttonClickEvent = MutableLiveData<Unit>()
    val buttonClickEvent: LiveData<Unit> get() = _buttonClickEvent

    fun setDate(date: String) {
        // setValue()를 사용하여 값 셋팅
        _liveDataDate.value = date
    }

    fun onButtonClick() {
        _buttonClickEvent.value = Unit
    }
}