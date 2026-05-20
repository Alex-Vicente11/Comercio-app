package com.oax.comercioapp.ui

/**
 * ¿Por qué reemplazar NetworkResult<T> en los ViewModels?
 * NetworkResult vive en data.api - es un concepto de infraestructura de red.
 * Los ViewModels pertenecen a la capa de presentación y no deberían conocer que los datos
 * vienen de una red. Si se agrega caché local con Room, el ViewModel no debería cambiar solo
 * porque cambió la fuente de datos.
 *
 * UiState es el contrato de PRESENTACIÓN: describe qué debe mostar la UI, no de dónde vienen
 * los datos. Es independiente de Retrofit, Room o cualquier fuente de datos.
 *
 * Comparación:
 *   NetworkResult.Loading -> UiState.Loading
 *   NetworkResult.Success -> UiState.Success
 *   NetworkResult.Error   -> UiState.Error  (con mensaje para el usuario, no técnico)
 *
 * La diferencia clave está en Error: NetworkResult.Error puede tener un código HTTP (detalle de red).
 * UiState.Error solo tiene el mensaje que se muestra al usuario. La traducción de "código 401" a
 * "Sesión expirada, inicia sesión" ocurre en el ViewModel - nunca en el Fragment.
 *
 * Uso con StateFlow en ViewModel:
 *   private val _uiState = MutableStateFlow<UiState<User>>(UiState.Idle)
 *   val uiState: StateFlow<UiState<User>> = _uiState.asStateFlow()
 *
 * Uso en Fragment:
 *   viewLifecycleOwner.lifecycleScope.launch {
 *     viewModel.uiState.collect { state ->
 *       when(state) {
 *          is UiState.Loading -> showLoading()
 *          is UiState.Success -> showUser(state.data)
 *          is UiState.Error   -> showError(state.message)
 *          is UiState.Idle    -> Unit
 *      }
 *    }
 *  }
 *
 * ¿Por qué StateFlow y no LiveData?
 * StateFlow es parte de Kotlin Coroutines - no depende de Android (testeable sin emulador).
 * LiveData depende del ciclo de vida de Android. Con StateFlow el ViewModel es Kotlin puro y más
 * fácil de testear unitariamente. La integración con el ciclo de vida se maneja en el Fragment con
 * repeatOnLifecycle / flowWithLifeCycle.
 *
 * NOTA: NetworkResult.kt se conserva temporalmente para el código existente que aún no se ha migrado.
 * Se elimina al final.
 */

sealed class UiState<out T> {
    /**
     * Estado inicial - antes de que el usuario haga cualquier acción o después de que se limpió
     * el estado anterior. Evita que la UI reaccione a un estado vacío como si fuera un error.
     */
    object Idle : UiState<Nothing>()

    /**
     * Operación en progreso - mostrar indicador de carga.
     * No tiene datos asociados porque mientras carga no hay nada que mostrar.
     */
    object Loading: UiState<Nothing>()

    /**
     * Operación exitosa con datos.
     * @param data El resultado de la operación, del tipo que necesite la UI.
     */
    data class Success<T>(val data: T) : UiState<T>()

    /**
     * Operación fallida con mensaje para el usuario.
     * @param message Mensaje legible para el usuario (no el mensaje técnico de la API).
     *                      La traducción técnica -> legible ocurre en el ViewModel.
     */
    data class Error(val message: String) : UiState<Nothing>()
}