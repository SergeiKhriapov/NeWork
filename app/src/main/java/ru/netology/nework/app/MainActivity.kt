package ru.netology.nework.app

import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import ru.netology.nework.R

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        setupActionBarWithNavController(navController)

        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_nav)
        NavigationUI.setupWithNavController(bottomNav, navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment, R.id.registerFragment -> bottomNav.visibility = View.GONE
                else -> bottomNav.visibility = View.VISIBLE
            }
        }
    }

    // Явно подключаем меню к тулбару
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_app_bar_menu, menu)
        return true
    }

    // Обрабатываем клики по пунктам меню
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navController = (supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController

        return when (item.itemId) {
            R.id.action_profile -> {
                val isLoggedIn = checkUserAuth()

                val anchorView = findViewById<View>(R.id.toolbar)
                val popup = PopupMenu(this, anchorView, Gravity.END)
                popup.menuInflater.inflate(R.menu.profile_menu, popup.menu)

                popup.menu.findItem(R.id.action_login).isVisible = !isLoggedIn
                popup.menu.findItem(R.id.action_register).isVisible = !isLoggedIn
                popup.menu.findItem(R.id.action_view_profile).isVisible = isLoggedIn

                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_login -> {
                            navController.navigate(R.id.loginFragment)
                            true
                        }
                        R.id.action_register -> {
                            navController.navigate(R.id.registerFragment)
                            true
                        }
                        R.id.action_view_profile -> {
                            // пока нет фрагмента профиля
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = (supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun checkUserAuth(): Boolean {
        // Заглушка: верни true если пользователь авторизован
        return false
    }
}