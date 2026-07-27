package fr.quinquenaire.p15_eventorias_jr.usecases.eventlist

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import fr.quinquenaire.p15_eventorias_jr.domain.location.GeocoderManager
import fr.quinquenaire.p15_eventorias_jr.domain.model.Event
import fr.quinquenaire.p15_eventorias_jr.domain.repository.EventRepository
import fr.quinquenaire.p15_eventorias_jr.domain.usecase.eventlist.CreateEventUseCase
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.Date

class CreateEventUseCaseTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerLeaf

    // Mocks communs
    val eventRepository = mockk<EventRepository>()
    val geocoderManager = mockk<GeocoderManager>()
    val useCase = CreateEventUseCase(eventRepository, geocoderManager)

    Given("des paramètres d'événement valides") {
        val name = "Concert"
        val description = "Super concert"
        val date = Timestamp(Date())
        val locationName = "Paris"
        val category = "Musique"
        val organizerId = "uid1"
        val imageUri = mockk<Uri>()
        val geoPoint = GeoPoint(48.8566, 2.3522)

        When("le géocodage réussit") {
            coEvery { geocoderManager.geocode(locationName) } returns geoPoint
            // On utilise "any()" pour l'event car il est construit à l'intérieur du UseCase
            coEvery { eventRepository.createEvent(any(), imageUri) } returns "newEventId"

            val result = useCase(name, description, date, locationName, category, organizerId, imageUri)

            Then("il retourne un succès avec l'ID") {
                result.isSuccess shouldBe true
                result.getOrNull() shouldBe "newEventId"
                coVerify { eventRepository.createEvent(any(), imageUri) }
            }
        }

        When("le géocodage échoue (adresse introuvable)") {
            coEvery { geocoderManager.geocode(locationName) } returns null

            val result = useCase(name, description, date, locationName, category, organizerId, imageUri)

            Then("il retourne un échec avec une exception") {
                result.isFailure shouldBe true
                result.exceptionOrNull()?.message shouldBe "Adresse introuvable : $locationName"
            }
        }
    }
})