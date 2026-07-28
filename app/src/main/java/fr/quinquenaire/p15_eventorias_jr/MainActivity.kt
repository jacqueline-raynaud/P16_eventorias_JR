package fr.quinquenaire.p15_eventorias_jr

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import fr.quinquenaire.p15_eventorias_jr.domain.usecase.userprofile.SyncNotificationSubscriptionUseCase
import fr.quinquenaire.p15_eventorias_jr.presentation.navigation.EventoriasBottomBar
import fr.quinquenaire.p15_eventorias_jr.presentation.navigation.EventoriasDestinations
import fr.quinquenaire.p15_eventorias_jr.presentation.navigation.EventoriasNavHost
import fr.quinquenaire.p15_eventorias_jr.presentation.theme.EventoriasjrTheme
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var syncNotificationSubscriptionUseCase: SyncNotificationSubscriptionUseCase

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()

        lifecycleScope.launch {
            syncNotificationSubscriptionUseCase()
        }

        setContent {
            EventoriasjrTheme {
                val navController = rememberNavController()
                val currentBackStack by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStack?.destination?.route

                // Écrans qui affichent la BottomBar
                val bottomBarRoutes = setOf(
                    EventoriasDestinations.EventList.route,
                    EventoriasDestinations.UserProfile.route
                )
                val showBottomBar = currentRoute in bottomBarRoutes

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            EventoriasBottomBar(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        // évite d'empiler les écrans principaux
                                        popUpTo(EventoriasDestinations.EventList.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    EventoriasNavHost(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val alreadyGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!alreadyGranted) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}