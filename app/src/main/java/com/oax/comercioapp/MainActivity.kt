package com.oax.comercioapp

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.oax.comercioapp.data.local.UserPreferences
import com.oax.comercioapp.databinding.ActivityMainBinding
import com.oax.comercioapp.utils.PreferencesManager
import com.oax.comercioapp.utils.SessionManager

class MainActivity : AppCompatActivity() {

  private lateinit var binding: ActivityMainBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    UserPreferences.init(this)

    // Inicializar sistemas de persistencia
    initializePersistenceSystems()

    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setupNavigation()

  }

  // Inicializar PreferencesManager y SessionManager
  private fun initializePersistenceSystems() {
    // Inicializar PreferencesManager con el contexto
    PreferencesManager.init(this)
    println("MainActivity: PreferencesManager inicializado")

    // Inicializar SessionManager (esto restaurara la sesion si existe)
    SessionManager.initialize()
    println("MainActivity: SessionManager inicializado")

    // Debug info (opcional - se comenta para produccion)
    println("=== INFORMACION DE INICIO ===")
    println(SessionManager.debugCurrentSession())
  }

  private fun setupNavigation() {
    val navView: BottomNavigationView = binding.navView

    val navController = findNavController(R.id.nav_host_fragment_activity_main)
    // Passing each menu ID as a set of Ids because each
    // menu should be considered as top level destinations.
    val appBarConfiguration = AppBarConfiguration(
      setOf(
        R.id.navigation_home, R.id.navigation_profiles, R.id.navigation_notifications
      )
    )
    setupActionBarWithNavController(navController, appBarConfiguration)
    navView.setupWithNavController(navController)
  }

  override fun onSupportNavigateUp(): Boolean {
    val navController = findNavController(R.id.nav_host_fragment_activity_main)
    return navController.navigateUp() || super.onSupportNavigateUp()
  }

  // METODOS PARA DEBUGGING (opcional)
  override fun onResume() {
    super.onResume()
    // Log del estado actual de la sesion cuando vuelve la app
    println("MainActivity: App resumed - ${SessionManager.debugCurrentSession()}")
  }

  override fun onPause() {
    super.onPause()
    // Log cuando la app pasa a background
    println("MainActivity: App paused - Usuario actual: ${SessionManager.getCurrentUserName()}")
  }

  // Metodo para debugging - para llamarlo desde cualquier fragment
  fun printSessionDebugInfo() {
    println(SessionManager.getCompleteSessionInfo())
  }
}