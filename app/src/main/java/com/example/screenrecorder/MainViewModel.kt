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

    private val _isTakingScreenShot : MutableState<Boolean?> = mutableStateOf(null)
    val isTakingScreenShot : State<Boolean?> = _isTakingScreenShot

    fun setRecordingStatus(status : Boolean){
        _isRecordingMutableState.value = status
    }

    fun setScreenShotStatus(status: Boolean){
        _isTakingScreenShot.value = status
    }
}