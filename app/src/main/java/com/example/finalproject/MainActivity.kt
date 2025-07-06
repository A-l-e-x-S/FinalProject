package com.example.finalproject

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.finalproject.Userdata.SessionManager

class MainActivity : AppCompatActivity() {

    lateinit var authNavController: NavController
    lateinit var mainNavController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        CloudinaryConfig.init(this)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val authNavHostFragment = supportFragmentManager.findFragmentById(R.id.auth_nav_host_fragment) as NavHostFragment
        authNavController = authNavHostFragment.navController

        val mainNavHostFragment = supportFragmentManager.findFragmentById(R.id.main_nav_host_fragment) as NavHostFragment
        mainNavController = mainNavHostFragment.navController
        setupActionBarWithAuthNav()

        val bottomNavView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        NavigationUI.setupWithNavController(bottomNavView, mainNavController)

        bottomNavView.setOnItemReselectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.homeFragment -> {
                    mainNavController.popBackStack(R.id.homeFragment, false)
                }
            }
        }

        authNavController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment -> {
                    supportActionBar?.hide()
                }
                R.id.UserRegistrationFragment -> {
                    supportActionBar?.show()
                    supportActionBar?.setDisplayHomeAsUpEnabled(true)
                    supportActionBar?.title = "Register"
                }
                R.id.homeFragment -> {
                    showMainNavigation()
                }
                else -> {
                    supportActionBar?.show()
                    supportActionBar?.setDisplayHomeAsUpEnabled(false)
                }
            }
        }

        mainNavController.addOnDestinationChangedListener { _, destination, _ ->
            if (findViewById<View>(R.id.main_nav_host_fragment).visibility == View.VISIBLE) {
                supportActionBar?.show()
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val userId = SessionManager.getUserSession(this)

        if (userId != -1 && authNavController.currentDestination?.id == R.id.loginFragment) {
            showMainNavigation()
            mainNavController.navigate(R.id.homeFragment)
        }
    }

    fun showMainNavigation() {
        findViewById<View>(R.id.auth_nav_host_fragment).visibility = View.GONE
        findViewById<View>(R.id.main_nav_host_fragment).visibility = View.VISIBLE
        findViewById<BottomNavigationView>(R.id.bottomNavigationView).visibility = View.VISIBLE

        setupActionBarWithMainNav()
    }

    fun showAuthNavigation() {
        findViewById<View>(R.id.auth_nav_host_fragment).visibility = View.VISIBLE
        findViewById<View>(R.id.main_nav_host_fragment).visibility = View.GONE
        findViewById<BottomNavigationView>(R.id.bottomNavigationView).visibility = View.GONE

        setupActionBarWithAuthNav()
    }

    override fun onSupportNavigateUp(): Boolean {
        return mainNavController.navigateUp() || authNavController.navigateUp() || super.onSupportNavigateUp()
    }

    fun setupActionBarWithAuthNav() {
        val appBarConfiguration = AppBarConfiguration(setOf(R.id.loginFragment))
        NavigationUI.setupActionBarWithNavController(this, authNavController, appBarConfiguration)
    }

    fun setupActionBarWithMainNav() {
        val appBarConfiguration = AppBarConfiguration(setOf(
            R.id.homeFragment,
            R.id.profileFragment,
            R.id.PersonalExpensesFragment
        ))
        NavigationUI.setupActionBarWithNavController(this, mainNavController, appBarConfiguration)
    }
}

