package com.tail_island.jetbus

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.google.android.material.navigation.NavigationView
import com.tail_island.jetbus.databinding.ActivityMainBinding
import com.tail_island.jetbus.model.Repository
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    @Inject lateinit var repository: Repository

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (application as App).component.inject(this)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        NavigationUI.setupWithNavController(binding.toolbar, findNavController(R.id.navControllerFragment), AppBarConfiguration(setOf(R.id.bookmarkList), binding.drawerLayout))

        binding.navigationView.setNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.clearDatabase -> run {
                lifecycleScope.launch {
                    repository.clearDatabase()

                    findNavController(R.id.navControllerFragment).navigate(R.id.splashFragment)
                }
                true
            }
            else -> false
        }.also {
            if (it) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            }
        }
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            return
        }

        if (findNavController(R.id.navControllerFragment).currentDestination?.id == R.id.bookmarkList) {  // AppBarConfiguration()したのでツールバーはハンバーガー・アイコンになっていますが、それでもバック・ボタンでは戻れちゃうので、チェックします。
            finish()
            return
        }

        super.onBackPressed()
    }
}
