package edu.cs371m.fcgooglemaps

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import edu.cs371m.fcgooglemaps.data.FirebaseRepository
import edu.cs371m.fcgooglemaps.databinding.FragmentProfileBinding
import edu.cs371m.fcgooglemaps.model.Post
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private var postsListener: ListenerRegistration? = null

    // 1. Image picker
    private val pickPic = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onProfilePicPicked(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val user = FirebaseRepository.currentUser ?: return
        val uid = user.uid

        // Show email
        binding.tvEmail.text = user.email

        // Load profile fields once
        lifecycleScope.launch {
            FirebaseRepository.getUserProfile(uid)
                .onSuccess { profile ->
                    // Bio
                    binding.tvBio.text = profile.bio.ifEmpty { "No bio set" }
                    // Car
                    val v = profile.vehicle
                    binding.tvCar.text =
                        if (v.year > 0 && v.make.isNotEmpty() && v.model.isNotEmpty())
                            "${v.year} ${v.make} ${v.model}"
                        else
                            "No car set"
                    // Profile picture
                    if (profile.profilePicUrl.isNotEmpty()) {
                        Glide.with(this@ProfileFragment)
                            .load(profile.profilePicUrl)
                            .circleCrop()
                            .into(binding.ivProfilePic)
                    }
                }
        }

        // My Posts RecyclerView
        val adapter = PostAdapter { postId ->
            lifecycleScope.launch { FirebaseRepository.toggleLike(postId) }
        }
        binding.rvMyPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMyPosts.adapter = adapter

        // Listen only to this user's posts
        postsListener = FirebaseRepository
            .postsCol()
            .whereEqualTo("userId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snaps, _ ->
                val list = snaps
                    ?.documents
                    ?.mapNotNull { it.toObject(Post::class.java) }
                    ?: emptyList()
                adapter.submitList(list)
            }

        // Buttons
        binding.btnChangePic.setOnClickListener { pickPic.launch("image/*") }
        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.editProfileFragment)
        }
        binding.btnEditCar.setOnClickListener {
            findNavController().navigate(R.id.editCarFragment)
        }
    }

    /** Handle a newly picked profile picture */
    private fun onProfilePicPicked(uri: Uri) {
        // 1) Show it immediately
        Glide.with(this)
            .load(uri)
            .circleCrop()
            .into(binding.ivProfilePic)

        // 2) Upload & persist
        val uid = FirebaseRepository.currentUser!!.uid
        lifecycleScope.launch {
            FirebaseRepository.uploadProfileImage(uid, uri)
                .onSuccess { url ->
                    // update only that field
                    FirebaseRepository.updateProfilePicture(uid, url)
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        postsListener?.remove()
        _binding = null
    }
}
