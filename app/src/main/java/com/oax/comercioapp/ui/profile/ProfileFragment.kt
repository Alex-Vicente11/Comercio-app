package com.oax.comercioapp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.oax.comercioapp.databinding.FragmentProfileBinding
import com.oax.comercioapp.R
import com.oax.comercioapp.domain.model.User
import com.oax.comercioapp.ui.UiState
import kotlinx.coroutines.launch

/**
 * CAMBIOS RESPECTO AL ORIGINAL:
 * 1. ELIMINADAS  las 8 referencias a SessionManager
 *    - SessionManager.currentUser.observe()
 *    - SessionManager.isRestoring.observe()
 *    - SessionManager.getCurrentUser()
 *    - SessionManager.getCurrentUserName()
 *    - SessionManager.logout() (x2)
 *    - SessionManager.isLoggedInt()
 *
 *    Todoo eso se reemplaza con ProfileViewModel.profileState y ProfileViewModel.logoutState que
 *    vienen de GetProfileUseCase. Un solo flujo de datos desde la fuente real (API/UserPreferences).
 *
 * 2. ELIMINADO: loadUserProfile() con profileId por argumentos
 *    El original recibía un "profile_id" como argumento del Fragment para cargar otro usuario
 *    por ID - patrón de pantalla admin. GetProfileUseCase obtiene el usuario del token JWT - no necesita ID.
 *
 * 3. ELIMINADO: CartViewModel inyectado en ProfileFragment
 *    El original mostraba el carrito dentro de la pantalla de perfil.
 *    Con el nuevo enfoque de app de pedidos:
 *      - La pantalla de perfil muestra datos del usuario y opción de logout
 *      - El carrito vive en CartFragment
 *      - El historial de pedidos irá en OrderHistoryFragment
 *   Un Fragment, una responsabilidad - SRP
 *
 * 4. SIMPLIFICADA: updateSessionButtons() sin SessionManager
 *    La visibilidad de botones (logout, cambiar usuario) se decide basándose en el estado del
 *    profileState - si hay un User cargado, hay sesión activa. Sin necesidad de consultar SessionManager
 *
 * 5. showChangeUserDialog() -> redirige al login
 *    El original navegaba a "lista de usuarios" (pantalla admin).
 *    Ahora cierra sesión y navega al LoginFragment - flujo correcto para una app de cliente.
 *
 * 6. ELIMINADO: formatCartItems() y updateCartInfo()
 *    Eran helpers para mostrar el carrito dentro del perfil.
 *    Ya no aplica con la separación de responsabilidades.
 */

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Estado del perfil
                launch {
                    viewModel.profileState.collect { state ->
                        when (state) {
                            is UiState.Loading -> {
                                binding.profileId.text = "Cargando perfil..."
                                setButtonsEnabled(false)
                            }

                            is UiState.Success -> {
                                showUserProfile(state.data)
                                setButtonsEnabled(true)
                                updateButtonsVisibility(isLoggedIn = true, user = state.data)
                            }

                            is UiState.Error -> {
                                binding.profileId.text = state.message
                                binding.sessionInfo.text = ""
                                setButtonsEnabled(false)
                                updateButtonsVisibility(isLoggedIn = false)
                            }

                            is UiState.Idle -> Unit
                        }
                    }
                }

                // Estado de actualización de perfil
                launch {
                    viewModel.updateState.collect { state ->
                        when (state) {
                            is UiState.Success -> {
                                Toast.makeText(
                                    context,
                                    "Perfil actualizado",
                                    Toast.LENGTH_SHORT
                                ).show()
                                viewModel.onUpdateHandled()
                            }

                            is UiState.Error -> {
                                Toast.makeText(
                                    context,
                                    state.message,
                                    Toast.LENGTH_LONG
                                ).show()

                                viewModel.onUpdateHandled()
                            }

                            else -> Unit
                        }
                    }
                }

                // Estado de logout
                launch {
                    viewModel.logoutState.collect { state ->
                        if (state is UiState.Success) {
                            viewModel.onLogoutHandled()
                            navigateToLogin()
                        }
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        // boton cerrar sesion
        binding.btnLogout.setOnClickListener { showLogoutDialog() }

        /**
         * btnChangeUser reutilizado para "Editar perfil".
         * El original lo usaba para cambiar entre usuarios (pantalla admin).
         *
         * TODOo: Si se prefiere, puede abrirse un BottomSheetDialog con el campo de texto -
         * más UX amigable que un AlertDialog simple.
         */
        // boton cambiar usuario
        binding.btnChangeUser.setOnClickListener { showEditProfileDialog() }

    }

    private fun showUserProfile(user: User) {
        binding.profileId.text = buildString {
            append("Usuario: ${user.userName}\n")
            if (!user.isGuest) append("Email: ${user.email ?: "-"}\n")
            append(if (user.isGuest) "Modo: Invitado" else "Cuenta verificada")
        }

        binding.sessionInfo.text = buildString {
            append("ID de usuario: ${user.id}\n\n")
            if (user.isGuest) {
                append("Estás en modo invitado.\n")
                append("Regístrate para guardar tu historial de pedidos.")
            } else {
                append("Funcionalidades disponibles:\n")
                append("* Historial de pedidos\n")
                append("* Carrito guardado\n")
                append("* Preferencias de cuenta")
            }
        }
    }

    private fun showLogoutDialog() {
        val userName = (viewModel.profileState.value as? UiState.Success)?.data?.userName ?: "tu cuenta"

        AlertDialog.Builder(requireContext())
            .setTitle("Cerrar Sesión")
            .setMessage("¿Cerrar la sesión de $userName?")
            .setPositiveButton("Cerrar Sesión") { _, _ ->
                viewModel.logout()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditProfileDialog() {
        // TODOO: implementar BottomSheetDialog o AlertDialog con EditText
        // para actualizar el nombre de usuario via viewModel.updateProfile(newName)
        Toast.makeText(
            context,
            "Editar perfil - próximamente",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun updateButtonsVisibility(isLoggedIn: Boolean, user: User? = null) {
        binding.btnLogout.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
        // Mostrar "Editar perfil" solo para usuarios autenticados (no guests)
        binding.btnChangeUser.visibility =
            if (isLoggedIn && user?.isGuest == false) View.VISIBLE else View.GONE

        if (isLoggedIn && user != null) {
            binding.btnChangeUser.text = "Editar perfil"
        }
    }

    private fun navigateToLogin() {
        try {
            findNavController().navigate(R.id.loginFragment)
        } catch (e: Exception) {
            // Si falla la navegacion, al menos mostrar el error
            Toast.makeText(
                context,
                "Error de navegación: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        binding.btnChangeUser.isEnabled = enabled
        binding.btnLogout.isEnabled = enabled
    }

    override fun onResume() {
        super.onResume()
        // Si el estado es Idle (primera vez o después de logout), cargar perfil
        if (viewModel.profileState.value is UiState.Idle ||
            viewModel.profileState.value is UiState.Error) {
            viewModel.loadProfile()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}