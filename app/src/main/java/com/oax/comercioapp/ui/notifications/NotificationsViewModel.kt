package com.oax.comercioapp.ui.notifications

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * FASE 3 — NotificationsViewModel
 *
 * El original solo tenía un _text hardcodeado — era un placeholder.
 * Con el nuevo enfoque de app de pedidos, las notificaciones tendrán
 * sentido real: "Tu pedido #123 fue confirmado", "Listo para recoger", etc.
 *
 * Por ahora se deja como placeholder limpio con UiState, listo para
 * conectar cuando el backend tenga endpoints de notificaciones o pedidos.
 *
 * PENDIENTE (cuando el backend soporte pedidos):
 *   - Conectar con GetOrderHistoryUseCase
 *   - Mostrar actualizaciones de estado de pedidos
 *   - Integrar con Firebase Cloud Messaging si se requieren push notifications
 */
class NotificationsViewModel : ViewModel() {

  private val _notificationsState = MutableStateFlow<String>("Sin notificaciones por ahora")
  val notificationsState: StateFlow<String> = _notificationsState.asStateFlow()
}


// ─── ProfilesViewModel → OrderHistoryViewModel (placeholder) ─────────────────

/**
 * FASE 3 — ProfilesViewModel renombrado a OrderHistoryViewModel
 *
 * El ProfilesViewModel original listaba usuarios (funcionalidad admin).
 * Con el nuevo enfoque de app de pedidos se convierte en la pantalla
 * de historial de pedidos del usuario actual.
 *
 * Se deja como placeholder documentado porque:
 *   1. El backend aún no tiene endpoints de pedidos (Order)
 *   2. Las entidades Order y PlaceOrderUseCase están definidas en el plan
 *      pero esperan el contrato del backend para implementarse
 *   3. La pantalla del dashboard se reutilizará para mostrar este historial
 *
 * CUANDO EL BACKEND ESTÉ LISTO, este ViewModel conectará:
 *   - GetOrderHistoryUseCase → lista de pedidos del usuario
 *   - PlaceOrderUseCase → confirmar el carrito como pedido
 *   - Estado de cada pedido (PENDING, CONFIRMED, SHIPPED, DELIVERED)
 */
class OrderHistoryViewModel : ViewModel() {

  private val _ordersState = MutableStateFlow<String>(
    "Historial de pedidos — disponible cuando el backend soporte órdenes"
  )
  val ordersState: StateFlow<String> = _ordersState.asStateFlow()

  // cuando Order esté implementado en backend:
  // private val _ordersState = MutableStateFlow<UiState<List<Order>>>(UiState.Loading)
  // fun loadOrders() { viewModelScope.launch { getOrderHistoryUseCase()... } }
}