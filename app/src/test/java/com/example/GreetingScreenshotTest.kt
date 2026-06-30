package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
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
  fun calculator_screen_screenshot() {
    val viewModel = CalculatorViewModel()
    composeTestRule.setContent {
      CalculatorTheme(DarkCalcColors) {
        CalculatorScreen(viewModel = viewModel, colors = DarkCalcColors)
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/calculator.png")
  }
}

