package fr.quinquenaire.p15_eventorias_jr.domain.location

import com.google.firebase.firestore.GeoPoint

interface GeocoderManager {
    suspend fun geocode(address: String): GeoPoint?
}