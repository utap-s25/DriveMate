package edu.cs371m.drivemate

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.firebase.firestore.GeoPoint
import edu.cs371m.drivemate.data.FirebaseRepository
import edu.cs371m.drivemate.databinding.FragmentCreatePostBinding
import kotlinx.coroutines.launch

class CreatePostFragment : Fragment() {
    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!

    private var selectedUri: Uri? = null
    private var selectedGeo: GeoPoint? = null

    // 1) Image picker
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            binding.ivPreview.visibility = View.VISIBLE
            Glide.with(this).load(uri).into(binding.ivPreview)
        }
    }

    // 2) Places Autocomplete
    private val AUTOCOMPLETE_REQUEST_CODE = 1001
    private fun startPlaceAutocomplete() {
        val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
        val intent = Autocomplete.IntentBuilder(
            AutocompleteActivityMode.OVERLAY, fields
        ).build(requireContext())
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize Places if not already
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), getString(R.string.google_maps_key))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ) = FragmentCreatePostBinding.inflate(inflater, container, false)
        .also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAddImages.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.etLocation.setOnClickListener {
            startPlaceAutocomplete()
        }

        binding.btnCancelPost.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSubmitPost.setOnClickListener {
            val uri = selectedUri
            val loc = selectedGeo
            val cap = binding.etCaption.text.toString().trim()

            // Validate
            when {
                uri == null -> Toast.makeText(context, "Pick an image first", Toast.LENGTH_SHORT).show()
                loc == null -> Toast.makeText(context, "Choose a location", Toast.LENGTH_SHORT).show()
                cap.isEmpty() -> Toast.makeText(context, "Enter a caption", Toast.LENGTH_SHORT).show()
                else -> {
                    // Create post
                    lifecycleScope.launch {
                        FirebaseRepository.createPost(uri, cap, loc, requireContext())
                            .onSuccess {
                                findNavController().popBackStack()
                            }
                            .onFailure {
                                Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                            }
                    }
                }
            }
        }
    }

    // Handle place autocomplete result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    val place = Autocomplete.getPlaceFromIntent(data!!)
                    val latLng = place.latLng
                    if (latLng != null) {
                        selectedGeo = GeoPoint(latLng.latitude, latLng.longitude)
                        binding.etLocation.setText(place.name)
                    }
                }
                AutocompleteActivity.RESULT_ERROR -> {
                    val status = Autocomplete.getStatusFromIntent(data!!)
                    Toast.makeText(context, status.statusMessage, Toast.LENGTH_SHORT).show()
                }
                Activity.RESULT_CANCELED -> { /* no-op */ }
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
