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

        // Настройка навигации
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        setupActionBarWithNavController(navController)

        bottomNav = findViewById(R.id.bottom_nav)
        NavigationUI.setupWithNavController(bottomNav, navController)

        // Слушатель смены фрагментов для обновления меню и bottom nav
        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateMenuVisibilityForDestination(destination.id)
            updateProfileIcon()
        }

        // Подписка на изменения токена и данных пользователя
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

        // Устанавливаем видимость пунктов в соответствии с текущим фрагментом
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
                handlePostAction()
                true
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

    /**
     * Обновляет видимость пунктов меню и нижней навигации в зависимости от текущего фрагмента.
     */
    private fun updateMenuVisibilityForDestination(destinationId: Int?) {
        when (destinationId) {
            R.id.feedFragment,
            R.id.usersFragment,      // добавлено
            R.id.eventsFragment -> {  // добавлено
                profileMenuItem?.isVisible = true
                postMenuItem?.isVisible = false
                bottomNav.visibility = View.VISIBLE
            }
            R.id.newPostFragment -> {
                profileMenuItem?.isVisible = false
                postMenuItem?.isVisible = true
                // Скрываем стандартную нижнюю навигацию, чтобы показать BottomAppBar фрагмента
                bottomNav.visibility = View.GONE
            }
            R.id.loginFragment, R.id.registerFragment -> {
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

    /**
     * Обрабатывает нажатие на галочку (сохранение поста).
     * Делегирует выполнение текущему фрагменту, если он поддерживает интерфейс OnPostActionListener.
     */
    private fun handlePostAction() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)?.childFragmentManager?.fragments?.firstOrNull()
        if (currentFragment is OnPostActionListener) {
            currentFragment.onPostAction()
        }
    }

    /**
     * Обновляет иконку профиля: если пользователь залогинен и есть аватар, загружает его,
     * если нет аватара, создаёт буквенный аватар, иначе стандартная иконка.
     */
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
                            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                                menuItem.icon = resource
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {
                                menuItem.icon = placeholder
                            }
                        })
                } else {
                    val name = currentUser?.name ?: return
                    val firstLetter = name.firstOrNull()?.toString() ?: "?"
                    val size = resources.getDimensionPixelSize(R.dimen.menu_icon_size) // убедитесь, что размер определён
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

    /**
     * Показывает всплывающее меню профиля при нажатии на иконку.
     */
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
                // переход на профиль — реализуйте навигацию к профилю пользователя
                // например, navController.navigate(R.id.profileFragment)
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

/**
 * Интерфейс для обработки действий из тулбара в фрагментах.
 * Фрагменты, которые должны реагировать на нажатие галочки, реализуют этот интерфейс.
 */
interface OnPostActionListener {
    fun onPostAction()
}