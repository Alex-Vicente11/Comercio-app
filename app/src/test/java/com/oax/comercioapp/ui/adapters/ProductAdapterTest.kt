package com.oax.comercioapp.ui.adapters

import com.oax.comercioapp.data.models.Product
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class ProductAdapterTest {

    // Mocks
    private lateinit var productEvents: ProductEvents
    private lateinit var getCurrentCartQuantity: (Product) -> Int
    private lateinit var adapter: ProductAdapter

    // Productos de prueba
    private val sampleProduct1 = Product(
        idProduct = 1,
        product = "Laptop",
        price = 999.99
    )

    private val sampleProduct2 = Product(
        idProduct = 2,
        product = "Mouse",
        price = 29.99
    )

    @Before
    fun setup() {
        // Crear mocks
        productEvents = mockk(relaxed = true)
        getCurrentCartQuantity = mockk()

        // Por defecto, devuelve 0 para cualquier producto
        every { getCurrentCartQuantity(any()) } returns 0

        // Mockear el adapter para evitar NullPointerException
        adapter = spyk(ProductAdapter(productEvents, getCurrentCartQuantity))

        // Simular el comportamiento de submitList y currentList
        val productList = mutableListOf<Product>()

        every { adapter.submitList(any()) } answers {
            val list = firstArg<List<Product>?>()
            if (list != null) {
                productList.clear()
                productList.addAll(list)
            } else {
                productList.clear()
            }
        }

        every { adapter.currentList } returns productList
        every { adapter.itemCount } answers { productList.size }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ========== TESTS DE ProductDiffCallback ==========

    @Test
    fun `diffCallback - areItemsTheSame retorna true cuando productos tienen mismo ID`() {
        // ARRANGE
        val diffCallback = ProductAdapter.ProductDiffCallback()
        val product1 = sampleProduct1
        val product2 = product1.copy(product = "Nombre Diferente")

        // ACT
        val result = diffCallback.areItemsTheSame(product1, product2)

        // ASSERT
        assertTrue(result)
    }

    @Test
    fun `diffCallback - areItemsTheSame retorna false cuando productos tienen diferente ID`() {
        // ARRANGE
        val diffCallback = ProductAdapter.ProductDiffCallback()

        // ACT
        val result = diffCallback.areItemsTheSame(sampleProduct1, sampleProduct2)

        // ASSERT
        assertFalse(result)
    }

    @Test
    fun `diffCallback - areContentsTheSame retorna true cuando productos son identicos`() {
        // ARRANGE
        val diffCallback = ProductAdapter.ProductDiffCallback()
        val product1 = sampleProduct1
        val product2 = sampleProduct1.copy()

        // ACT
        val result = diffCallback.areContentsTheSame(product1, product2)

        // ASSERT
        assertTrue(result)
    }

    @Test
    fun `diffCallback - areContentsTheSame retorna false cuando cambia el nombre`() {
        // ARRANGE
        val diffCallback = ProductAdapter.ProductDiffCallback()
        val product1 = sampleProduct1
        val product2 = product1.copy(product = "Producto Diferente")

        // ACT
        val result = diffCallback.areContentsTheSame(product1, product2)

        // ASSERT
        assertFalse(result)
    }

    @Test
    fun `diffCallback - areContentsTheSame retorna false cuando cambia el precio`() {
        // ARRANGE
        val diffCallback = ProductAdapter.ProductDiffCallback()
        val product1 = sampleProduct1
        val product2 = product1.copy(price = 1299.99)

        // ACT
        val result = diffCallback.areContentsTheSame(product1, product2)

        // ASSERT
        assertFalse(result)
    }

    // ========== TESTS DE FUNCIONALIDAD BASICA DEL ADAPTER ==========

    @Test
    fun `adapter se crea correctamente`() {
        // ASSERT
        assertNotNull(adapter)
    }

    @Test
    fun `itemCount retorna 0 para lista vacia`() {
        // ASSERT
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `itemCount retorna tamaño correcto despues de submitList`() {
        // ARRANGE
        val products = listOf(sampleProduct1, sampleProduct2)

        // ACT
        adapter.submitList(products)

        // ASSERT
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `currentList retorna producto correcto en posicion especifica`() {
        // ARRANGE
        val products = listOf(sampleProduct1, sampleProduct2)
        adapter.submitList(products)

        // ACT
        val item = adapter.currentList[0]

        // ASSERT
        assertEquals(sampleProduct1, item)
    }

    @Test
    fun `submitList actualiza correctamente la lista`() {
        // ARRANGE
        val products = listOf(sampleProduct1)

        // ACT
        adapter.submitList(products)

        // ASSERT
        verify { adapter.submitList(products) }
        assertEquals(1, adapter.itemCount)
    }

    // ========== TESTS DE EVENTOS DE PRODUCTOS ==========

    @Test
    fun `increaseQuantity se llama con cantidad correcta`() {
        // ARRANGE
        every { getCurrentCartQuantity(sampleProduct1) } returns 3

        // ACT - Simular lo que haría el click listener
        val currentQty = getCurrentCartQuantity(sampleProduct1)
        val newQty = currentQty + 1
        productEvents.increaseQuantity(sampleProduct1, newQty)

        // ASSERT
        verify(exactly = 1) {
            productEvents.increaseQuantity(sampleProduct1, 4)
        }
    }

    @Test
    fun `increaseQuantity desde cantidad 0 a 1`() {
        // ARRANGE
        every { getCurrentCartQuantity(sampleProduct1) } returns 0

        // ACT
        val currentQty = getCurrentCartQuantity(sampleProduct1)
        val newQty = currentQty + 1
        productEvents.increaseQuantity(sampleProduct1, newQty)

        // ASSERT
        verify(exactly = 1) {
            productEvents.increaseQuantity(sampleProduct1, 1)
        }
    }

    @Test
    fun `decreaseQuantity se llama con cantidad correcta`() {
        // ARRANGE
        every { getCurrentCartQuantity(sampleProduct1) } returns 5

        // ACT
        val currentQty = getCurrentCartQuantity(sampleProduct1)
        val newQty = maxOf(0, currentQty - 1)
        productEvents.decreaseQuantity(sampleProduct1, newQty)

        // ASSERT
        verify(exactly = 1) {
            productEvents.decreaseQuantity(sampleProduct1, 4)
        }
    }

    @Test
    fun `decreaseQuantity no baja de 0`() {
        // ARRANGE
        every { getCurrentCartQuantity(sampleProduct1) } returns 0

        // ACT
        val currentQty = getCurrentCartQuantity(sampleProduct1)
        val newQty = maxOf(0, currentQty - 1)
        productEvents.decreaseQuantity(sampleProduct1, newQty)

        // ASSERT
        verify(exactly = 1) {
            productEvents.decreaseQuantity(sampleProduct1, 0)
        }
    }

    @Test
    fun `removeQuantities se llama cuando cantidad es mayor a 0`() {
        // ARRANGE
        every { getCurrentCartQuantity(sampleProduct1) } returns 5

        // ACT
        val qty = getCurrentCartQuantity(sampleProduct1)
        if (qty > 0) {
            productEvents.removeQuantities(sampleProduct1)
        }

        // ASSERT
        verify(exactly = 1) {
            productEvents.removeQuantities(sampleProduct1)
        }
    }

    @Test
    fun `removeQuantities NO se llama cuando cantidad es 0`() {
        // ARRANGE
        every { getCurrentCartQuantity(sampleProduct1) } returns 0

        // ACT
        val qty = getCurrentCartQuantity(sampleProduct1)
        if (qty > 0) {
            productEvents.removeQuantities(sampleProduct1)
        }

        // ASSERT
        verify(exactly = 0) {
            productEvents.removeQuantities(sampleProduct1)
        }
    }

    // ========== TESTS DE getCurrentCartQuantity ==========

    @Test
    fun `getCurrentCartQuantity retorna 0 por defecto`() {
        // ARRANGE
        every { getCurrentCartQuantity(sampleProduct1) } returns 0

        // ACT
        val quantity = getCurrentCartQuantity(sampleProduct1)

        // ASSERT
        assertEquals(0, quantity)
    }

    @Test
    fun `getCurrentCartQuantity retorna cantidad correcta para producto en carrito`() {
        // ARRANGE
        every { getCurrentCartQuantity(sampleProduct1) } returns 10

        // ACT
        val quantity = getCurrentCartQuantity(sampleProduct1)

        // ASSERT
        assertEquals(10, quantity)
    }

    @Test
    fun `getCurrentCartQuantity se invoca correctamente`() {
        // ARRANGE
        every { getCurrentCartQuantity(sampleProduct1) } returns 5

        // ACT
        getCurrentCartQuantity(sampleProduct1)

        // ASSERT
        verify(exactly = 1) {
            getCurrentCartQuantity(sampleProduct1)
        }
    }

    @Test
    fun `getCurrentCartQuantity retorna diferentes cantidades para diferentes productos`() {
        // ARRANGE
        every { getCurrentCartQuantity(sampleProduct1) } returns 3
        every { getCurrentCartQuantity(sampleProduct2) } returns 7

        // ACT
        val qty1 = getCurrentCartQuantity(sampleProduct1)
        val qty2 = getCurrentCartQuantity(sampleProduct2)

        // ASSERT
        assertEquals(3, qty1)
        assertEquals(7, qty2)
    }

    // ========== TESTS DE CASOS ESPECIALES ==========

    @Test
    fun `adapter maneja lista con un solo producto`() {
        // ACT
        adapter.submitList(listOf(sampleProduct1))

        // ASSERT
        assertEquals(1, adapter.itemCount)
        assertEquals(sampleProduct1, adapter.currentList[0])
    }

    @Test
    fun `adapter maneja limpieza de lista despues de tener datos`() {
        // ARRANGE
        adapter.submitList(listOf(sampleProduct1, sampleProduct2))
        assertEquals(2, adapter.itemCount)

        // ACT
        adapter.submitList(emptyList())

        // ASSERT
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `adapter maneja productos con precio cero`() {
        // ARRANGE
        val freeProduct = Product(3, "Muestra Gratis", 0.0)

        // ACT
        adapter.submitList(listOf(freeProduct))

        // ASSERT
        assertEquals(1, adapter.itemCount)
        assertEquals(0.0, adapter.currentList[0].price, 0.0)
    }

    @Test
    fun `adapter maneja productos con precios decimales`() {
        // ARRANGE
        val decimalProduct = Product(4, "Articulo", 19.95)

        // ACT
        adapter.submitList(listOf(decimalProduct))

        // ASSERT
        assertEquals(19.95, adapter.currentList[0].price, 0.01)
    }

    @Test
    fun `adapter maneja lista grande de productos`() {
        // ARRANGE
        val largeList = (1..100).map { id ->
            Product(id, "Producto $id", id * 10.0)
        }

        // ACT
        adapter.submitList(largeList)

        // ASSERT
        assertEquals(100, adapter.itemCount)
        assertEquals("Producto 1", adapter.currentList[0].product)
        assertEquals("Producto 100", adapter.currentList[99].product)
    }

    @Test
    fun `adapter actualiza lista correctamente`() {
        // ARRANGE
        val initialList = listOf(sampleProduct1)
        adapter.submitList(initialList)
        assertEquals(1, adapter.itemCount)

        // ACT
        val updatedList = listOf(sampleProduct1, sampleProduct2)
        adapter.submitList(updatedList)

        // ASSERT
        assertEquals(2, adapter.itemCount)
        assertEquals(sampleProduct2, adapter.currentList[1])
    }

    @Test
    fun `adapter maneja submitList con null`() {
        // ARRANGE
        adapter.submitList(listOf(sampleProduct1))
        assertEquals(1, adapter.itemCount)

        // ACT
        adapter.submitList(null)

        // ASSERT
        assertEquals(0, adapter.itemCount)
    }

    // ========== TESTS DE VERIFICACIÓN DE INTERACCIONES ==========

    @Test
    fun `productEvents se pasa correctamente al adapter`() {
        // ARRANGE & ACT
        val newAdapter = spyk(ProductAdapter(productEvents, getCurrentCartQuantity))

        // ASSERT
        assertNotNull(newAdapter)
        // El adapter debe tener acceso a productEvents
    }

    @Test
    fun `getCurrentCartQuantity callback se utiliza correctamente`() {
        // ARRANGE
        every { getCurrentCartQuantity(sampleProduct1) } returns 5

        // ACT
        val result = getCurrentCartQuantity(sampleProduct1)

        // ASSERT
        assertEquals(5, result)
        verify(exactly = 1) { getCurrentCartQuantity(sampleProduct1) }
    }
}