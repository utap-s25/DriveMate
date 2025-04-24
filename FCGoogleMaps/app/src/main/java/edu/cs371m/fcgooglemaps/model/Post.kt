package edu.cs371m.fcgooglemaps.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class Post(
    val postId: String = "",
    val userId: String = "",
    val imageUrl: String = "",
    val caption: String = "",
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val createdAt: Timestamp? = null,
    val likeCount: Long = 0L,
    val liked: Boolean = false  // 👈 new
)
