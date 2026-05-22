package com.oax.comercioapp.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.oax.comercioapp.databinding.ItemProductBinding
import com.oax.comercioapp.domain.model.Product

/**
 * CAMBIOS RESPECTO AL ORIGINAL:
 * 1. Import cambiado: data.models.Product -> domain.model.Product
 *    Este es el cambio que conecta el adapter con la nueva arquitectura. Ahora recibe entidades
 *    de dominio limpias - sin @SerializedName, sin idProduct (ahora es 'id'), sin 'product' (ahora es 'name')
 *
 * 2. product.product -> product.name
 *    product.idProduct -> product.id
 *    Exactamente el problema que mencionamos al analizar el adapter original:
 *    "bindig.textProductName.text = product.product" era confuso.
 *    Ahora es "binding.textProductName.text "product.name" - semántico y claro
 *
 * 3. ELIMINADOS los Log.d de cada operación
 *
 * 4. ProductEvens CONSERVADA con ajuste de firma
 *    La interfaz sigue siendo el contrato entre adapter y Fragment/ViewModel.
 *    Solo cambia el tipo de Product importado. La arquitectura de callbacks es correcta y se mantiene.
 *
 * 5. DiffCallback usa product.id en vez de product.idProduct
 *    Crítico para que DiffUtil identifique correctamente los mismos ítems y solo redibuje los que cambiaron -
 *    sin esto, cada update recarga toda la lista visualmente.
 *
 * 6. updateQuantity() conservada con payloads
 *    El mecanismo de partial bind via payloads es una optimización válida que evita redibujar toda la vista
 *    del ítem cuando solo cambia la cantidad. Se conserva porque funciona bien y es una buena práctica.
 */

interface ProductEvents {
    fun increaseQuantity(product: Product, quantity: Int)
    fun decreaseQuantity(product: Product, quantity: Int)
    fun removeQuantities(product: Product)
}

class ProductAdapter(
    private val onProductEvent: ProductEvents,
    private val getCurrentCartQuantity: (Product) -> Int = {0} // Nueva funcion callback
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int, payloads: MutableList<Any?>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            holder.updateQuantity(getItem(position))
        }
    }

    inner class ProductViewHolder(
        private val binding: ItemProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.textProductName.text = product.name
            binding.textProductPrice.text = "$${"%.2f".format(product.price)}"

            // Actualizar cantidad
            updateQuantity(product)

            binding.addToCartButton.increaseQuantity.setOnClickListener {
                // Obtener cantidad freca cada vez
                val qty = getCurrentCartQuantity(product)
                val newQty = qty + 1
                binding.addToCartButton.quantityAdded.text = (newQty).toString()
                onProductEvent.increaseQuantity(product, newQty)
            }

            binding.addToCartButton.decreaseQuantity.setOnClickListener {
                val qty = getCurrentCartQuantity(product)
                val newQty = maxOf(0, qty - 1)
                binding.addToCartButton.quantityAdded.text = newQty.toString()
                onProductEvent.decreaseQuantity(product, newQty)
            }

            binding.removeQuantitiesButton.removeQuantities.setOnClickListener {
                val qty = getCurrentCartQuantity(product)
                if (qty > 0) {
                    binding.addToCartButton.quantityAdded.text = "0"
                    onProductEvent.removeQuantities(product)
                } else {
                    // nothing
                }
            }
        }
        fun updateQuantity(product: Product) {
            val currentQuantity = getCurrentCartQuantity(product)
            binding.addToCartButton.quantityAdded.text = currentQuantity.toString()
        }

    }


    class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
}