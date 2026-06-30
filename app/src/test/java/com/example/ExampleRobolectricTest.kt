package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Calculator", appName)
  }

  @Test
  fun `test VM basic calculation flow`() {
    val viewModel = CalculatorViewModel()
    
    // Type: 1 2 + 8
    viewModel.onKeyPress("1")
    viewModel.onKeyPress("2")
    viewModel.onKeyPress("+")
    viewModel.onKeyPress("8")
    
    assertEquals("12+", viewModel.expression.value.substring(0, 3))
    assertEquals("12+8", viewModel.expression.value)
    
    // Preview should show 20
    assertEquals("20", viewModel.realtimeResult.value)
    
    // Evaluate and commit
    viewModel.onKeyPress("=")
    assertEquals("20", viewModel.realtimeResult.value)
    assertEquals(1, viewModel.history.value.size)
    assertEquals("12+8", viewModel.history.value[0].expression)
    assertEquals("20", viewModel.history.value[0].result)
  }

  @Test
  fun `test VM permutation flow`() {
    val viewModel = CalculatorViewModel()
    
    // 180 nPr 2
    viewModel.onKeyPress("1")
    viewModel.onKeyPress("8")
    viewModel.onKeyPress("0")
    viewModel.onKeyPress("nPr")
    viewModel.onKeyPress("2")
    
    assertEquals("180nPr2", viewModel.expression.value)
    assertEquals("32220", viewModel.realtimeResult.value)
    
    // Commit
    viewModel.onKeyPress("=")
    assertEquals("32220", viewModel.realtimeResult.value)
  }

  @Test
  fun `test VM degree vs radian mode toggle`() {
    val viewModel = CalculatorViewModel()
    
    // Type sin(30)
    viewModel.onKeyPress("sin")
    viewModel.onKeyPress("3")
    viewModel.onKeyPress("0")
    
    // By default Degree Mode: sin(30) = 0.5
    assertEquals("sin(30", viewModel.expression.value)
    assertEquals("0.5", viewModel.realtimeResult.value)
    
    // Toggle to Radian mode: sin(30 radians) approx -0.9880316
    viewModel.toggleDegreeMode()
    assertEquals(false, viewModel.isDegreeMode.value)
    assertEquals("-0.9880316241", viewModel.realtimeResult.value)
  }
}
