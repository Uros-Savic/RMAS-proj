package com.example.rmas_uross.worker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class LocationTrackingViewModel(application: Application) : AndroidViewModel(application) {

    private val workManager = WorkManager.getInstance(application)

    companion object {
        const val LOCATION_WORK_NAME = "location_tracking_work"
    }

    fun startLocationTracking(userId: String, intervalMinutes: Long = 15) {
        val inputData = workDataOf("userId" to userId)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val locationCheckRequest = PeriodicWorkRequestBuilder<LocationCheckWorker>(
            intervalMinutes, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInputData(inputData)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            LOCATION_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            locationCheckRequest
        )
    }
}