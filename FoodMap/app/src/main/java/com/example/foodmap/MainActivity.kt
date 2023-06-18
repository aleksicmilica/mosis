package com.example.foodmap

import android.location.Location
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.foodmap.data.ILocationClient
import com.example.foodmap.data.LocationClient
import com.example.foodmap.databinding.ActivityMainBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity(), ILocationClient {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController


    companion object {
        var curLocation: Location? = null
        var iLocationClient: ILocationClient? = null
        var locationClient: LocationClient? = null

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationClient = LocationClient(applicationContext, this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)


        navController.addOnDestinationChangedListener { controller, destination, argument ->
            if (destination.id == R.id.EditFragment || destination.id == R.id.ViewFragment || destination.id == R.id.UsersFragment)
                binding.fab.hide()
            else
                binding.fab.show()


        }
        binding.fab.setOnClickListener { view ->

            if (navController.currentDestination?.id == R.id.ListFragment)
                navController.navigate(R.id.action_ListFragment_to_EditFragment)
            else if (navController.currentDestination?.id == R.id.MapFragment)
                navController.navigate(R.id.action_MapFragment_to_EditFragment)
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {

            R.id.action_show_map -> {
                if (navController.currentDestination?.id == R.id.ListFragment)
                    navController.navigate(R.id.action_ListFragment_to_MapFragment)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onNewLocation(location: Location) {
        curLocation = location
        iLocationClient?.onNewLocation(location)
    }

    override fun onDestroy() {
        locationClient?.stop()
        super.onDestroy()
    }
}