package com.oax.comercioapp.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.oax.comercioapp.databinding.FragmentNotificationsBinding
import com.oax.comercioapp.utils.SessionManager

class NotificationsFragment : Fragment() {

  private var _binding: FragmentNotificationsBinding? = null

  // This property is only valid between onCreateView and
  // onDestroyView.
  private val binding get() = _binding!!

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val notificationsViewModel =
      ViewModelProvider(this).get(NotificationsViewModel::class.java)

    _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
    val root: View = binding.root

    val textView: TextView = binding.textNotifications

    setupSessionObserver(textView, notificationsViewModel)

    return root
  }

  private fun setupSessionObserver(textView: TextView, viewModel: NotificationsViewModel) {
    SessionManager.currentUser.observe(viewLifecycleOwner) { user ->
      if (user != null) {
        showPersonalizedNotifications(textView, user.userName, user.idUser)
      }else {
        showDefaultNotifications(textView, viewModel)
      }
    }
  }

  private fun showPersonalizedNotifications(textView: TextView, userName: String, userId: Int) {
    val personalizedContent = """
            📢 Notificaciones para $userName
            👤 ID de Usuario: $userId
            
            🎉 ¡Bienvenido de vuelta!
            
            📊 Tu actividad:
            🛒 Carrito: Vacío (próximamente)
            📦 Pedidos: Sin pedidos recientes
            ⭐ Favoritos: 0 productos guardados
            
            🔔 Novedades:
            • Nuevos productos disponibles
            • Ofertas especiales esta semana
            • Descuentos por primera compra
            
            💡 Sugerencias:
            • Ve a 'Productos' para explorar el catálogo
            • Próximamente: carrito personalizado
            • Guarda tus productos favoritos
            
            🔄 Estado de sesión: Activo ✅
        """.trimIndent()

    textView.text = personalizedContent
  }

  private fun showDefaultNotifications(textView: TextView, viewModel: NotificationsViewModel) {

    viewModel.text.observe(viewLifecycleOwner) { vmText ->
      val defaultContent = """
                📢 Notificaciones Generales
                
                ⚠️ No hay sesión activa
                
                Para ver notificaciones personalizadas:
                1️⃣ Ve a la pestaña 'Usuarios'
                2️⃣ Selecciona un usuario de la lista
                3️⃣ Inicia sesión con ese perfil
                4️⃣ Regresa aquí para ver tus notificaciones
                
                🎯 Una vez que inicies sesión podrás ver:
                • Tu carrito de compras
                • Historial de pedidos
                • Productos favoritos
                • Ofertas personalizadas
                • Estado de cuenta
                
                ℹ️ Estado: Sin sesión activa
            """.trimIndent()

      textView.text = defaultContent
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}