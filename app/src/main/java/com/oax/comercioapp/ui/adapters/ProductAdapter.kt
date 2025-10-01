package com.oax.comercioapp.ui.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.oax.comercioapp.data.models.Product
import com.oax.comercioapp.databinding.ItemProductBinding

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

    inner class ProductViewHolder(
        private val binding: ItemProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.textProductName.text = product.product
            binding.textProductPrice.text = "$${"%.2f".format(product.price)}"

            // Actualizar el TextView cada vez que se hace bind
            val currentQuantity = getCurrentCartQuantity(product)
            binding.addToCartButton.quantityAdded.text = currentQuantity.toString()

            binding.addToCartButton.increaseQuantity.setOnClickListener {
                // Obtener cantidad freca cada vez
                val qty = getCurrentCartQuantity(product)
                binding.addToCartButton.quantityAdded.text = (qty + 1).toString()
                onProductEvent.increaseQuantity(product, qty + 1)
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
                    Log.d("ProductAdapter", "Product not in cart, quantity is 0")
                }
            }
        }

    }


    class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.idProduct == newItem.idProduct
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
}