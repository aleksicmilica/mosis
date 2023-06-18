package com.example.foodmap

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.foodmap.data.User
import com.example.foodmap.data.UserObject
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        var editUserName: EditText = findViewById(R.id.loginUserNameET)
        var editPassword: EditText = findViewById(R.id.loginSifraET)
        var db = Firebase.firestore

        var loginBtn: Button = findViewById(R.id.loginBtnLog)
        loginBtn.setOnClickListener {
            var userName: String = editUserName.text.toString()
            var password: String = editPassword.text.toString()
            if (userName.equals(""))
                Toast.makeText(
                    this@LoginActivity,
                    "Morate uneti korisnicko ime",
                    Toast.LENGTH_SHORT
                ).show()
            else if (password.equals(""))
                Toast.makeText(this@LoginActivity, "Morate uneti lozinku", Toast.LENGTH_SHORT)
                    .show()
            else {

                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val result = withContext(Dispatchers.IO) {
                            db.collection("users")
                                .whereEqualTo("username", userName)
                                .get()
                                .await()
                        }


                        if (!result.isEmpty) {
                            for (document in result.documents) {
                                if (document != null)
                                    if (document.data?.get("password").toString()
                                            .equals(password)
                                    ) {
                                        val intent =
                                            Intent(this@LoginActivity, MainActivity::class.java)

                                        var user: User = User(
                                            userName,
                                            password,
                                            document.data?.get("firstname").toString(),
                                            document.data?.get("lastname").toString(),
                                            document.data?.get("phoneNumber").toString(),
                                            document.data?.get("url").toString(),
                                            (document.data?.get("addCount") as? Number)?.toDouble()
                                                ?: 0.0,
                                            (document.data?.get("starsCount") as? Number)?.toDouble()
                                                ?: 0.0,
                                            (document.data?.get("commentsCount") as? Number)?.toDouble()
                                                ?: 0.0,
                                            document.reference.id.toString()
                                        )

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
                                    } else
                                        Toast.makeText(
                                            this@LoginActivity,
                                            "Pogresna lozinka",
                                            Toast.LENGTH_SHORT
                                        ).show()
                            }

                        } else
                            Toast.makeText(
                                this@LoginActivity,
                                "Ne postoji osoba sa tim korisnickim imenom",
                                Toast.LENGTH_SHORT
                            ).show()

                    } catch (e: java.lang.Exception) {
                        Log.w("TAGA", "Greska", e)
                    }
                }
            }
        }

        var registerButton: Button = findViewById(R.id.loginbtnRegister)
        registerButton.setOnClickListener {
            var intent: Intent = Intent(this@LoginActivity, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }


    }
}