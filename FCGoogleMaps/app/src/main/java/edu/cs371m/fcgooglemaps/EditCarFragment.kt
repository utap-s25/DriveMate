package edu.cs371m.fcgooglemaps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import edu.cs371m.fcgooglemaps.data.FirebaseRepository
import edu.cs371m.fcgooglemaps.databinding.FragmentEditCarBinding
import edu.cs371m.fcgooglemaps.model.UserProfile
import edu.cs371m.fcgooglemaps.model.Vehicle
import kotlinx.coroutines.launch

class EditCarFragment : Fragment() {
    private var _binding: FragmentEditCarBinding? = null
    private val binding get() = _binding!!

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

        // Load existing
        lifecycleScope.launch {
            FirebaseRepository.getUserProfile(uid).onSuccess { p ->
                binding.etMake.setText(p.vehicle.make)
                binding.etModel.setText(p.vehicle.model)
                binding.etYear.setText(p.vehicle.year.toString())
                binding.etVin.setText(p.vehicle.vin)
            }
        }

        binding.btnSaveCar.setOnClickListener {
            val newVehicle = Vehicle(
                make  = binding.etMake.text.toString(),
                model = binding.etModel.text.toString(),
                year  = binding.etYear.text.toString().toIntOrNull() ?: 0,
                vin   = binding.etVin.text.toString()
            )
            val uid = FirebaseRepository.currentUser!!.uid
            lifecycleScope.launch {
                // fetch existing
                FirebaseRepository.getUserProfile(uid)
                    .onSuccess { existing ->
                        // merge vehicle only
                        FirebaseRepository.updateUserProfile(
                            existing.copy(vehicle = newVehicle)
                        )
                        findNavController().popBackStack()
                    }
            }
        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
