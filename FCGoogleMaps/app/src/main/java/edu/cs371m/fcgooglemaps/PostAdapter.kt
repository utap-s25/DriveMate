package edu.cs371m.fcgooglemaps

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import edu.cs371m.fcgooglemaps.data.FirebaseRepository
import edu.cs371m.fcgooglemaps.databinding.ItemPostBinding
import edu.cs371m.fcgooglemaps.model.Post
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PostAdapter(
    private val onLikeToggle: (postId: String) -> Unit
) : ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(
        private val binding: ItemPostBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            // Caption and like count
            binding.tvCaption.text = post.caption
            binding.tvLikeCount.text = post.likeCount.toString()

            // Load post image
            Glide.with(binding.root)
                .load(post.imageUrl)
                .into(binding.ivPost)

            // Determine if current user has liked this post
            CoroutineScope(Dispatchers.Main).launch {
                val liked = FirebaseRepository.isPostLiked(post.postId)
                val iconRes = if (liked)
                    R.drawable.ic_like_filled
                else
                    R.drawable.ic_like_outline

                binding.ivLike.setImageResource(iconRes)
            }

            // Toggle like when heart icon is clicked
            binding.ivLike.setOnClickListener {
                onLikeToggle(post.postId)
            }
        }
    }
}

private class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
    override fun areItemsTheSame(oldItem: Post, newItem: Post) =
        oldItem.postId == newItem.postId

    override fun areContentsTheSame(oldItem: Post, newItem: Post) =
        oldItem == newItem
}
