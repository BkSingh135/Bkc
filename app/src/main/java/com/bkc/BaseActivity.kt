package com.bkc

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {
    /**
     * transparent Status Bar With White Icon
     */
    @SuppressLint("ResourceType", "InlinedApi")
    fun removeStatusBarWithWhiteIcon() {
        val window = window
        val winParams = window.attributes
        winParams.flags =
            winParams.flags and WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS.inv()
        window.attributes = winParams
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        //SYSTEM_UI_FLAG_LIGHT_STATUS_BAR is used to make
        window.statusBarColor = Color.TRANSPARENT
    }

    /**
     * transparent Status Bar With Black Icon
     */
    @SuppressLint("InlinedApi")
    fun removeStatusBarWithBlackIcon() {
        val window = window
        val winParams = window.attributes
        winParams.flags =
            winParams.flags and WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS.inv()
        window.attributes = winParams
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        window.statusBarColor = Color.TRANSPARENT
    }

    /**
     * removeStatusBarAndBottomNavWithWhiteIcon
     */
    fun removeStatusBarAndBottomNavWithWhiteIcon() {
        val window = window
        val winParams = window.attributes
        winParams.flags =
            winParams.flags and WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS.inv()
        window.attributes = winParams
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or  //is used to hide bottom navigation background
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or  //is used to hide bottom nav
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    /**
     * transparent Status Bar With White Icon
     */
    fun transparentStatusBarAndBottomBar() {
        val window = window
        ///WindowManager.LayoutParams winParams = window.getAttributes();
        ///winParams.flags &= ~WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        ///window.setAttributes(winParams);
        // /window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        //window.setStatusBarColor(Color.WHITE);
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        )
    }

    fun hideStatusBar() {
        val window = window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController!!.hide(WindowInsets.Type.statusBars())
        } else {
            getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
        //or
//           View decorView = getWindow().getDecorView();
//            // Hide the status bar.
//            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
// /           decorView.setSystemUiVisibility(uiOptions);
    }

    /**
     * hide Status Bar With Bottom Nav With Shadow Nav
     */
    fun hideStatusBarWithBottomNavWithShadowNav() {
        val window = window
        if (actionBar != null) {
            //getSupportActionBar().hide();
            actionBar!!.hide()
        } else {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or  //for making it full screen by removing background of status bar
                        View.SYSTEM_UI_FLAG_FULLSCREEN or  //is used to show bottom nav on click but it has default background white
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or  //is used to make bottom nav transparent and if we click it show if i again click it hides
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or  //is used to hide bottom navigation background
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or  //is used to hide bottom with icons  navigation background
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }
    }
}