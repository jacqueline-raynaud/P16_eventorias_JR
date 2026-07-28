package fr.quinquenaire.p15_eventorias_jr.data.remote

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await


class FirebaseStorageManager(private val firebaseStorage: FirebaseStorage) {

    suspend fun uploadEventImage(eventId: String, imageUri: Uri): String {
        return try {
            val storageRef = firebaseStorage.reference
            val imageRef = storageRef.child("events/$eventId/image.jpg")
            imageRef.putFile(imageUri).await()
            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e("EventoriasApp", "Error uploading event image", e)
            throw e
        }
    }

suspend fun deleteEventImage(eventId: String) {
    try {
        firebaseStorage.reference
            .child("events/$eventId/image.jpg")
            .delete()
            .await()
    } catch (e: Exception) {
        Log.e("EventoriasApp", "Error deleting event image", e)
    }
}

    suspend fun uploadUserAvatar(uid: String, avatarUri: Uri): String {
        return try {
            val storageRef = firebaseStorage.reference
            val avatarRef = storageRef.child("users/$uid/avatar.jpg")
            avatarRef.putFile(avatarUri).await()
            avatarRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e("EventoriasApp", "Error uploading UserProfile avatar", e)
            throw e
        }
    }

    suspend fun deleteAvatarByUrl(avatarUrl: String) {
        if (avatarUrl.isBlank()) return
        if (!avatarUrl.contains("firebasestorage")) return

        try {
            firebaseStorage.getReferenceFromUrl(avatarUrl).delete().await()
        } catch (e: Exception) {
            Log.e("EventoriasApp", "Error deleting avatar", e)
        }
    }



}
