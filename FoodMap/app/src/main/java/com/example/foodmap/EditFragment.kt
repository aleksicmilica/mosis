package com.example.foodmap

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Context.*
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.foodmap.data.MyPlace
import com.example.foodmap.data.UserObject
import com.example.foodmap.model.MyPlacesViewModel
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.firebase.firestore.QuerySnapshot
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
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


class EditFragment : Fragment() {
    private val myPlacesViewModel: MyPlacesViewModel by activityViewModels()
    private var CAMERA_REQUEST_CODE: Int = 0
    private var GALLERY_REQUEST_CODE: Int = 0
    private val db = Firebase.firestore
    private var storage = Firebase.storage
    var storageRef = storage.reference
    var userName: String = UserObject.username!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var addButton: Button = requireView().findViewById<Button>(R.id.editFragmentAddButton)

        val editName: EditText = requireView().findViewById<EditText>(R.id.EditFragmentNameET)
        val editDesc: EditText = requireView().findViewById<EditText>(R.id.EditFragmentOpisET)
        val editLongitude: EditText = requireView().findViewById<EditText>(R.id.EditFragmentLonET)
        val editLatitude: EditText = requireView().findViewById<EditText>(R.id.EditFragmentLatET)
        val editAutor: EditText = requireView().findViewById<EditText>(R.id.EditFragmentAutorET)
        val editDate: EditText = requireView().findViewById(R.id.EditFragmentDate)

