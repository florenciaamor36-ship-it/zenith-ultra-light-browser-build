package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.tabs.WebTab
import com.example.ui.components.BrowserAddressBar
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun browser_address_bar_screenshot() {
    val sampleTab = WebTab(
      id = "tab_1",
      title = "AeroWeb Fast Engine",
      url = "https://www.google.com"
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        BrowserAddressBar(
          tab = sampleTab,
          tabCount = 1,
          isBottomBar = true,
          isAdBlockActive = true,
          onNavigate = {},
          onReload = {},
          onStop = {},
          onGoBack = {},
          onGoForward = {},
          onOpenTabs = {},
          onOpenMenu = {},
          onOpenQrScanner = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/address_bar.png")
  }
}
