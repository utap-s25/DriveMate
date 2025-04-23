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
import com.google.firebase.auth.FirebaseAuth
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

    // image‐picker launcher
    private val pickPic = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onProfilePicPicked(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewer = FirebaseRepository.currentUser ?: return
        val viewerUid = viewer.uid

        // Always show email for now
        binding.tvEmail.text = viewer.email
        binding.tvEmail.visibility = View.VISIBLE

        // Load your own profile data
        lifecycleScope.launch {
            FirebaseRepository.getUserProfile(viewerUid)
                .onSuccess { profile ->
                    binding.tvBio.text = profile.bio.ifEmpty { "No bio set" }
                    val v = profile.vehicle
                    binding.tvCar.text = if (v.year > 0 && v.make.isNotEmpty() && v.model.isNotEmpty())
                        "${v.year} ${v.make} ${v.model}"
                    else
                        "No car set"
                    if (profile.profilePicUrl.isNotEmpty()) {
                        Glide.with(this@ProfileFragment)
                            .load(profile.profilePicUrl)
                            .circleCrop()
                            .into(binding.ivProfilePic)
                    }
                }
        }

        // Set up RecyclerView to show ALL posts (no filter)
        val adapter = PostAdapter(
            onLikeToggle = { postId ->
                lifecycleScope.launch { FirebaseRepository.toggleLike(postId) }
            },
            onDelete = { postId ->
                lifecycleScope.launch { FirebaseRepository.deletePost(postId) }
            },
            onUserClick = { uid ->
                if (uid != viewerUid) {
                    findNavController().navigate(
                        R.id.profileFragment,
                        Bundle().apply { putString("userId", uid) }
                    )
                }
            }
        )
        binding.rvMyPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMyPosts.adapter = adapter

        // Listen to ALL posts for debugging
        postsListener = FirebaseRepository.postsCol()
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
        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            findNavController().navigate(R.id.loginFragment)
        }

    }

    private fun onProfilePicPicked(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .circleCrop()
            .into(binding.ivProfilePic)

        val uid = FirebaseRepository.currentUser!!.uid
        lifecycleScope.launch {
            FirebaseRepository.uploadProfileImage(uid, uri)
                .onSuccess { url ->
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
