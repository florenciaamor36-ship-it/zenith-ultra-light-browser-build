package com.example

import android.app.Application
import com.example.adblock.AdBlocker
import com.example.data.local.BrowserDatabase
import com.example.data.repository.BrowserRepository
import com.example.tabs.TabManager

/**
 * Main Application Class for AeroWeb.
 *
 * Performance Tuning & Battery Optimizations:
 * 1. Global Singleton Scopes: Prevents multiple database connection pools or duplicate host sets.
 * 2. System onTrimMemory Hook: Implements aggressive background RAM shedding when Android OS signals
 *    memory constraints, avoiding process termination and OOM exceptions.
 */
class AeroWebApplication : Application() {

    lateinit var database: BrowserDatabase
        private set

    lateinit var repository: BrowserRepository
        private set

    val tabManager by lazy { TabManager(this) }

    override fun onCreate() {
        super.onCreate()
        database = BrowserDatabase.getInstance(this)
        repository = BrowserRepository(database)

        AdBlocker.isEnabled = true
        AdBlocker.loadBundledFilters(this)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Delegate to TabManager to throttle or destroy background WebViews
        tabManager.onTrimMemory(level)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        tabManager.onTrimMemory(TRIM_MEMORY_COMPLETE)
    }
}
