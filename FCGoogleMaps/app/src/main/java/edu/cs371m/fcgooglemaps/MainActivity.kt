package edu.cs371m.fcgooglemaps

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import edu.cs371m.fcgooglemaps.databinding.ActivityMainBinding
import edu.cs371m.fcgooglemaps.databinding.ContentMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var geocoder: Geocoder
    private var locationPermissionGranted = false
    private lateinit var binding: ContentMainBinding

    // Use a suspend function to get addresses from location name (Android 33+)
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun getAddresses(query: String): List<Address> = suspendCoroutine { cont ->
        geocoder.getFromLocationName(query, 1) { addresses ->
            cont.resume(addresses)
        }
    }

    private suspend fun processAddresses(addresses: List<Address>) {
        // If we found an address, move the camera to its lat/long
        withContext(Dispatchers.Main) {
            if (addresses.isNotEmpty()) {
                val lat = addresses[0].latitude
                val lng = addresses[0].longitude
                val latLng = LatLng(lat, lng)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "No matching address found",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)
        setSupportActionBar(activityMainBinding.toolbar)

        binding = activityMainBinding.contentMain

        // Check Google Play Services and request permissions
        checkGooglePlayServices()
        requestPermission()

        // Load the map, get its async reference
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFrag) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Create Geocoder
        geocoder = Geocoder(this, Locale.getDefault())

        // "Go" button logic
        binding.goBut.setOnClickListener {
            val locationName = binding.mapET.text.toString()
            Log.d("Geocoding", "Requesting location for: $locationName")

            // If we’re on Android 33 or above, we can use the new API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                MainScope().launch {
                    val addresses = getAddresses(locationName)
                    processAddresses(addresses)
                }
            } else {
                // Use the (deprecated) blocking call on older devices
                MainScope().launch(Dispatchers.IO) {
                    val addresses = geocoder.getFromLocationName(locationName, 1)
                    if (addresses != null) {
                        processAddresses(addresses)
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@MainActivity,
                                "No matching address found",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }

        // Let the user hit "Enter" to trigger geocoding
        binding.mapET.setOnEditorActionListener { _, actionId, event ->
            if ((event != null
                        && (event.action == KeyEvent.ACTION_DOWN)
                        && (event.keyCode == KeyEvent.KEYCODE_ENTER))
                || (actionId == EditorInfo.IME_ACTION_DONE)
            ) {
                hideKeyboard()
                binding.goBut.callOnClick()
            }
            false
        }

        // Clear button: remove all markers
        binding.clearBut.setOnClickListener {
            map.clear()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // If location permissions are granted, enable the My Location features
        if (locationPermissionGranted) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                map.isMyLocationEnabled = true
                map.uiSettings.isMyLocationButtonEnabled = true
            }
        }

        // Start the map at the Harry Ransom Center in Austin
        val ransomCenter = LatLng(30.2849, -97.7413)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(ransomCenter, 15f))

        // Tapping the map adds a marker with lat/long to 3 decimal places
        map.setOnMapClickListener { latLng ->
            val title = String.format(
                Locale.getDefault(),
                "%.3f, %.3f",
                latLng.latitude,
                latLng.longitude
            )
            map.addMarker(MarkerOptions().position(latLng).title(title))
        }

        // Long click clears all markers
        map.setOnMapLongClickListener {
            map.clear()
        }
    }

    // Hides the software keyboard
    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.rootView.windowToken, 0)
    }

    private fun checkGooglePlayServices() {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode, 257)?.show()
            } else {
                Log.i(javaClass.simpleName, "This device must install Google Play Services.")
                finish()
            }
        }
    }

    private fun requestPermission() {
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    locationPermissionGranted = true
                }
                else -> {
                    Toast.makeText(
                        this,
                        "Unable to show location - permission required",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
    }
}
