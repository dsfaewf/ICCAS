package com.example.testfolder.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class FirebaseViewModel : ViewModel() {
    private val _OX_table_deleted = MutableLiveData<Boolean>()
    private val _MCQ_table_deleted = MutableLiveData<Boolean>()
    private val _blank_table_deleted = MutableLiveData<Boolean>()
    private val _hint_table_deleted = MutableLiveData<Boolean>()
    private val _allQuizSaved = MutableLiveData<Boolean>()
    val OX_table_deleted: LiveData<Boolean> get() = _OX_table_deleted
    val MCQ_table_deleted: LiveData<Boolean> get() = _MCQ_table_deleted
    val blank_table_deleted: LiveData<Boolean> get() = _blank_table_deleted
    val hint_table_deleted: LiveData<Boolean> get() = _hint_table_deleted
    val allQuizSaved: LiveData<Boolean> get() = _allQuizSaved

    fun set_OX_table_deleted(value: Boolean) {
        // setValue()를 사용하여 값 셋팅
        _OX_table_deleted.postValue(value)
    }

    fun set_MCQ_table_deleted(value: Boolean) {
        // setValue()를 사용하여 값 셋팅
        _MCQ_table_deleted.postValue(value)
    }

    fun set_blank_table_deleted(value: Boolean) {
        // setValue()를 사용하여 값 셋팅
        _blank_table_deleted.postValue(value)
    }

    fun set_hint_table_deleted(value: Boolean) {
        // setValue()를 사용하여 값 셋팅
        _hint_table_deleted.postValue(value)
    }

    fun setAllQuizSaved(value: Boolean) {
        // setValue()를 사용하여 값 셋팅
        _allQuizSaved.postValue(value)
    }
}