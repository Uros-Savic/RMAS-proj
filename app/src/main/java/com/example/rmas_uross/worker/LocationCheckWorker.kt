package com.example.rmas_uross.worker

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.annotation.RequiresPermission
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.rmas_uross.data.repository.LocationRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class LocationCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRepository: LocationRepository

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override suspend fun doWork(): Result {
        try {
            if (!isLocationEnabled()) {
                return Result.failure()
            }

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
            locationRepository = LocationRepository(FirebaseFirestore.getInstance())

            val userId = inputData.getString("userId") ?: return Result.failure()

            val location = getCurrentLocation()
            location?.let {
                locationRepository.saveUserLocation(
                    userId = userId,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    accuracy = it.accuracy
                )
            }

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private suspend fun getCurrentLocation(): Location? {
        return try {
            fusedLocationClient.lastLocation.await()
        } catch (e: Exception) {
            null
        }
    }
}