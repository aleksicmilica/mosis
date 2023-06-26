package com.example.foodmap

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.foodmap.data.MyPlace
import com.example.foodmap.data.User
import com.example.foodmap.data.UserObject
import com.example.foodmap.databinding.FragmentListBinding
import com.example.foodmap.model.MyPlacesListAdapter
import com.example.foodmap.model.MyPlacesViewModel
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ListFragment : Fragment() {

    private var _binding: FragmentListBinding? = null

    private val myPlacesViewModel: MyPlacesViewModel by activityViewModels()
    private val db = Firebase.firestore
    private val binding get() = _binding!!
    private var searchType: String = "name"
    private var userName: String = UserObject.username!!
    var lastCheckedRadioButton: RadioButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

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

    fun getList() {



        viewLifecycleOwner.lifecycleScope.launch {
            try {

                val result = withContext(Dispatchers.IO) {
                    db.collection("places")
                        .get()
                        .await()
                }
                myPlacesViewModel.myPlacesList.clear()
                myPlacesViewModel.myPlacesList.addAll(createList(result))
                Log.d("TAGA","s"+myPlacesViewModel.myPlacesList.size.toString())
                showList(requireView(), myPlacesViewModel.myPlacesList)
            } catch (e: Exception) {
                Log.w("TAGA", "Greska", e)
            }
        }


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root

    }


    fun showList(view: View, arrayList: ArrayList<MyPlace>) {


        val listView: ListView = requireView().findViewById(R.id.my_places_list)

        val arrayAdapter = MyPlacesListAdapter(
            view.context,
            arrayList
        )
        listView.adapter = arrayAdapter

        listView.setOnItemClickListener(object : AdapterView.OnItemClickListener {
            override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                var myPlace: MyPlace = p0?.adapter?.getItem(p2) as MyPlace
                myPlacesViewModel.selected = myPlace
                findNavController().navigate(R.id.action_ListFragment_to_ViewFragment)

            }
        })


        listView.setOnItemLongClickListener { parent, view, position, id ->
            var myPlace: MyPlace = parent?.adapter?.getItem(position) as MyPlace
            myPlacesViewModel.selected = myPlace

            showPopupMenu(view, position)
            true
        }


    }

    fun getAndShowFiltredList(field: String, query: String, tip: Int) {
        myPlacesViewModel.myPlacesList.clear()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                var result: QuerySnapshot
                if (field.equals("ocena")) {
                    result = withContext(Dispatchers.IO) {
                        db.collection("places")
                            .get()
                            .await()

                    }
                    for (document in result) {
                        var data = document.data
                        var grades = HashMap<String, Double>()
                        var sum: Double = 0.0
                        if (data["grades"] != null) {
                            for (g in data["grades"] as HashMap<String, Double>) {
                                grades[g.key] = g.value
                                sum += g.value
                            }
                            sum /= grades.size

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
                        var url: String =
                            "places/" + data["name"] + data["latitude"].toString() + data["longitude"].toString() + ".jpg"
                        if (sum >= query.toDouble())
                            myPlacesViewModel
                                .addPlace(
                                    MyPlace(
                                        data["name"].toString(),
                                        data["latitude"].toString(),
                                        data["longitude"].toString(),
                                        data["autor"].toString(),
                                        data["tip"].toString(),
                                        date,
                                        url,
                                        //data["description"].toString(),
                                        grades,
                                        comments, document.id

                                    )
                                )

                    }

                    showList(requireView(), myPlacesViewModel.myPlacesList)

                } else {
                    if (searchType.equals("datum")) {
                        var ts: List<String> = query.split(",")

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

                    } else {

                        if (tip == 0) {

                            result = withContext(Dispatchers.IO) {
                                db.collection("places")
                                    .whereEqualTo(field, query)

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
                                    "autor"->it.autor.startsWith(query)
                                    "tip" ->it.tip.startsWith(query)
                                    else->it.name.startsWith(query)
                                }
                            } as ArrayList<MyPlace>
                            myPlacesViewModel.myPlacesList.addAll(list)
                        }

                    }



                    //Log.d("TAGA",myPlacesViewModel.myPlacesList.size.toString())
                    showList(requireView(), myPlacesViewModel.myPlacesList)
                }
            } catch (e: Exception) {
                Log.w("TAGA", "GReska", e)
            }
        }


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getList()


        val radioGroup: RadioGroup = view.findViewById(R.id.rgTable)
        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.rbAutorTabela -> {
                    searchType = "autor"

                }
                R.id.rbTip -> {
                    searchType = "tip"

                }
                R.id.rbOcena -> {
                    searchType = "ocena"

                }
                R.id.rbDatumTabela -> {
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
                binding.svTable.setQuery("",false)
                binding.svTable.clearFocus()
                val datod = binding.editTextDate
                datod.setText("")
                val datdo = binding.editTextDate2
                datdo.setText("")
            }
        }

        var btn2: Button = binding.button3

        btn2.setOnClickListener {
            radioGroup.clearCheck()
            binding.svTable.setQuery("",false)
            binding.svTable.clearFocus()
            getList()
        }

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

                } catch (e: Exception) {
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
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Datum mora biti u formatu ddMMYYYY",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            var query: String = timestamp1.toString() + "," + timestamp2.toString()
            getAndShowFiltredList("datum", query, 0)


        }
    }

    private fun showPopupMenu(view: View, position: Int) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.popup_menu, popupMenu.menu)
        if (!myPlacesViewModel.selected!!.autor.equals(userName)) {
            val menu = popupMenu.menu.findItem(R.id.editPlace)
            menu.isVisible = false

        }

        popupMenu.setOnMenuItemClickListener { menuItem ->

            when (menuItem.itemId) {
                R.id.viewPlace -> {

                    findNavController().navigate(R.id.action_ListFragment_to_ViewFragment)
                    true
                }

                R.id.editPlace -> {

                    findNavController().navigate(R.id.action_ListFragment_to_EditFragment)

                    true
                }

                R.id.showOnMap -> {

                    this.findNavController().navigate(R.id.action_ListFragment_to_MapFragment)

                    true
                }
                R.id.rankPlace -> {

                    this.findNavController().navigate(R.id.action_ListFragment_to_RankFragment)
                    true
                }

                else -> false
            }
        }

        popupMenu.show()
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.action_new_place -> {
                this.findNavController().navigate(R.id.action_ListFragment_to_EditFragment)
                true
            }
            R.id.action_users -> {
                this.findNavController().navigate(R.id.action_ListFragment_to_UsersFragment)
                true
            }
            R.id.action_show_map -> {
                this.findNavController().navigate(R.id.action_ListFragment_to_MapFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

    }

    override fun onResume() {
        super.onResume()
        val searchView: SearchView = binding.svTable
        searchType = "name"

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                if (searchType.equals("ocena") && query.toIntOrNull() == null)
                    Toast.makeText(requireContext(), "Unesite broj", Toast.LENGTH_SHORT).show()
                else
                    getAndShowFiltredList(searchType, query, 0)
                return true
            }


            override fun onQueryTextChange(newText: String): Boolean {


                    if (newText.isNotEmpty())
                        if (searchType.equals("ocena") && newText.toIntOrNull() == null) {

                            Toast.makeText(requireContext(), "Unesite broj", Toast.LENGTH_SHORT).show()
                        } else
                            getAndShowFiltredList(searchType, newText, 1)
                    else
                        getList()

                return true
            }
        })
    }

    override fun onPause() {
        val radioGroup: RadioGroup = requireView().findViewById(R.id.rgTable)
        radioGroup.clearCheck()
        val searchView: SearchView = binding.svTable
        searchView.setQuery("",false)
        searchView.clearFocus()
        super.onPause()
    }
}