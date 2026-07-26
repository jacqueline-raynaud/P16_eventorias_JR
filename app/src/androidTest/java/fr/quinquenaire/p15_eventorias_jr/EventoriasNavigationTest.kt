package fr.quinquenaire.p15_eventorias_jr

import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import fr.quinquenaire.p15_eventorias_jr.domain.model.Event
import fr.quinquenaire.p15_eventorias_jr.domain.model.UserProfile
import fr.quinquenaire.p15_eventorias_jr.domain.repository.EventRepository
import fr.quinquenaire.p15_eventorias_jr.domain.repository.UserProfileRepository
import fr.quinquenaire.p15_eventorias_jr.fake.FakeEventRepository
import fr.quinquenaire.p15_eventorias_jr.fake.FakeUserProfileRepository
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class EventoriasNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createEmptyComposeRule()

    @get:Rule(order = 2)
    // accord automatique des permissions pour éviter les popups bloquants sur github actions
    val permissionRule: GrantPermissionRule = if (Build.VERSION.SDK_INT >= 33) {
        // Sur API 33+ (comme GitHub), on demande tout
        GrantPermissionRule.grant(
            android.Manifest.permission.POST_NOTIFICATIONS,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else {
        // Sur les vieux émulateurs ( PC ), on ignore POST_NOTIFICATIONS
        GrantPermissionRule.grant(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    @Inject lateinit var eventRepository: EventRepository
    @Inject lateinit var userProfileRepository: UserProfileRepository

    private lateinit var scenario: ActivityScenario<MainActivity>

    private val organizerId = "fake_current_uid"

    private val fakeEvent = Event(
        id = "event1",
        name = "Soirée Jazz au Parc",
        description = "Une soirée inoubliable sous les étoiles.",
        date = Timestamp.now(),
        locationName = "Parc de la Tête d'Or, Lyon",
        location = GeoPoint(45.7597, 4.8422),
        category = "MUSIQUE",
        imageUrl = "",
        organizerId = organizerId
    )

    @Before
    fun setup() {
        hiltRule.inject()

        (eventRepository as FakeEventRepository).setEvents(listOf(fakeEvent))
        (userProfileRepository as FakeUserProfileRepository).apply {
            // initialisation du profil de l'organisateur
            setCurrentUserId(null)  // ← Réinitialiser d'abord
            setProfiles(emptyList())  // ← Réinitialiser d'abord
            // données à utiliser
            setCurrentUserId(organizerId)
            setProfiles(
                listOf(
                    UserProfile(
                        uid = organizerId,
                        firstName = "Jacqueline",
                        lastName = "G",
                        email = "j@test.com",
                        avatarUrl = "",
                        notificationEnabled = false
                    )
                )
            )
        }
        (eventRepository as FakeEventRepository).setEvents(listOf(fakeEvent))
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    // -----------------------------------------------------------
    // Parcours 1 : liste -> détail -> édition -> annulation suppression -> retours
    // -----------------------------------------------------------

 @Test
    fun parcours1_detailEditCancelDeleteBackBack() {
        // 1. Liste -> clic sur l'événement -> détail
        composeTestRule.onNodeWithText("Soirée Jazz au Parc").performClick()
        composeTestRule.onNodeWithText("Une soirée inoubliable sous les étoiles.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Event Detail Menu").assertIsDisplayed()


        // 2. Menu -> Modifier -> EventEdit, pré-rempli avec le bon événement
        composeTestRule.onNodeWithContentDescription("Event Detail Menu").performClick()
        composeTestRule.onNodeWithText("Edit Event").performClick()
        composeTestRule.onNodeWithText("Soirée Jazz au Parc").assertIsDisplayed() // champ nom pré-rempli

        // 3. Save Changes -> retour détail
        composeTestRule.onNodeWithText("Save changes")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText("Une soirée inoubliable sous les étoiles.")
            .assertIsDisplayed()

        // 4. Menu -> Supprimer -> boîte de dialogue affichée
        composeTestRule.onNodeWithContentDescription("Event Detail Menu").performClick()
        composeTestRule.onNodeWithText("Delete Event").performClick()
        composeTestRule.onNodeWithText("Did your confirm delete event ?").assertIsDisplayed()

        // 5. Cancel -> retour détail (pas de suppression)
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.onNodeWithText("Une soirée inoubliable sous les étoiles.")
            .assertIsDisplayed()

        // 6. Menu -> Modifier à nouveau -> même événement
        composeTestRule.onNodeWithContentDescription("Event Detail Menu").performClick()
        composeTestRule.onNodeWithText("Edit Event").performClick()
        composeTestRule.onNodeWithText("Soirée Jazz au Parc").assertIsDisplayed()

        // 7. Flèche retour -> retour détail
        composeTestRule.onNodeWithContentDescription("Back to the list").performClick()
        composeTestRule.onNodeWithText("Une soirée inoubliable sous les étoiles.")
            .assertIsDisplayed()

        // 8. Flèche retour -> retour à la liste
        composeTestRule.onNodeWithContentDescription("Back to the list").performClick()
        composeTestRule.onNodeWithText("Soirée Jazz au Parc").assertIsDisplayed() // dans la liste, cette fois
    }

    // -----------------------------------------------------------
    // Parcours 2 : navigation BottomBar
    // -----------------------------------------------------------

    @Test
    fun parcours2_bottomBarNavigation() {
        // On démarre sur la liste
        composeTestRule.onNodeWithText("Soirée Jazz au Parc").assertIsDisplayed()

        // Onglet Profil
        composeTestRule.onNodeWithText("Profil").performClick()
        composeTestRule.onNodeWithText("Jacqueline").assertIsDisplayed()

        // Onglet Événements -> retour à la liste
        composeTestRule.onNodeWithText("Événements").performClick()
        composeTestRule.onNodeWithText("Soirée Jazz au Parc").assertIsDisplayed()
    }
}