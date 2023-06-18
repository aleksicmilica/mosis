package com.example.foodmap

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.foodmap.data.User
import com.example.foodmap.data.UserObject
import com.example.foodmap.databinding.ActivityRegisterBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class RegisterActivity : AppCompatActivity() {


    private lateinit var binding: ActivityRegisterBinding
    private var CAMERA_REQUEST_CODE = 0
    private var GALLERY_REQUEST_CODE = 0
    private var db = Firebase.firestore
    private var storage = Firebase.storage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var storageRef = storage.reference

        var editName: EditText = findViewById(R.id.regetname)
        var editSurname: EditText = findViewById(R.id.regetprezime)
        var editUserName: EditText = findViewById(R.id.regetusername)
        var editPassword: EditText = findViewById(R.id.regetpassword)
        var editPhone: EditText = findViewById(R.id.regetphone)

        var closeButton: Button = findViewById(R.id.regbtnclose)
        var okButton: Button = findViewById(R.id.regbtnReg)

        var cameraButton: Button = findViewById(R.id.regbtnKamera)
        var galerijaButton: Button = findViewById(R.id.regbtnGalerija)


        closeButton.setOnClickListener {
            var intent = Intent(this@RegisterActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()

        }
        cameraButton.setOnClickListener {
            val cameraPermission = android.Manifest.permission.CAMERA
            val hasCameraPermission = ContextCompat.checkSelfPermission(
                this,
                cameraPermission
            ) == PackageManager.PERMISSION_GRANTED
            CAMERA_REQUEST_CODE = 1
            if (!hasCameraPermission) {
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            } else {
                startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE), CAMERA_REQUEST_CODE)
            }

        }
        galerijaButton.setOnClickListener {
            val galleryPermission = android.Manifest.permission.READ_EXTERNAL_STORAGE
            val hasGalleryPermission = ContextCompat.checkSelfPermission(
                this,
                galleryPermission
            ) == PackageManager.PERMISSION_GRANTED
            GALLERY_REQUEST_CODE = 1
            if (!hasGalleryPermission) {
                requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                startActivityForResult(
                    Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    ), GALLERY_REQUEST_CODE
                )
            }
        }

        okButton.setOnClickListener {

            var userName: String = editUserName.text.toString()
            var password: String = editPassword.text.toString()
            var firstName: String = editName.text.toString()
            var lastName: String = editSurname.text.toString()
            var phone: String = editPhone.text.toString()
            if (userName.equals(""))
                Toast.makeText(this, "Morate uneti korisnicko ime", Toast.LENGTH_SHORT).show()
            else if (password.equals(""))
                Toast.makeText(this, "Morate uneti sifru", Toast.LENGTH_SHORT).show()
            else if (firstName.equals(""))
                Toast.makeText(this, "Morate uneti ime", Toast.LENGTH_SHORT).show()
            else if (lastName.equals(""))
                Toast.makeText(this, "Morate uneti prezime", Toast.LENGTH_SHORT).show()
            else if (phone.equals(""))
                Toast.makeText(this, "Morate uneti broj telefona", Toast.LENGTH_SHORT).show()
            else {


                var user = hashMapOf(
                    "username" to userName,
                    "password" to password,
                    "first name" to firstName,
                    "last name" to lastName,
                    "phone" to phone,
                    "addCount" to 0,
                    "starsCount" to 0,
                    "commentsCount" to 0,
                    "url" to "users/" + editUserName.text.toString() + ".jpg"
                )

                var url = "users/" + editUserName.text.toString() + ".jpg"
                var imageRef: StorageReference? = storageRef.child("images/" + url)
                var userRef = storageRef.child(url)





                CoroutineScope(Dispatchers.Main).launch {
                    try {


                        val result = withContext(Dispatchers.IO) {
                            db.collection("users")
                                .whereEqualTo("username", editUserName.text.toString())
                                .get()
                                .await()
                        }


                        if (!result.isEmpty) {

                            Toast.makeText(
                                this@RegisterActivity,
                                "Postoji osoba sa tim korisnickim imenom",
                                Toast.LENGTH_SHORT
                            ).show()

                        } else {
                            var imageView: ImageView = binding.imgUser
                            imageView.isDrawingCacheEnabled = true
                            imageView.buildDrawingCache()
                            val bitmap = (imageView.drawable as BitmapDrawable).bitmap
                            val baos = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                            val data = baos.toByteArray()
                            userRef.putBytes(data).await()


                            db.collection("users")
                                .add(user)
                                .addOnSuccessListener { documentReference ->

                                    var id = documentReference.id.toString()
                                    var user: User = User(
                                        editUserName.text.toString(),
                                        editPassword.text.toString(),
                                        editName.text.toString(),
                                        editSurname.text.toString(),
                                        editPhone.text.toString(),
                                        "users/" + editUserName.text.toString() + ".jpg",
                                        0.0,
                                        0.0,
                                        0.0,
                                        id
                                    )
                                    var intent: Intent =
                                        Intent(this@RegisterActivity, MainActivity::class.java)
                                    intent.putExtra("user", user)
                                    UserObject.apply {
                                        this.username = user.username
                                        this.password = user.password
                                        this.firstName = user.firstName
                                        this.lastName = user.lastName
                                        this.addCount = user.addCount
                                        this.commentsCount = user.commentsCount
                                        this.startCount = user.startCount


                                    }
                                    startActivity(intent)
                                    finish()

                                }
                                .addOnFailureListener { e ->
                                    Log.w("TAGA", "Error", e)
                                }

                        }
                    } catch (e: java.lang.Exception) {
                        Log.w("TAGA", "Greska", e)
                    }


                }
            }

        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                if (CAMERA_REQUEST_CODE == 1) {

                    startActivityForResult(
                        Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                        CAMERA_REQUEST_CODE
                    )
                } else if (GALLERY_REQUEST_CODE == 1) {
                    startActivityForResult(
                        Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        ), GALLERY_REQUEST_CODE
                    )
                }
            }
        }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val imageView: ImageView = findViewById<ImageView>(R.id.imgUser)






        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val image: Bitmap? = data.extras?.get("data") as Bitmap
            imageView.setImageBitmap(image)
            CAMERA_REQUEST_CODE = 0
        } else if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val selectedImage: Uri? = data.data
            imageView.setImageURI(selectedImage)
            GALLERY_REQUEST_CODE = 0
        }
    }


}