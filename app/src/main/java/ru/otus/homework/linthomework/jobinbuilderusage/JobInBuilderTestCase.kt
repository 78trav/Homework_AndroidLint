package ru.otus.homework.linthomework.jobinbuilderusage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class JobInBuilderTestCase(
    private val job: Job
) : ViewModel() {

    fun case1() {
        viewModelScope.launch(SupervisorJob() + Dispatchers.IO) {
            delay(1000)
            println("Hello World")
        }
    }

    fun case2() {
        viewModelScope.launch(start = CoroutineStart.DEFAULT, context = Job()) {
            delay(1000)
            println("Hello World")
        }
    }

    fun case3() {
        viewModelScope.launch(job) {
            delay(1000)
            println("Hello World")
        }
    }

    fun case4() {
        viewModelScope.launch(NonCancellable) {
            delay(1000)
            println("Hello World")
        }
    }
}
