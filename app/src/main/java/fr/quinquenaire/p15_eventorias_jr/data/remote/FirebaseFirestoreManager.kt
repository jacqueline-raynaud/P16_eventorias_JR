package fr.quinquenaire.p15_eventorias_jr.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import fr.quinquenaire.p15_eventorias_jr.domain.model.Event
import fr.quinquenaire.p15_eventorias_jr.domain.model.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await


class FirebaseFirestoreManager(private val firestore: FirebaseFirestore) {

    // Events Collection
    fun getEvents(): Flow<List<Event>> = callbackFlow {
        val listener = firestore.collection("events")
            .orderBy("date", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("EventoriasApp", "Error fetching events", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val events = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Event::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(events)
            }
        awaitClose { listener.remove() }
    }

    fun getEventDetail(eventId: String): Flow<Event?> = callbackFlow {
        val listener = firestore.collection("events").document(eventId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("EventoriasApp", "Error fetching event detail", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                val event = snapshot?.toObject(Event::class.java)?.copy(id = snapshot.id)
                trySend(event)
            }
        awaitClose { listener.remove() }
    }


    /*if the database grows
    fun searchEvents(query: String): Flow<List<Event>> = callbackFlow {
        val listener = firestore.collection("events")
            .orderBy("name")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val events = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Event::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(events)
            }
        awaitClose { listener.remove() }
    }*/

    suspend fun createEvent(event: Event): String {
        return try {
            val ref = firestore.collection("events").document()
            val eventToSave = event.copy(
                id = ref.id
            )
            ref.set(eventToSave).await()
            ref.id
        } catch (e: Exception) {
            Log.e("EventoriasApp", "Error creating event", e)
            throw e
        }
    }

    suspend fun updateEventImageUrl(eventId: String, imageUrl: String) {
        try {
            firestore.collection("events").document(eventId)
                .update("imageUrl", imageUrl)
                .await()
        } catch (e: Exception) {
            Log.e("EventoriasApp", "Error updating event image url", e)
            throw e
        }
    }

    suspend fun updateEvent(event: Event) {
        try {
            val updateMap = mapOf(
                "name" to event.name,
                "description" to event.description,
                "date" to event.date,
                "locationName" to event.locationName,
                "location" to event.location,
                "category" to event.category,
                "imageUrl" to event.imageUrl,
                "organizerId" to event.organizerId,
                "guests" to event.guests

            )
            firestore.collection("events").document(event.id).update(updateMap).await()
        } catch (e: Exception) {
            Log.e("EventoriasApp", "Error updating event", e)
            throw e
        }
    }

    suspend fun deleteEvent(eventId: String) {
        try {
            firestore.collection("events").document(eventId).delete().await()
        } catch (e: Exception) {
            Log.e("EventoriasApp", "Error deleting event", e)
            throw e
        }
    }

    // --- User Profile ---

    fun getUserProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("EventoriasApp", "Error getting user profile", error)
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(UserProfile::class.java))
            }
        awaitClose { listener.remove() }
    }

    suspend fun createUserProfileIfMissing(profile: UserProfile) {
        try {
            val docRef = firestore.collection("users").document(profile.uid)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                docRef.set(profile).await()
            }
        } catch (e: Exception) {
            Log.e("EventoriasApp", "Error creating user profile", e)
            throw e
        }
    }

    suspend fun updateUserProfile(profile: UserProfile) {
        try {
            val updateMap = mapOf(
                "firstName" to profile.firstName,
                "lastName" to profile.lastName,
                "avatarUrl" to profile.avatarUrl
            )
            firestore.collection("users")
                .document(profile.uid)
                .update(updateMap)
                .await()
        } catch (e: Exception) {
            Log.e("EventoriasApp", "Error updating user profile", e)
            throw e
        }
    }

    suspend fun updateNotificationSetting(uid: String, enabled: Boolean) {
        try {
            firestore.collection("users")
                .document(uid)
                .update("notificationEnabled", enabled)
                .await()
        } catch (e: Exception) {
            Log.e("EventoriasApp", "Error updating notification setting", e)
            throw e
        }
    }

    companion object {
        const val ANONYMOUS_ORGANIZER_ID = "utilisateur_supprime"
    }

    suspend fun updateFcmToken(uid: String, token: String) {
        try {
            firestore.collection("users").document(uid)
                .update("fcmToken", token)
                .await()
        } catch (e: Exception) {
            Log.e("EventoriasApp", "Erreur lors de la mise à jour du FCM Token", e)
        }
    }

    suspend fun anonymizeOrganizerEvents(uid: String) {
        try {
            val snapshot = firestore.collection("events")
                .whereEqualTo("organizerId", uid)
                .get()
                .await()
            Log.e("EventoriasApp", "anonymizeOrganizerEvents: $snapshot")

            if (snapshot.isEmpty) return

            val batch = firestore.batch()
            snapshot.documents.forEach { doc ->
                batch.update(doc.reference, "organizerId", ANONYMOUS_ORGANIZER_ID)
            }
            batch.commit().await()
        } catch (e: Exception) {
            Log.e("EventoriasApp", "Error anonymizing events", e)
            throw e
        }
    }

    suspend fun deleteUserProfile(uid: String) {
        try {
            firestore.collection("users").document(uid).delete().await()
        } catch (e: Exception) {
            Log.e("EventoriasApp", "Error deleting user profile", e)
            throw e
        }
    }
}