package com.example.foodmap

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ListView
import android.widget.Switch
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.foodmap.data.User
import com.example.foodmap.databinding.FragmentUsersBinding
import com.example.foodmap.model.UserListAdapter
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class UsersFragment : Fragment() {

    private var _binding: FragmentUsersBinding? = null


    private val binding get() = _binding!!
    private var ascending = 0
    var list: ArrayList<User> = ArrayList()
    var db = Firebase.firestore
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentUsersBinding.inflate(inflater, container, false)
        return binding.root

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        createList()

        var switch: Switch = binding.switch1
        switch.setOnCheckedChangeListener { compoundButton, b ->
            if (b)
                ascending = 1
            else
                ascending = 0

            createList()
        }

    }

    fun createList() {
        list.clear()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result: QuerySnapshot
                if (ascending == 1)
                    result = withContext(Dispatchers.IO) {
                        db.collection("users")
                            .orderBy("addCount", Query.Direction.ASCENDING)
                            .get()
                            .await()
                    }
                else {
                    result = withContext(Dispatchers.IO) {
                        db.collection("users")
                            .orderBy("addCount", Query.Direction.DESCENDING)
                            .get()
                            .await()
                    }
                }

                for (document in result) {
                    var data = document.data

                    list.add(
                        User(
                            data["username"].toString()!!,
                            data.get("password").toString()!!,
                            data["first name"].toString()!!,
                            data.get("last name").toString()!!,
                            data.get("phone").toString()!!,
                            data.get("url").toString()!!,
                            data.get("addCount").toString().toDouble()!!,
                            data.get("starsCount").toString().toDouble()!!,
                            data.get("commentsCount").toString().toDouble()!!,
                            document.id

                        )
                    )

                }
                showList(requireView(), list)
            } catch (e: java.lang.Exception) {
                Log.w("TAGA", "GReska", e)
            }
        }
    }

    fun showList(view: View, arrayList: java.util.ArrayList<User>) {


        val listView: ListView = requireView().findViewById(R.id.listUsers)

        val arrayAdapter = UserListAdapter(
            view.context,
            arrayList
        )
        listView.adapter = arrayAdapter

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)
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
                this.findNavController().navigate(R.id.action_HomeFragment_to_EditFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}