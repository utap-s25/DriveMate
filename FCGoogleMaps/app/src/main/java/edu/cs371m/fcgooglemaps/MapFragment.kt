package edu.cs371m.fcgooglemaps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import edu.cs371m.fcgooglemaps.databinding.FragmentMapBinding

class MapFragment : Fragment() {
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO: grab the SupportMapFragment by ID and set up OnMapReadyCallback
        // e.g.:
        // val mapFrag = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        // mapFrag.getMapAsync(…)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
