package fr.quinquenaire.p15_eventorias_jr.states


import android.net.Uri
import fr.quinquenaire.p15_eventorias_jr.domain.model.EventCategory
import fr.quinquenaire.p15_eventorias_jr.presentation.eventedit.model.EventEditMutableState
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import java.util.Calendar

class EventEditMutableStateTest : BehaviorSpec({

// --- validité du formulaire ---

    Given("la validation du formulaire") {
        val baseValide = EventEditMutableState(
            name = "Concert",
            category = EventCategory.MUSIQUE,
            dateMillis = 123L,
            hour = 20,
            address = "Paris"
        )

        Then("un formulaire complet est valide") {
            baseValide.isFormValid shouldBe true
        }
        Then("un nom vide invalide le formulaire") {
            baseValide.copy(name = "").isFormValid shouldBe false
        }
        Then("une catégorie nulle invalide le formulaire") {
            baseValide.copy(category = null).isFormValid shouldBe false
        }
        Then("une date nulle invalide le formulaire") {
            baseValide.copy(dateMillis = null).isFormValid shouldBe false
        }
        Then("une heure nulle invalide le formulaire") {
            baseValide.copy(hour = null).isFormValid shouldBe false
        }
        Then("une adresse vide invalide le formulaire") {
            baseValide.copy(address = "").isFormValid shouldBe false
        }
    }

/// --- formatage date et heure ---

    Given("les labels formatés") {

        Then("dateLabel formate une date en français avec majuscule initiale") {
            // Midi pour éviter toute bascule de jour entre le poste (FR) et le runner (UTC)
            val millis = Calendar.getInstance().apply {
                set(2026, Calendar.JULY, 12, 12, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val state = EventEditMutableState(dateMillis = millis)
            state.dateLabel.first().isUpperCase() shouldBe true
            state.dateLabel.contains("2026") shouldBe true
        }

        Then("dateLabel est vide quand la date est nulle") {
            EventEditMutableState(dateMillis = null).dateLabel shouldBe ""
        }

        Then("timeLabel formate heure et minute sur deux chiffres") {
            EventEditMutableState(hour = 9, minute = 5).timeLabel shouldBe "09h05"
        }

        Then("timeLabel est vide si l'heure est nulle") {
            EventEditMutableState(hour = null, minute = 30).timeLabel shouldBe ""
        }

        Then("timeLabel est vide si la minute est nulle") {
            EventEditMutableState(hour = 20, minute = null).timeLabel shouldBe ""
        }
    }

    // --- test de l'image ---
    Given("l'aperçu d'image") {

        Then("privilégie la nouvelle image locale si présente") {
            val uri = mockk<Uri>()
            val state = EventEditMutableState(imageUri = uri, existingImageUrl = "http://old.url")
            state.imagePreview shouldBe uri
        }

        Then("retombe sur l'image existante si pas de nouvelle image") {
            val state = EventEditMutableState(imageUri = null, existingImageUrl = "http://old.url")
            state.imagePreview shouldBe "http://old.url"
        }

        Then("est null si aucune image n'est disponible") {
            val state = EventEditMutableState(imageUri = null, existingImageUrl = "")
            state.imagePreview shouldBe null
        }
    }
})