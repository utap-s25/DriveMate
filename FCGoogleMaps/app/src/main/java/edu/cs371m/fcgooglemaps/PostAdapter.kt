package edu.cs371m.fcgooglemaps

import android.view.LayoutInflater
import android.view.View
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
    private val onLikeToggle: (Post) -> Unit,
    private val onDelete: (String) -> Unit,
    private val onUserClick: (String) -> Unit
) : ListAdapter<Post, PostAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Post>() {
            override fun areItemsTheSame(a: Post, b: Post) = a.postId == b.postId
            override fun areContentsTheSame(a: Post, b: Post) = a == b
        }
    }

    inner class VH(private val b: ItemPostBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(p: Post) {
            val uid = FirebaseRepository.currentUser?.uid

            // Load user name dynamically
            CoroutineScope(Dispatchers.Main).launch {
                val profile = FirebaseRepository.getUserProfile(p.userId).getOrNull()
                b.tvUsername.text = profile?.username ?: "User"
            }

            // Navigate to profile
            b.tvUsername.setOnClickListener { onUserClick(p.userId) }

            if (p.userId == uid) {
                b.ivDelete.visibility = View.VISIBLE
                b.ivDelete.setOnClickListener { onDelete(p.postId) }
            } else {
                b.ivDelete.visibility = View.GONE
            }

            Glide.with(b.root).load(p.imageUrl).into(b.ivPost)
            b.tvLocation.text = "${p.location.latitude}, ${p.location.longitude}"
            b.tvCaption.text = p.caption
            b.tvLikeCount.text = p.likeCount.toString()

            b.ivLike.setImageResource(
                if (p.liked) R.drawable.ic_like_filled else R.drawable.ic_like_outline
            )
            b.ivLike.setOnClickListener { onLikeToggle(p) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }
}
