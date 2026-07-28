package fr.quinquenaire.p15_eventorias_jr.presentation.eventcreation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.quinquenaire.p15_eventorias_jr.domain.usecase.eventlist.CreateEventUseCase
import fr.quinquenaire.p15_eventorias_jr.domain.usecase.userprofile.GetCurrentUidUseCase
import fr.quinquenaire.p15_eventorias_jr.presentation.eventcreation.contract.CreateEventAction
import fr.quinquenaire.p15_eventorias_jr.presentation.eventcreation.contract.CreateEventEffect
import fr.quinquenaire.p15_eventorias_jr.presentation.eventcreation.model.CreateEventMutableState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class CreateEventViewModel @Inject constructor(
    private val createEventUseCase: CreateEventUseCase,
    private val getCurrentUidUseCase: GetCurrentUidUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEventMutableState())
    val uiState: StateFlow<CreateEventMutableState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<CreateEventEffect>()
    val effect: SharedFlow<CreateEventEffect> = _effect.asSharedFlow()

    fun handleAction(action: CreateEventAction) {
        when (action) {
            is CreateEventAction.OnNameChange ->
                _uiState.update { it.copy(name = action.value) }

            is CreateEventAction.OnDescriptionChange ->
                _uiState.update { it.copy(description = action.value) }

            is CreateEventAction.OnCategoryChange ->
                _uiState.update { it.copy(category = action.value) }

            is CreateEventAction.OnDateSelected ->
                _uiState.update { it.copy(dateMillis = action.millis) }

            is CreateEventAction.OnTimeSelected ->
                _uiState.update { it.copy(hour = action.hour, minute = action.minute) }

            is CreateEventAction.OnAddressChange ->
                _uiState.update { it.copy(address = action.value) }

            is CreateEventAction.OnSaveClick -> saveEvent()

            is CreateEventAction.OnBackClick -> emitEffect(CreateEventEffect.NavigateBack)

            is CreateEventAction.OnImageSelected ->
                _uiState.update { it.copy(imageUri = action.uri) }

        }
    }

    private fun buildTimestamp(dateMillis: Long, hour: Int, minute: Int): Timestamp {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return Timestamp(calendar.time)
    }

    private fun emitEffect(effect: CreateEventEffect) {
        viewModelScope.launch { _effect.emit(effect) }
    }

    private fun saveEvent() {
        val state = _uiState.value
        if (!state.isFormValid || state.isSaving) return

        val organizerId = getCurrentUidUseCase()
        if (organizerId == null) {
            emitEffect(CreateEventEffect.ShowSnackbar("Utilisateur non connecté"))
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            try {
                // données obligatoires
                val dateMillis = state.dateMillis ?: throw IllegalArgumentException("Date manquante")
                val hour = state.hour ?: throw IllegalStateException("Heure manquante")
                val minute = state.minute ?: throw IllegalStateException("Minutes manquantes")
                val category = state.category ?: throw IllegalStateException("Catégorie manquante")

                // 2Construction  timestamp
                val dateTimestamp = buildTimestamp(dateMillis, hour, minute)

                // 3. Appel du Use Case
                // getOrThrow() lancera une exception si l'enregistrement échoue côté base de données
                createEventUseCase(
                    name = state.name,
                    description = state.description,
                    date = dateTimestamp,
                    locationName = state.address,
                    category = category.name,
                    organizerId = organizerId,
                    imageUri = state.imageUri
                ).getOrThrow()

                // ok on revient à la liste
                emitEffect(CreateEventEffect.NavigateBack)

            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                emitEffect(CreateEventEffect.ShowSnackbar(e.message ?: "Erreur inattendue"))
            }
        }
    }
}