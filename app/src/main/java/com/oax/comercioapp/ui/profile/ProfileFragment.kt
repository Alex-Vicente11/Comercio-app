package com.oax.comercioapp.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.cast.framework.SessionManager
import com.oax.comercioapp.data.api.NetworkResult
import com.oax.comercioapp.data.models.User
import com.oax.comercioapp.databinding.FragmentProfileBinding
import java.lang.NumberFormatException
import com.oax.comercioapp.R
import com.oax.comercioapp.data.models.CartItem
import com.oax.comercioapp.ui.home.CartViewModel
import com.oax.comercioapp.utils.SessionManager.currentUser

class ProfileFragment : Fragment() {

  companion object {
    fun newInstance() = ProfileFragment()
  }

  private var profileId: String? = null
  private lateinit var binding: FragmentProfileBinding

  private val viewModel: ProfileViewModel by viewModels()

  private val cartViewModel: CartViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    profileId = arguments?.getString("profile_id") ?: "NO HAY PROFILE ID"

  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentProfileBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    setupObservers()
    setupClickListeners()
    loadUserProfile()
    updateSessionButtons()
  }

  private fun setupObservers() {
    // Observer para la carga del usuario
    viewModel.user.observe(viewLifecycleOwner) { result ->
      when (result) {
        is NetworkResult.Loading -> {
          binding.profileId.text = "Cargando usuario..."
          setButtonsEnabled(false)
        }
        is NetworkResult.Success -> {
          Log.d("ProfileFragment", "Usuario cargado exitosamente: ${result.data}")
          viewModel.setCurrentUser(result.data)
          setButtonsEnabled(true)
        }
        is NetworkResult.Error -> {
          binding.profileId.text = "Error: ${result.message}\nID recibido: $profileId"
          setButtonsEnabled(false)
        }
      }
    }
    // Observer para el usuario actual en sesion
    viewModel.currentUser.observe(viewLifecycleOwner) { user ->
      updateUserUI(user)
      updateSessionButtons()
    }

    // Observer para el estado de restauracion de sesion
    com.oax.comercioapp.utils.SessionManager.isRestoring.observe(viewLifecycleOwner) { isRestoring ->
      if (isRestoring) {
        binding.profileId.text = "Restaurando sesión..."
        setButtonsEnabled(false)
      }
    }

    cartViewModel.cartItems.observe(viewLifecycleOwner) { result ->
      when (result) {
        is NetworkResult.Loading -> {
          updateCartInfo("Cargando carrito...")
        }
        is NetworkResult.Success -> {
          val cartInfo = formatCartItems(result.data)
          updateCartInfo(cartInfo)
        }
        is NetworkResult.Error -> {
          updateCartInfo("Error al cargar carrito: ${result.message}")
        }
      }
    }
  }

  private fun formatCartItems(cartItems: List<CartItem>): String { //carItems = result
    if (cartItems.isEmpty()) {
      return "Carrito vacío\n\nVe a 'Productos' para agregar productos a tu carrito"
    }

    val itemsText = cartItems.joinToString("\n") { item ->
      ". ${item.product.product} - Cantidad: ${item.quantity} - $${String.format("%.2f", item.product.price * item.quantity)}"
    }

    val total = cartItems.sumOf { it.product.price * it.quantity }

    return "Productos en carrito (${cartItems.size}):\n$itemsText\n\nTotal: $${String.format("%.2f", total)}"

  }

  private fun updateCartInfo(cartInfo: String) {
    val currentUser = com.oax.comercioapp.utils.SessionManager.getCurrentUser()
    currentUser?.let { user ->
      binding.sessionInfo.text = "¡Sesión Activa!\n\n" +
              "Usuario : ${user.userName}\n" +
              "ID: ${user.idUser}\n\n" +
              "$cartInfo\n\n" +
              "Funcionalidades disponibles:\n" +
              ". Carrito personalizado\n" +
              ". Historial de pedidos\n" +
              ". Preferencias guardadas"
    }
  }

  private fun setupClickListeners() {
    // boton cambiar usuario
    binding.btnChangeUser.setOnClickListener {
      showChangeUserDialog()
    }
    // boton cerrar sesion
    binding.btnLogout.setOnClickListener {
      showLogoutDialog()
    }

    // boton debug (util para desarrollo) pendiente
  }

  private fun showChangeUserDialog() {
    AlertDialog.Builder(requireContext())
      .setTitle("Cambiar Usuario")
      .setMessage("¿Quieres volver a la lista de usuarios para seleccionar otro?")
      .setPositiveButton("Sí") { _, _ ->
        val currentUserName = com.oax.comercioapp.utils.SessionManager.getCurrentUserName()

        // Hacer logout completo antes de navegar
        com.oax.comercioapp.utils.SessionManager.logout()

        // Navegar a la lista de usuarios
        navigateToUsersList()

        Toast.makeText(
          requireContext(),
          "Sesión cerrada para: $currentUserName. Selecciona otro usuario",
          Toast.LENGTH_SHORT
        ).show()
      }
      .setNegativeButton("Cancelar", null)
      .show()

  }

  private fun showLogoutDialog() {
    val currentUser = com.oax.comercioapp.utils.SessionManager.getCurrentUser()
    val userName = currentUser?.userName ?: "Usuario actual"

    AlertDialog.Builder(requireContext())
      .setTitle("Cerrar Sesión")
      .setMessage("¿Estás seguro que quieres cerrar la sesión de $userName?\n\nEsto limpiará todos los datos de sesión guardados.")
      .setPositiveButton("Cerrar Sesión") { _, _ ->
          performLogout()
      }
      .setNegativeButton("Cancelar", null)
      .show()
  }

  // PENDIENTE showSessionDebugInfo() {}

  private fun performLogout() {
    val userName = com.oax.comercioapp.utils.SessionManager.getCurrentUserName()

    // Cerrar sesion
    com.oax.comercioapp.utils.SessionManager.logout()

    // Mostrar confirmacion
    Toast.makeText(
      requireContext(),
      "Sesión cerrada para: $userName",
      Toast.LENGTH_LONG
    ).show()

    // Navegar a la lista de usuarios
    navigateToUsersList()
  }

  private fun navigateToUsersList() {
    try {
        findNavController().navigate(R.id.navigation_profiles)
    } catch (e: Exception) {
      // Si falla la navegacion, al menos mostrar el error
      Toast.makeText(
        requireContext(),
        "Error al navegar: ${e.message}",
        Toast.LENGTH_SHORT
      ).show()
    }
  }

  private fun updateUserUI(user: User?) {
    user?.let { currentUser ->
      binding.profileId.text = "Perfil de:${currentUser.userName}\nID: ${currentUser.idUser}"

      Log.d("ProfileFragment", "Setting user session: ${currentUser.userName}")

      // Cargar items del carrito
      cartViewModel.loadCartItems(currentUser.idUser)

      binding.sessionInfo.text = buildSessionInfo(currentUser)

      Toast.makeText(
        requireContext(),
        "¡Sesión iniciada para: ${currentUser.userName}",
        Toast.LENGTH_LONG
      ).show()
    }?: run {
      binding.profileId.text = "No hay sesión activa"
      binding.sessionInfo.text = "No hay usuario logueado\n\n" +
              "Selecciona un usuario de la lista para iniciar sesión"
    }
  }

  private fun buildSessionInfo(user: User): String {
    return "¡Sesión Activa!\n\n" +
            "Usuario: ${user.userName}\n" +
            "ID: ${user.idUser}\n\n" +
            "Carrito de compras:\n" +
            "Cargando productos..."  // se actualiza con Observer
  }

  private fun updateSessionButtons() {
    val isLoggedIn = com.oax.comercioapp.utils.SessionManager.isLoggedIn()

    // Mostrar/ocultar botones segun el estado de sesion
    binding.btnChangeUser.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
    binding.btnLogout.visibility = if (isLoggedIn) View.VISIBLE else View.GONE

    // El boton de debug siempre visible para desarrollo PENDIENTE

    // Actualizar texto del boton cambiar usuario
    if (isLoggedIn) {
      val userName = com.oax.comercioapp.utils.SessionManager.getCurrentUserName()
      binding.btnChangeUser.text = "Cambiar Usuario ($userName)"
    }
  }

  private fun setButtonsEnabled(enabled: Boolean) {
    binding.btnChangeUser.isEnabled = enabled
    binding.btnLogout.isEnabled = enabled
  }

  private fun loadUserProfile() {
    profileId?.let { id ->
      if (id != "No hay profile id") {
        try {
          val userId = id.toInt()
          viewModel.loadUser(userId)
        }catch (e: NumberFormatException) {
          binding.profileId.text = "Error: ID de usuario inválido ($id)"
          setButtonsEnabled(false)
        }
      }else {
        binding.profileId.text = "Error: No se recibió ID de usuario válido"
        setButtonsEnabled(false)
      }
    }
  }

  fun getCurrentUser() = viewModel.getCurrentUser()

  fun isUserLoggedIn() = viewModel.isUserLoggedIn()

  override fun onResume() {
    super.onResume()
    // Actualizar UI cuando se regresa al fragment
    updateSessionButtons()
  }

  override fun onDestroy() {
    super.onDestroy()
    // No hacer binding null aquí porque puede causar problemas con los observers
  }

}