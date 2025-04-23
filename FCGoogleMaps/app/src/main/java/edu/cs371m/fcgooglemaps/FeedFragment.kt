package edu.cs371m.fcgooglemaps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import edu.cs371m.fcgooglemaps.data.FirebaseRepository
import edu.cs371m.fcgooglemaps.databinding.FragmentFeedBinding
import kotlinx.coroutines.launch

class FeedFragment : Fragment() {
    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ) = FragmentFeedBinding.inflate(inflater, container, false)
        .also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) Setup RecyclerView + Adapter
        adapter = PostAdapter(
            onLikeToggle = { pid -> lifecycleScope.launch { FirebaseRepository.toggleLike(pid) } },
            onDelete     = { pid -> lifecycleScope.launch { FirebaseRepository.deletePost(pid) } },
            onUserClick  = { userId ->
                findNavController().navigate(
                    R.id.profileFragment,
                    bundleOf("userId" to userId)
                )
            }
        )

        binding.rvPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPosts.adapter = adapter

        // 2) Listen to posts updates
        FirebaseRepository.listenToPosts { posts ->
            adapter.submitList(posts)
        }

        // 3) Navigate to CreatePost
        binding.fabCreatePost.setOnClickListener {
            findNavController().navigate(R.id.createPostFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
