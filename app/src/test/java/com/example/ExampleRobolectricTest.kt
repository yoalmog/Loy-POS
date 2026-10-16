package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DiningOption
import com.example.data.model.TicketItemEntity
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
    assertEquals("Loyverse POS", appName)
  }

  @Test
  fun `test ticket item calculations`() {
    val item = TicketItemEntity(
      ticketId = 1,
      itemId = 1,
      itemName = "Bistro Burger",
      unitPrice = 10.0,
      quantity = 2.0,
      modifiersCost = 1.5,
      discountPercent = 10.0
    )
    // Base = (10 + 1.5) * 2 = 23.0. Discount 10% = 2.3. Line total = 20.7
    assertEquals(20.7, item.lineTotal, 0.001)
  }

  @Test
  fun `test product entity fields`() {
    val product = com.example.data.model.Product(
      id = 1,
      name = "Deluxe Cheeseburger",
      price = 10.99,
      category = "Burgers",
      stockLevel = 25,
      imageUrl = "https://example.com/burger.jpg"
    )
    assertEquals("Deluxe Cheeseburger", product.name)
    assertEquals(10.99, product.price, 0.001)
    assertEquals("Burgers", product.category)
    assertEquals(25, product.stockLevel)
    assertEquals("https://example.com/burger.jpg", product.imageUrl)
  }

  @Test
  fun `test ticket entity fields and order naming`() {
    val ticket = com.example.data.model.Ticket(
      id = 10,
      name = "Patio Table 3",
      tableName = "Table 3",
      diningOption = "Dine in",
      serverName = "Alex",
      note = "VIP birthday group"
    )
    assertEquals("Patio Table 3", ticket.name)
    assertEquals("Table 3", ticket.tableName)
    assertEquals("Dine in", ticket.diningOption)
    assertEquals(false, ticket.isClosed)
  }

  @Test
  fun `test current order subtotal calculation`() {
    val state = com.example.ui.pos.CurrentOrderState(
      selectedItems = listOf(
        TicketItemEntity(ticketId = 1, itemId = 1, itemName = "Burger", unitPrice = 9.50, quantity = 2.0),
        TicketItemEntity(ticketId = 1, itemId = 2, itemName = "Fries", unitPrice = 4.25, quantity = 1.0)
      )
    )
    // 9.50 * 2 + 4.25 = 23.25
    assertEquals(23.25, state.subtotal, 0.001)
  }
}
