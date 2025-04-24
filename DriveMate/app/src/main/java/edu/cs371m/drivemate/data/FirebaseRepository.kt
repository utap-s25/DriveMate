package edu.cs371m.drivemate.data

import android.content.Context
import android.location.Geocoder
import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import edu.cs371m.drivemate.model.Post
import edu.cs371m.drivemate.model.UserProfile
import kotlinx.coroutines.tasks.await

object FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storageRef: StorageReference =
        FirebaseStorage.getInstance().reference

    // --- AUTH ---
    suspend fun signUp(email: String, password: String): Result<FirebaseUser> =
        runCatching {
            val res = auth.createUserWithEmailAndPassword(email, password).await()
            res.user ?: throw IllegalStateException("No user")
        }

    suspend fun login(email: String, password: String): Result<FirebaseUser> =
        runCatching {
            val res = auth.signInWithEmailAndPassword(email, password).await()
            res.user ?: throw IllegalStateException("No user")
        }

    fun signOut() = auth.signOut()
    val currentUser: FirebaseUser? get() = auth.currentUser

    // --- USER PROFILE ---
    fun userDoc(uid: String) = db.collection("users").document(uid)

    suspend fun getUserProfile(uid: String): Result<UserProfile> =
        runCatching {
            val snap = userDoc(uid).get().await()
            snap.toObject<UserProfile>()!!.copy(uid = uid)
        }

    suspend fun updateUserProfile(profile: UserProfile): Result<Void> =
        runCatching {
            userDoc(profile.uid)
                .set(profile, SetOptions.merge())
                .await()
        }

    suspend fun uploadProfileImage(uid: String, uri: Uri): Result<String> =
        runCatching {
            val ref = storageRef.child("profileImages/$uid.jpg")
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        }

    /** Only update the profilePicUrl field, without touching others */
    suspend fun updateProfilePicture(uid: String, url: String): Result<Void> =
        runCatching {
            db.collection("users")
                .document(uid)
                .update("profilePicUrl", url)
                .await()
        }

    // --- POSTS ---
    fun postsCol() = db.collection("posts")

    suspend fun createPost(
        imageUri: Uri,
        caption: String,
        location: GeoPoint,
        context: Context // Pass context from the activity/fragment
    ): Result<Void> = runCatching {
        val uid = currentUser!!.uid

        val postId = postsCol().document().id
        val imgRef = storageRef.child("postImages/$postId/${imageUri.lastPathSegment}")
        imgRef.putFile(imageUri).await()
        val imageUrl = imgRef.downloadUrl.await().toString()

        // 👇 Reverse geocode to get place name
        val geocoder = Geocoder(context)
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        val placeName = addresses?.firstOrNull()?.getAddressLine(0) ?: "Unknown location"

        val post = Post(
            postId = postId,
            userId = uid,
            imageUrl = imageUrl,
            caption = caption,
            location = location,
            placeName = placeName,
            createdAt = Timestamp.now(),
            likeCount = 0L
        )

        postsCol().document(postId).set(post).await()
    }

    suspend fun deletePost(postId: String): Result<Void> = runCatching {
        // delete all images under postImages/{postId}/
        val list = storageRef.child("postImages/$postId").listAll().await()
        list.items.forEach { it.delete().await() }

        // delete the Firestore document
        postsCol().document(postId).delete().await()
    }

    /** Listen to all posts in descending order */
    fun listenToPosts(
        onUpdate: (List<Post>) -> Unit
    ): ListenerRegistration {
        return postsCol()
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener(EventListener { snaps, _ ->
                val list = snaps!!.documents
                    .mapNotNull { it.toObject<Post>() }
                onUpdate(list)
            })
    }

    // --- LIKES ---
    private fun likeDoc(postId: String, userId: String) =
        postsCol().document(postId)
            .collection("likes")
            .document(userId)

    suspend fun isPostLiked(postId: String): Boolean =
        runCatching {
            likeDoc(postId, currentUser!!.uid)
                .get().await().exists()
        }.getOrDefault(false)

    suspend fun toggleLike(postId: String): Boolean {
        val uid = currentUser!!.uid
        val postRef = postsCol().document(postId)
        val likeRef = likeDoc(postId, uid)

        var liked = false

        db.runTransaction { tx ->
            val postSnap = tx.get(postRef)
            val currentCount = postSnap.getLong("likeCount") ?: 0L
            val alreadyLiked = tx.get(likeRef).exists()

            if (alreadyLiked) {
                tx.delete(likeRef)
                tx.update(postRef, "likeCount", currentCount - 1)
                liked = false
            } else {
                tx.set(likeRef, mapOf("createdAt" to FieldValue.serverTimestamp()))
                tx.update(postRef, "likeCount", currentCount + 1)
                liked = true
            }
        }.await()

        return liked
    }

}
