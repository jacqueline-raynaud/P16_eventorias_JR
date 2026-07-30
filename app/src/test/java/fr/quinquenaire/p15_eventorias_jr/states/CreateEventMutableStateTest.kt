package fr.quinquenaire.p15_eventorias_jr.states

import android.net.Uri
import fr.quinquenaire.p15_eventorias_jr.domain.model.EventCategory
import fr.quinquenaire.p15_eventorias_jr.presentation.eventcreation.model.CreateEventMutableState
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import java.util.Calendar

class CreateEventMutableStateTest : BehaviorSpec({

   // --- validité du formulaire ---

    Given("la validation du formulaire") {
        val baseValide = CreateEventMutableState(
            name = "Concert",
            category = EventCategory.MUSIQUE,
            dateMillis = 123L,
            hour = 20,
            minute = 30,
            address = "Paris"
        )

        Then("un formulaire complet est valide") {
            baseValide.isFormValid shouldBe true
        }
        Then("un nom vide (ou espaces) invalide le formulaire") {
            baseValide.copy(name = "   ").isFormValid shouldBe false
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
        Then("une minute nulle invalide le formulaire") {
            baseValide.copy(minute = null).isFormValid shouldBe false
        }
        Then("une adresse vide (ou espaces) invalide le formulaire") {
            baseValide.copy(address = "   ").isFormValid shouldBe false
        }
    }

    // --- Date et heure formatés ---
    Given("les labels formatés") {

        Then("dateLabel formate une date en français avec majuscule initiale") {
            // Midi pour éviter  bascule de jour entre le poste (FR) et le runner (UTC)
            val millis = Calendar.getInstance().apply {
                set(2026, Calendar.JULY, 12, 12, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val state = CreateEventMutableState(dateMillis = millis)
            state.dateLabel.first().isUpperCase() shouldBe true
            state.dateLabel.contains("2026") shouldBe true
        }

        Then("dateLabel est vide quand la date est nulle") {
            CreateEventMutableState(dateMillis = null).dateLabel shouldBe ""
        }

        Then("timeLabel formate heure et minute sur deux chiffres") {
            CreateEventMutableState(hour = 9, minute = 5).timeLabel shouldBe "09h05"
        }

        Then("timeLabel est vide si l'heure est nulle") {
            CreateEventMutableState(hour = null, minute = 30).timeLabel shouldBe ""
        }

        Then("timeLabel est vide si la minute est nulle") {
            CreateEventMutableState(hour = 20, minute = null).timeLabel shouldBe ""
        }
    }
})