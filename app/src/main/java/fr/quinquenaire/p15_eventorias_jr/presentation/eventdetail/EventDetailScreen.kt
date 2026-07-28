package fr.quinquenaire.p15_eventorias_jr.presentation.eventdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import fr.quinquenaire.p15_eventorias_jr.R
import fr.quinquenaire.p15_eventorias_jr.presentation.common.eventcomponent.ErrorContent
import fr.quinquenaire.p15_eventorias_jr.presentation.eventdetail.contract.EventDetailAction
import fr.quinquenaire.p15_eventorias_jr.presentation.eventdetail.contract.EventDetailEffect
import fr.quinquenaire.p15_eventorias_jr.presentation.eventdetail.model.EventDetailMutableState
import fr.quinquenaire.p15_eventorias_jr.presentation.eventdetail.model.EventDetailUiState
import fr.quinquenaire.p15_eventorias_jr.presentation.theme.EventoriasjrTheme
import kotlinx.coroutines.flow.collectLatest

// ---------------------------------------------------------------------------
// Screen entry point — STATEFUL (connaît le ViewModel)
// ---------------------------------------------------------------------------

@Composable
fun EventDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is EventDetailEffect.NavigateBack ->
                    onNavigateBack()

                is EventDetailEffect.NavigateToEdit ->
                    onNavigateToEdit(effect.eventId)

                is EventDetailEffect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    EventDetailContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::handleAction,
        modifier = modifier
    )
}

// ---------------------------------------------------------------------------
// Content — STATELESS (ne connaît pas le ViewModel)
// ---------------------------------------------------------------------------

@Composable
fun EventDetailContent(
    uiState: EventDetailMutableState,
    snackbarHostState: SnackbarHostState,
    onAction: (EventDetailAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { EventDetailTopBar(
            title = uiState.event?.name ?: "",
            isOrganizer = uiState.isOrganizer, // à vérifier
            onAction = onAction) },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator()

                uiState.error != null -> ErrorContent(
                    message = uiState.error,
                    onRetry = { onAction(EventDetailAction.OnRetry) }
                )

                uiState.event != null -> EventDetailBody(event = uiState.event)
            }
        }
        if (uiState.showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { onAction(EventDetailAction.OnDismissDeleteDialog) },
                title = { Text(stringResource(R.string.delete_event_confirm_title)) },
                text = { Text(stringResource(R.string.delete_event_confirm_message)) },
                confirmButton = {
                    TextButton(onClick = { onAction(EventDetailAction.OnConfirmDelete) }) {
                        Text(stringResource(R.string.delete_event), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onAction(EventDetailAction.OnDismissDeleteDialog) }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// TopBar : titre + retour + menu (modifier / supprimer)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDetailTopBar(
    title: String,
    isOrganizer: Boolean,
    onAction: (EventDetailAction) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },

        navigationIcon = {
            IconButton(onClick = { onAction(EventDetailAction.OnBackClick) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.Back_to_the_list)
                )
            }
        },

        actions = {
            if (isOrganizer) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.event_detail_menu)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {

                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit_event)) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onAction(EventDetailAction.OnEditClick)
                        }
                    )

                    DropdownMenuItem(

                        text = { Text(stringResource(R.string.delete_event)) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onAction(EventDetailAction.OnDeleteClick)
                        }
                    )
                }
            }
        }
    )
}

// ---------------------------------------------------------------------------
// Corps du détail
// ---------------------------------------------------------------------------

@Composable
private fun EventDetailBody(event: EventDetailUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Image de l'événement
        AsyncImage(
            model = event.imageUrl,
            contentDescription = event.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
        )

        // Date + heure à gauche, avatar organisateur à droite
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)

            ) {

                // Date avec logo calendrier
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)

                ) {

                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = event.date,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Heure avec logo horloge

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = event.time,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Avatar du créateur

            AsyncImage(
                model = event.organizerAvatarUrl,
                contentDescription = stringResource(R.string.organizer_avatar),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .testTag("organizer_avatar")
            )
        }

        // Description
        Text(
            text = event.description,
            style = MaterialTheme.typography.bodyMedium
        )

        // Adresse
        Text(
            text = event.locationName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Carte statique Google Maps
        if (event.staticMapUrl.isNotBlank()) {
            AsyncImage(
                model = event.staticMapUrl,
                contentDescription = stringResource(R.string.event_map),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("static_map")
            )
        }

    }
}


// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
private fun EventDetailContentPreview() {
    EventoriasjrTheme {
        EventDetailContent(
            uiState = EventDetailMutableState(
                event = EventDetailUiState(
                    id = "1",
                    name = "Soirée Jazz au Parc",
                    description = "Une soirée inoubliable sous les étoiles avec les meilleurs musiciens de jazz de la région.",
                    date = "15 Juillet 2025",
                    time = "20:00",
                    locationName = "Parc de la Tête d'Or, Lyon",
                    imageUrl = "",
                    organizerId = "user123",
                    organizerAvatarUrl = "",
                    staticMapUrl = ""
                )
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EventDetailContentLoadingPreview() {
    EventoriasjrTheme {
        EventDetailContent(
            uiState = EventDetailMutableState(isLoading = true),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EventDetailContentErrorPreview() {
    EventoriasjrTheme {
        EventDetailContent(
            uiState = EventDetailMutableState(error = "Une erreur est survenue lors du chargement."),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {}
        )
    }
}
