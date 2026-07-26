package fr.quinquenaire.p15_eventorias_jr.fake

import com.google.firebase.firestore.GeoPoint
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import fr.quinquenaire.p15_eventorias_jr.domain.location.GeocoderManager
import fr.quinquenaire.p15_eventorias_jr.di.GeocoderModule
import io.mockk.coEvery
import io.mockk.mockk
import javax.inject.Singleton

@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [GeocoderModule::class]
)
@Module
object TestGeocoderModule {

    @Provides
    @Singleton
    fun provideGeocoderManager(): GeocoderManager {
        // On implémente juste l'interface, pas de constructeur capricieux, pas de Context !
        return object : GeocoderManager {
            override suspend fun geocode(address: String): GeoPoint? {
                return GeoPoint(45.7772, 4.8686)
            }
        }
    }
}
