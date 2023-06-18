package com.example.foodmap

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.foodmap.data.UserObject
import com.example.foodmap.databinding.FragmentRankBinding
import com.example.foodmap.model.MyPlacesViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class RankFragment : Fragment() {

    private val myPlacesViewModel: MyPlacesViewModel by activityViewModels()
    private var _binding: FragmentRankBinding? = null
    private val binding get() = _binding!!
    private val db = Firebase.firestore
    var userName: String = UserObject.username!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentRankBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var tvName: TextView = view.findViewById(R.id.RankFragmentName)
        tvName.setText(myPlacesViewModel.selected?.name)
        var rate: RatingBar = view.findViewById(R.id.RankFragmentRating)
        var kom: EditText = view.findViewById(R.id.RankFragmentKomentar)
        var confirmbtn: Button = view.findViewById(R.id.RankFragmentbtnConfirm)
        var cancelbtn: Button = view.findViewById(R.id.RankFragmentbtnCancel)



        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    myPlacesViewModel.selected?.id?.let {
                        db.collection("places")
                            .document(it)
                            .get()
                            .await()


                    }

                }

                if (result != null) {
                    val document = result.data
                    var hmGrades: HashMap<String, Double>? =
                        document?.get("grades") as HashMap<String, Double>?

                    if (hmGrades?.get(userName) != null)
                        rate.rating = (hmGrades[userName] as Double).toFloat()

                    var hmComments: HashMap<String, String>? =
                        document?.get("comments") as HashMap<String, String>?
                    if (hmComments?.get(userName) != null)
                        kom.setText(hmComments[userName]!!)

                    var oldRate = rate.rating
                    val oldKomm = kom.text.toString()

                    confirmbtn.setOnClickListener {
                        if (rate.rating != 0f)
                            myPlacesViewModel.selected?.addGrade(userName, rate.rating.toDouble())


                        myPlacesViewModel.selected?.addComment(userName, kom.text.toString())
                        if (document != null) {
                            document["grades"] = myPlacesViewModel.selected?.grades!!
                            document["comments"] = myPlacesViewModel.selected?.comments!!

                            result.reference.set(document)
                        }

                        var starsCount: Long = 0
                        if (rate.rating != 0f && oldRate == 0f)
                            starsCount = 2
                        var kommCount: Long = 0
                        if (kom.text.isNotEmpty() && oldKomm.isEmpty())
                            kommCount = 1


                        db.collection("users")
                            .whereEqualTo("username", userName)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                for (documentSnapshot in querySnapshot.documents) {
                                    val documentRef =
                                        db.collection("users").document(documentSnapshot.id)

                                    starsCount += documentSnapshot.get("starsCount") as Long

                                    kommCount += documentSnapshot.get("commentsCount") as Long

                                    val noviPodaci = hashMapOf<String, Any>(

                                        "starsCount" to starsCount,
                                        "commentsCount" to kommCount
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



                        findNavController().navigate(R.id.action_RankFragment_to_ListFragment)
                    }
                }

            } catch (e: java.lang.Exception) {
                Log.w("TAGA", "Greska", e)
            }
            cancelbtn.setOnClickListener {
                myPlacesViewModel.selected = null
                findNavController().navigate(R.id.action_RankFragment_to_ListFragment)
            }
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        myPlacesViewModel.selected = null
    }

}