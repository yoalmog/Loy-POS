package com.example

import com.example.util.AppConfig
import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun backOfficeUrl_pointsToDeployedVercelProduction() {
    val expected = "https://loy-pos.vercel.app/admin"
    assertEquals(expected, AppConfig.BACK_OFFICE_URL)
    assertTrue("URL must be HTTPS", AppConfig.BACK_OFFICE_URL.startsWith("https://"))
    assertFalse("Must not point to stale AI Studio", AppConfig.BACK_OFFICE_URL.contains("run.app"))
    assertFalse("Must not point to localhost", AppConfig.BACK_OFFICE_URL.contains("localhost"))
  }
}
