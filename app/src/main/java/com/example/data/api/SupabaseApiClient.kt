package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// Data Transfer Objects matching Supabase PostgreSQL Schema
data class SupabaseItemDto(
    val id: Long? = null,
    val name: String,
    val category_id: Long? = null,
    val category_name: String? = null,
    val price: Double,
    val cost: Double? = 0.0,
    val sku: String? = "",
    val barcode: String? = "",
    val sold_by: String? = "EACH",
    val in_stock: Double = 0.0,
    val low_stock_threshold: Double = 5.0,
    val track_stock: Boolean = true,
    val color_hex: String? = "#0288D1",
    val modifier_ids: String? = ""
)

data class SupabaseCategoryDto(
    val id: Long? = null,
    val name: String,
    val color_hex: String? = "#0288D1"
)

data class SupabaseCustomerDto(
    val id: Long? = null,
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val customer_code: String? = null,
    val points_balance: Double = 0.0,
    val total_visits: Int = 0,
    val total_spent: Double = 0.0,
    val note: String? = null
)

data class SupabaseReceiptDto(
    val id: String,
    val receipt_number: String,
    val created_at: String? = null,
    val shift_id: Long? = null,
    val cashier_name: String = "Cashier",
    val ticket_name: String? = null,
    val dining_option: String? = "Dine in",
    val subtotal: Double = 0.0,
    val tax_amount: Double = 0.0,
    val discount_amount: Double = 0.0,
    val tip_amount: Double = 0.0,
    val total_amount: Double = 0.0,
    val payment_type: String = "CASH",
    val payment_details_json: String? = null,
    val customer_id: Long? = null,
    val customer_name: String? = null,
    val is_refunded: Boolean = false,
    val items_summary_json: String? = null,
    val idempotency_key: String? = null
)

data class SupabaseShiftDto(
    val id: Long? = null,
    val opened_at: String? = null,
    val closed_at: String? = null,
    val cashier_name: String,
    val starting_cash: Double = 0.0,
    val cash_payments: Double = 0.0,
    val cash_refunds: Double = 0.0,
    val cash_paid_in: Double = 0.0,
    val cash_paid_out: Double = 0.0,
    val card_payments: Double = 0.0,
    val actual_cash_counted: Double? = null,
    val is_open: Boolean = true
)

interface SupabaseRestApi {

    @GET("rest/v1/items")
    suspend fun getItems(@Query("select") select: String = "*"): List<SupabaseItemDto>

    @POST("rest/v1/items")
    @Headers("Prefer: return=representation, resolution=merge-duplicates")
    suspend fun upsertItems(@Body items: List<SupabaseItemDto>): Response<List<SupabaseItemDto>>

    @GET("rest/v1/categories")
    suspend fun getCategories(@Query("select") select: String = "*"): List<SupabaseCategoryDto>

    @POST("rest/v1/categories")
    @Headers("Prefer: return=representation, resolution=merge-duplicates")
    suspend fun upsertCategories(@Body categories: List<SupabaseCategoryDto>): Response<List<SupabaseCategoryDto>>

    @GET("rest/v1/customers")
    suspend fun getCustomers(@Query("select") select: String = "*"): List<SupabaseCustomerDto>

    @POST("rest/v1/customers")
    @Headers("Prefer: return=representation, resolution=merge-duplicates")
    suspend fun upsertCustomers(@Body customers: List<SupabaseCustomerDto>): Response<List<SupabaseCustomerDto>>

    @GET("rest/v1/receipts")
    suspend fun getReceipts(@Query("select") select: String = "*"): List<SupabaseReceiptDto>

    @POST("rest/v1/receipts")
    @Headers("Prefer: return=representation, resolution=merge-duplicates")
    suspend fun postReceipt(@Body receipt: SupabaseReceiptDto): Response<List<SupabaseReceiptDto>>

    @GET("rest/v1/shifts")
    suspend fun getShifts(@Query("select") select: String = "*"): List<SupabaseShiftDto>

    @POST("rest/v1/shifts")
    @Headers("Prefer: return=representation, resolution=merge-duplicates")
    suspend fun upsertShift(@Body shift: SupabaseShiftDto): Response<List<SupabaseShiftDto>>
}

object SupabaseApiClient {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private fun getBaseUrl(): String {
        val url = try {
            BuildConfig::class.java.getField("SUPABASE_URL").get(null) as? String
        } catch (e: Exception) {
            null
        }
        return if (!url.isNullOrEmpty() && url.startsWith("http")) {
            if (url.endsWith("/")) url else "$url/"
        } else {
            "https://placeholder-project.supabase.co/"
        }
    }

    private fun getAnonKey(): String {
        return try {
            (BuildConfig::class.java.getField("SUPABASE_ANON_KEY").get(null) as? String) ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val apiKey = getAnonKey()
        val requestBuilder = original.newBuilder()
            .header("apikey", apiKey)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
        chain.proceed(requestBuilder.build())
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    val api: SupabaseRestApi by lazy {
        Retrofit.Builder()
            .baseUrl(getBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SupabaseRestApi::class.java)
    }

    fun isConfigured(): Boolean {
        val url = getBaseUrl()
        val key = getAnonKey()
        return !url.contains("placeholder-project") && key.isNotEmpty()
    }
}
