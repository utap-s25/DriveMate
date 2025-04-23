package edu.cs371m.fcgooglemaps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
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
import android.view.View


class PostAdapter(
    private val onLikeToggle: (String) -> Unit,
    private val onDelete: (String) -> Unit,
    private val onUserClick: (String) -> Unit
) : ListAdapter<Post, PostAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object: DiffUtil.ItemCallback<Post>() {
            override fun areItemsTheSame(a: Post, b: Post) = a.postId == b.postId
            override fun areContentsTheSame(a: Post, b: Post) = a == b
        }
    }

    inner class VH(val b: ItemPostBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(p: Post) {
            val uid = FirebaseRepository.currentUser!!.uid

            // Username & user-click
            CoroutineScope(Dispatchers.Main).launch {
                val profile = FirebaseRepository.getUserProfile(p.userId).getOrNull()
                val name = profile?.username ?: "User"
                b.tvUsername.text = name
                b.tvUsername.setOnClickListener {
                    onUserClick(p.userId)
                }
            }

            // Delete visibility
            b.ivDelete.visibility =
                if (p.userId == uid) View.VISIBLE else View.GONE
            b.ivDelete.setOnClickListener {
                onDelete(p.postId)
            }

            // Image
            Glide.with(b.root).load(p.imageUrl).into(b.ivPost)

            // Location
            b.tvLocation.text = "${p.location.latitude}, ${p.location.longitude}"

            // Caption & like count
            b.tvCaption.text = p.caption
            b.tvLikeCount.text = p.likeCount.toString()

            // Like state
            CoroutineScope(Dispatchers.Main).launch {
                val liked = FirebaseRepository.isPostLiked(p.postId)
                b.ivLike.setImageResource(
                    if (liked) R.drawable.ic_like_filled else R.drawable.ic_like_outline
                )
            }
            b.ivLike.setOnClickListener { onLikeToggle(p.postId) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, pos: Int) =
        holder.bind(getItem(pos))
}
