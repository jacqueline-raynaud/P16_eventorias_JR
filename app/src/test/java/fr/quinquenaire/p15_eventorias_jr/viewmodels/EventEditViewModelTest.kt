package fr.quinquenaire.p15_eventorias_jr.viewmodels

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import fr.quinquenaire.p15_eventorias_jr.domain.model.Event
import fr.quinquenaire.p15_eventorias_jr.domain.model.EventCategory
import fr.quinquenaire.p15_eventorias_jr.domain.usecase.eventdetail.GetEventDetailUseCase
import fr.quinquenaire.p15_eventorias_jr.domain.usecase.eventlist.UpdateEventUseCase
import fr.quinquenaire.p15_eventorias_jr.presentation.eventedit.EventEditViewModel
import fr.quinquenaire.p15_eventorias_jr.presentation.eventedit.contract.EventEditAction
import fr.quinquenaire.p15_eventorias_jr.presentation.eventedit.contract.EventEditEffect
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class EventEditViewModelTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerLeaf

    val testDispatcher = UnconfinedTestDispatcher()
    beforeSpec { Dispatchers.setMain(testDispatcher) }
    afterSpec { Dispatchers.resetMain() }

    val eventId = "event123"
    val organizerId = "uid-organizer"

    val referenceCalendar = Calendar.getInstance().apply {
        set(2026, Calendar.JULY, 12, 19, 30, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val referenceTimestamp = Timestamp(referenceCalendar.time)
    val referenceLocation = GeoPoint(48.8584, 2.2945)

    val fakeEvent = Event(
        id = eventId,
        name = "Concert",
        description = "Un super concert",
        date = referenceTimestamp,
        locationName = "Paris",
        location = referenceLocation,
        category = "MUSIQUE",
        imageUrl = "https://storage/image.jpg",
        organizerId = organizerId
    )

    fun buildViewModel(
        eventFlow: Flow<Event?> = flowOf(fakeEvent),
        updateError: String? = null
    ): Triple<EventEditViewModel, UpdateEventUseCase, CapturingSlot<Event>> {
        val getEventDetailUseCase = mockk<GetEventDetailUseCase>()
        val updateEventUseCase = mockk<UpdateEventUseCase>()
        val eventSlot = slot<Event>()

        every { getEventDetailUseCase(eventId) } returns eventFlow

        if (updateError == null) {
            coEvery { updateEventUseCase(any(), capture(eventSlot), any()) } returns Result.success(Unit)
        } else {
            coEvery { updateEventUseCase(any(), any(), any()) } returns Result.failure(Exception(updateError))
        }

        val viewModel = EventEditViewModel(
            savedStateHandle = SavedStateHandle(mapOf("eventId" to eventId)),
            getEventDetailUseCase = getEventDetailUseCase,
            updateEventUseCase = updateEventUseCase
        )
        return Triple(viewModel, updateEventUseCase, eventSlot)
    }

    // --- Tests de chargement ---
    Given("un événement existant à charger") {
        val (viewModel, _, _) = buildViewModel()
        Then("le formulaire est pré-rempli") {
            viewModel.uiState.value.name shouldBe "Concert"
        }
    }

    Given("l'événement à charger est introuvable") {
        val (viewModel, _, _) = buildViewModel(eventFlow = flowOf(null))
        Then("l'état porte l'erreur 'Event not found'") {
            viewModel.uiState.value.error shouldBe "Event not found"
            viewModel.uiState.value.isLoading shouldBe false
        }
    }

    Given("le chargement initial a échoué") {
        val getEventDetailUseCase = mockk<GetEventDetailUseCase>()
        val updateEventUseCase = mockk<UpdateEventUseCase>()
        every { getEventDetailUseCase(eventId) } returns flow { throw RuntimeException("boom") }

        val viewModel = EventEditViewModel(SavedStateHandle(mapOf("eventId" to eventId)), getEventDetailUseCase, updateEventUseCase)

        When("handleAction(OnRetry)") {
            every { getEventDetailUseCase(eventId) } returns flowOf(fakeEvent)
            viewModel.handleAction(EventEditAction.OnRetry)
            Then("le formulaire se charge enfin") {
                verify(exactly = 2) { getEventDetailUseCase(eventId) }
                viewModel.uiState.value.name shouldBe "Concert"
            }
        }
    }

// --- Mise à jour du formulaire ---

    Given("les actions de mise à jour du formulaire") {

        When("OnNameChange") {
            val (viewModel, _, _) = buildViewModel()
            viewModel.handleAction(EventEditAction.OnNameChange("Nouveau nom"))
            Then("le nom est mis à jour") {
                viewModel.uiState.value.name shouldBe "Nouveau nom"
            }
        }

        When("OnDescriptionChange") {
            val (viewModel, _, _) = buildViewModel()
            viewModel.handleAction(EventEditAction.OnDescriptionChange("Nouvelle desc"))
            Then("la description est mise à jour") {
                viewModel.uiState.value.description shouldBe "Nouvelle desc"
            }
        }

        When("OnCategoryChange") {
            val (viewModel, _, _) = buildViewModel()
            viewModel.handleAction(EventEditAction.OnCategoryChange(EventCategory.MUSIQUE))
            Then("la catégorie est mise à jour") {
                viewModel.uiState.value.category shouldBe EventCategory.MUSIQUE
            }
        }

        When("OnDateSelected") {
            val (viewModel, _, _) = buildViewModel()
            viewModel.handleAction(EventEditAction.OnDateSelected(123456789L))
            Then("la date est mise à jour") {
                viewModel.uiState.value.dateMillis shouldBe 123456789L
            }
        }

        When("OnTimeSelected") {
            val (viewModel, _, _) = buildViewModel()
            viewModel.handleAction(EventEditAction.OnTimeSelected(20, 45))
            Then("l'heure et les minutes sont mises à jour") {
                viewModel.uiState.value.hour shouldBe 20
                viewModel.uiState.value.minute shouldBe 45
            }
        }

        When("OnImageSelected") {
            val (viewModel, _, _) = buildViewModel()
            val uri = mockk<Uri>()
            viewModel.handleAction(EventEditAction.OnImageSelected(uri))
            Then("l'image est mise à jour") {
                viewModel.uiState.value.imageUri shouldBe uri
            }
        }

        When("OnBackClick") {
            val (viewModel, _, _) = buildViewModel()
            Then("l'effet NavigateBack est émis") {
                viewModel.effect.test {
                    viewModel.handleAction(EventEditAction.OnBackClick)
                    awaitItem() shouldBe EventEditEffect.NavigateBack
                }
            }
        }
    }

    // --- Tests de sauvegarde ---
    Given("l'adresse n'a pas été modifiée") {
        val (viewModel, updateEventUseCase, eventSlot) = buildViewModel()

        When("OnSaveClick") {
            Then("la localisation existante est conservée") {
                viewModel.effect.test {
                    viewModel.handleAction(EventEditAction.OnSaveClick)
                    awaitItem() shouldBe EventEditEffect.NavigateBack
                }
                coVerify { updateEventUseCase(eventId, capture(eventSlot), null) }
                eventSlot.captured.location shouldBe referenceLocation
                eventSlot.captured.locationName shouldBe "Paris"
            }
        }
    }

    Given("l'adresse a été modifiée") {
        val (viewModel, updateEventUseCase, eventSlot) = buildViewModel()
        viewModel.handleAction(EventEditAction.OnAddressChange("Lyon"))

        When("OnSaveClick") {
            Then ("la localisation est mise à null pour le géocodage")
            viewModel.effect.test {
                viewModel.handleAction(EventEditAction.OnSaveClick)
                awaitItem() shouldBe EventEditEffect.NavigateBack
            }
            coVerify { updateEventUseCase(eventId, capture(eventSlot), null) }
            eventSlot.captured.location shouldBe null
            eventSlot.captured.locationName shouldBe "Lyon"
            }
        }

    Given("le formulaire est invalide") {
        val (viewModel, updateEventUseCase, _) = buildViewModel()
        viewModel.handleAction(EventEditAction.OnNameChange("")) // adapte au champ qui invalide

        When("OnSaveClick") {
            viewModel.handleAction(EventEditAction.OnSaveClick)
            Then("aucune sauvegarde n'est déclenchée") {
                coVerify(exactly = 0) { updateEventUseCase(any(), any(), any()) }
            }
        }
    }


    Given("la sauvegarde échoue") {
        val (viewModel, _, _) = buildViewModel(updateError = "Erreur de mise à jour")

        When("OnSaveClick") {
            Then("un snackbar d'erreur s'affiche") {
                viewModel.effect.test {
                    viewModel.handleAction(EventEditAction.OnSaveClick)
                    awaitItem() shouldBe EventEditEffect.ShowSnackbar("Erreur de mise à jour")
                }
                viewModel.uiState.value.isSaving shouldBe false
            }
        }
    }
})