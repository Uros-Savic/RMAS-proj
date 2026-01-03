package com.example.rmas_uross.worker

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.example.rmas_uross.data.repository.LocationRepository
import com.google.android.gms.location.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var userId: String? = null
    private lateinit var locationRepository: LocationRepository

    companion object {
        const val EXTRA_USER_ID = "user_id"
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"

        fun startService(context: Context, userId: String) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_USER_ID, userId)
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRepository = LocationRepository(FirebaseFirestore.getInstance())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                userId = intent.getStringExtra(EXTRA_USER_ID)
                // Proveri da li je lokacija uključena pre nego što pokreneš
                if (isLocationEnabled()) {
                    startLocationUpdates()
                } else {
                    // Ako lokacija nije uključena, zaustavi servis
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                stopLocationUpdates()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000L
        ).apply {
            setMinUpdateIntervalMillis(5000L)
            setWaitForAccurateLocation(false)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    handleLocationUpdate(location)
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun handleLocationUpdate(location: Location) {
        serviceScope.launch {
            userId?.let { uid ->
                try {
                    locationRepository.saveUserLocation(
                        userId = uid,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun stopLocationUpdates() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}