package com.example.finalproject

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.finalproject.Userdata.SessionManager
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    lateinit var authNavController: NavController
    lateinit var mainNavController: NavController
    private var didAutoLogin = false

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

        bottomNavView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.homeFragment -> {
                    mainNavController.popBackStack(R.id.homeFragment, false)
                    mainNavController.navigate(R.id.homeFragment)
                    true
                }
                R.id.profileFragment -> {
                    mainNavController.popBackStack(R.id.profileFragment, false)
                    mainNavController.navigate(R.id.profileFragment)
                    true
                }
                else -> false
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
            invalidateOptionsMenu()
        }

        mainNavController.addOnDestinationChangedListener { _, destination, _ ->
            if (findViewById<View>(R.id.main_nav_host_fragment).visibility == View.VISIBLE) {
                supportActionBar?.show()
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
            }
            invalidateOptionsMenu()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.clear()
        val currentDestination = if (findViewById<View>(R.id.main_nav_host_fragment).visibility == View.VISIBLE) {
            mainNavController.currentDestination
        } else {
            authNavController.currentDestination
        }

        return when (currentDestination?.id) {
            R.id.homeFragment -> {
                super.onCreateOptionsMenu(menu)
            }
            else -> {
                false
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val uid = SessionManager.getUserSession(this)

        if (!didAutoLogin && uid != null && FirebaseAuth.getInstance().currentUser != null) {
            didAutoLogin = true
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
        ))
        NavigationUI.setupActionBarWithNavController(this, mainNavController, appBarConfiguration)
    }
}