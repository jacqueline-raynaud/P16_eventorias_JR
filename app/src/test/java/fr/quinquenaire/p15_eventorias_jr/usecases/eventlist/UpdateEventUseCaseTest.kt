package fr.quinquenaire.p15_eventorias_jr.usecases.eventlist

import android.net.Uri
import com.google.firebase.firestore.GeoPoint
import fr.quinquenaire.p15_eventorias_jr.domain.location.GeocoderManager
import fr.quinquenaire.p15_eventorias_jr.domain.model.Event
import fr.quinquenaire.p15_eventorias_jr.domain.repository.EventRepository
import fr.quinquenaire.p15_eventorias_jr.domain.usecase.eventlist.UpdateEventUseCase
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf

class UpdateEventUseCaseTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerLeaf

    val eventRepository = mockk<EventRepository>()
    val geocoderManager = mockk<GeocoderManager>()
    val useCase = UpdateEventUseCase(eventRepository, geocoderManager)

    Given("un événement existant") {
        val eventId = "event1"
        val oldLocation = "Paris"
        val oldGeoPoint = GeoPoint(48.8566, 2.3522)
        val currentEvent = Event(id = eventId, name = "Ancien Nom", locationName = oldLocation, location = oldGeoPoint)
        
        // On simule que le repo trouve bien l'événement actuel
        every { eventRepository.getEventDetail(eventId) } returns flowOf(currentEvent)

        When("on modifie le nom sans changer l'adresse") {
            val updatedEvent = currentEvent.copy(name = "Nouveau Nom")
            coEvery { eventRepository.updateEvent(any(), any()) } returns Unit

            val result = useCase(eventId, updatedEvent, null)

            Then("il retourne un succès et ne relance pas le géocodage") {
                result.isSuccess shouldBe true
                coVerify(exactly = 0) { geocoderManager.geocode(any()) }
                coVerify { eventRepository.updateEvent(match { it.name == "Nouveau Nom" && it.location == oldGeoPoint }, null) }
            }
        }

        When("on change l'adresse et le géocodage réussit") {
            val newLocation = "Lyon"
            val newGeoPoint = GeoPoint(45.7640, 4.8357)
            val updatedEvent = currentEvent.copy(locationName = newLocation, location=null)
            
            coEvery { geocoderManager.geocode(newLocation) } returns newGeoPoint
            coEvery { eventRepository.updateEvent(any(), any()) } returns Unit

            val result = useCase(eventId, updatedEvent, null)

            Then("il met à jour la localisation avec les nouvelles coordonnées") {
                result.isSuccess shouldBe true
                coVerify { geocoderManager.geocode(newLocation) }
                coVerify { eventRepository.updateEvent(match { it.location == newGeoPoint }, null) }
            }
        }
    }
})
