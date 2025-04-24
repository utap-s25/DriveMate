package edu.cs371m.drivemate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import edu.cs371m.drivemate.data.FirebaseRepository
import edu.cs371m.drivemate.databinding.FragmentEditCarBinding
import edu.cs371m.drivemate.model.Vehicle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class EditCarFragment : Fragment() {
    private var _binding: FragmentEditCarBinding? = null
    private val binding get() = _binding!!

    private var selectedYear: String? = null
    private var selectedMake: String? = null
    private var selectedModel: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditCarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(v: View, saved: Bundle?) {
        super.onViewCreated(v, saved)
        val uid = FirebaseRepository.currentUser!!.uid

        // Load years first
        lifecycleScope.launch {
            val years = getYears()
            val yearAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, years)
            binding.spinnerYear.adapter = yearAdapter

            // Load existing profile
            FirebaseRepository.getUserProfile(uid).onSuccess { profile ->
                profile.vehicle.year.takeIf { it > 0 }?.toString()?.let {
                    val idx = years.indexOf(it)
                    if (idx >= 0) binding.spinnerYear.setSelection(idx)
                }
            }
        }

        // Year selection changes → fetch makes
        binding.spinnerYear.setOnItemSelectedListener { _, _, _, _ ->
            selectedYear = binding.spinnerYear.selectedItem.toString()
            selectedYear?.let { year ->
                lifecycleScope.launch {
                    val makes = getMakes(year)
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, makes)
                    binding.spinnerMake.adapter = adapter
                }
            }
        }

        // Make selection changes → fetch models
        binding.spinnerMake.setOnItemSelectedListener { _, _, _, _ ->
            selectedMake = binding.spinnerMake.selectedItem.toString()
            val year = selectedYear ?: return@setOnItemSelectedListener
            lifecycleScope.launch {
                val models = getModels(year, selectedMake!!)
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, models)
                binding.spinnerModel.adapter = adapter
            }
        }

        // Save
        binding.btnSaveCar.setOnClickListener {
            val year = binding.spinnerYear.selectedItem?.toString()?.toIntOrNull() ?: 0
            val make = binding.spinnerMake.selectedItem?.toString().orEmpty()
            val model = binding.spinnerModel.selectedItem?.toString().orEmpty()

            if (year == 0 || make.isBlank() || model.isBlank()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newVehicle = Vehicle(make = make, model = model, year = year)
            lifecycleScope.launch {
                FirebaseRepository.getUserProfile(uid).onSuccess { profile ->
                    FirebaseRepository.updateUserProfile(profile.copy(vehicle = newVehicle))
                    findNavController().popBackStack()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private suspend fun getYears(): List<String> = withContext(Dispatchers.IO) {
        val json = URL("https://www.carqueryapi.com/api/0.3/?cmd=getYears").readText()
        val obj = JSONObject(json.removePrefix("callback(").removeSuffix(");"))
        val years = obj.getJSONObject("Years")
        val min = years.getInt("min_year")
        val max = years.getInt("max_year")
        (max downTo min).map { it.toString() }
    }

    private suspend fun getMakes(year: String): List<String> = withContext(Dispatchers.IO) {
        val json = URL("https://www.carqueryapi.com/api/0.3/?cmd=getMakes&year=$year").readText()
        val obj = JSONObject(json.removePrefix("callback(").removeSuffix(");"))
        obj.getJSONArray("Makes").let { arr ->
            List(arr.length()) { idx ->
                arr.getJSONObject(idx).getString("make_display")
            }.sorted()
        }
    }

    private suspend fun getModels(year: String, make: String): List<String> = withContext(Dispatchers.IO) {
        val json = URL("https://www.carqueryapi.com/api/0.3/?cmd=getModels&make=$make&year=$year").readText()
        val obj = JSONObject(json.removePrefix("callback(").removeSuffix(");"))
        obj.getJSONArray("Models").let { arr ->
            List(arr.length()) { idx ->
                arr.getJSONObject(idx).getString("model_name")
            }.sorted()
        }
    }

    // Extension for cleaner spinner selection
    private fun View.setOnItemSelectedListener(onSelected: (parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) -> Unit) {
        if (this is android.widget.Spinner) {
            this.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                    onSelected(parent, view, position, id)
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
            }
        }
    }
}