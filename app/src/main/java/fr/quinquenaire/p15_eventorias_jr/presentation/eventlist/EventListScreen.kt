package fr.quinquenaire.p15_eventorias_jr.presentation.eventlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import fr.quinquenaire.p15_eventorias_jr.domain.SortOrder
import fr.quinquenaire.p15_eventorias_jr.domain.model.EventCategory
import fr.quinquenaire.p15_eventorias_jr.presentation.common.eventcomponent.ErrorContent
import fr.quinquenaire.p15_eventorias_jr.presentation.eventlist.contract.EventListAction
import fr.quinquenaire.p15_eventorias_jr.presentation.eventlist.contract.EventListEffect
import fr.quinquenaire.p15_eventorias_jr.presentation.eventlist.model.EventListMutableState
import fr.quinquenaire.p15_eventorias_jr.presentation.eventlist.model.EventListUiState
import fr.quinquenaire.p15_eventorias_jr.presentation.theme.EventoriasjrTheme
import kotlinx.coroutines.flow.collectLatest


// --- Screen entry point - Statefull ---

@Composable
fun EventListScreen(
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToCreate: () -> Unit,
    viewModel: EventListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Effets ponctuels (navigation, snackbar)
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is EventListEffect.NavigateToEventDetail ->
                    onNavigateToDetail(effect.eventId)

                is EventListEffect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.message)

                is EventListEffect.NavigateToCreateEvent ->
                    onNavigateToCreate()
            }
        }
    }
    // Délégation à la partie stateless : état + un seul point d'entrée d'actions
    EventListContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::handleAction,
        modifier = modifier
    )
}

// --- content - Stateless---

@Composable
fun EventListContent(
    uiState: EventListMutableState,
    snackbarHostState: SnackbarHostState,
    onAction: (EventListAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            EventListTopBar(
                sortOrder = uiState.sortOrder,
                onSortOrderChanged = { order ->
                    onAction(EventListAction.ChangeSortOrder(order))
                },
                searchQuery = uiState.searchQuery,
                onSearchQueryChanged = { query ->
                    onAction(EventListAction.OnSearchQueryChanged(query))
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAction(EventListAction.OnCreateEventClick) }

            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.create_event)
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->

        Column(modifier = Modifier.padding(innerPadding)) {

            // Barre de filtres catégories
            CategoryFilterBar(
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { category ->
                    onAction(EventListAction.FilterByCategory(category))
                }
            )

            // Zone principale : loading / error / liste
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    uiState.isLoading -> LoadingContent()
                    uiState.error != null -> ErrorContent(
                        message = uiState.error,
                        onRetry = { onAction(EventListAction.LoadEvents) }
                    )

                    uiState.events.isEmpty() -> EmptyContent()
                    else -> EventList(
                        events = uiState.events,
                        onEventClick = { eventId ->
                            onAction(EventListAction.OnEventClick(eventId))
                        }
                    )
                }
            }
        }
    }
}

// --- TopBar avec tri et recherche---
// aiguilleur
@Composable
private fun EventListTopBar(
    sortOrder: SortOrder,
    searchQuery: String,
    onSortOrderChanged: (SortOrder) -> Unit,
    onSearchQueryChanged: (String) -> Unit
) {
     var searchActive by rememberSaveable{mutableStateOf(searchQuery.isNotEmpty())}

    if (searchActive) {
        SearchTopBar(
            searchQuery = searchQuery,
            onSearchQueryChanged = onSearchQueryChanged,
            onCloseSearch = {
                searchActive = false
                onSearchQueryChanged("")
            }
        )
    } else {
        DefaultTopBar(
            sortOrder = sortOrder,
            onSortOrderChanged = onSortOrderChanged,
            onOpenSearch = { searchActive = true }
        )
    }
}

// barre de recherche
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onCloseSearch: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    TopAppBar(
        title = {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true
            )
        },
        navigationIcon = {
            IconButton(onClick = onCloseSearch) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.Back_to_the_list)
                )
            }
        },
        actions = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChanged("") }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.delete))
                }
            }
        }
    )
}

