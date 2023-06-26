package com.example.foodmap

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.example.foodmap.data.ILocationClient
import com.example.foodmap.data.MyPlace
import com.example.foodmap.databinding.FragmentMapBinding
import com.example.foodmap.model.MyPlacesViewModel
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.*

class MapFragment : Fragment(), ILocationClient {
    lateinit var map: MapView

    private val myPlacesViewModel: MyPlacesViewModel by activityViewModels()


    private var searchType = "name"
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private var db = Firebase.firestore

    private lateinit var myMarker: Marker
    var lastCheckedRadioButton: RadioButton? = null

    override fun onDestroyView() {
        MainActivity.iLocationClient = null
        super.onDestroyView()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MainActivity.iLocationClient = this

        var ctx: Context? = getActivity()?.getApplicationContext()

        var btn2: Button = binding.button3

        val radioGroup: RadioGroup = view.findViewById(R.id.rgMap)

        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.rbAutoMap -> {
                    searchType = "autor"


                }
                R.id.rbTipMap -> {
                    searchType = "tip"

                }
                R.id.rbRadisuMap -> {
                    searchType = "radius"

                }
                R.id.rbOcenaMap -> {

                    searchType = "ocena"
                }
                R.id.rbDatum -> {
                    searchType = "datum"
                    showDatePicker()
                }
            }


        }


        for (i in 0 until radioGroup.childCount) {
            val radioButton: RadioButton = radioGroup.getChildAt(i) as RadioButton
            radioButton.setOnClickListener {
                if (lastCheckedRadioButton == radioButton) {
                    radioGroup.clearCheck()
                    lastCheckedRadioButton = null
                } else {
                    lastCheckedRadioButton = radioButton
                    radioButton.isChecked = true
                }
                binding.swMapa.setQuery("",false)
                binding.swMapa.clearFocus()
                var datod = binding.editTextDate
                datod.setText("")
                var datdo = binding.editTextDate2
                datdo.setText("")
            }
        }

        btn2.setOnClickListener {
            radioGroup.clearCheck()
            binding.swMapa.setQuery("",false)
            binding.swMapa.clearFocus()
            var datod = binding.editTextDate
            datod.setText("")
            var datdo = binding.editTextDate2
            datdo.setText("")
            resetMap()
        }


        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        map = requireView().findViewById<MapView>(R.id.map)

        map.setMultiTouchControls(true)
        if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireActivity(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            Log.d("TAGA",myPlacesViewModel.myPlacesList.size.toString())
            setupMap(myPlacesViewModel.myPlacesList)
        }


    }

    fun createList(result: QuerySnapshot): kotlin.collections.ArrayList<MyPlace> {

        var list: kotlin.collections.ArrayList<MyPlace> = ArrayList()
        for (document in result) {
            var data = document.data
            var grades = HashMap<String, Double>()
            if (data["grades"] != null) {
                for (g in data["grades"] as HashMap<String, Double>)
                    grades[g.key] = g.value
            }
            var comments = HashMap<String, String>()
            if (data["comments"] != null) {
                for (c in data["comments"] as HashMap<String, String>)
                    comments[c.key] = c.value
            }
            var date: Date? = null
            if (data["date"] != null) {

                val timestamp: com.google.firebase.Timestamp? =
                    document.getTimestamp("date")
                date = timestamp?.toDate()

            }

            list.add(
                MyPlace(
                    data["name"].toString(),
                    data["latitude"].toString(),
                    data["longitude"].toString(),
                    data["autor"].toString(),
                    data["tip"].toString(),
                    date,
                    data["url"].toString(),
                    //data["description"].toString(),
                    grades,
                    comments, document.id

                )
            )

        }
        return list
    }

    fun resetMap() {
        myPlacesViewModel.myPlacesList.clear()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    db.collection("places")

                        .get()
                        .await()
                }

                myPlacesViewModel.myPlacesList.addAll(createList(result))

                setupMap(myPlacesViewModel.myPlacesList)
            } catch (e: java.lang.Exception) {
                Log.w("TAGA", "GReska", e)
            }
        }
    }

    fun removeAllMarkers(map: MapView) {
        val markers = map.overlays
            .filterIsInstance<Marker>()
            .toMutableList()

        markers.forEach { map.overlays.remove(it) }
        map.invalidate()
    }

    private fun setupMap(list: ArrayList<MyPlace>) {
        removeAllMarkers(map)
        val location = MainActivity.curLocation
        myMarker = Marker(map)


        val drawable =
            ResourcesCompat.getDrawable(resources, org.osmdroid.library.R.drawable.person, null)

        myMarker.apply {

            if (location != null)
                this.position=GeoPoint(location.latitude, location.longitude)
            else
                this.position = GeoPoint(0.0, 0.0)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            setOnMarkerClickListener { _, _ ->
                false
            }
            drawable?.toBitmap()?.let { bitmap ->
                icon = BitmapDrawable(
                    resources,
                    Bitmap.createScaledBitmap(
                        bitmap,
                        ((50.0f * resources.displayMetrics.density).toInt()),
                        ((50.0f * resources.displayMetrics.density).toInt()),
                        true
                    )
                )
            }
        }




        var startPoint: GeoPoint = if (location == null)
            GeoPoint(43.32289, 21.8925)
        else
            GeoPoint(location.latitude, location.longitude)
        map.controller.setZoom(15.0)
        if (myPlacesViewModel.selected != null) {
            startPoint = GeoPoint(
                myPlacesViewModel.selected!!.latitude.toDouble(),
                myPlacesViewModel.selected!!.longitude.toDouble()
            )
        }


        map.controller.animateTo(startPoint)

        list.forEach { place ->
            val marker = Marker(map)
            marker.position = GeoPoint(place.latitude.toDouble(), place.longitude.toDouble())
            marker.title = place.name
            map.overlays.add(marker)
        }
        map.overlays.add(myMarker)


    }

    private fun showDatePicker() {

        var datePicker1: EditText = binding.editTextDate

        var datePicker2: EditText = binding.editTextDate2

        var btn1: Button = binding.btnOk




        btn1.setOnClickListener {
            searchType = "datum"
            val format = SimpleDateFormat("ddMMyyyy", Locale.getDefault())
            var timestamp1: Number = 0
            var timestamp2: Number = 0
            if (datePicker1.text.isNotEmpty()) {
                try {

                    val dateString1 = format.parse(datePicker1.text.toString())

                    timestamp1 = dateString1.time / 1000


                } catch (e: java.lang.Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Datum mora biti u formatu ddMMyyyy",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            if (datePicker2.text.isNotEmpty()) {
                try {

                    val dateString2 = format.parse(datePicker2.text.toString())
                    timestamp2 = dateString2.time / 1000
                } catch (e: java.lang.Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Datum mora biti u formatu ddMMYYYY",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            var query: String = timestamp1.toString() + "," + timestamp2.toString()
            filterMap("datum", query, 0)


        }
    }

    private fun filterMap(field: String, value: String, type: Int) {
        if (value != "") {
            myPlacesViewModel.myPlacesList.clear()

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    var result: QuerySnapshot
                    if (field == "radius") {
                        val location = MainActivity.curLocation
                        var center: GeoLocation = if (location == null)
                            GeoLocation(43.32289, 21.8925)
                        else
                            GeoLocation(location.latitude, location.longitude)

                        val radius = value.toDouble()

                        val bounds = GeoFireUtils.getGeoHashQueryBounds(center, radius)
                        val tasks: MutableList<Task<QuerySnapshot>> = ArrayList()
                        for (b in bounds) {
                            val q = db.collection("places")
                                .orderBy("geohash")
                                .startAt(b.startHash)
                                .endAt(b.endHash)
                            tasks.add(q.get())
                        }
                        Tasks.whenAllComplete(tasks)
                            .addOnCompleteListener {
                                val matchingDocuments: MutableList<DocumentSnapshot> = ArrayList()
                                for (task in tasks) {
                                    val snap = task.result
                                    for (doc in snap!!.documents) {
                                        val lat = doc.getString("latitude")!!
                                        val lng = doc.getString("longitude")!!
                                        val docLocation =
                                            GeoLocation(lat.toDouble(), lng.toDouble())
                                        val distance =
                                            GeoFireUtils.getDistanceBetween(docLocation, center)
                                        if (distance <= radius)
                                            matchingDocuments.add(doc)
                                    }

                                    for (document in matchingDocuments) {
                                        var data = document.getData()
                                        var grades = HashMap<String, Double>()
                                        if (data?.get("grades") != null) {
                                            for (g in data["grades"] as HashMap<String, Double>)
                                                grades[g.key] = g.value
                                        }
                                        var comments = HashMap<String, String>()
                                        if (data?.get("comments") != null) {


                                            for (c in data["comments"] as HashMap<String, String>)
                                                comments[c.key] = c.value
                                        }
                                        var date: Date? = null
                                        if (data?.get("date") != null) {

                                            val timestamp: com.google.firebase.Timestamp? =
                                                document.getTimestamp("date")
                                            date = timestamp?.toDate()

                                        }
                                        var url: String =
                                            "places/" + document.reference.id + ".jpg"
                                        Log.d("TAGA",url)
                                        myPlacesViewModel
                                            .addPlace(
                                                MyPlace(
                                                    data?.get("name").toString(),
                                                    data?.get("latitude").toString(),
                                                    data?.get("longitude").toString(),
                                                    data?.get("autor").toString(),
                                                    data?.get("tip").toString(),
                                                    date,
                                                    //data?.get("description").toString(),
                                                    url,
                                                    grades,
                                                    comments,
                                                    document.reference.id
                                                )
                                            )

                                    }

                                    setupMap(myPlacesViewModel.myPlacesList)
                                }
                            }

                    } else
                        if (field.equals("ocena")) {
                            result = withContext(Dispatchers.IO) {
                                db.collection("places")

                                    .get()
                                    .await()

                            }
                            for (document in result) {
                                var data = document.data
                                var grades = java.util.HashMap<String, Double>()
                                var sum: Double = 0.0
                                if (data["grades"] != null) {
                                    for (g in data["grades"] as java.util.HashMap<String, Double>) {
                                        grades[g.key] = g.value
                                        sum += g.value
                                    }
                                    sum /= grades.size

                                }
                                var comments = kotlin.collections.HashMap<String, String>()
                                if (data["comments"] != null) {
                                    for (c in data["comments"] as kotlin.collections.HashMap<String, String>)
                                        comments[c.key] = c.value
                                }
                                var date: Date? = null
                                if (data["date"] != null) {

                                    val timestamp: com.google.firebase.Timestamp? =
                                        document.getTimestamp("date")
                                    date = timestamp?.toDate()

                                }
                                var url: String =
                                    "places/" + data["name"] + data["latitude"].toString() + data["longitude"].toString() + ".jpg"
                                if (sum >= value.toDouble())
                                    myPlacesViewModel
                                        .addPlace(
                                            MyPlace(
                                                data["name"].toString(),
                                                data["latitude"].toString(),
                                                data["longitude"].toString(),
                                                data["autor"].toString(),
                                                data["tip"].toString(),
                                                date,
                                                //data["description"].toString(),
                                                url,
                                                grades,
                                                comments,
                                                document.id

                                            )
                                        )

                            }

                            setupMap(myPlacesViewModel.myPlacesList)

                        } else if (searchType.equals("datum")) {
                            var ts: kotlin.collections.List<String> = value.split(",")

                            var ts1: Long = ts[0].toLong()
                            var ts2: Long = ts[1].toLong()

                            var tms1 = com.google.firebase.Timestamp(ts1, 0)
                            var tms2 = com.google.firebase.Timestamp(ts2, 0)


                            if (ts2 != 0L)
                                result = withContext(Dispatchers.IO) {
                                    db.collection("places")
                                        .whereGreaterThanOrEqualTo("date", tms1)
                                        .whereLessThanOrEqualTo("date", tms2)

                                        .get()
                                        .await()

                                }

                            else
                                result = withContext(Dispatchers.IO) {
                                    db.collection("places")
                                        .whereGreaterThanOrEqualTo("date", tms1)

                                        .get()
                                        .await()

                                }

                            myPlacesViewModel.myPlacesList.addAll(createList(result))
                            setupMap(myPlacesViewModel.myPlacesList)


                        } else {
                            if (type == 0) {
                                result = withContext(Dispatchers.IO) {
                                    db.collection("places")
                                        .whereEqualTo(field, value)

                                        .get()
                                        .await()

                                }
                                myPlacesViewModel.myPlacesList.addAll(createList(result))

                            } else {
                                result = withContext(Dispatchers.IO) {
                                    db.collection("places")
                                        .get()
                                        .await()
                                }
                                var list=createList(result)
                                list = list.filter { it ->
                                    when(field){
                                        "autor"->it.autor.startsWith(value)
                                        "tip" ->it.tip.startsWith(value)
                                        else->it.name.startsWith(value)
                                    }
                                } as ArrayList<MyPlace>
                                myPlacesViewModel.myPlacesList.addAll(list)

                            }


                            setupMap(myPlacesViewModel.myPlacesList)
                        }
                } catch (e: java.lang.Exception) {
                    Log.w("TAGA", "GReska", e)
                }
            }


        }

    }




    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                resetMap()
            }
        }

    override fun onResume() {
        super.onResume()
        map.onResume()
        val searchView: SearchView = binding.swMapa
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {


                filterMap(searchType, query, 0)
                return true
            }


            override fun onQueryTextChange(newText: String): Boolean {


                filterMap(searchType, newText, 1)
                return true
            }
        })

    }

    override fun onPause() {
        val radioGroup: RadioGroup = requireView().findViewById(R.id.rgMap)
        radioGroup.clearCheck()
        val searchView: SearchView = binding.swMapa
        searchView.setQuery("",false)
        searchView.clearFocus()
        super.onPause()
        map.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_new_place -> {

                this.findNavController().navigate(R.id.action_MapFragment_to_EditFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
         var item = menu.findItem(R.id.action_show_map)
        item.isVisible = false;
    }



    override fun onNewLocation(location: Location) {
        map.controller.animateTo(GeoPoint(location.latitude, location.longitude))
        myMarker.position = GeoPoint(location.latitude, location.longitude)
    }
}


