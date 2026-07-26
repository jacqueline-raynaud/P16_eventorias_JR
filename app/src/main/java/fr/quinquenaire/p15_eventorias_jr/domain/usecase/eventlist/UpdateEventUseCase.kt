package fr.quinquenaire.p15_eventorias_jr.domain.usecase.eventlist

import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.GeoPoint
import fr.quinquenaire.p15_eventorias_jr.domain.location.GeocoderManager
import fr.quinquenaire.p15_eventorias_jr.domain.model.Event
import fr.quinquenaire.p15_eventorias_jr.domain.repository.EventRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpdateEventUseCase @Inject constructor(
    private val eventRepository: EventRepository,
    private val geocoderManager: GeocoderManager
) {
    suspend operator fun invoke(
        eventId: String,
        updatedEvent: Event,
        imageUri: Uri?
    ): Result<Unit> = try {

       val finalLocation = if (updatedEvent.location == null) {
           val newLocation = geocoderManager.geocode(updatedEvent.locationName)
           newLocation ?: return Result.failure(Exception("Adresse introuvable : ${updatedEvent.locationName}"))
       } else {
           updatedEvent.location
       }

       // 3. objet à sauvegarder
       val eventToSave = updatedEvent.copy(location = finalLocation)

       // 4. Appel au Repository pour la persistance (et upload image si besoin)
       eventRepository.updateEvent(eventToSave, imageUri)

       Result.success(Unit)

   } catch (e: Exception) {
       Result.failure(e)
   }
}