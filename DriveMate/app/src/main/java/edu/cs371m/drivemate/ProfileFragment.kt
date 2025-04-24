package edu.cs371m.drivemate

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
import edu.cs371m.drivemate.data.FirebaseRepository
import edu.cs371m.drivemate.databinding.FragmentProfileBinding
import edu.cs371m.drivemate.model.Post
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private var postsListener: ListenerRegistration? = null
    private lateinit var adapter: PostAdapter

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
        val profileUid = arguments?.getString("userId") ?: viewer.uid
        val isOwnProfile = profileUid == viewer.uid

        // Show email only if it's your own profile
        binding.tvEmail.text = viewer.email
        binding.tvEmail.visibility = if (isOwnProfile) View.VISIBLE else View.GONE

        // Load profile info
        lifecycleScope.launch {
            FirebaseRepository.getUserProfile(profileUid)
                .onSuccess { profile ->
                    binding.tvBio.text = profile.bio.ifEmpty { "No bio set" }
                    val v = profile.vehicle
                    binding.tvCar.text = if (v.year > 0 && v.make.isNotEmpty() && v.model.isNotEmpty())
                        "${v.year} ${v.make} ${v.model}"
                    else "No car set"

                    if (profile.profilePicUrl.isNotEmpty()) {
                        Glide.with(this@ProfileFragment)
                            .load(profile.profilePicUrl)
                            .circleCrop()
                            .into(binding.ivProfilePic)
                    }
                }
        }

        // Hide edit/logout buttons if not own profile
        binding.btnLogout.visibility = if (isOwnProfile) View.VISIBLE else View.GONE
        binding.btnEditProfile.visibility = if (isOwnProfile) View.VISIBLE else View.GONE
        binding.btnEditCar.visibility = if (isOwnProfile) View.VISIBLE else View.GONE
        binding.btnChangePic.visibility = if (isOwnProfile) View.VISIBLE else View.GONE

        // Set up posts adapter
        lateinit var adapter: PostAdapter
        adapter = PostAdapter(
            onLikeToggle = { post ->
                lifecycleScope.launch {
                    val liked = FirebaseRepository.toggleLike(post.postId)
                    val updated = post.copy(
                        liked = liked,
                        likeCount = if (liked) post.likeCount + 1 else post.likeCount - 1
                    )
                    val newList = adapter.currentList.map {
                        if (it.postId == post.postId) updated else it
                    }
                    adapter.submitList(newList)
                }
            },
            onDelete = { postId ->
                lifecycleScope.launch { FirebaseRepository.deletePost(postId) }
            },
            onUserClick = { uid ->
                if (uid != viewer.uid) {
                    findNavController().navigate(
                        R.id.profileFragment,
                        Bundle().apply { putString("userId", uid) }
                    )
                }
            }
        )
        binding.rvMyPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMyPosts.adapter = adapter

        // Filter posts by the profile being viewed
        FirebaseRepository.postsCol()
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snaps, _ ->
            val allDocs = snaps?.documents ?: emptyList()
            val rawList = mutableListOf<Post>()

            for (doc in allDocs) {
                try {
                    val post = doc.toObject(Post::class.java)
                    if (post != null) {
                        rawList.add(post)
                    } else {
                        android.util.Log.w("ProfilePosts", "Null post for doc ${doc.id}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ProfilePosts", "Failed to parse doc ${doc.id}", e)
                }
            }

            android.util.Log.d("ProfilePosts", "Loaded ${rawList.size} posts from ${allDocs.size} docs")
                val filtered = rawList.filter { it.userId == profileUid }
                android.util.Log.d("ProfilePosts", "Filtered ${filtered.size} posts from ${allDocs.size} docs")

                lifecycleScope.launch {
                val updated = filtered.map { post ->
                    val liked = FirebaseRepository.isPostLiked(post.postId)
                    post.copy(liked = liked)
                }
                android.util.Log.d("ProfilePosts", "Filtered ${filtered.size} posts from ${allDocs.size} docs")

                adapter.submitList(updated)
            }
        }



        // Only allow your own profile interactions
        binding.btnChangePic.setOnClickListener {
            if (isOwnProfile) pickPic.launch("image/*")
        }

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
