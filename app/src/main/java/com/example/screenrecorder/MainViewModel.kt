package com.example.screenrecorder

import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {

    private val _isRecordingMutableState : MutableState<Boolean> = mutableStateOf(false)
    val isRecordingState : State<Boolean> = _isRecordingMutableState

    fun setRecordingStatus(status : Boolean){
        _isRecordingMutableState.value = status
    }
}