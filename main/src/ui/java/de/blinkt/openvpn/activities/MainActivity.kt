package de.blinkt.openvpn.activities

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import de.blinkt.openvpn.R
import de.blinkt.openvpn.fragments.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)

        toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)


        navigationView.setNavigationItemSelectedListener(this)

        // Default fragment
        if (savedInstanceState == null) {
            loadFragment(VPNProfileList())
            navigationView.setCheckedItem(R.id.nav_vpn_list)
            navigationView.menu.getItem(0).isChecked = true
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        toggle.syncState()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(@NonNull item: MenuItem): Boolean {
        var fragment: Fragment? = null
        when (item.itemId) {
            R.id.nav_vpn_list -> fragment = VPNProfileList()
            R.id.nav_stats -> fragment = GraphFragment()
            R.id.nav_general_settings -> fragment = GeneralSettings()
            R.id.nav_faq -> fragment = FaqFragment()
            R.id.nav_about -> fragment = AboutFragment()
            // Add other cases for other menu items as needed
        }

        if (fragment != null) {
            loadFragment(fragment)
            for (i in 0 until navigationView.menu.size()) {
                navigationView.menu.getItem(i).isChecked = false
            }
            item.isChecked = true // Установить состояние checked для выбранного элемента
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        if (item.title == getString(R.string.home)) {
            setTitle(R.string.app)
        } else {
            title = item.title
        }
        return true
    }

    private fun loadFragment(fragment: Fragment) {
        val fragmentManager: FragmentManager = supportFragmentManager
        fragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .commit()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        val intent = intent
        if (intent != null) {
            val action = intent.action
            if (Intent.ACTION_VIEW == action) {
                val uri = intent.data
                uri?.let { checkUriForProfileImport(it) }
            }
            val page = intent.getStringExtra("PAGE")
            if ("graph" == page) {
                navigationView.setCheckedItem(R.id.nav_stats)
                loadFragment(GraphFragment())
            }
            setIntent(null)
        }
    }

    private fun checkUriForProfileImport(uri: Uri) {
        if ("openvpn" == uri.scheme && "import-profile" == uri.host) {
            var realUrl = uri.encodedPath + "?" + uri.encodedQuery
            if (!realUrl.startsWith("/https://")) {
                Toast.makeText(
                    this,
                    "Cannot use openvpn://import-profile/ URL that does not use https://",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
            realUrl = realUrl.substring(1)
            startOpenVPNUrlImport(realUrl)
        }
    }

    private fun startOpenVPNUrlImport(url: String) {
        val asImportFrag = ImportRemoteConfig.newInstance(url)
        asImportFrag.show(supportFragmentManager, "dialog")
    }

//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        menuInflater.inflate(R.menu.main_menu, menu)
//        return super.onCreateOptionsMenu(menu)
//    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        toggle.onConfigurationChanged(newConfig)
    }
}