// barre par défaut
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultTopBar(
    sortOrder: SortOrder,
    onSortOrderChanged: (SortOrder) -> Unit,
    onOpenSearch: () -> Unit
) {
    TopAppBar(
        title = { Text(text = stringResource(R.string.event_list_title)) },
        actions = {
            IconButton(onClick = onOpenSearch) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search_placeholder)
                )
            }
            IconButton(
                onClick = {
                    val newSortOrder = when (sortOrder) {
                        SortOrder.DEFAULT -> SortOrder.BY_DATE_ASC
                        SortOrder.BY_DATE_ASC -> SortOrder.BY_DATE_DESC
                        SortOrder.BY_DATE_DESC -> SortOrder.DEFAULT
                    }
                    onSortOrderChanged(newSortOrder)
                }
            ) {
                Icon(
                    imageVector = when (sortOrder) {
                        SortOrder.DEFAULT -> Icons.Default.UnfoldMore
                        SortOrder.BY_DATE_ASC -> Icons.Default.KeyboardArrowUp
                        SortOrder.BY_DATE_DESC -> Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = stringResource(R.string.sort_by_date),
                    tint = if (sortOrder == SortOrder.BY_DATE_ASC)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}


// --- Filtre catégories (chips horizontaux) ---

private val CATEGORIES = EventCategory.entries

@Composable
private fun CategoryFilterBar(
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Chip "Tous"
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text(text = stringResource(R.string.filter_all)) }
            )
        }
        // Chips catégories
        items(CATEGORIES) { category ->
            FilterChip(
                selected = selectedCategory == category.label,
                onClick = { onCategorySelected(category.label) },
                label = { Text(text = category.label) },
                modifier = Modifier.testTag("Chip_${category.label}")
            )
        }
    }
}

// --- État : chargement---

@Composable
private fun LoadingContent() {
    CircularProgressIndicator()
}

// ---------------------------------------------------------------------------
// État : liste vide
// ---------------------------------------------------------------------------

@Composable
private fun EmptyContent() {
    Text(
        text = stringResource(R.string.event_list_empty),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// ---------------------------------------------------------------------------
// Liste des événements
// ---------------------------------------------------------------------------

@Composable
private fun EventList(
    events: List<EventListUiState>,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = events,
            key = { it.id }           // stabilise les recompositions
        ) { event ->
            EventCard(
                event = event,
                onClick = { onEventClick(event.id) }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Carte événement
// ---------------------------------------------------------------------------

@Composable
private fun EventCard(
    event: EventListUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("EventCard_${event.name}"),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {

            // Image événement
            AsyncImage(
                model = event.imageUrl,
                contentDescription = event.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
            )

            // Informations
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Badge catégorie
                CategoryBadge(category = event.category)

                // Nom
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Date & heure
                Text(
                    text = "${event.date} · ${event.time}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Lieu
                Text(
                    text = event.locationName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Badge catégorie
// ---------------------------------------------------------------------------

@Composable
private fun CategoryBadge(category: String) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

// ---------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------
@Preview(showBackground = true)
@Composable
private fun EventListContentPreview() {
    EventoriasjrTheme {
        EventListContent(
            uiState = EventListMutableState(
                events = listOf(
                    EventListUiState(
                        id = "1", name = "Soirée Jazz", date = "2025-06-15",
                        time = "20:00", category = "Musique", imageUrl = "",
                        locationName = "Lyon", organizerId = "u1",
                        latitude = null, longitude = null,
                        rawDate = null
                    )
                )
            ),
            snackbarHostState = SnackbarHostState(),
            onAction = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EventListContentLoadingPreview() {
    EventoriasjrTheme {
        EventListContent(
            uiState = EventListMutableState(isLoading = true),
            snackbarHostState = SnackbarHostState(),
            onAction = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EventListContentErrorPreview() {
    EventoriasjrTheme {
        EventListContent(
            uiState = EventListMutableState(error = "Erreur réseau"),
            snackbarHostState = SnackbarHostState(),
            onAction = {}
        )
    }
}