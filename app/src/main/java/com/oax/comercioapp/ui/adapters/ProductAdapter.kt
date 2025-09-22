package com.oax.comercioapp.ui.adapters

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
}

class ProductAdapter(
    private val onProductEvent: ProductEvents
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

            binding.addToCartButton.increaseQuantity.setOnClickListener {
                onProductEvent.increaseQuantity(
                    product,
                    getNextQuantity()
                )
            }

            binding.addToCartButton.decreaseQuantity.setOnClickListener {
                onProductEvent.decreaseQuantity(
                    product,
                    getPreviousQuantity()
                )
            }
        }

        private fun getNextQuantity() =
            (Integer.getInteger(binding.addToCartButton.quantityAdded.text.toString()) ?: 0) + 1

        private fun getPreviousQuantity(): Int {
            val result =
                (Integer.getInteger(binding.addToCartButton.quantityAdded.text.toString()) ?: 0) - 1

            return if (result < 0) {
                0
            } else {
                result
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