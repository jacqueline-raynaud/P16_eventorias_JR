package fr.quinquenaire.p15_eventorias_jr.data.repository

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import fr.quinquenaire.p15_eventorias_jr.data.remote.FirebaseFirestoreManager
import fr.quinquenaire.p15_eventorias_jr.data.remote.FirebaseStorageManager
import fr.quinquenaire.p15_eventorias_jr.domain.EventQueryParams
import fr.quinquenaire.p15_eventorias_jr.domain.SortOrder
import fr.quinquenaire.p15_eventorias_jr.domain.model.Event
import fr.quinquenaire.p15_eventorias_jr.domain.repository.EventRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class EventRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firestoreManager: FirebaseFirestoreManager,
    private val storageManager: FirebaseStorageManager
) : EventRepository {

    override fun getEventsStream(params: EventQueryParams): Flow<List<Event>> = callbackFlow {
        var query: Query = firestore.collection("events")


        // 1. Filtrage par catégorie (Natifs Firestore)
        if (params.category != null) {
            query = query.whereEqualTo("category", params.category)
        }

        // 2. Tri (Natifs Firestore)
        // Attention : Firestore nécessite un index composite si vous combinez where + orderBy
        query = when (params.sortOrder) {
            SortOrder.BY_DATE_ASC -> query.orderBy("date", Query.Direction.ASCENDING)
            SortOrder.BY_DATE_DESC -> query.orderBy("date", Query.Direction.DESCENDING)
            SortOrder.DEFAULT -> query.orderBy("date", Query.Direction.ASCENDING)
            //SortOrder.BY_CATEGORY -> query.orderBy("category",Query.Direction.ASCENDING)
        }

        // 3. Limite
        query = query.limit(params.limit.toLong())

        // Requête Inscription au listener
        android.util.Log.d("DEBUG_QUERY", "Building query with category: ${params.category} and sort: ${params.sortOrder}")
        val registration = query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                close(e) // Ferme le flow avec l'erreur
                return@addSnapshotListener
            }

            val events = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Event::class.java)?.copy(id = doc.id)
            } ?: emptyList()

            android.util.Log.d("DEBUG_REPO 1", "Snapshot received. Docs count: ${snapshot?.documents?.size}")
            android.util.Log.d("DEBUG_REPO 1", "Events mapped count: ${events.size}")

            // 4. Recherche textuelle
            val filteredEvents = if (params.searchQuery.isBlank()) {
                events
            } else {
                // ajouter description ? description
                events.filter {
                    it.name.contains(params.searchQuery, ignoreCase = true) ||
                            it.locationName.contains(params.searchQuery, ignoreCase = true) ||
                            it.description.contains(params.searchQuery, ignoreCase = true)
                }
            }
            android.util.Log.d("DEBUG_REPO 2", "Snapshot received. Docs count: ${snapshot?.documents?.size}")
            android.util.Log.d("DEBUG_REPO 2", "Events mapped count: ${events.size}")

            // Envoie les données dans le flow
            trySend(filteredEvents)
        }

        // nettoyage flow annulé (sort ecran)
        awaitClose {
            registration.remove()
        }
    }


    override fun getEventDetail(eventId: String): Flow<Event?> {
        return firestoreManager.getEventDetail(eventId)
    }

    override suspend fun createEvent(event: Event, imageUri: Uri?): String {

        val eventId = firestoreManager.createEvent(event)
        if (imageUri != null) {
            val imageUrl = storageManager.uploadEventImage(eventId, imageUri)
            firestoreManager.updateEventImageUrl(eventId, imageUrl)
        }
        return eventId
    }

    override suspend fun updateEvent(event: Event, imageUri: Uri?) {

        val imageUrl = if (imageUri != null) {
            storageManager.uploadEventImage(event.id, imageUri)
        } else {
            event.imageUrl
        }
        firestoreManager.updateEvent(event.copy(imageUrl = imageUrl))
    }

    override suspend fun deleteEvent(eventId: String, imageUrl: String) {
        firestoreManager.deleteEvent(eventId)          // source de vérité : le document disparaît
        if (imageUrl.isNotBlank()) {
            storageManager.deleteEventImage(eventId)   // best-effort, erreur déjà absorbée dans le manager
        }
    }

    override suspend fun anonymizeOrganizerEvents(uid: String) {
        firestoreManager.anonymizeOrganizerEvents(uid)
    }
}

