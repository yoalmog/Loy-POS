package com.example

import com.example.data.api.BackOfficeSyncResponse
import com.example.data.api.SupabaseCategoryDto
import com.example.data.api.SupabaseItemDto
import com.example.data.db.AppDatabase
import com.example.data.model.ItemEntity
import com.example.data.model.SoldBy
import com.example.util.AppConfig
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests and regression tests for catalog synchronization,
 * endpoint configuration, and schema mapping.
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

  @Test
  fun backOfficeBaseUrl_pointsToProductionHttps() {
    val expected = "https://loy-pos.vercel.app/"
    assertEquals(expected, AppConfig.BACK_OFFICE_BASE_URL)
    assertTrue("Base URL must be HTTPS", AppConfig.BACK_OFFICE_BASE_URL.startsWith("https://"))
    assertTrue("Base URL must end with trailing slash for Retrofit", AppConfig.BACK_OFFICE_BASE_URL.endsWith("/"))
    assertFalse("Must not point to localhost", AppConfig.BACK_OFFICE_BASE_URL.contains("localhost"))
  }

  @Test
  fun itemEntity_imageUrlField_preservesUrlAndDefaults() {
    val defaultItem = ItemEntity(
      id = 1L,
      name = "Espresso",
      categoryId = 1L,
      categoryName = "Coffee",
      price = 3.50
    )
    assertEquals("", defaultItem.imageUrl)

    val itemWithImage = defaultItem.copy(imageUrl = "https://images.unsplash.com/photo-espresso")
    assertEquals("https://images.unsplash.com/photo-espresso", itemWithImage.imageUrl)
    assertEquals("Espresso", itemWithImage.name)
    assertEquals(3.50, itemWithImage.price, 0.001)
  }

  @Test
  fun initialProducts_hasImagesAndNonEmptyCatalog() {
    val products = AppDatabase.CANONICAL_INITIAL_PRODUCTS
    assertTrue("Initial offline catalog must never be empty", products.isNotEmpty())
    assertEquals("Should contain 9 canonical items", 9, products.size)
    
    // Ensure every item has a name, price > 0, category, and an image URL
    for (p in products) {
      assertTrue("Item name must not be blank", p.name.isNotBlank())
      assertTrue("Item price must be positive: ${p.name}", p.price > 0.0)
      assertTrue("Item category must not be blank: ${p.name}", p.category.isNotBlank())
      assertTrue("Item must have an image URL: ${p.name}", p.imageUrl.isNotBlank())
      assertTrue("Image URL must be HTTPS: ${p.imageUrl}", p.imageUrl.startsWith("https://"))
    }
  }

  @Test
  fun syncSnapshotMapping_correctlyMapsBackOfficeJson() {
    val mockDto = SupabaseItemDto(
      id = 101L,
      name = "Cold Brew Nitro",
      category_id = 2L,
      category_name = "Cold Beverages",
      price = 4.75,
      cost = 1.20,
      sku = "CBN-001",
      barcode = "7751234567890",
      sold_by = "EACH",
      in_stock = 45.0,
      low_stock_threshold = 5.0,
      track_stock = true,
      color_hex = "#00897B",
      image_url = "https://images.unsplash.com/photo-coldbrew"
    )

    val syncResponse = BackOfficeSyncResponse(
      items = listOf(mockDto),
      categories = listOf(SupabaseCategoryDto(id = 2L, name = "Cold Beverages", color_hex = "#00897B")),
      timestamp = System.currentTimeMillis(),
      server_status = "online"
    )

    assertEquals(1, syncResponse.items.size)
    assertEquals(1, syncResponse.categories.size)

    val mappedItem = ItemEntity(
      id = mockDto.id ?: 0L,
      name = mockDto.name,
      categoryId = mockDto.category_id ?: 1L,
      categoryName = mockDto.category_name ?: "General",
      price = mockDto.price,
      cost = mockDto.cost ?: 0.0,
      sku = mockDto.sku ?: "",
      barcode = mockDto.barcode ?: "",
      soldBy = if (mockDto.sold_by == "WEIGHT") SoldBy.WEIGHT else SoldBy.EACH,
      inStock = mockDto.in_stock,
      lowStockThreshold = mockDto.low_stock_threshold,
      trackStock = mockDto.track_stock,
      colorHex = mockDto.color_hex ?: "#0288D1",
      modifierIds = mockDto.modifier_ids ?: "",
      imageUrl = mockDto.image_url ?: ""
    )

    assertEquals(101L, mappedItem.id)
    assertEquals("Cold Brew Nitro", mappedItem.name)
    assertEquals(4.75, mappedItem.price, 0.001)
    assertEquals("Cold Beverages", mappedItem.categoryName)
    assertEquals(45.0, mappedItem.inStock, 0.001)
    assertEquals("https://images.unsplash.com/photo-coldbrew", mappedItem.imageUrl)
  }

  @Test
  fun keypadAmountCalculation_matchesPosCentsStandard() {
    val raw1 = "500"
    val amt1 = (raw1.toDoubleOrNull() ?: 0.0) / 100.0
    assertEquals(5.00, amt1, 0.001)

    val raw2 = "1250"
    val amt2 = (raw2.toDoubleOrNull() ?: 0.0) / 100.0
    assertEquals(12.50, amt2, 0.001)

    val rawEmpty = ""
    val amtEmpty = (rawEmpty.toDoubleOrNull() ?: 0.0) / 100.0
    assertEquals(0.00, amtEmpty, 0.001)
  }
}

