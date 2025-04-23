package edu.cs371m.fcgooglemaps.data

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import edu.cs371m.fcgooglemaps.model.Post
import edu.cs371m.fcgooglemaps.model.UserProfile
import kotlinx.coroutines.tasks.await

object FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()
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
        location: com.google.firebase.firestore.GeoPoint
    ): Result<Void> = runCatching {
        val uid = currentUser!!.uid
        // 1) Upload image
        val postId = postsCol().document().id
        val imgRef = storageRef.child("postImages/$postId/${imageUri.lastPathSegment}")
        imgRef.putFile(imageUri).await()
        val imageUrl = imgRef.downloadUrl.await().toString()

        // 2) Save post doc
        val post = Post(
            postId   = postId,
            userId   = uid,
            imageUrl = imageUrl,
            caption  = caption,
            location = location,
            createdAt= Timestamp.now(),
            likeCount= 0L
        )
        postsCol().document(postId)
            .set(post)
            .await()
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

    suspend fun toggleLike(postId: String): Result<Transaction> = runCatching {
        val uid = currentUser!!.uid
        val postRef = postsCol().document(postId)
        val likeRef = likeDoc(postId, uid)

        db.runTransaction { tx ->
            val postSnap = tx.get(postRef)
            val currentCount = postSnap.getLong("likeCount") ?: 0L

            if (tx.get(likeRef).exists()) {
                tx.delete(likeRef)
                tx.update(postRef, "likeCount", currentCount - 1)
            } else {
                tx.set(likeRef, mapOf("createdAt" to FieldValue.serverTimestamp()))
                tx.update(postRef, "likeCount", currentCount + 1)
            }
        }.await()
    }
}
