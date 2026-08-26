package com.example

import com.example.adblock.AdBlocker
import com.example.tabs.TabManager
import com.example.util.BrowserTools
import com.example.util.SearchEngine
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testAdBlockerDomainMatching() {
        AdBlocker.isEnabled = true
        assertTrue(AdBlocker.isAdHost("doubleclick.net"))
        assertTrue(AdBlocker.isAdHost("pagead2.googlesyndication.com"))
        assertTrue(AdBlocker.isAdHost("ads.pubmatic.com"))
        assertTrue(AdBlocker.isAdHost("subdomain.criteo.com"))
        assertFalse(AdBlocker.isAdHost("wikipedia.org"))
        assertFalse(AdBlocker.isAdHost("github.com"))
    }

    @Test
    fun testBrowserToolsUrlFormatting() {
        val googleSearch = BrowserTools.formatInputToUrl("kotlin android", SearchEngine.GOOGLE)
        assertTrue(googleSearch.startsWith("https://www.google.com/search?q="))

        val directUrl = BrowserTools.formatInputToUrl("https://news.ycombinator.com")
        assertEquals("https://news.ycombinator.com", directUrl)

        val domainUrl = BrowserTools.formatInputToUrl("github.com")
        assertEquals("https://github.com", domainUrl)
    }

    @Test
    fun testTabManagerCreation() {
        val tabManager = TabManager()
        val tab = tabManager.createTab(url = "https://example.com", isIncognito = false)
        assertEquals(1, tabManager.tabs.value.size)
        assertEquals(tab.id, tabManager.activeTabId.value)
        assertEquals("https://example.com", tab.url)
        assertFalse(tab.isIncognito)

        val incognitoTab = tabManager.createTab(url = "https://duckduckgo.com", isIncognito = true)
        assertEquals(2, tabManager.tabs.value.size)
        assertEquals(incognitoTab.id, tabManager.activeTabId.value)
        assertTrue(incognitoTab.isIncognito)
    }
}
