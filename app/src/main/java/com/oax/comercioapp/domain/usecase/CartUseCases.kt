package com.oax.comercioapp.domain.usecase

import com.oax.comercioapp.domain.model.Cart
import com.oax.comercioapp.domain.model.CartItem
import com.oax.comercioapp.domain.model.Product
import com.oax.comercioapp.domain.repository.ICartRepository
import com.oax.comercioapp.domain.repository.IProductRepository

/**
 * Enfocados en el nuevo rumbo: tipo refaccionaria
 * El cliente consulta productos y construye su pedido en el carrito.
 * No hay operaciones de administración de inventario aquí.
 */

// Productos (cátalogo, solo lectura)

/**
 * Obtiene el catálogo completo de productos disponibles.
 *
 * ¿Por qué existe este use case si solo llama al repositorio?
 * Por el momento solo delega, pero después puede agregar:
 *      - Filtrar productos sin stock antes de mostrarlos
 *      - Ordenar por categoría o precio
 *      - Combinar productos de local (Room) con actualizaciones de red
 * Todoo eso ocurre aquí, sin tocar el ViewModel ni el repositorio.
 * Eso es en el Open/Closed Principles: el use case está abierto a esa extensión sin necesidad de modificar otras capas.
 */
class GetProductsUseCase(
    private val productRepository: IProductRepository
) {
    suspend operator fun invoke(): Result<List<Product>> {
        return productRepository.getProducts()
    }
}

class GetProductDetailUseCase(
    private val productRepository: IProductRepository
) {
    suspend operator fun invoke(productId: Int): Result<Product> {
        if (productId <= 0) {
            return Result.failure(IllegalArgumentException("ID de producto inválido"))
        }
        return productRepository.getProductById(productId)
    }
}

// Carrito
class GetCartUseCase(
    private val cartRepository: ICartRepository
) {
    suspend operator fun invoke(): Result<Cart> {
        return cartRepository.getCart()
    }
}

/**
 * Agrega un producto al carrito con validación de cantidad.
 *
 * La validación quantity > 0 es una REGLA de NEGOCIO:
 * no tiene sentido agregar 0 o menos unidades de un producto.
 * Esta regla vive aquí, no en el Fragment (que podría bypassearla)
 * ni en el repositorio (que no debería conocer reglas de UI).
 *
 * Relación con ProductAdapter:
 * El Adapter calcula la nueva cantidad y la pasa al ViewModel.
 * El ViewModel llama a este use case. Si la cantidad es inválida,
 * el use case retorna failure y el ViewModel actualiza la UI con el error.
 * El adapter nunca se entera del detalle - solo reacciona al estado de la UI.
 */

class AddToCartUseCase(
    private val cartRepository: ICartRepository
) {
    suspend operator fun invoke(productId: Int, quantity: Int): Result<CartItem> {
        if (quantity <= 0) {
            return Result.failure(
                IllegalArgumentException("La cantidad debe ser mayor a cero")
            )
        }

        if (productId <= 0) {
            return Result.failure(IllegalArgumentException("Producto inválido"))
        }
        return cartRepository.addToCart(productId, quantity)
    }
}

class UpdateCartItemUseCase(
    private val cartRepository: ICartRepository
) {
    /**
     * Si quantity llaga a 0 desde el adapter (botón "-" llegó al límite),
     * en vez de enviar una actualización a 0, eliminamos el ítem.
     * Eso es lógica de negocio: cantidad 0 = fuera del carrito.
     *
     * NOTA para revisión futura de ProductAdapter:
     * Este use case hace que el adapter no necesite distinguir entre "actualizar a 0" y "eliminar"
     * - simplifica la lógica del adapter.
     */
    suspend operator fun invoke(productId: Int, quantity: Int): Result<CartItem> {
        if (quantity < 0) {
            return Result.failure(IllegalArgumentException("La cantidad no puede ser negativa"))
        }

        // Si quantity == 0, delegar a remove para mantener consistencia
        if (quantity == 0) {
            cartRepository.removeFromCart(productId)
            // Retornamos failure informativa - el ViewModel sabrá que fue eliminado
            return Result.failure(IllegalStateException("ITEM_REMOVED"))
        }
        return cartRepository.updateCartItem(productId, quantity)
    }
}

class RemoveFromCartUseCase(
    private val cartRepository: ICartRepository
) {
    suspend operator fun invoke(productId: Int): Result<Unit> {
        if (productId == 0) {
            return Result.failure(IllegalArgumentException("Producto inválido"))
        }
        return cartRepository.removeFromCart(productId)
    }
}

class GetCartCountUseCase(
    private val cartRepository: ICartRepository
) {
    suspend operator fun invoke(): Result<Int> = cartRepository.getCartCount()
}
