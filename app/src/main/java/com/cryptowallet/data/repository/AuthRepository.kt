package com.cryptowallet.data.repository

import com.cryptowallet.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Firebase-backed authentication. Users are authenticated via FirebaseAuth
 * and their profile data is persisted in Firestore ("users" collection).
 */
class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    val currentUser get() = auth.currentUser

    suspend fun login(email: String, password: String): Result<User> = runCatching {
        val authResult = auth.signInWithEmailAndPassword(email, password).await()
        val uid = authResult.user?.uid ?: error("Invalid email or password.")
        fetchUser(uid)
    }

    suspend fun register(name: String, email: String, password: String): Result<User> = runCatching {
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = authResult.user?.uid ?: error("Could not create account.")

        val user = User(uid = uid, name = name, email = email, createdAt = System.currentTimeMillis())
        firestore.collection(USERS_COLLECTION).document(uid).set(user).await()
        user
    }

    fun signOut() {
        auth.signOut()
    }

    private suspend fun fetchUser(uid: String): User {
        val snapshot = firestore.collection(USERS_COLLECTION).document(uid).get().await()
        return snapshot.toObject(User::class.java) ?: error("User data not found.")
    }

    private companion object {
        const val USERS_COLLECTION = "users"
    }
}
