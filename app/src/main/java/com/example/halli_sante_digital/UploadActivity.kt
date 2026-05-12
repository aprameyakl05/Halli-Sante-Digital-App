package com.example.halli_sante_digital

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.halli_sante_digital.databinding.ActivityUploadBinding
import com.example.halli_sante_digital.model.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.util.*

class UploadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadBinding
    private var imageUri: Uri? = null
    private var existingImageUrl: String? = null
    private var isEditMode = false
    private var productId: String? = null
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val categories = arrayOf("Dairy", "Pottery", "Arts", "Toys")

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            imageUri = it
            binding.ivProduct.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCategoryDropdown()
        checkEditMode()

        binding.btnSelectImage.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        binding.btnUpload.setOnClickListener {
            validateAndUpload()
        }
    }

    private fun setupCategoryDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.actvCategory.setAdapter(adapter)
    }

    private fun checkEditMode() {
        isEditMode = intent.getBooleanExtra("edit_mode", false)
        if (isEditMode) {
            productId = intent.getStringExtra("product_id")
            existingImageUrl = intent.getStringExtra("product_image")
            
            binding.apply {
                etProductName.setText(intent.getStringExtra("product_name"))
                etPrice.setText(intent.getStringExtra("product_price"))
                actvCategory.setText(intent.getStringExtra("product_category"), false)
                etDescription.setText(intent.getStringExtra("product_description"))
                etSellerName.setText(intent.getStringExtra("product_seller_name"))
                etSellerPhone.setText(intent.getStringExtra("product_seller_phone"))
                
                if (intent.getBooleanExtra("product_in_stock", true)) {
                    rbInStock.isChecked = true
                } else {
                    rbOutStock.isChecked = true
                }

                btnUpload.text = "Update Product"
                
                // Load existing image if possible
                if (existingImageUrl != null) {
                    val imageSource: Any = if (existingImageUrl!!.startsWith("data:image") || !existingImageUrl!!.startsWith("http")) {
                        try {
                            val base64String = if (existingImageUrl!!.contains(",")) existingImageUrl!!.split(",")[1] else existingImageUrl!!
                            android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                        } catch (e: Exception) {
                            android.R.drawable.ic_menu_gallery
                        }
                    } else {
                        existingImageUrl!!
                    }
                    com.bumptech.glide.Glide.with(this@UploadActivity)
                        .load(imageSource)
                        .into(ivProduct)
                }
            }
        }
    }

    private fun validateAndUpload() {
        val name = binding.etProductName.text.toString().trim()
        val price = binding.etPrice.text.toString().trim()
        val category = binding.actvCategory.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val sellerName = binding.etSellerName.text.toString().trim()
        val sellerPhone = binding.etSellerPhone.text.toString().trim()
        val inStock = binding.rbInStock.isChecked

        if (name.isEmpty() || price.isEmpty() || category.isEmpty() || description.isEmpty() || sellerName.isEmpty() || sellerPhone.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (imageUri == null && !isEditMode) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }

        if (imageUri != null) {
            uploadImage(name, price, category, description, inStock, sellerName, sellerPhone)
        } else {
            // Edit mode with existing image
            saveProductToFirestore(name, price, category, existingImageUrl ?: "", description, inStock, sellerName, sellerPhone)
        }
    }

    private fun uploadImage(name: String, price: String, category: String, description: String, inStock: Boolean, sellerName: String, sellerPhone: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnUpload.isEnabled = false

        try {
            val inputStream = contentResolver.openInputStream(imageUri!!)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            
            val maxDimension = 600
            val ratio = Math.min(maxDimension.toDouble() / originalBitmap.width, maxDimension.toDouble() / originalBitmap.height)
            val width = (originalBitmap.width * ratio).toInt()
            val height = (originalBitmap.height * ratio).toInt()
            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true)

            val baos = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos)
            val byteArray = baos.toByteArray()
            val base64Image = android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)

            saveProductToFirestore(name, price, category, base64Image, description, inStock, sellerName, sellerPhone)
        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            binding.btnUpload.isEnabled = true
            Toast.makeText(this, "Image processing error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProductToFirestore(name: String, price: String, category: String, imageUrl: String, description: String, inStock: Boolean, sellerName: String, sellerPhone: String) {
        val id = productId ?: firestore.collection("products").document().id
        val sellerId = auth.currentUser?.uid ?: ""
        val product = Product(id, name, price, category, imageUrl, sellerName, sellerPhone, sellerId, description, inStock, System.currentTimeMillis())

        firestore.collection("products").document(id)
            .set(product)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                val msg = if (isEditMode) "Product updated successfully!" else "Product uploaded successfully!"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                binding.btnUpload.isEnabled = true
                Toast.makeText(this, "Error saving product: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
