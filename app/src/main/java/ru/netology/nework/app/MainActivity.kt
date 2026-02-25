package ru.netology.nework.app

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.data.datastore.TokenManager
import ru.netology.nework.model.User
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    @Inject
    lateinit var tokenManager: TokenManager

    private var isLoggedIn = false
    private var currentUser: User? = null
    private var profilePopup: PopupWindow? = null
    private lateinit var toolbar: MaterialToolbar
    private lateinit var navController: NavController
    private lateinit var bottomNav: BottomNavigationView
    private var profileMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        setupActionBarWithNavController(navController)

        bottomNav = findViewById(R.id.bottom_nav)
        NavigationUI.setupWithNavController(bottomNav, navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment,
                R.id.registerFragment -> {
                    bottomNav.visibility = View.GONE
                    toolbar.menu.findItem(R.id.action_profile)?.isVisible = false
                }
                else -> {
                    bottomNav.visibility = View.VISIBLE
                    toolbar.menu.findItem(R.id.action_profile)?.isVisible = true
                }
            }
        }

        // Следим за состоянием авторизации и данными пользователя
        lifecycleScope.launch {
            combine(tokenManager.tokenFlow, tokenManager.currentUser) { token, user ->
                isLoggedIn = token != null
                currentUser = user
                updateProfileIcon()
            }.collect { }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_app_bar_menu, menu)
        profileMenuItem = menu?.findItem(R.id.action_profile)
        updateProfileIcon()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                showProfilePopup()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateProfileIcon() {
        profileMenuItem?.let { menuItem ->
            if (isLoggedIn && !currentUser?.avatar.isNullOrBlank()) {
                // Загружаем аватар в иконку меню
                Glide.with(this)
                    .asDrawable()
                    .load(currentUser?.avatar)
                    .placeholder(R.drawable.ic_account_circle)
                    .error(R.drawable.ic_account_circle)
                    .circleCrop()
                    .into(object : CustomTarget<Drawable>() {
                        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                            menuItem.icon = resource
                        }
                        override fun onLoadCleared(placeholder: Drawable?) {
                            menuItem.icon = placeholder
                        }
                    })
            } else {
                menuItem.icon = ContextCompat.getDrawable(this, R.drawable.ic_account_circle)
            }
        }
    }

    private fun showProfilePopup() {
        profilePopup?.dismiss()

        val popupView = layoutInflater.inflate(R.layout.profile_popup, null)

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        profilePopup = popupWindow
        popupWindow.animationStyle = R.style.PopupExpandDownAnimation

        // Используем иконку профиля как якорь, как в рабочем варианте
        val anchorView = toolbar.findViewById<View>(R.id.action_profile)
        popupWindow.showAsDropDown(anchorView)

        val navOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setPopUpTo(navController.graph.startDestinationId, false)
            .build()

        // Здесь можно добавить блок информации о пользователе в popup (если нужно)
        // Если в profile_popup.xml есть блок user_info, его тоже можно обновить

        // Логин
        popupView.findViewById<TextView>(R.id.action_login)?.apply {
            visibility = if (isLoggedIn) View.GONE else View.VISIBLE
            setOnClickListener {
                popupWindow.dismiss()
                navController.navigate(R.id.loginFragment, null, navOptions)
            }
        }

        // Регистрация
        popupView.findViewById<TextView>(R.id.action_registration)?.apply {
            visibility = if (isLoggedIn) View.GONE else View.VISIBLE
            setOnClickListener {
                popupWindow.dismiss()
                navController.navigate(R.id.registerFragment, null, navOptions)
            }
        }

        // Профиль (только для авторизованных)
        popupView.findViewById<TextView>(R.id.action_view_profile)?.apply {
            visibility = if (isLoggedIn) View.VISIBLE else View.GONE
            setOnClickListener {
                popupWindow.dismiss()
                // Переход на фрагмент профиля, когда создадите
                // navController.navigate(R.id.profileFragment)
            }
        }

        // Выход (только для авторизованных)
        popupView.findViewById<TextView>(R.id.action_logout)?.apply {
            visibility = if (isLoggedIn) View.VISIBLE else View.GONE
            setOnClickListener {
                popupWindow.dismiss()
                lifecycleScope.launch {
                    tokenManager.clearToken()
                    /*// После выхода можно перенаправить на логин
                    navController.navigate(R.id.loginFragment, null, navOptions)*/
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}