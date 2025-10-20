package com.oax.comercioapp.ui.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.oax.comercioapp.data.api.NetworkResult
import com.oax.comercioapp.data.models.Product
import com.oax.comercioapp.data.models.ProductRequest
import com.oax.comercioapp.data.models.ProductResponse
import com.oax.comercioapp.data.repository.ProductRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

//import org.junit.jupiter.api.Assertions.*

@ExperimentalCoroutinesApi
class HomeViewModelTest {

    // Regla para ejecutar LiveData de forma sincrona en tests
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    // Dispatcher de prueba para coroutines
    private val testDispatcher = UnconfinedTestDispatcher()

    // Mock del repositorio usando MockK
    private lateinit var productRepository: ProductRepository

    // ViewModel a testear
    private lateinit var viewModel: HomeViewModel

    @Before
    fun onBefore() {
        // Crear mock del repositorio (relaxed para retornar valores por defecto)
        productRepository = mockk(relaxed = true)

        // Configurar dispatcher de prueba
        Dispatchers.setMain(testDispatcher)

        // Crear ViewModel con repositorio mock
        viewModel = HomeViewModel(productRepository)
    }

    @After
    fun tearDown() {
        // Limpiar dispatcher
        Dispatchers.resetMain()
    }

    @Test
    fun `loadProducts emite Loading y luego Success cuando el repositorio retorna productos`() =
        runTest {
            // ARRANGE (Preparar)
            val mockProducts = listOf(
                Product(idProduct = 1, product = "Product 1", price = 100.0),
                Product(2, "Product 2", 200.0)
            )
            val successResult = NetworkResult.Success(mockProducts)

            // Simular comportamiento del repositorio
            coEvery { productRepository.getProducts() } returns flowOf(successResult)

            // ACT (Actuar)
            viewModel.loadProducts()

            // ASSERT (Verificar)
            val result = viewModel.products.value
            assert(result is NetworkResult.Success)
            assert((result as NetworkResult.Success).data.size == 2)
            assert(result.data[0].product == "Product 1")
            assert(result.data[0].price == 100.0)
        }

    @Test
    fun `loadProducts emite Error cuando el repositorio falla`() = runTest {
        // ARRANGE
        val errorMessage = "Error de conexión"
        val errorResult = NetworkResult.Error<List<Product>>(errorMessage, 500)

        coEvery { productRepository.getProducts() } returns flowOf(errorResult)

        // ACT
        viewModel.loadProducts()

        // ASSERT
        val result = viewModel.products.value
        assert(result is NetworkResult.Error)
        assert((result as NetworkResult.Error).message == errorMessage)
        assert(result.code == 500)
    }

    @Test
    fun `loadProducts emite Loading antes de obtener resultados`() = runTest {
        // ARRANGE
        val loadingResult = NetworkResult.Loading<List<Product>>()

        coEvery { productRepository.getProducts() } returns flowOf(loadingResult)

        // ACT
        viewModel.loadProducts()

        // ASSERT
        val result = viewModel.products.value
        assert(result is NetworkResult.Loading)
    }

    @Test
    fun `createProduct emite Success cuando el producto se crea correctamente`() = runTest {
        //ARRANGE
        val productRequest = ProductRequest(
            product = "Nuevo producto",
            price = 150.0
        )
        val productResponse = ProductResponse(
            message = "Producto creado exitosamente",
            id = 5

        )
        val successResult = NetworkResult.Success(productResponse)

        coEvery { productRepository.createProduct(productRequest) } returns flowOf(successResult)

        // ACT
        viewModel.createProduct(productRequest)

        // ASSERT
        val result = viewModel.createProductResult.value
        assert(result is NetworkResult.Success)
        assert((result as NetworkResult.Success).data.message == "Producto creado exitosamente")
        assert(result.data.id == 5)
    }

    @Test
    fun `createProduct emite Error cuando falla la conexion`() = runTest {
        // ARRANGE
        val productRequest = ProductRequest(
            product = "Producto Inválido",
            price = -100.0
        )

        val errorResult = NetworkResult.Error<ProductResponse>("Precio inválido", 400)

        coEvery { productRepository.createProduct(productRequest) } returns flowOf(errorResult)

        // ACT
        viewModel.createProduct(productRequest)

        // ASSERT
        val result = viewModel.createProductResult.value
        assert(result is NetworkResult.Error)
        assert((result as NetworkResult.Error).message == "Precio inválido")
    }

    @Test
    fun `refreshProducts llama a loadProducts y actualiza el LiveData`() = runTest {
        // ARRANGE
        val mockProducts = listOf(
            Product(idProduct = 1, product = "Product 1", price = 100.0)
        )
        coEvery { productRepository.getProducts() } returns flowOf(NetworkResult.Success(mockProducts))

        // ACT
        viewModel.refreshProducts()

        // ASSERT
        // Se llama 2 veces: 1 en init + 1 en refresh
        coVerify(exactly = 2) { productRepository.getProducts() }
        assert(viewModel.products.value is NetworkResult.Success)
    }

    @Test
    fun `init carga productos automáticamente`() = runTest {
        // ASSERT
        // El viewModel ya se creó en setup(), verificamos que se llamó getProducts()
        coVerify(atLeast = 1) { productRepository.getProducts() }
    }

    @Test
    fun `products LiveData está vacio antes de cargar datos`() = runTest {
        // ARRANGE - Crear un nuevo ViewModel sin cargar datos
        val emptyResult = NetworkResult.Success(emptyList<Product>())
        coEvery { productRepository.getProducts() } returns flowOf(emptyResult)

        val newViewModel = HomeViewModel(productRepository)

        // ASSERT
        val result = newViewModel.products.value
        assert(result is NetworkResult.Success)
        assert((result as NetworkResult.Success).data.isEmpty())
    }
}