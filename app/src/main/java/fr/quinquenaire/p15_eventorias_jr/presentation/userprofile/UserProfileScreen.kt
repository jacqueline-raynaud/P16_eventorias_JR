package fr.quinquenaire.p15_eventorias_jr.presentation.userprofile

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.firebase.ui.auth.AuthUI
import fr.quinquenaire.p15_eventorias_jr.FirebaseUiActivity
import fr.quinquenaire.p15_eventorias_jr.R
import fr.quinquenaire.p15_eventorias_jr.presentation.theme.EventoriasjrTheme
import fr.quinquenaire.p15_eventorias_jr.presentation.userprofile.contract.UserProfileAction
import fr.quinquenaire.p15_eventorias_jr.presentation.userprofile.contract.UserProfileEffect
import fr.quinquenaire.p15_eventorias_jr.presentation.userprofile.model.UserProfileMutableState
import fr.quinquenaire.p15_eventorias_jr.presentation.userprofile.model.UserProfileUiState
import kotlinx.coroutines.flow.collectLatest

// ---------------------------------------------------------------------------
// Screen entry point — STATEFUL (connaît le ViewModel)
// ---------------------------------------------------------------------------

@Composable
fun UserProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is UserProfileEffect.NavigateToLogin -> {
                    // Déconnexion Firebase PUIS retour à l'écran de connexion.
                    // Fait ici et pas dans le ViewModel : AuthUI exige un Context.
                    AuthUI.getInstance().signOut(context).addOnCompleteListener {
                        val intent = Intent(context, FirebaseUiActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        context.startActivity(intent)
                    }
                }

                is UserProfileEffect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    UserProfileContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::handleAction,
        modifier = modifier
    )

}

// ---------------------------------------------------------------------------
// Content — STATELESS (ne connaît pas le ViewModel)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileContent(
    uiState: UserProfileMutableState,
    snackbarHostState: SnackbarHostState,
    onAction: (UserProfileAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.user_profile_title)) }
            )
        },
        modifier = modifier.testTag("user_profile_screen")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator()

                uiState.error != null -> Text(
                    text = uiState.error,
                    color = MaterialTheme.colorScheme.error
                )

                uiState.profile != null -> UserProfileBody(
                    state=uiState,
                    onAction = onAction
                )
            }
        }
        if (uiState.showDeleteAccountConfirmation) {
            AlertDialog(
                onDismissRequest = { onAction(UserProfileAction.OnDismissDeleteAccountDialog) },
                title = { Text(stringResource(R.string.delete_account_confirm_title)) },
                text = { Text(stringResource(R.string.delete_account_confirm_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onAction(UserProfileAction.OnConfirmDeleteAccount) },
                        enabled = !uiState.isDeletingAccount,
                        modifier = Modifier.testTag("confirm_delete_dialog_button")
                    ) {
                        if (uiState.isDeletingAccount) {
                            Log.e("eventorias_jr", "isDeletingAccount = true")
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Log.e("eventorias_jr", "isDeletingAccount = false")
                            Text(stringResource(R.string.delete_account), color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { onAction(UserProfileAction.OnDismissDeleteAccountDialog) },
                        enabled = !uiState.isDeletingAccount
                    ) { Text(stringResource(R.string.cancel)) }
                }
            )
        }
    }

}

// ---------------------------------------------------------------------------
// Corps du profil
// ---------------------------------------------------------------------------

@Composable
private fun UserProfileBody(
    state: UserProfileMutableState,
    onAction: (UserProfileAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = state.profile ?: return

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { onAction(UserProfileAction.OnAvatarSelected(it)) }
    }


    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)

    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            AsyncImage(
                // priorité au nouvel avatar sélectionné (Uri local),
                // sinon l'URL Firestore
                model = state.editedAvatarUri ?: profile.avatarUrl,
                contentDescription = stringResource(R.string.user_avatar),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_person_placeholder),
                error = painterResource(R.drawable.ic_person_placeholder),
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }
                    .testTag("user_avatar")
            )
            // Petit badge crayon pour signaler que c'est modifiable
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(4.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        // Prénom — éditable
        OutlinedTextField(
            value = state.displayedFirstName,
            onValueChange = { onAction(UserProfileAction.OnFirstNameChanged(it)) },
            label = { Text(stringResource(R.string.first_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Nom — éditable
        OutlinedTextField(
            value = state.displayedLastName,
            onValueChange = { onAction(UserProfileAction.OnLastNameChanged(it)) },
            label = { Text(stringResource(R.string.last_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Email — lecture seule
        OutlinedTextField(
            value = profile.email,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.email)) },
            modifier = Modifier.fillMaxWidth()
        )

        // Notifications
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Switch(
                checked = profile.notificationEnabled,
                onCheckedChange = { enabled ->
                    onAction(UserProfileAction.OnNotificationToggle(enabled))
                },
                modifier = Modifier.testTag("notification_switch")
            )
            Text(
                text = stringResource(R.string.notifications),
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        // Boutons Enregistrer / Annuler — visibles seulement si modifications
        if (state.hasChanges) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onAction(UserProfileAction.OnCancelEdit) },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isSaving
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = { onAction(UserProfileAction.OnSaveClick) },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isSaving
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))


        // Déconnexion
        Button(
            onClick = { onAction(UserProfileAction.OnSignOutClick) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sign_out_button")
        ) {
            Text(stringResource(R.string.sign_out))
        }
        Spacer(modifier = Modifier.height(24.dp))
        // suppression compte
        Button (
            onClick = { onAction(UserProfileAction.OnDeleteAccountClick) },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("delete_account_button")
        ) {
            Text(stringResource(R.string.delete_account))
        }

    }
}
// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
private fun UserProfileContentPreview() {
    EventoriasjrTheme {
        UserProfileContent(
            uiState = UserProfileMutableState(
                profile = UserProfileUiState(
                    uid = "user_001",
                    firstName = "Jacqueline",
                    lastName = "Dupont",
                    email = "jacqueline.dupont@example.com",
                    avatarUrl = "",   // vide en preview, Coil ne charge pas d'URL
                    notificationEnabled = true
                )
            ),
            snackbarHostState = SnackbarHostState(),
            onAction = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UserProfileContentLoadingPreview() {
    EventoriasjrTheme {
        UserProfileContent(
            uiState = UserProfileMutableState(isLoading = true),
            snackbarHostState = SnackbarHostState(),
            onAction = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UserProfileContentErrorPreview() {
    EventoriasjrTheme {
        UserProfileContent(
            uiState = UserProfileMutableState(error = "Profil introuvable"),
            snackbarHostState = SnackbarHostState(),
            onAction = {}
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun UserProfileContentDarkPreview() {
    EventoriasjrTheme {
        UserProfileContent(
            uiState = UserProfileMutableState(
                profile = UserProfileUiState(
                    firstName = "Jacqueline",
                    lastName = "Dupont",
                    email = "jacqueline.dupont@example.com",
                    notificationEnabled = true,
                    avatarUrl = ""
                )
            ),
            snackbarHostState = SnackbarHostState(),
            onAction = {}
        )
    }
}