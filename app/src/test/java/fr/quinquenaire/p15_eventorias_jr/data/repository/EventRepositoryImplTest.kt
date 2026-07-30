package fr.quinquenaire.p15_eventorias_jr.data.repository

import android.net.Uri
import android.util.Log
import app.cash.turbine.test
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import fr.quinquenaire.p15_eventorias_jr.data.remote.FirebaseFirestoreManager
import fr.quinquenaire.p15_eventorias_jr.data.remote.FirebaseStorageManager
import fr.quinquenaire.p15_eventorias_jr.domain.EventQueryParams
import fr.quinquenaire.p15_eventorias_jr.domain.SortOrder
import fr.quinquenaire.p15_eventorias_jr.domain.model.Event
import fr.quinquenaire.p15_eventorias_jr.domain.model.EventCategory
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.verify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class EventRepositoryImplTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerLeaf

    beforeSpec {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    afterSpec {
        unmockkStatic(Log::class)
    }

    val firestore = mockk<FirebaseFirestore>()
    val firestoreManager = mockk<FirebaseFirestoreManager>()
    val storageManager = mockk<FirebaseStorageManager>()

    val repository = EventRepositoryImpl(firestore, firestoreManager, storageManager)

    Given("getEventDetail") {
        val eventId = "1"
        val event = Event(id = eventId, name = "Event 1")
        every { firestoreManager.getEventDetail(eventId) } returns flowOf(event)

        When("appel de getEventDetail") {
            Then("doit retourner le flow de firestoreManager") {
                repository.getEventDetail(eventId).collect {
                    it shouldBe event
                }
            }
        }
    }

    Given("createEvent") {
        val event = Event(name = "New Event")
        val imageUri = mockk<Uri>()
        val eventId = "generated_id"

        When("une image est fournie") {
            coEvery { firestoreManager.createEvent(event) } returns eventId
            coEvery {
                storageManager.uploadEventImage(
                    eventId,
                    imageUri
                )
            } returns "http://image.url"
            coEvery {
                firestoreManager.updateEventImageUrl(
                    eventId,
                    "http://image.url"
                )
            } returns Unit

            runTest {
                val result = repository.createEvent(event, imageUri)
                result shouldBe eventId
            }

            Then("on crée l'event puis on upload l'image") {
                coVerify { firestoreManager.createEvent(event) }
                coVerify { storageManager.uploadEventImage(eventId, imageUri) }
                coVerify { firestoreManager.updateEventImageUrl(eventId, "http://image.url") }
            }
        }

        When("aucune image n'est fournie") {
            coEvery { firestoreManager.createEvent(event) } returns eventId

            runTest {
                val result = repository.createEvent(event, null)
                result shouldBe eventId
            }

            Then("seul firestoreManager.createEvent est appelé") {
                coVerify { firestoreManager.createEvent(event) }
                coVerify(exactly = 0) { storageManager.uploadEventImage(any(), any()) }
            }
        }
    }

    Given("updateEvent") {
        val event = Event(id = "1", name = "Updated Event", imageUrl = "old_url")

        When("une nouvelle image est fournie") {
            val imageUri = mockk<Uri>()
            val newImageUrl = "http://new.url"
            coEvery { storageManager.uploadEventImage("1", imageUri) } returns newImageUrl
            coEvery { firestoreManager.updateEvent(any()) } returns Unit

            runTest {
                repository.updateEvent(event, imageUri)
            }

            Then("l'image est uploadée et l'event mis à jour avec la nouvelle URL") {
                coVerify { storageManager.uploadEventImage("1", imageUri) }
                coVerify { firestoreManager.updateEvent(match { it.imageUrl == newImageUrl }) }
            }
        }

        When("aucune image n'est fournie") {
            coEvery { firestoreManager.updateEvent(any()) } returns Unit

            runTest {
                repository.updateEvent(event, null)
            }

            Then("l'event est mis à jour avec son URL existante") {
                coVerify { firestoreManager.updateEvent(match { it.imageUrl == "old_url" }) }
                coVerify(exactly = 0) { storageManager.uploadEventImage(any(), any()) }
            }
        }
    }

    Given("deleteEvent") {
        val eventId = "1"
        val imageUrl = "http://image.url"

        When("suppression avec image") {
            coEvery { firestoreManager.deleteEvent(eventId) } returns Unit
            coEvery { storageManager.deleteEventImage(eventId) } returns Unit

            runTest {
                repository.deleteEvent(eventId, imageUrl)
            }

            Then("on supprime le document et l'image") {
                coVerify { firestoreManager.deleteEvent(eventId) }
                coVerify { storageManager.deleteEventImage(eventId) }
            }
        }
    }

    Given("anonymizeOrganizerEvents") {
        val uid = "user_123"
        coEvery { firestoreManager.anonymizeOrganizerEvents(uid) } returns Unit

        When("appel de anonymizeOrganizerEvents") {
            runTest {
                repository.anonymizeOrganizerEvents(uid)
            }
            Then("doit déléguer à firestoreManager") {
                coVerify { firestoreManager.anonymizeOrganizerEvents(uid) }
            }
        }
    }

    Given("getEventsStream") {

        // --- Socle commun : chaîne de requête Firestore mockée ---
        val collectionRef = mockk<CollectionReference>()
        val query = mockk<Query>()
        val registration = mockk<ListenerRegistration>(relaxed = true)
        val listenerSlot = slot<EventListener<QuerySnapshot>>()

        every { firestore.collection("events") } returns collectionRef
        // whereEqualTo/orderBy peuvent être appelés sur collectionRef OU sur query
        every { collectionRef.whereEqualTo(any<String>(), any()) } returns query
        every { collectionRef.orderBy(any<String>(), any<Query.Direction>()) } returns query
        every { query.whereEqualTo(any<String>(), any()) } returns query
        every { query.orderBy(any<String>(), any<Query.Direction>()) } returns query
        every { query.limit(any()) } returns query
        every { query.addSnapshotListener(capture(listenerSlot)) } returns registration

        // Helper : construit un QuerySnapshot à partir d'une liste d'events
        fun mockSnapshot(events: List<Event>): QuerySnapshot {
            val snapshot = mockk<QuerySnapshot>()
            val docs = events.map { ev ->
                val doc = mockk<DocumentSnapshot>()
                every { doc.toObject(Event::class.java) } returns ev
                every { doc.id } returns ev.id
                doc
            }
            every { snapshot.documents } returns docs
            return snapshot
        }

        // --- CAS 1 : catégorie nulle + tri DEFAULT + pas de recherche ---
        When("snapshot avec events, sans filtre") {
            val params = EventQueryParams(
                category = null,
                sortOrder = SortOrder.DEFAULT,
                searchQuery = "",
                limit = 20
            )
            val event = Event(id = "doc1", name = "Concert")
            Then("le flow émet la liste mappée") {
                runTest {
                    repository.getEventsStream(params).test {
                        listenerSlot.captured.onEvent(mockSnapshot(listOf(event)), null)
                        awaitItem() shouldBe listOf(event)
                        cancelAndIgnoreRemainingEvents()
                    }
                }
            }
        }

        // --- CAS 2 : catégorie NON nulle → branche whereEqualTo ---
        When("une catégorie est fournie") {
            val params = EventQueryParams(
                category = "MUSIQUE",
                sortOrder = SortOrder.DEFAULT,
                searchQuery = "",
                limit = 20
            )
            Then("whereEqualTo est appelé sur la catégorie") {
                runTest {
                    repository.getEventsStream(params).test {
                        listenerSlot.captured.onEvent(mockSnapshot(emptyList()), null)
                        awaitItem() shouldBe emptyList()
                        cancelAndIgnoreRemainingEvents()
                    }
                }
                verify { collectionRef.whereEqualTo("category", "MUSIQUE") }
            }
        }

        // --- CAS 3 : tri ASC → branche BY_DATE_ASC ---
        When("tri par date ascendante") {
            val params = EventQueryParams(
                category = null,
                sortOrder = SortOrder.BY_DATE_ASC,
                searchQuery = "",
                limit = 20
            )
            Then("orderBy ASCENDING est appelé") {
                runTest {
                    repository.getEventsStream(params).test {
                        listenerSlot.captured.onEvent(mockSnapshot(emptyList()), null)
                        awaitItem() shouldBe emptyList()
                        cancelAndIgnoreRemainingEvents()
                    }
                }
                verify { collectionRef.orderBy("date", Query.Direction.ASCENDING) }
            }
        }

        // --- CAS 4 : tri DESC → branche BY_DATE_DESC ---
        When("tri par date descendante") {
            val params = EventQueryParams(
                category = null,
                sortOrder = SortOrder.BY_DATE_DESC,
                searchQuery = "",
                limit = 20
            )
            Then("orderBy DESCENDING est appelé") {
                runTest {
                    repository.getEventsStream(params).test {
                        listenerSlot.captured.onEvent(mockSnapshot(emptyList()), null)
                        awaitItem() shouldBe emptyList()
                        cancelAndIgnoreRemainingEvents()
                    }
                }
                verify { collectionRef.orderBy("date", Query.Direction.DESCENDING) }
            }
        }

        // --- CAS 5 : recherche qui matche le NOM (1er opérande du ||) ---
        When("recherche correspondant au nom") {
            val params = EventQueryParams(
                category = null,
                sortOrder = SortOrder.DEFAULT,
                searchQuery = "jazz",
                limit = 20
            )
            val match = Event(id = "1", name = "Soirée Jazz", locationName = "Paris", description = "x")
            val noMatch = Event(id = "2", name = "Foot", locationName = "Lyon", description = "y")
            Then("seul l'event correspondant est émis") {
                runTest {
                    repository.getEventsStream(params).test {
                        listenerSlot.captured.onEvent(mockSnapshot(listOf(match, noMatch)), null)
                        awaitItem() shouldBe listOf(match)
                        cancelAndIgnoreRemainingEvents()
                    }
                }
            }
        }

        // --- CAS 6 : recherche qui matche le LIEU (2e opérande) ---
        When("recherche correspondant au lieu") {
            val params = EventQueryParams(
                category = null,
                sortOrder = SortOrder.DEFAULT,
                searchQuery = "marseille",
                limit = 20
            )
            val match = Event(id = "1", name = "Foot", locationName = "Marseille", description = "x")
            Then("l'event est trouvé via locationName") {
                runTest {
                    repository.getEventsStream(params).test {
                        listenerSlot.captured.onEvent(mockSnapshot(listOf(match)), null)
                        awaitItem() shouldBe listOf(match)
                        cancelAndIgnoreRemainingEvents()
                    }
                }
            }
        }

        // --- CAS 7 : recherche qui matche la DESCRIPTION (3e opérande) ---
        When("recherche correspondant à la description") {
            val params = EventQueryParams(
                category = null,
                sortOrder = SortOrder.DEFAULT,
                searchQuery = "gratuit",
                limit = 20
            )
            val match = Event(id = "1", name = "Foot", locationName = "Lyon", description = "Entrée gratuite")
            Then("l'event est trouvé via description") {
                runTest {
                    repository.getEventsStream(params).test {
                        listenerSlot.captured.onEvent(mockSnapshot(listOf(match)), null)
                        awaitItem() shouldBe listOf(match)
                        cancelAndIgnoreRemainingEvents()
                    }
                }
            }
        }

        // --- CAS 8 : recherche sans correspondance → liste vide ---
        When("recherche sans correspondance") {
            val params = EventQueryParams(
                category = null,
                sortOrder = SortOrder.DEFAULT,
                searchQuery = "xyz_introuvable",
                limit = 20
            )
            val event = Event(id = "1", name = "Foot", locationName = "Lyon", description = "match")
            Then("aucun event n'est émis") {
                runTest {
                    repository.getEventsStream(params).test {
                        listenerSlot.captured.onEvent(mockSnapshot(listOf(event)), null)
                        awaitItem() shouldBe emptyList()
                        cancelAndIgnoreRemainingEvents()
                    }
                }
            }
        }

        // --- CAS 9 : erreur Firestore → le flow se ferme en erreur ---
        When("Firestore renvoie une erreur") {
            val params = EventQueryParams(
                category = null,
                sortOrder = SortOrder.DEFAULT,
                searchQuery = "",
                limit = 20
            )
            val exception = mockk<com.google.firebase.firestore.FirebaseFirestoreException>(relaxed = true)
            Then("le flow propage l'erreur") {
                runTest {
                    repository.getEventsStream(params).test {
                        listenerSlot.captured.onEvent(null, exception)
                        awaitError()
                    }
                }
            }
        }

        // --- CAS 10 : snapshot null → liste vide (branche ?: emptyList) ---
        When("le snapshot est null") {
            val params = EventQueryParams(
                category = null,
                sortOrder = SortOrder.DEFAULT,
                searchQuery = "",
                limit = 20
            )
            Then("le flow émet une liste vide") {
                runTest {
                    repository.getEventsStream(params).test {
                        listenerSlot.captured.onEvent(null, null)
                        awaitItem() shouldBe emptyList()
                        cancelAndIgnoreRemainingEvents()
                    }
                }
            }
        }
    }
})
