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

            // Inicializar la cantidad si no esta establecida
            if (binding.addToCartButton.quantityAdded.text.toString().isEmpty()) {
                binding.addToCartButton.quantityAdded.text = "0"
            }

            binding.addToCartButton.increaseQuantity.setOnClickListener {
                val newQuantity = getNextQuantity()
                binding.addToCartButton.quantityAdded.text = newQuantity.toString()
                onProductEvent.increaseQuantity(product, newQuantity)
            }

            binding.addToCartButton.decreaseQuantity.setOnClickListener {
                val newQuantity = getPreviousQuantity()
                binding.addToCartButton.quantityAdded.text = newQuantity.toString()
                onProductEvent.decreaseQuantity(product, newQuantity)
            }
        }

        private fun getNextQuantity(): Int {
            val currentText = binding.addToCartButton.quantityAdded.text.toString()
            val currentQuantity = try {
                currentText.toInt()
            } catch (e: NumberFormatException) {
                0
            }
            return currentQuantity + 1
        }


        private fun getPreviousQuantity(): Int {
            val currentText = binding.addToCartButton.quantityAdded.text.toString()
            val currentQuantity = try {
                currentText.toInt()
            } catch (e: java.lang.NumberFormatException) {
                0
            }

            val result = currentQuantity - 1
            return if (result < 0 ) 0 else result
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