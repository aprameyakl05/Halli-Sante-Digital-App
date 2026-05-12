package com.example.halli_sante_digital

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.halli_sante_digital.databinding.ActivityDetailBinding
import com.google.firebase.auth.FirebaseAuth

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val name = intent.getStringExtra("product_name")
        val price = intent.getStringExtra("product_price")
        val category = intent.getStringExtra("product_category")
        val imageUrl = intent.getStringExtra("product_image")
        val sellerName = intent.getStringExtra("product_seller_name")
        val sellerPhone = intent.getStringExtra("product_seller_phone")
        val sellerId = intent.getStringExtra("product_seller_id")
        val description = intent.getStringExtra("product_description")
        val inStock = intent.getBooleanExtra("product_in_stock", true)
        val productId = intent.getStringExtra("product_id")

        binding.apply {
            if (auth.currentUser?.uid == sellerId) {
                fabEdit.visibility = View.VISIBLE
            }

            fabEdit.setOnClickListener {
                val editIntent = Intent(this@DetailActivity, UploadActivity::class.java).apply {
                    putExtra("edit_mode", true)
                    putExtra("product_id", productId)
                    putExtra("product_name", name)
                    putExtra("product_price", price)
                    putExtra("product_category", category)
                    putExtra("product_image", imageUrl)
                    putExtra("product_seller_name", sellerName)
                    putExtra("product_seller_phone", sellerPhone)
                    putExtra("product_description", description)
                    putExtra("product_in_stock", inStock)
                }
                startActivity(editIntent)
            }

            tvName.text = name
            tvPrice.text = "₹$price"
            tvCategory.text = category
            tvSellerName.text = "Seller: $sellerName"
            tvSellerPhone.text = "Phone: $sellerPhone"
            tvDescription.text = description ?: "No description available."
            
            if (inStock) {
                tvStockStatus.text = "In Stock"
                tvStockStatus.setTextColor(getColor(R.color.secondary))
            } else {
                tvStockStatus.text = "Out of Stock"
                tvStockStatus.setTextColor(getColor(R.color.error))
            }

            val imageSource: Any = if (imageUrl?.startsWith("data:image") == true || (imageUrl != null && !imageUrl.startsWith("http"))) {
                try {
                    val base64String = if (imageUrl!!.contains(",")) imageUrl.split(",")[1] else imageUrl
                    android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                } catch (e: Exception) {
                    imageUrl ?: android.R.drawable.ic_menu_gallery
                }
            } else {
                imageUrl ?: android.R.drawable.ic_menu_gallery
            }

            Glide.with(this@DetailActivity)
                .load(imageSource)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(ivProduct)

            btnContact.setOnClickListener {
                val message = "Hello $sellerName, I am interested in your product: $name listed on Halli-Sante."
                val uri = Uri.parse("smsto:$sellerPhone")
                val intent = Intent(Intent.ACTION_SENDTO, uri)
                intent.putExtra("sms_body", message)
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this@DetailActivity, "No messaging app found", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
