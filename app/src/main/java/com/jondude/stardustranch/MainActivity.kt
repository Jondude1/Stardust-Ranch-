package com.jondude.stardustranch

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import com.jondude.stardustranch.game.GameView
import com.jondude.stardustranch.game.Ui
import com.jondude.stardustranch.update.Updater

class MainActivity : Activity(), Ui.Host {
    private lateinit var view: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        view = GameView(this, this)
        setContentView(view)
        hideBars()
    }

    private fun hideBars() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) { super.onWindowFocusChanged(hasFocus); if (hasFocus) hideBars() }

    override fun onResume() {
        super.onResume()
        Updater.checkQuietly { view.ui.updateDismissed = false }
    }

    override fun onPause() {
        super.onPause()
        synchronized(view) { view.game.save() }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { view.onBack() }

    override fun installUpdate(r: Updater.Release) {
        Updater.downloadAndInstall(applicationContext, r) {}
    }

    override fun checkUpdate() {
        Updater.check({ view.ui.updateDismissed = false }, {})
    }
}
