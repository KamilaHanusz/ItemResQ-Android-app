package com.example.itemresq.activity

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.icu.util.Calendar
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.marginBottom
import com.example.itemresq.R
import com.example.itemresq.databinding.ActivityAddReportBinding
import com.example.itemresq.model.ReportModel
import com.example.itemresq.util.UiUtil
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

class AddReportActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var binding : ActivityAddReportBinding
    private val calendar = Calendar.getInstance()
    private var selectedCategory : String = ""
    private var occurrenceDate : Timestamp = Timestamp.now()
    private var latitudeToFirebase : Double = 0.0
    private var longitudeToFirebase : Double = 0.0
    private lateinit var mMap : GoogleMap
    private lateinit var fusedLocationProviderClient : FusedLocationProviderClient
    private val selectedImageUris = mutableListOf<Uri?>()
    private val imageViewsList = mutableListOf<ImageView>()
    private val closeButtonsList = mutableListOf<ImageView>()
    private val imageUriMap = mutableMapOf<ImageView, Uri?>()

    private val imagePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                if (data != null) {
                    val selectedImageUri = data.data
                    selectedImageUris.add(selectedImageUri)
                    addChosenImageToLayout(selectedImageUri!!)

                }
            }
        }

    private val imageCapturer = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as Bitmap?

            val capturedImageUri = saveImageToGallery(imageBitmap)
            selectedImageUris.add(capturedImageUri)
            addCapturedImageToLayout(imageBitmap, capturedImageUri!!)

        }
    }

    private val setReportLocationActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val address = data?.getStringExtra("ADDRESS")
            val latitude = data?.getDoubleExtra("LATITUDE", 0.0)
            val longitude = data?.getDoubleExtra("LONGITUDE", 0.0)

            val latLngToSet = LatLng(latitude!!,longitude!!)
            setMapAfterChoosingLocation(latLngToSet)
            latitudeToFirebase = latitude
            longitudeToFirebase = longitude

            binding.reportLocalizationAddress.text = address
            binding.reportLocalizationAddress.visibility =  View.VISIBLE
            binding.settingLocationInformation.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        imageViewsList.add(binding.imageView1)
        imageViewsList.add(binding.imageView2)
        imageViewsList.add(binding.imageView3)
        imageViewsList.add(binding.imageView4)
        imageViewsList.add(binding.imageView5)

        closeButtonsList.add(binding.closeButton1)
        closeButtonsList.add(binding.closeButton2)
        closeButtonsList.add(binding.closeButton3)
        closeButtonsList.add(binding.closeButton4)
        closeButtonsList.add(binding.closeButton5)

        binding.addPictureIcon0.visibility = View.VISIBLE
        binding.layoutAfterPicAdded.visibility = View.GONE

        imageViewsList.forEach { imageView ->
            imageView.visibility = View.GONE
        }

        closeButtonsList.forEach { closeButton ->
            closeButton.visibility = View.GONE
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        closeButtonsList.forEachIndexed { index, closeButton ->
            closeButton.setOnClickListener {
                val imageView = imageViewsList[index]
                val imageUri = imageUriMap[imageView]

                imageView.visibility = View.GONE
                closeButton.visibility = View.GONE

                imageUri?.let {
                    selectedImageUris.remove(it)
                    imageUriMap.remove(imageView)
                }

                binding.addPictureIcon0.visibility = if (selectedImageUris.isEmpty()) View.VISIBLE else View.GONE
                binding.layoutAfterPicAdded.visibility = if (selectedImageUris.isNotEmpty()) View.VISIBLE else View.GONE
                binding.addPictureIcon.visibility = if (selectedImageUris.size >= 5) View.GONE else View.VISIBLE
            }
        }

        val items = listOf("Portfel", "Dokument", "Klucze", "Elektronika", "Biżuteria", "Odzież", "Inne")
        val adapter = ArrayAdapter(this, R.layout.list_item, items)
        binding.categoryField.setAdapter(adapter)

        // Updatowanie błedu związanego z niewybraniem kategorii
        binding.categoryField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s.toString().isNotEmpty()) {
                    binding.categoryField.error = null // Clear the error message
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        binding.categoryField.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = adapter.getItem(position)
            if (selectedItem != null) {
                selectedCategory = selectedItem
            }
        }

        binding.datePickerBtn.setOnClickListener{
            showDatePicker()
        }

        binding.reportType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.found -> {
                    binding.rewardCheckbox.visibility = View.GONE
                    binding.rewardInput.setText("")
                    binding.rewardInputLayout.visibility = View.GONE
                    binding.locationInfoNote.visibility = View.GONE
                }
                R.id.lost -> {
                    binding.rewardCheckbox.isChecked = false
                    binding.rewardCheckbox.visibility = View.VISIBLE
                    binding.locationInfoNote.visibility = View.VISIBLE
                }
            }
        }

        binding.rewardCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.rewardInputLayout.visibility = if (isChecked) {
                View.VISIBLE
            } else {
                binding.rewardInput.setText("")
                View.GONE
            }
        }

        binding.addPictureIcon0.setOnClickListener {
            showImagePickerOptions()
        }

        binding.addPictureIcon.setOnClickListener {
            showImagePickerOptions()
        }

        binding.submitButton.setOnClickListener {
            postReport()
        }

        binding.setLocation.setOnClickListener {
            val intent = Intent(this, SetReportLocationActivity::class.java)
            setReportLocationActivityResultLauncher.launch(intent)
        }

        binding.cancelButton.setOnClickListener {
            finish()
        }

        binding.goBackBtn.setOnClickListener {
            finish()
        }

        if (selectedImageUris.size == 5) {
            binding.addPictureIcon.visibility = View.GONE
        }
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

    private fun addChosenImageToLayout(imageUri : Uri) {
        val nextAvailableImageView = imageViewsList.find { it.visibility == View.GONE }

        nextAvailableImageView?.apply {
            setImageURI(imageUri)
            visibility = View.VISIBLE

            val index = imageViewsList.indexOf(this)
            val closeButton = closeButtonsList[index]
            closeButton.visibility = View.VISIBLE

            imageUriMap[this] = imageUri
        }

        binding.addPictureIcon0.visibility = View.GONE
        binding.layoutAfterPicAdded.visibility = View.VISIBLE
        binding.addPictureIcon.visibility = if (selectedImageUris.size >= 5) View.GONE else View.VISIBLE

    }

    private fun addCapturedImageToLayout(imageBitmap: Bitmap?, imageUri : Uri) {
        val nextAvailableImageView = imageViewsList.find { it.visibility == View.GONE }

        nextAvailableImageView?.apply {
            setImageBitmap(imageBitmap)
            visibility = View.VISIBLE

            val index = imageViewsList.indexOf(this)
            val closeButton = closeButtonsList[index]
            closeButton.visibility = View.VISIBLE

            imageUriMap[this] = imageUri
        }

        binding.addPictureIcon0.visibility = View.GONE
        binding.layoutAfterPicAdded.visibility = View.VISIBLE
        binding.addPictureIcon.visibility = if (selectedImageUris.size >= 5) View.GONE else View.VISIBLE
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(this, {_, year: Int, monthOfYear: Int, dayOfMonth: Int ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(year, monthOfYear, dayOfMonth)

            occurrenceDate = Timestamp(selectedDate.time)

            binding.settingDateInformation.visibility = View.GONE

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val formattedDate = dateFormat.format(selectedDate.time)
            binding.datePickerBtn.text = formattedDate
        },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

        val locale = Locale("pl", "PL")
        datePickerDialog.context.resources.configuration.setLocale(locale)

        datePickerDialog.show()
    }

    private fun getSelectedType(): String {
        val selectedType = when (binding.reportType.checkedRadioButtonId) {
            R.id.lost -> "Zagubione"
            R.id.found -> "Znalezione"
            else -> { "" }
        }
        return selectedType
    }

    private fun postReport() {
        if (binding.titleInput.text.toString().isEmpty()) {
            binding.titleInput.setError("Tytuł jest wymagany")
            return
        }
        if (binding.categoryField.text.toString().isEmpty()) {
            binding.categoryField.setError("Wybierz kategorię")
            return
        }
        if (binding.phoneNumberInput.text.toString().isEmpty()) {
            binding.phoneNumberInput.setError("Numer telefonu jest wymagany")
            return
        } else if (binding.phoneNumberInput.text.toString().length < 9) {
            binding.phoneNumberInput.setError("Numer telefonu musi mieć co najmniej 9 cyfr")
            return
        }
        if (binding.rewardInputLayout.visibility == View.VISIBLE && binding.rewardInput.text.toString().isEmpty()) {
            binding.rewardInput.setError("Jeśli chcesz zaoferować nagrodę, musisz uzupełnić to pole")
            return
        }
        setInProgress(true)

        val imageUris = mutableListOf<Uri>()

        if (selectedImageUris.isEmpty()) {
            postToFirestore(emptyList(), getSelectedType())
        } else {
            selectedImageUris.forEach { imageUri ->
                imageUri?.apply {
                    val imageRef = FirebaseStorage.getInstance()
                        .reference
                        .child("images/reports/" + this.lastPathSegment)
                    imageRef.putFile(this)
                        .addOnSuccessListener { taskSnapshot ->
                            taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                                imageUris.add(downloadUri)
                                if (imageUris.size == selectedImageUris.size) {
                                    postToFirestore(
                                        imageUris.map { it.toString() },
                                        getSelectedType()
                                    )
                                }
                            }
                        }
                }
            }
        }
    }

    private fun postToFirestore(urls: List<String>, type : String) {
        val reportModel = ReportModel(
            reportId = FirebaseAuth.getInstance().currentUser?.uid!! + "_"+ Timestamp.now().toString(),
            type = type,
            title = binding.titleInput.text.toString(),
            description = binding.descriptionInput.text.toString(),
            category = selectedCategory,
            occurrenceDate = occurrenceDate,
            reward = binding.rewardInput.text.toString(),
            phoneNumber = binding.phoneNumberInput.text.toString(),
            email = binding.emailInput.text.toString(),
            imageUrls = urls,
            latitude = latitudeToFirebase,
            longitude = longitudeToFirebase,
            reporterId = FirebaseAuth.getInstance().currentUser?.uid!!,
            createdTime = Timestamp.now(),
        )
        Firebase.firestore.collection("reports")
            .document(reportModel.reportId)
            .set(reportModel)
            .addOnSuccessListener {
                updateAddedReportsCount()
                setInProgress(false)
                UiUtil.showToast(applicationContext, "Zgłoszenie dodane pomyślnie")
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }.addOnFailureListener {
                setInProgress(false)
                UiUtil.showToast(applicationContext, "Wystąpił błąd podczas dodawania zgłoszenia")
            }
    }

    private fun setInProgress(inProgress : Boolean) {
        if(inProgress) {
            binding.progressBar.visibility = View.VISIBLE
            binding.submitButton.visibility = View.GONE
        } else {
            binding.progressBar.visibility = View.GONE
            binding.submitButton.visibility = View.VISIBLE
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.setAllGesturesEnabled(false)
        mMap.uiSettings.isMyLocationButtonEnabled = false
        mMap.isMyLocationEnabled
        setUpMap()
    }

    private fun setUpMap() {
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 101)
            return
        }

        fusedLocationProviderClient.lastLocation.addOnSuccessListener(this) { location ->
            if(location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                latitudeToFirebase = location.latitude
                longitudeToFirebase = location.longitude
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
            }
        }
    }

    private fun setMapAfterChoosingLocation(position : LatLng) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 12f))
        mMap.addMarker(MarkerOptions().position(position))
    }

    private fun updateAddedReportsCount() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid
        val db = FirebaseFirestore.getInstance()
        val usersCollection = db.collection("users")

        userId?.let { uid ->
            val userRef = usersCollection.document(uid)

            userRef.get().addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val currentReportsCount = documentSnapshot.getLong("addedReportsCount") ?: 0
                    val updatedReportsCount = currentReportsCount + 1
                    userRef.update("addedReportsCount", updatedReportsCount)
                }
            }
        }
    }

    override fun onMarkerClick(p0: Marker) = false
}