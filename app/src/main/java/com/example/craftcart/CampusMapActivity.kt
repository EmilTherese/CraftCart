package com.example.craftcart

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.os.Looper
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.example.craftcart.network.NetworkUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

class CampusMapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private var maplibreMap: MapLibreMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userMarker: Marker? = null
    private var mapProgressBar: ProgressBar? = null
    private var locationCancellationTokenSource: CancellationTokenSource? = null

    companion object {
        private const val TAG = "CampusMapActivity"

        // OpenFreeMap Liberty Style URL
        const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

        // Verified FCRIT Vashi Coordinates (Agnel Technical Education Complex, Sector 9A, Vashi)
        const val FCRIT_LAT = 19.075551
        const val FCRIT_LNG = 72.991490
        const val CAMPUS_NAME = "Fr. C. Rodrigues Institute of Technology (FCRIT)"
        const val CAMPUS_ADDRESS = "Agnel Technical Education Complex, Sector 9A, Vashi, Navi Mumbai, Maharashtra 400703"
    }

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            moveToUserLocation()
        } else {
            Toast.makeText(
                this,
                R.string.campus_location_permission_denied,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize MapLibre Native SDK
        MapLibre.getInstance(this)

        setContentView(R.layout.activity_campus_map)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mapProgressBar = findViewById(R.id.mapProgressBar)

        // Back button
        findViewById<TextView>(R.id.btnBack)?.setOnClickListener {
            finish()
        }

        // Check network availability
        val offlineBanner = findViewById<TextView>(R.id.offlineBanner)
        if (!NetworkUtils.isNetworkAvailable(this)) {
            offlineBanner?.visibility = View.VISIBLE
            mapProgressBar?.visibility = View.GONE
            Toast.makeText(this, "No internet connection. Map tiles may not load.", Toast.LENGTH_LONG).show()
        } else {
            offlineBanner?.visibility = View.GONE
        }

        // Initialize MapView
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)

        mapView.addOnDidFinishLoadingStyleListener {
            Log.d(TAG, "Map style loaded successfully")
            mapProgressBar?.visibility = View.GONE
        }

        mapView.addOnDidFailLoadingMapListener { errorMessage ->
            Log.e(TAG, "Map failed to load: $errorMessage")
            mapProgressBar?.visibility = View.GONE
            if (NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, R.string.campus_map_load_failed, Toast.LENGTH_SHORT).show()
            }
        }

        mapView.getMapAsync { map ->
            maplibreMap = map
            val fcritLatLng = LatLng(FCRIT_LAT, FCRIT_LNG)

            // Set initial camera position immediately
            map.cameraPosition = CameraPosition.Builder()
                .target(fcritLatLng)
                .zoom(15.5)
                .build()

            map.setStyle(STYLE_URL) {
                // Ensure camera is moved/centered on FCRIT when the style loads
                map.moveCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(fcritLatLng)
                            .zoom(15.5)
                            .build()
                    )
                )

                // Add FCRIT Campus Marker on style loaded
                map.addMarker(
                    MarkerOptions()
                        .position(fcritLatLng)
                        .title(CAMPUS_NAME)
                        .snippet(CAMPUS_ADDRESS)
                )
            }
        }

        // My Location button click listener
        findViewById<Button>(R.id.btnMyLocation)?.setOnClickListener {
            if (hasLocationPermission()) {
                moveToUserLocation()
            } else {
                requestLocationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }

        // Get Directions button click listener
        findViewById<Button>(R.id.btnGetDirections)?.setOnClickListener {
            openCampusDirections()
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    private var singleLocationCallback: LocationCallback? = null

    private fun isLocationServiceEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        val isGloballyEnabled = LocationManagerCompat.isLocationEnabled(locationManager)
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return isGloballyEnabled || isGpsEnabled || isNetworkEnabled
    }

    @SuppressLint("MissingPermission")
    private fun moveToUserLocation() {
        if (!hasLocationPermission()) {
            requestLocationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        if (!isLocationServiceEnabled()) {
            Toast.makeText(
                this,
                R.string.campus_location_services_disabled,
                Toast.LENGTH_LONG
            ).show()
            return
        }

        Toast.makeText(this, R.string.campus_fetching_location, Toast.LENGTH_SHORT).show()

        // Attempt 1: Try fresh current location with cancellation token
        locationCancellationTokenSource?.cancel()
        val cts = CancellationTokenSource()
        locationCancellationTokenSource = cts

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cts.token
        ).addOnSuccessListener { currentLocation ->
            if (currentLocation != null) {
                applyUserLocationToMap(currentLocation)
            } else {
                // Attempt 2: Try lastLocation
                fallbackToLastLocation()
            }
        }.addOnFailureListener { exception ->
            Log.w(TAG, "getCurrentLocation failed, trying lastLocation fallback: ${exception.message}")
            fallbackToLastLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun fallbackToLastLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
            if (lastLocation != null) {
                applyUserLocationToMap(lastLocation)
            } else {
                // Attempt 3: Request a single active update (especially reliable in emulator)
                requestSingleLocationUpdate()
            }
        }.addOnFailureListener {
            requestSingleLocationUpdate()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestSingleLocationUpdate() {
        removeSingleLocationCallback()

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMaxUpdates(1)
            .setDurationMillis(5000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation
                removeSingleLocationCallback()
                if (loc != null) {
                    applyUserLocationToMap(loc)
                } else {
                    Toast.makeText(
                        this@CampusMapActivity,
                        R.string.campus_location_unavailable,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        singleLocationCallback = callback

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            ).addOnFailureListener {
                removeSingleLocationCallback()
                Toast.makeText(
                    this@CampusMapActivity,
                    R.string.campus_location_failed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            removeSingleLocationCallback()
            Toast.makeText(
                this@CampusMapActivity,
                R.string.campus_location_failed,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun removeSingleLocationCallback() {
        singleLocationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            singleLocationCallback = null
        }
    }

    private fun applyUserLocationToMap(location: Location) {
        val userLatLng = LatLng(location.latitude, location.longitude)

        maplibreMap?.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(userLatLng)
                    .zoom(16.0)
                    .build()
            ),
            1000
        )

        userMarker?.let { maplibreMap?.removeMarker(it) }
        userMarker = maplibreMap?.addMarker(
            MarkerOptions()
                .position(userLatLng)
                .title("My Location")
                .snippet("Lat: %.5f, Lng: %.5f".format(location.latitude, location.longitude))
        )

        Toast.makeText(this, R.string.campus_location_centered, Toast.LENGTH_SHORT).show()
    }

    private fun openCampusDirections() {
        val destinationQuery = "$CAMPUS_NAME, $CAMPUS_ADDRESS"
        val directionsUri = Uri.parse("https://www.google.com/maps/dir/?api=1")
            .buildUpon()
            .appendQueryParameter("destination", destinationQuery)
            .build()

        val mapIntent = Intent(Intent.ACTION_VIEW, directionsUri).apply {
            setPackage("com.google.android.apps.maps")
        }

        try {
            startActivity(mapIntent)
        } catch (e: ActivityNotFoundException) {
            // Fallback if Google Maps app is not installed (open in browser)
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, directionsUri)
                startActivity(browserIntent)
            } catch (ex: Exception) {
                Toast.makeText(
                    this,
                    R.string.campus_directions_app_not_found,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // MapLibre View Lifecycle methods
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        locationCancellationTokenSource?.cancel()
        removeSingleLocationCallback()
        super.onDestroy()
        mapView.onDestroy()
    }
}