        val editTip: EditText = requireView().findViewById(R.id.EditFragmentTipET)
        val ratGrade: RatingBar = requireView().findViewById(R.id.ratingBar)


        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy")
        val inputFormat: DateFormat =
            SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH)





        if (myPlacesViewModel.selected != null) {
            addButton.isEnabled = true
            editName.setText(myPlacesViewModel.selected?.name)
            editDesc.setText(myPlacesViewModel.selected?.description)
            editLongitude.setText(myPlacesViewModel.selected?.longitude.toString())
            editLatitude.setText(myPlacesViewModel.selected?.latitude.toString())
            editTip.setText(myPlacesViewModel.selected?.tip)

            val date = inputFormat.parse(myPlacesViewModel.selected?.createdAt.toString())
            val formattedDate: String = dateFormat.format(date)
            editDate.setText(formattedDate)





            editAutor.setText(myPlacesViewModel.selected?.autor)
            var sum: Double = 0.0
            for (g in myPlacesViewModel.selected?.grades!!)
                sum += g.value
            if (myPlacesViewModel.selected?.grades!!.size != 0)
                sum /= myPlacesViewModel.selected?.grades!!.size

            ratGrade.rating = sum.toFloat()


            addButton.setText(R.string.edit_fragment_save_label)
            editAutor.isEnabled = false
            ratGrade.isEnabled = false



        } else {
            addButton.isEnabled = false

            val location = MainActivity.curLocation
            if (location == null) {
                editLongitude.setText("21.8925")
                editLatitude.setText("43.32289")
            }
            else
            {
                editLongitude.setText(location.longitude.toString())
                editLatitude.setText(location.latitude.toString())
            }
            val currentDate = dateFormat.format(calendar.time)
            editDate.setText(currentDate)



            editAutor.setText(userName)

        }





        addButton.setOnClickListener {


            val name: String = editName.text.toString()
            val desc: String = editDesc.text.toString()
            val longitude: String = editLongitude.text.toString()
            val latitude: String = editLatitude.text.toString()
            val autor: String = editAutor.text.toString()
            val date: Date = dateFormat.parse(editDate.text.toString())
            var starsCount: Long = 0L
            var kommCount: Long = 0L

            val grades = HashMap<String, Double>()
            if (ratGrade.rating.toDouble() != 0.0) {
                grades[userName] = ratGrade.rating.toDouble()
                starsCount += 2
            }
            val tip = editTip.text.toString()



            if (myPlacesViewModel.selected != null) {
                myPlacesViewModel.selected?.name = name
                myPlacesViewModel.selected?.description = desc
                myPlacesViewModel.selected?.latitude = latitude
                myPlacesViewModel.selected?.longitude = longitude

                myPlacesViewModel.selected?.createdAt = date


                val documentRef = db.collection("places").document(myPlacesViewModel.selected!!.id)

                val place = hashMapOf(
                    "name" to name,
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "tip" to tip,
                    "date" to date,
                    "descriptiom" to desc,

                    )

                var fragmentContext = requireContext()
                documentRef.update(place as Map<String, Any>)
                    .addOnSuccessListener {
                        Toast.makeText(
                            fragmentContext,
                            "Uspesno promenjene informacije o mestu",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { exception ->

                        Log.e("TAG", "Greška pri ažuriranju dokumenta", exception)
                    }


            } else {

                var Place: MyPlace = MyPlace(
                    name,
                    latitude,
                    longitude,
                    autor,
                    tip,
                    date,
                    desc,
                    "",
                    grades,HashMap() , ""
                )
                myPlacesViewModel.addPlace(
                    Place
                )
                val hash = GeoFireUtils.getGeoHashForLocation(
                    GeoLocation(
                        Place.latitude.toDouble(),
                        Place.longitude.toDouble()
                    )
                )

                val place = hashMapOf(
                    "name" to name,
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "autor" to autor,
                    "tip" to tip,
                    "date" to date,
                    "descriptiom" to desc,
                    "url" to "",
                    "grades" to grades,
                    "geohash" to hash
                )


                var imageView: ImageView =
                    this@EditFragment.requireView().findViewById(R.id.EditFragmentImg)
                var url: String = ""

                var context:Context = requireContext()
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        var result: QuerySnapshot
                        result = withContext(Dispatchers.IO) {
                            db.collection("places")
                                .whereEqualTo("latitude", latitude)
                                .whereEqualTo("longitude", longitude)
                                .get()
                                .await()
                        }
                        if (result.isEmpty) {
                            db.collection("places")
                                .add(place)
                                .addOnSuccessListener { documentReference ->

                                    Place.id = documentReference.id.toString()
                                    Place.url = "places/" + Place.id + ".jpg"
                                    url = "places/" + Place.id + ".jpg"
                                    var imageRef: StorageReference? =
                                        storageRef.child("images/" + url)
                                    var PlaceRef = storageRef.child(url)


                                    imageView.isDrawingCacheEnabled = true
                                    imageView.buildDrawingCache()

                                    if (imageView.drawable is BitmapDrawable) {
                                        val bitmap = (imageView.drawable as BitmapDrawable).bitmap
                                        val baos = ByteArrayOutputStream()
                                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                                        val data = baos.toByteArray()
                                        var uploadTask = PlaceRef.putBytes(data)
                                        uploadTask.addOnFailureListener { e ->
                                            Log.w("TAGA", "Greska", e)
                                        }
                                    }
                                    val documentRef = db.collection("places").document(Place.id)
                                    val placeUrl = hashMapOf(
                                        "url" to "places/" + Place.id + ".jpg"
                                    )

                                    documentRef.update(placeUrl as Map<String, Any>)
                                        .addOnSuccessListener {
                                        }
                                        .addOnFailureListener { exception ->

                                        }

                                }
                                .addOnFailureListener { e ->
                                    Log.w("TAGA", "Error", e)
                                }


                            db.collection("users")
                                .whereEqualTo("username", userName)
                                .get()
                                .addOnSuccessListener { querySnapshot ->
                                    for (documentSnapshot in querySnapshot.documents) {
                                        val documentRef =
                                            db.collection("users").document(documentSnapshot.id)
                                        val addCount: Long =
                                            documentSnapshot.get("addCount") as Long
                                        starsCount += documentSnapshot.get("starsCount") as Long
                                        val noviPodaci = hashMapOf<String, Any>(
                                            "addCount" to (addCount + 3L).toString().toLong(),
                                            "starsCount" to starsCount

                                        )
                                        documentRef.update(noviPodaci)
                                            .addOnSuccessListener {

                                            }
                                            .addOnFailureListener { exception ->
                                                Log.w("TAGA", "Error", exception)
                                            }
                                    }
                                }
                            myPlacesViewModel.selected = null
                            findNavController().popBackStack()

                        } else
                            Toast.makeText(
                                context,
                                "Nije uspelo dodavanje jer na lokaciji postoji objekat",
                                Toast.LENGTH_SHORT
                            ).show()



                    } catch (e: java.lang.Exception) {
                        Log.w("TAGA", "Greska", e)
                    }
                }




            }


        }
        val cancelButton: Button = requireView().findViewById<Button>(R.id.editFragmentCancelButton)
        cancelButton.setOnClickListener {
            myPlacesViewModel.selected = null

            findNavController().popBackStack()
        }

        val cameraButton: Button = requireView().findViewById(R.id.btnCamera)
        cameraButton.setOnClickListener {
            val cameraPermission = android.Manifest.permission.CAMERA
            val hasCameraPermission = ContextCompat.checkSelfPermission(
                requireContext(),
                cameraPermission
            ) == PackageManager.PERMISSION_GRANTED
            CAMERA_REQUEST_CODE = 1
            if (!hasCameraPermission) {
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            } else {
                startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE), CAMERA_REQUEST_CODE)
            }


        }

        val galleryButton: Button = requireView().findViewById(R.id.btnGalerija)
        galleryButton.setOnClickListener {
            val galleryPermission = android.Manifest.permission.READ_EXTERNAL_STORAGE
            val hasGalleryPermission = ContextCompat.checkSelfPermission(
                requireContext(),
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
        val imageView: ImageView = requireView().findViewById<ImageView>(R.id.EditFragmentImg)




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


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_edit, container, false)
    }

    override fun onDestroyView() {
        myPlacesViewModel.selected = null
        super.onDestroyView()

    }

}