package com.oax.comercioapp.ui.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.oax.comercioapp.data.api.NetworkResult
import com.oax.comercioapp.data.models.Cart
import com.oax.comercioapp.data.models.CartItem
import com.oax.comercioapp.data.models.Product
import com.oax.comercioapp.data.models.ProductRequest
import com.oax.comercioapp.data.models.User
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class HomeFragmentTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private val sampleProduct1 = Product(
        idProduct = 1,
        "Laptop",
        price = 1000.0
    )

    private val sampleProduct2 = Product(
        2,
        "Mouse",
        29.99
    )

    private val sampleUser = User(
        idUser = 1,
        userName = "TestUser"
    )

    private val sampleCart = Cart(
        idCart = 1,
        idUser = 1,
        idProduct = 1,
        quantity = 5,
        addedDate = "2024-01-01"
    )

    private val sampleCartItem = CartItem(
        product = sampleProduct1,
        quantity = 5,
        addedDate = "2024-01-01"

    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun validateProductInput(name: String, price: String): Boolean {
        if (name.isEmpty()) {
            return false
        }

        if (price.isEmpty()){
            return false
        }

        return try {
            val priceValue = price.toDouble()
            priceValue > 0
        } catch (e: NumberFormatException) {
            false
        }
    }


    // ====== Tests de validación de productos =============
    @Test
    fun `validateInput - nombre vacio retorna false`() {
        // ARRANGE
        val name = ""
        val price = "100.0"

        // ACT
        val result = validateProductInput(name, price)

        assertFalse(result)
    }

    @Test
    fun `validateInput - nombre solo espacios retorna false`() {
        // ARRANGE
        val name = " "
        val price = "100.0"

        // ACT
        val result = validateProductInput(name.trim(), price)

        // ASSERT
        assertFalse(result)
    }

    @Test
    fun `validateInput - precio vacio retorna false`() {
        val name = " "
        val price = " "

        // ACT
        val result = validateProductInput(name, price)

        // ASSERT
        assertFalse(result)
    }

    @Test
    fun `validateInput - precio cero retorna false`() {
        // ARRANGE
        val name = "Laptop"
        val price = "0"

        // ACT
        val result = validateProductInput(name, price)

        // ASSERT
        assertFalse(result)
    }

    @Test
    fun `validateInput - precio negativo retorna false`() {
        // ARRANGE
        val name = "Laptop"
        val price = "-100.0"

        // ACT
        val result = validateProductInput(name, price)

        // ASSERT
        assertFalse(result)
    }

    @Test
    fun `validateInput - precio con letras retorna false`() {
        // ARRANGE
        val name = "Laptop"
        val price = "abc"

        // ACT
        val result = validateProductInput(name, price)

        // ASSERT
        assertFalse(result)
    }

    @Test
    fun `validateInput - precio con caracteres especiales retorna false`() {
        // ARRANGE
        val name = "Laptop"
        val price = "100.0$"

        // ACT
        val result = validateProductInput(name, price)

        // ASSERT
        assertFalse(result)
    }

    @Test
    fun `validateInput - datos validos retorna true`() {
        // ARRANGE
        val name = "Laptop"
        val price = "100.0"

        // ACT
        val result = validateProductInput(name, price)

        // ASSERT
        assertTrue(result)
    }

    @Test
    fun `validateInput - price entero es valido`() {
        // ARRANGE
        val name = "Mouse"
        val price = "40"

        // ACT
        val result = validateProductInput(name, price)

        // ASSERT
        assertTrue(result)
    }

    @Test
    fun `validateInput - precio con decimales es valido`(){
        // ARRANGE
        val name = "Mouse"
        val price = "40.15"

        // ACT
        val result = validateProductInput(name, price)

        // ASSERT
        assertTrue(result)
    }

    @Test
    fun `validateInput - nombre con espacios es valido`() {
        // ARRANGE
        val name = "Teclado mecánico RGB"
        val price = "200.50"

        // ACT
        val result = validateProductInput(name, price)

        // ASSERT
        assertTrue(result)
    }

    @Test
    fun `validateInput - precio muy alto es valido`() {
        // ARRANGE
        val name = "Servidor"
        val price = "999999.99"

        // ACT
        val result = validateProductInput(name, price)

        // ASSERT
        assertTrue(result)
    }

    @Test
    fun `validateInput - precio minimo valido`() {
        // ARRANGE
        val name = "Producto"
        val price = "0.01"

        // ACT
        val result = validateProductInput(name, price)

        // ASSERT
        assertTrue(result)
    }

    // ======== TESTS de lógica de cantidades del carrito ======
    @Test
    fun `getCurrentCartQuantity - producto no en carrito retorna 0`() {
        // ARRANGE
        val cartQuantities = mutableMapOf<Int, Int>()
        val productId = 1

        // ACT
        val quantity = cartQuantities[productId] ?: 0

        // ASSERT
        assertEquals(0, quantity)
    }

    @Test
    fun `getCurrentCartQuantity - producto en carrito retorna cantidad correcta`() {
        // ARRANGE
        val cartQuantities = mutableMapOf<Int, Int>()
        cartQuantities[1] = 5

        // ACT
        val quantity = cartQuantities[1] ?: 0

        // ASSERT
        assertEquals(5, quantity)
    }

    @Test
    fun `updateCartQuantities - actualiza mapa correctamente desde CartItem`() {
        // ARRANGE
        val cartQuantities = mutableMapOf<Int, Int>()
        val cartItems = listOf(
            CartItem(sampleProduct1, 3, "2024-01-01"),
            CartItem(sampleProduct2, 5, "2024-01-02")
        )

        // ACT
        cartQuantities.clear()
        cartItems.forEach { cartItem ->
            cartQuantities[cartItem.product.idProduct] = cartItem.quantity
        }

        // ASSERT
        assertEquals(3, cartQuantities[1])
        assertEquals(5, cartQuantities[2])
        assertEquals(2, cartQuantities.size)
    }

    @Test
    fun `updateCartQuantities - limpia cantidades previas antes de actualizar`() {
        // ARRANGE
        val cartQuantities = mutableMapOf<Int, Int>()
        cartQuantities[1] = 10
        cartQuantities[2] = 20

        val newCartItems = listOf(
            CartItem(sampleProduct1, 5, "2024-01-01")
        )

        // ACT
        cartQuantities.clear()
        newCartItems.forEach { cartItem ->
            cartQuantities[cartItem.product.idProduct] = cartItem.quantity
        }

        // ASSERT
        assertEquals(1, cartQuantities.size)
        assertEquals(5, cartQuantities[1])
        assertNull(cartQuantities[2])
    }

    @Test
    fun `clearCartQuantities - limpia el mapa completamente`() {
        // ARRANGE
        val cartQuantities = mutableMapOf<Int, Int>()
        cartQuantities[1] = 5
        cartQuantities[2] = 3
        cartQuantities[3] = 10

        // ACT
        cartQuantities.clear()

        // ASSERT
        assertEquals(0, cartQuantities.size)
        assertTrue(cartQuantities.isEmpty())
    }

    @Test
    fun `cartQuantities - maneja lista vacia de items`() {
        // ARRANGE
        val cartQuantities = mutableMapOf<Int, Int>()
        val emptyCartItems = emptyList<CartItem>()

        // ACT
        cartQuantities.clear()
        emptyCartItems.forEach { cartItem ->
            cartQuantities[cartItem.product.idProduct] = cartItem.quantity
        }

        //ASSERT
        assertEquals(0, cartQuantities.size)
    }

    @Test
    fun `cartQuantities - maneja multiples productos correctamente`() {
        // ARRANGE
        val cartQuantities = mutableMapOf<Int, Int>()

        // ACT
        cartQuantities[1] = 5
        cartQuantities[2] = 3
        cartQuantities[3] = 10

        // ASSERT
        assertEquals(3, cartQuantities.size)
        assertEquals(5, cartQuantities[1])
        assertEquals(3, cartQuantities[2])
        assertEquals(10, cartQuantities[3])
    }

    // TESTS de logica de sesion
    @Test
    fun `shouldReloadCart - retorna true cuando cambia userId`() {
        // ARRANGE
        val lastLoadedUserId: Int ?= 1
        val newUserId = 2

        // ACT
        val shouldReload = lastLoadedUserId != newUserId

        // ASSERT
        assertTrue(shouldReload)
    }

    @Test
    fun `shouldReloadCart - retorna false cuando userId es el mismo`() {
        // ARRANGE
        val lastLoadedUserId: Int ?= 1
        val newUserId = 1

        // ACT
        val shouldReload = lastLoadedUserId != newUserId

        // ASSERT
        assertFalse(shouldReload)
    }

    @Test
    fun `shouldReloadCart - retorna true cuando lastLoadedUserId es null`() {
        // ARRANGE
        val lastLoadedUserId: Int ?= null
        val newUserId = 1

        // ACT
        val shouldReload = lastLoadedUserId != newUserId

        assertTrue(shouldReload)
    }

    @Test
    fun `shouldReloadCart - retorna true cuando newUserId cambia a null`() {
        // ARRANGE
        val lastLoadedUserId: Int ?= 1
        val newUserId: Int ?= null

        // ACT
        val shouldReload = lastLoadedUserId != newUserId

        assertTrue(shouldReload)
    }

    @Test
    fun `isValidUser - usuario con id mayor a 0 es valido`() {
        // ARRANGE
        val user = User(1, "TestUser")

        // ACT
        val isValid = user.idUser > 0

        // ASSERT
        assertTrue(isValid)
    }

    @Test
    fun `isValidUser - usuario con id 0 o menor no es valido`() {
        // ARRANGE
        val user1 = User(0, "TestUser")
        val user2 = User(-1, "TestUser")

        // ACT & ASSERT
        assertFalse(user1.idUser > 0)
        assertFalse(user2.idUser > 0)
    }

    // =========  Test de formato de titulo ======
    @Test
    fun `formatHomTitle - con usuario incluye nombre correctamente`() {
        // ARRANGE
        val userName = "TestUser"
        val baseTitle = "Productos"

        // ACT
        val result = "$baseTitle\n Sesión: $userName"

        // ASSERT
        assertTrue(result.contains(userName))
        assertTrue(result.contains(baseTitle))
        assertTrue(result.contains("Sesión:"))
    }

    @Test
    fun formatHomeTitle_manejaNombresDeUsuarioEspeciales(){
        // ARRANGE
        val userName = "Usuario@123"
        val baseTitle = "Productos"

        // ACT
        val result = "$baseTitle\n Sesión: $userName"

        // ASSERT
        assertTrue(result.contains("Usuario@123"))
    }

    // ======== Test logica de eliminacion =========
    @Test
    fun shouldShowRemoveDialog_TrueCuandoCantidadMayor_a_0(){
        // ARRANGE
        val currentQty = 5

        // ACT
        val shouldShow = currentQty > 0

        // ASSERT
        assertTrue(shouldShow)
    }

    @Test
    fun shouldShowRemoveDialog_FalseCuandoCantidad_es_0() {
        // ARRANGE
        val currentQty = 0

        // ACT
        val shouldShow = currentQty > 0

        // ASSERT
        assertFalse(shouldShow)
    }

    // ========= Tests de NetworkResult Handling =========

    @Test
    fun handleProductResult_LoadingMuestraProgressBar() {
        // ARRANGE
        val result = NetworkResult.Loading<List<Product>>()

        // ACT
        val isLoagin = result is NetworkResult.Loading

        // ASSERT
        assertTrue(isLoagin)
    }

    @Test
    fun handleProductResult_SuccessContieneDatos() {
        // ARRANGE
        val products = listOf(sampleProduct1, sampleProduct2)
        val result = NetworkResult.Success(products)

        // ACT
        val isSuccess = result is NetworkResult.Success
        val data = (result as NetworkResult.Success).data

        // ASSERT
        assertTrue(isSuccess)
        assertEquals(2, data.size)
        assertEquals("Laptop", data[0].product)
    }

    @Test
    fun handleProductResult_ErrorContieneMensaje(){
        // ARRANGE
        val errorMessage = "Error de red"
        val result = NetworkResult.Error<List<Product>>(errorMessage)

        // ACT
        val isError = result is NetworkResult.Error
        val message = (result as NetworkResult.Error).message

        // ASSERT
        assertTrue(isError)
        assertEquals(errorMessage, message)
    }

    @Test
    fun handleCartItemsResult_SuccessConListaVacia_es_valido() {
        // ARRANGE
        val emptyList = emptyList<CartItem>()
        val result = NetworkResult.Success(emptyList)

        // ACT
        val isSuccess = result is NetworkResult.Success
        val data = (result as NetworkResult.Success).data

        // ASSERT
        assert(isSuccess)
        assertTrue(data.isEmpty())
    }

    @Test
    fun handleCartItemsResult_ErrorLimpiaCantidades() {
        // ARRANGE
        val cartQuantities = mutableMapOf<Int, Int>()
        cartQuantities[1] = 5
        val result = NetworkResult.Error<List<CartItem>>("Error")

        // ACT
        if (result is NetworkResult.Error) {
            cartQuantities.clear()
        }

        // ASSERT
        assertEquals(0, cartQuantities.size)
    }

    // =========== Test de modelos ============

    @Test
    fun Product_seCreaCorrectamenteConTodosLosCampos() {
        // ACT
        val product = Product(1, "Laptop", 1000.0)

        // ASSERT
        assertEquals(1, product.idProduct,)
        assertEquals("Laptop", product.product)
        assertEquals(1000.0, product.price, 0.01)
    }

    @Test
    fun CartItem_seCreaCorrectamente() {
        // ACT
        val cartItem = CartItem(sampleProduct1, 5, "2024-01-01")

        // ASSERT
        assertEquals(sampleProduct1, cartItem.product)
        assertEquals(5, cartItem.quantity)
        assertEquals("2024-01-01", cartItem.addedDate)
    }

    @Test
    fun User_seCreaCorrectamente() {
        // ACT
        val user = User(1, "TestUser")

        // ASSERT
        assertEquals(1, user.idUser)
        assertEquals("TestUser", user.userName)
    }

    @Test
    fun ProductRequest_seCreaCorrectamenteParaAPI() {
        // ACT
        val request = ProductRequest("Laptop", 1000.0)

        // ASSERT
        assertEquals("Laptop", request.product)
        assertEquals(1000.0, request.price, 0.01)
    }

    // ======== Tests de casos edge ==============
    @Test
    fun validateInput_manejaPrecioConMuchosDecimales() {
        // ARRANGE
        val name = "Product"
        val price = "999.9999999999"

        // ACT
        val result = validateProductInput(name, price)

        // ASSERT
        assertTrue(result)
    }

    @Test
    fun validateInput_manejaNombreMuyLargo() {
        // ARRANGE
        val name = "A".repeat(200)
        val price = "100.0"

        // ACT
        val result = validateProductInput(name, price)

        // ASSERT
        assertTrue(result)
    }

    @Test
    fun validateInput_trimEliminaEspaciosEnBlanco() {
        // ARRANGE
        val name = " Laptop "
        val price = " 100.0 "

        // ACT
        val result = validateProductInput(name.trim(), price.trim())

        // ASSERT
        assertTrue(result)
    }

    @Test
    fun cartQuantities_sobrescribeCantidadExistente() {
        // ARRANGE
        val cartQuantities = mutableMapOf<Int, Int>()
        cartQuantities[1] = 5

        // ACT
        cartQuantities[1] = 10

        // ASSERT
        assertEquals(10, cartQuantities[1])
    }

    @Test
    fun cartQuantities_permiteCantidad_0_explicita() {
        // ARRANGE
        val cartQuantities = mutableMapOf<Int, Int>()

        // ACT
        cartQuantities[1] = 0

        assertEquals(0, cartQuantities[1])
        assertTrue(cartQuantities.containsKey(1))
    }

    // ============== Tests de integracion de logica ===========
    @Test
    fun flujoCompleto_cambioDeUsuarioLimpiaYrecargaCarrito() {
        val cartQuantities = mutableMapOf<Int, Int>()
        cartQuantities[1] = 5
        cartQuantities[2] = 3

        var lastLoadedUserId: Int ?= 1
        val newUser = User(2, "NewUser")

        // ACT y Simular cambio de usuario
        if (lastLoadedUserId != newUser.idUser) {
            cartQuantities.clear()
            lastLoadedUserId = newUser.idUser
        }

        // ASSERT
        assertEquals(0, cartQuantities.size)
        assertEquals(2, lastLoadedUserId)
    }

    @Test
    fun flujoCompleto_actualizarCantidadesDesdeAPI() {
        // ARRANGE
        val cartQuantities = mutableMapOf<Int, Int>()
        val cartItems = listOf(
            CartItem(Product(1, "Laptop", 1000.0), 3 , "2024-01-01"),
            CartItem(Product(2, "Mouse", 30.0), 5, "2024-01-02")
        )

        // ACT
        cartQuantities.clear()
        cartItems.forEach { item ->
            cartQuantities[item.product.idProduct] = item.quantity
        }

        // ASSERT
        assertEquals(2, cartQuantities.size)
        assertEquals(3, cartQuantities[1])
        assertEquals(5, cartQuantities[2])
    }
}