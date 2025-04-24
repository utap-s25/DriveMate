package edu.cs371m.fcgooglemaps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import edu.cs371m.fcgooglemaps.databinding.FragmentMapBinding
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class MapFragment : Fragment(), OnMapReadyCallback {
    private var _b: FragmentMapBinding? = null
    private val b get() = _b!!

    private lateinit var map: GoogleMap
    private val client = OkHttpClient()
    private val apiKey by lazy { getString(R.string.google_maps_key) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentMapBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) Create the SupportMapFragment inside mapHost
        val mapFrag = SupportMapFragment.newInstance().also {
            childFragmentManager.beginTransaction()
                .replace(b.mapContainer.id, it)
                .commitNow()
        }
        mapFrag.getMapAsync(this)

        // 2) Wire up "Search Here" button
        b.fabSearchHere.setOnClickListener {
            if (::map.isInitialized) {
                performNearbySearch(map.cameraPosition.target)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true

        // 3) Initial load at fixed center (e.g. Austin)
        val center = LatLng(30.2849, -97.7413)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 13f))
        performNearbySearch(center)
    }

    /** Clears markers and runs 3 searches around `center`. */
    private fun performNearbySearch(center: LatLng) {
        map.clear()  // remove old markers

        CoroutineScope(Dispatchers.IO).launch {
            // Car dealerships (blue)
            fetchPlaces(center, type="car_dealer")?.let { places ->
                withContext(Dispatchers.Main) {
                    places.forEach { (name, lat, lng) ->
                        map.addMarker(MarkerOptions()
                            .position(LatLng(lat, lng))
                            .title(name)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        )
                    }
                }
            }
            // Mechanics (orange)
            fetchPlaces(center, type="car_repair")?.let { places ->
                withContext(Dispatchers.Main) {
                    places.forEach { (name, lat, lng) ->
                        map.addMarker(MarkerOptions()
                            .position(LatLng(lat, lng))
                            .title(name)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                        )
                    }
                }
            }
            // Car events (violet)
            fetchPlaces(center, type="event", keyword="car")?.let { places ->
                withContext(Dispatchers.Main) {
                    places.forEach { (name, lat, lng) ->
                        map.addMarker(MarkerOptions()
                            .position(LatLng(lat, lng))
                            .title(name)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
                        )
                    }
                }
            }
        }
    }

    /**
     * Queries Google Places Nearby Search Web API.
     * @return list of (name, lat, lng), or null on error.
     */
    private fun fetchPlaces(
        center: LatLng,
        type: String,
        radius: Int = 5000,
        keyword: String? = null
    ): List<Triple<String, Double, Double>>? {
        val url = buildString {
            append("https://maps.googleapis.com/maps/api/place/nearbysearch/json")
            append("?location=${center.latitude},${center.longitude}")
            append("&radius=$radius")
            append("&type=$type")
            keyword?.let { append("&keyword=${it}") }
            append("&key=$apiKey")
        }
        return try {
            val resp = client.newCall(Request.Builder().url(url).build()).execute()
            val arr = JSONObject(resp.body?.string().orEmpty())
                .getJSONArray("results")
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                val name = obj.optString("name")
                val loc = obj.getJSONObject("geometry")
                    .getJSONObject("location")
                Triple(name, loc.getDouble("lat"), loc.getDouble("lng"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
