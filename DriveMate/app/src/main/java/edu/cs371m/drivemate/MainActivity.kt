package edu.cs371m.drivemate

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import edu.cs371m.drivemate.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get the NavHostFragment that lives in activity_main.xml
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController   = navHostFragment.navController

        // Wire the BottomNavigationView to Navigation Component
        binding.bottomNav.setupWithNavController(navController)

        // Hide bottom‑nav on auth screens
        navController.addOnDestinationChangedListener { _, dest, _ ->
            binding.bottomNav.visibility =
                if (dest.id == R.id.loginFragment || dest.id == R.id.signUpFragment)
                    View.GONE else View.VISIBLE
        }
    }
}
