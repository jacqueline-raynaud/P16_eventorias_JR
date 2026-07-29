package fr.quinquenaire.p15_eventorias_jr.states

import android.net.Uri
import fr.quinquenaire.p15_eventorias_jr.presentation.userprofile.model.UserProfileMutableState
import fr.quinquenaire.p15_eventorias_jr.presentation.userprofile.model.UserProfileUiState
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

class UserProfileMutableStateTest : BehaviorSpec({

    // NB : adapte le constructeur de UserProfileUiState à ta vraie signature.
    // On suppose ici qu'il expose au moins firstName et lastName.
    fun profil(firstName: String = "Jean", lastName: String = "Dupont", avatarUrl: String = "https://storage/avatar.jpg") =
        UserProfileUiState(firstName = firstName, lastName = lastName, avatarUrl = avatarUrl)

// --- modification des données ---
    Given("hasChanges") {

        Then("est faux quand aucun champ n'a été édité") {
            UserProfileMutableState().hasChanges shouldBe false
        }
        Then("est vrai si le prénom a été édité") {
            UserProfileMutableState(editedFirstName = "Nouveau").hasChanges shouldBe true
        }
        Then("est vrai si le nom a été édité") {
            UserProfileMutableState(editedLastName = "Nouveau").hasChanges shouldBe true
        }
        Then("est vrai si l'avatar a été édité") {
            UserProfileMutableState(editedAvatarUri = mockk<Uri>()).hasChanges shouldBe true
        }
    }

// --- affichage des données ---

    Given("displayedFirstName") {

        Then("affiche le brouillon si présent (priorité)") {
            val state = UserProfileMutableState(
                profile = profil(firstName = "Jean"),
                editedFirstName = "Brouillon"
            )
            state.displayedFirstName shouldBe "Brouillon"
        }
        Then("retombe sur le profil Firestore si pas de brouillon") {
            val state = UserProfileMutableState(profile = profil(firstName = "Jean"))
            state.displayedFirstName shouldBe "Jean"
        }
        Then("est une chaîne vide si ni brouillon ni profil") {
            val state = UserProfileMutableState(profile = null, editedFirstName = null)
            state.displayedFirstName shouldBe ""
        }
    }

    Given("displayedLastName") {

        Then("affiche le brouillon si présent (priorité)") {
            val state = UserProfileMutableState(
                profile = profil(lastName = "Dupont"),
                editedLastName = "Brouillon"
            )
            state.displayedLastName shouldBe "Brouillon"
        }
        Then("retombe sur le profil Firestore si pas de brouillon") {
            val state = UserProfileMutableState(profile = profil(lastName = "Dupont"))
            state.displayedLastName shouldBe "Dupont"
        }
        Then("est une chaîne vide si ni brouillon ni profil") {
            val state = UserProfileMutableState(profile = null, editedLastName = null)
            state.displayedLastName shouldBe ""
        }
    }
})