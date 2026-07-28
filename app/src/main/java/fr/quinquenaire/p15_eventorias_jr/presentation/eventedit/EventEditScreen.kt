package fr.quinquenaire.p15_eventorias_jr.presentation.eventedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.quinquenaire.p15_eventorias_jr.R
import fr.quinquenaire.p15_eventorias_jr.domain.model.EventCategory
import fr.quinquenaire.p15_eventorias_jr.presentation.common.eventcomponent.CategoryField
import fr.quinquenaire.p15_eventorias_jr.presentation.common.eventcomponent.DateField
import fr.quinquenaire.p15_eventorias_jr.presentation.common.eventcomponent.ErrorContent
import fr.quinquenaire.p15_eventorias_jr.presentation.common.eventcomponent.ImagePickerField
import fr.quinquenaire.p15_eventorias_jr.presentation.common.eventcomponent.TimeField
import fr.quinquenaire.p15_eventorias_jr.presentation.eventedit.contract.EventEditAction
import fr.quinquenaire.p15_eventorias_jr.presentation.eventedit.contract.EventEditEffect
import fr.quinquenaire.p15_eventorias_jr.presentation.eventedit.model.EventEditMutableState
import fr.quinquenaire.p15_eventorias_jr.presentation.theme.EventoriasjrTheme
import kotlinx.coroutines.flow.collectLatest

@Composable
fun EventEditScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is EventEditEffect.NavigateBack -> onNavigateBack()
                is EventEditEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    EventEditContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::handleAction,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditContent(
    uiState: EventEditMutableState,
    snackbarHostState: SnackbarHostState,
    onAction: (EventEditAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_event_title)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(EventEditAction.OnBackClick) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.Back_to_the_list)
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator()

                uiState.error != null -> ErrorContent(
                    message = uiState.error,
                    onRetry = { onAction(EventEditAction.OnRetry) }
                )

                else -> EventEditForm(uiState = uiState, onAction = onAction)
            }
        }
    }
}

@Composable
private fun EventEditForm(
    uiState: EventEditMutableState,
    onAction: (EventEditAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = uiState.name,
            onValueChange = { onAction(EventEditAction.OnNameChange(it)) },
            label = { Text(stringResource(R.string.event_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.description,
            onValueChange = { onAction(EventEditAction.OnDescriptionChange(it)) },
            label = { Text(stringResource(R.string.event_description)) },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        CategoryField(
            selectedCategory = uiState.category?.label.orEmpty(),
            categories = EventCategory.entries,
            onCategorySelected = { onAction(EventEditAction.OnCategoryChange(it)) }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DateField(
                dateLabel = uiState.dateLabel,
                onDateSelected = { onAction(EventEditAction.OnDateSelected(it)) },
                modifier = Modifier.weight(1f)
            )
            TimeField(
                timeLabel = uiState.timeLabel,
                onTimeSelected = { h, m -> onAction(EventEditAction.OnTimeSelected(h, m)) },
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = uiState.address,
            onValueChange = { onAction(EventEditAction.OnAddressChange(it)) },
            label = { Text(stringResource(R.string.event_address)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        ImagePickerField(
            previewModel = uiState.imagePreview,
            onImageSelected = { onAction(EventEditAction.OnImageSelected(it)) }
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { onAction(EventEditAction.OnSaveClick) },
            enabled = uiState.isFormValid && !uiState.isSaving,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(stringResource(R.string.save_changes))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------


@Preview(showBackground = true)
@Composable
fun EventEditContentPreview() {
    EventoriasjrTheme {
        EventEditContent(
            uiState = EventEditMutableState(
                name = "Concert de Jazz",
                description = "Un super concert de jazz en plein air.",
                category = EventCategory.MUSIQUE,
                dateMillis = System.currentTimeMillis(),
                hour = 20,
                minute = 30,
                address = "123 Rue du Jazz, Paris",
                isLoading = false
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {}
        )
    }
}