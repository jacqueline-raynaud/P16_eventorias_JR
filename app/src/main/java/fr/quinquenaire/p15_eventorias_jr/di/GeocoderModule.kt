package fr.quinquenaire.p15_eventorias_jr.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.quinquenaire.p15_eventorias_jr.data.location.AndroidGeocoderManager
import fr.quinquenaire.p15_eventorias_jr.domain.location.GeocoderManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GeocoderModule {
    @Provides
    @Singleton
    // La fonction promet de renvoyer l'INTERFACE (le contrat)
    fun provideGeocoderManager(
        @ApplicationContext context: Context
    ): GeocoderManager {
        // Mais elle fabrique et livre la VRAIE CLASSE Android
        return AndroidGeocoderManager(context)
    }
}