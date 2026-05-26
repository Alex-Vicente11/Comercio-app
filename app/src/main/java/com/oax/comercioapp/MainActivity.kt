package com.oax.comercioapp

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.oax.comercioapp.databinding.ActivityMainBinding
import com.oax.comercioapp.ui.UiState
import com.oax.comercioapp.ui.auth.AuthViewModel
import kotlinx.coroutines.launch

/**
 * CAMBIOS RESPECTO AL ORIGINAL:
 * 1. Eliminado UserPreferences.init(this)
 *    Con UserPreferences como clase inyectable (no object), ya no se inicializa manualmente.
 *
 * 2. Eliminado PreferencesManager y SessionManager estáticos
 *    Eran sistemas redundantes de gestión de sesión. Con UserPreferences refactorizado y los
 *    use cases, ya no se necesitan 2 sistemas paralelos.
 *
 * 3. println() -> no logging en MainActivity
 *    println es Java puro y no usa el sistema de logging de Android. En producción no aparece
 *    en Logcat con filtros. Los logs de sesión ya existen en UserPreferences con Log.d apropiado.
 *
 * 4. initializeGuestUserIfNeeded() usa StateFlow en lugar de LiveData.
 *    El patrón es el mismo - observar el resultado y reaccionar.
 */

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()

        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        initializeSessionIfNeeded()
    }

    /**
     * Verifica si hay sesión activa al iniciar la app.
     * Si no hay sesión (ni guest ni autenticado), crea una sesión de invitado
     *
     * Flujo:
     *       validateSession() -> sessionState
     *          - Success(true) -> hay sesión válida, continuar normalmente
     *          - Success(false) -> no hay sesión -> crear guest
     *          - Error           -> problema de red -> crear guest igualmente
     */
    private fun initializeSessionIfNeeded() {
        lifecycleScope.launch {
            authViewModel.sessionState.collect { state ->
                when (state) {
                    is UiState.Success -> {
                        if (!state.data) {
                            // No hay sesión válida - iniciar como invitado
                            authViewModel.createGuestSession()
                        }
                        // Si state.data = true, la sesión sigue activa, no hacer nada
                    }

                    is UiState.Error -> {
                        // Error de red al validad - crear guest como fallback
                        authViewModel.createGuestSession()
                    }

                    else -> Unit
                }
            }
        }

        authViewModel.validateSession()
    }


    private fun setupNavigation() {
        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_cart,
                R.id.navigation_profiles,  // despues cambiar por navigation_dashboard
                R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}