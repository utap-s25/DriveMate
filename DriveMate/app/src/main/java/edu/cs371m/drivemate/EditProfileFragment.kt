package edu.cs371m.drivemate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import edu.cs371m.drivemate.data.FirebaseRepository
import edu.cs371m.drivemate.databinding.FragmentEditProfileBinding
import edu.cs371m.drivemate.model.UserProfile
import kotlinx.coroutines.launch

class EditProfileFragment : Fragment() {
    private var _b: FragmentEditProfileBinding? = null
    private val b get() = _b!!

    override fun onCreateView(
        inflater: LayoutInflater, c: ViewGroup?, s: Bundle?
    ) = FragmentEditProfileBinding.inflate(inflater, c, false)
        .also { _b = it }.root

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)
        val uid = FirebaseRepository.currentUser!!.uid

        // Pre-fill existing bio
        lifecycleScope.launch {
            FirebaseRepository.getUserProfile(uid).onSuccess { p ->
                b.etBio.setText(p.bio)
            }
        }

        b.btnSaveProfile.setOnClickListener {
            val newBio = b.etBio.text.toString().trim()
            lifecycleScope.launch {
                // Fetch full existing profile, then merge new bio
                val existing = FirebaseRepository.getUserProfile(uid)
                    .getOrNull() ?: UserProfile(uid=uid)
                val updated = existing.copy(bio = newBio)
                FirebaseRepository.updateUserProfile(updated)
                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
