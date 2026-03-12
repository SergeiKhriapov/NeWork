package ru.netology.nework.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
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
import androidx.navigation.ui.AppBarConfiguration
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
import ru.netology.nework.utils.LetterAvatarDrawable
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
    private var postMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.feedFragment,
                R.id.usersFragment,
                R.id.eventsFragment,
                R.id.postDetailFragment
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        bottomNav = findViewById(R.id.bottom_nav)
        NavigationUI.setupWithNavController(bottomNav, navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateMenuVisibilityForDestination(destination.id)
            updateProfileIcon()
        }

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
        postMenuItem = menu?.findItem(R.id.action_post)

        val currentDestinationId = navController.currentDestination?.id
        updateMenuVisibilityForDestination(currentDestinationId)
        updateProfileIcon()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                showProfilePopup()
                true
            }

            R.id.action_post -> {
                val currentFragment = supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment)
                    ?.childFragmentManager
                    ?.fragments
                    ?.firstOrNull()
                if (currentFragment is OnPostActionListener) {
                    currentFragment.onPostAction()
                    true
                } else {
                    false
                }
            }

            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun updateMenuVisibilityForDestination(destinationId: Int?) {
        when (destinationId) {
            R.id.feedFragment,
            R.id.usersFragment,
            R.id.eventsFragment -> {
                profileMenuItem?.isVisible = true
                postMenuItem?.isVisible = false
                bottomNav.visibility = View.VISIBLE
            }

            R.id.newPostFragment -> {
                profileMenuItem?.isVisible = false
                postMenuItem?.isVisible = true
                bottomNav.visibility = View.GONE
            }

            R.id.loginFragment, R.id.registerFragment -> {
                profileMenuItem?.isVisible = false
                postMenuItem?.isVisible = false
                bottomNav.visibility = View.GONE
            }

            R.id.locationPickerFragment -> {
                profileMenuItem?.isVisible = false
                postMenuItem?.isVisible = false
                bottomNav.visibility = View.GONE
            }

            R.id.userSelectionFragment -> {
                profileMenuItem?.isVisible = false
                postMenuItem?.isVisible = true
                bottomNav.visibility = View.GONE
            }

            R.id.postDetailFragment -> {
                profileMenuItem?.isVisible = false
                postMenuItem?.isVisible = false
                bottomNav.visibility = View.GONE
            }

            else -> {
                profileMenuItem?.isVisible = true
                postMenuItem?.isVisible = false
                bottomNav.visibility = View.VISIBLE
            }
        }
    }

    private fun handlePostAction() {
        val currentFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment)
            ?.childFragmentManager
            ?.fragments
            ?.firstOrNull()
        if (currentFragment is OnPostActionListener) {
            currentFragment.onPostAction()
        }
    }

    private fun updateProfileIcon() {
        profileMenuItem?.let { menuItem ->
            if (isLoggedIn) {
                if (!currentUser?.avatar.isNullOrBlank()) {
                    Glide.with(this)
                        .asDrawable()
                        .load(currentUser?.avatar)
                        .placeholder(R.drawable.ic_account_circle)
                        .error(R.drawable.ic_account_circle)
                        .circleCrop()
                        .into(object : CustomTarget<Drawable>() {
                            override fun onResourceReady(
                                resource: Drawable,
                                transition: Transition<in Drawable>?
                            ) {
                                menuItem.icon = resource
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {
                                menuItem.icon = placeholder
                            }
                        })
                } else {
                    val name = currentUser?.name ?: return
                    val firstLetter = name.firstOrNull()?.toString() ?: "?"
                    val size = resources.getDimensionPixelSize(R.dimen.menu_icon_size)
                    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    val drawable = LetterAvatarDrawable(
                        letter = firstLetter,
                        backgroundColor = ContextCompat.getColor(this, R.color.purple_primary)
                    )
                    drawable.setBounds(0, 0, size, size)
                    drawable.draw(canvas)
                    menuItem.icon = BitmapDrawable(resources, bitmap)
                }
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
        ).apply {
            animationStyle = R.style.PopupExpandDownAnimation
        }

        profilePopup = popupWindow

        val anchorView = toolbar.findViewById<View>(R.id.action_profile) ?: toolbar
        popupWindow.showAsDropDown(anchorView)

        val navOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setPopUpTo(navController.graph.startDestinationId, false)
            .build()

        popupView.findViewById<TextView>(R.id.action_login)?.apply {
            visibility = if (isLoggedIn) View.GONE else View.VISIBLE
            setOnClickListener {
                popupWindow.dismiss()
                navController.navigate(R.id.loginFragment, null, navOptions)
            }
        }

        popupView.findViewById<TextView>(R.id.action_registration)?.apply {
            visibility = if (isLoggedIn) View.GONE else View.VISIBLE
            setOnClickListener {
                popupWindow.dismiss()
                navController.navigate(R.id.registerFragment, null, navOptions)
            }
        }

        popupView.findViewById<TextView>(R.id.action_view_profile)?.apply {
            visibility = if (isLoggedIn) View.VISIBLE else View.GONE
            setOnClickListener {
                popupWindow.dismiss()
                // переход на профиль
            }
        }

        popupView.findViewById<TextView>(R.id.action_logout)?.apply {
            visibility = if (isLoggedIn) View.VISIBLE else View.GONE
            setOnClickListener {
                popupWindow.dismiss()
                lifecycleScope.launch {
                    tokenManager.clearToken()
                }
            }
        }
    }
}

interface OnPostActionListener {
    fun onPostAction()
}