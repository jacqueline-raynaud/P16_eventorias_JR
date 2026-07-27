package fr.quinquenaire.p15_eventorias_jr.data.location

import android.content.Context
import android.location.Geocoder
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.quinquenaire.p15_eventorias_jr.domain.location.GeocoderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

class AndroidGeocoderManager @Inject constructor(
    @ApplicationContext private val context: Context
) : GeocoderManager {
    /**
     * Convertit une adresse texte en GeoPoint.
     * Retourne null si l'adresse est introuvable.
     * Lance IOException si le service est indisponible (pas de réseau...).
     */
    override suspend fun geocode(address: String): GeoPoint? = withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())

        @Suppress("DEPRECATION")   // getFromLocationName sync : seule API < 33
        val results = geocoder.getFromLocationName(address, 1)

        results?.firstOrNull()?.let { GeoPoint(it.latitude, it.longitude) }
    }
}