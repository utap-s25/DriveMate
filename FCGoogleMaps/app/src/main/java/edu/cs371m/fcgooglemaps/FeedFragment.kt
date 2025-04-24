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
import edu.cs371m.fcgooglemaps.model.Post
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
            onDelete = { pid ->
                lifecycleScope.launch { FirebaseRepository.deletePost(pid) }
            },
            onUserClick = { userId ->
                findNavController().navigate(
                    R.id.profileFragment,
                    bundleOf("userId" to userId)
                )
            }
        )

        binding.rvPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPosts.adapter = adapter

        FirebaseRepository.listenToPosts { posts ->
            lifecycleScope.launch {
                val updated = posts.map {
                    val liked = FirebaseRepository.isPostLiked(it.postId)
                    it.copy(liked = liked)
                }
                adapter.submitList(updated)
            }
        }

        binding.fabCreatePost.setOnClickListener {
            findNavController().navigate(R.id.createPostFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
