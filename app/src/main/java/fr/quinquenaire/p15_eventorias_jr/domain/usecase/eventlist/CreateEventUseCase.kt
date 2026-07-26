package fr.quinquenaire.p15_eventorias_jr.domain.usecase.eventlist

import android.net.Uri
import com.google.firebase.Timestamp
import fr.quinquenaire.p15_eventorias_jr.domain.location.GeocoderManager
import fr.quinquenaire.p15_eventorias_jr.domain.model.Event
import fr.quinquenaire.p15_eventorias_jr.domain.repository.EventRepository
import javax.inject.Inject

class CreateEventUseCase @Inject constructor(
    private val eventRepository: EventRepository,
    private val geocoderManager: GeocoderManager
) {
    suspend operator fun invoke(
        name: String,
        description: String,
        date: Timestamp,
        locationName: String,
        category: String,
        organizerId: String,
        imageUri: Uri?
    ): Result<String> = try {
        // 1. Géocodage
        val location = geocoderManager.geocode(locationName)
        if (location == null) {
            return Result.failure(Exception("Adresse introuvable : $locationName"))
        }

        // 2. Construction de l'objet Event
        val event = Event(
            name = name.trim(),
            description = description.trim(),
            date = date,
            locationName = locationName.trim(),
            category = category,
            organizerId = organizerId,
            location = location, // resultat du geocodage
            imageUrl = "", // Sera mis à jour par le repo si image
            guests = emptyList()
        )

        // 3. Sauvegarde
        val eventId = eventRepository.createEvent(event, imageUri)
        Result.success(eventId)

    } catch (e: Exception) {
        Result.failure(e)
    }
}