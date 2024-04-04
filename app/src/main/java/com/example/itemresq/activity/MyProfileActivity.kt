package com.example.itemresq.activity

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.itemresq.R
import com.example.itemresq.database.FirebaseManager
import com.example.itemresq.databinding.ActivityMyProfileBinding
import com.example.itemresq.util.UiUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale


class MyProfileActivity : AppCompatActivity() {

    lateinit var binding: ActivityMyProfileBinding
    private var auth = FirebaseAuth.getInstance()
    private var profilePictureUri: String = ""
    private var selectedImageUri : Uri? = null

    private val imagePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            if (data != null) {
                selectedImageUri = data.data
                binding.profilePicture.setImageURI(selectedImageUri)
                uploadProfilePicture(selectedImageUri!!)
            }
        }
    }

    private val imageCapturer = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as Bitmap?
            binding.profilePicture.setImageBitmap(imageBitmap)
            selectedImageUri = saveImageToGallery(imageBitmap)
            uploadProfilePicture(selectedImageUri!!)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavBar.menu.findItem(R.id.unchecked_item).isChecked = true
        binding.bottomNavBar.menu.findItem(R.id.unchecked_item).isVisible = false

        binding.bottomNavBar.setOnItemSelectedListener {menuItem->
            when(menuItem.itemId) {
                R.id.bottom_menu_main ->{
                    startActivity(Intent(this, MainActivity::class.java))
                }
                R.id.bottom_menu_map ->{
                    startActivity(Intent(this, MapActivity::class.java))
                }
                R.id.bottom_menu_add_report ->{
                    startActivity(Intent(this, AddReportActivity::class.java))
                }
                R.id.bottom_menu_list_reports ->{
                    startActivity(Intent(this, ListAllReportsActivity::class.java))
                }
            }
            false
        }

        binding.goBackBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.managePictureBtn.setOnClickListener {
            showImagePickerOptions()
        }

        binding.deletePictureBtn.setOnClickListener {
            showDeletePictureDialog()
        }

        binding.logOutBtn.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.deleteAccountBtn.setOnClickListener {
            UiUtil.showDeleteAccountDialog(this)
        }

        auth = FirebaseAuth.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid
        val db = FirebaseFirestore.getInstance()
        val usersCollection = db.collection("users")

        usersCollection.document(userId!!).get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val email = documentSnapshot.getString("email")
                profilePictureUri = documentSnapshot.getString("profilePictureUri") ?: ""
                val addedReportsCount = documentSnapshot.getLong("addedReportsCount")
                val registrationTimeConverted = documentSnapshot.getTimestamp("registrationTime")?.toDate()
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                binding.emailAddress.text = email
                binding.addedReportsCount.text = addedReportsCount.toString()
                binding.usingAppFrom.text = sdf.format(registrationTimeConverted!!)

                if (profilePictureUri == "") {
                    binding.managePictureBtn.text = "Dodaj zdjęcie profilowe"
                    binding.profilePicture.setImageResource(R.drawable.icon_profile)
                } else {
                    binding.managePictureBtn.text = "Zmień zdjęcie profilowe"
                    binding.deletePictureBtn.visibility = View.VISIBLE
                    Picasso.get().load(profilePictureUri)
                        .placeholder(R.drawable.icon_time)
                        .error(R.drawable.icon_error)
                        .into(binding.profilePicture)
                }
            } else {
                UiUtil.showToast(this, "Wystąpił błąd. Użytkownik nie istnieje.")
                auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }.addOnFailureListener { exception ->
            UiUtil.showToast(this, "Wystąpił błąd. Spróbuj ponownie później.")
        }

    }

    private fun saveImageToGallery(bitmap: Bitmap?): Uri? {
        bitmap?.let {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, "image_${System.currentTimeMillis()}.jpg")
            val imageUri = Uri.fromFile(image)
            try {
                FileOutputStream(image).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                    fos.flush()
                }

                MediaScannerConnection.scanFile(
                    applicationContext,
                    arrayOf(image.absolutePath),
                    arrayOf("image/jpeg"),
                    null
                )

                return imageUri
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return null
    }

    private fun showImagePickerOptions() {
        val options = arrayOf("Wybierz z galerii", "Zrób zdjęcie")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Wybierz opcję")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkPermissionAndOpenGallery()
                    1 -> checkPermissionAndOpenCamera()

                }
            }
            .setNegativeButton("Anuluj") { dialog, _ ->
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.show()
    }

    private fun checkPermissionAndOpenGallery() {
        var readExternalImage : String = ""
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            readExternalImage = Manifest.permission.READ_MEDIA_IMAGES
        } else {
            readExternalImage = Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if(ContextCompat.checkSelfPermission(this, readExternalImage) == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(readExternalImage), 100)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePicker.launch(intent)
    }

    private fun checkPermissionAndOpenCamera() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), 102)
            }
        }
        openCamera()
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        imageCapturer.launch(intent)
    }

    private fun uploadProfilePicture(selectedImageUri : Uri) {
        selectedImageUri?.apply {
            val imageRef = FirebaseStorage.getInstance()
                .reference
                .child("images/profile_pictures/"+ this.lastPathSegment)
            imageRef.putFile(this)
                .addOnSuccessListener { uploadTask ->
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        currentUser?.uid?.let { userId ->
                            val db = FirebaseFirestore.getInstance()
                            val userRef = db.collection("users").document(userId)
                            userRef.update("profilePictureUri", downloadUri.toString())
                                .addOnSuccessListener {
                                    profilePictureUri = downloadUri.toString()
                                    Picasso.get().load(downloadUri)
                                        .placeholder(R.drawable.icon_time)
                                        .error(R.drawable.icon_error)
                                        .into(binding.profilePicture)
                                }
                        }
                    }
                }
            binding.deletePictureBtn.visibility = View.VISIBLE
        }
    }

    private fun showDeletePictureDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Usuń zdjęcie profilowe")
            .setMessage("Czy jesteś pewny, że chcesz usunąć obecne zdjęcie profilowe?")
            .setPositiveButton("Usuń") { dialog, which ->
                dialog.dismiss()
                deleteProfilePicture()
            }
            .setNegativeButton("Anuluj") { dialog, which ->
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.show()
    }

    private fun deleteProfilePicture() {
        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(profilePictureUri)

        storageRef.delete().addOnSuccessListener {
            binding.profilePicture.setImageResource(R.drawable.icon_profile)
            binding.deletePictureBtn.visibility = View.GONE
            binding.managePictureBtn.text = "Dodaj zdjęcie profilowe"

            val currentUser = FirebaseAuth.getInstance().currentUser
            val userId = currentUser?.uid
            val db = FirebaseFirestore.getInstance()
            val usersCollection = db.collection("users")
            usersCollection.document(userId!!).update("profilePictureUri", "")
            profilePictureUri = ""
        }.addOnFailureListener {
            UiUtil.showToast(this, "Nie udało się usunąć zdjęcia profilowego.")
        }
    }
}