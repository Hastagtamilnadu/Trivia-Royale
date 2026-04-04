package com.triviaroyale.billing

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.triviaroyale.TriviaRoyaleApplication
import com.triviaroyale.analytics.ClanAnalytics
import com.triviaroyale.firebase.FirebaseCloudRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Manages Google Play Billing for crown pack in-app purchases.
 *
 * Flow:
 * 1. Client calls launchPurchaseFlow(activity, productId)
 * 2. BillingClient shows the Google Play purchase sheet
 * 3. On success, we send the purchase token to the backend (purchaseCrowns callable)
 * 4. Backend verifies with Google Play Developer API and grants crowns
 * 5. Client acknowledges the purchase
 *
 * Placeholder product IDs are used — replace with actual Play Console IDs.
 */
class BillingManager(
    private val cloudRepository: FirebaseCloudRepository,
) {
    companion object {
        private const val TAG = "BillingManager"

        /** Placeholder product IDs — replace with actual Play Console product IDs. */
        val PRODUCT_ID_MAP = mapOf(
            "crowns_80" to "crowns_80",
            "crowns_220" to "crowns_220",
            "crowns_600" to "crowns_600",
            "crowns_1300" to "crowns_1300",
        )

        private val CROWNS_BY_PACK = mapOf(
            "crowns_80" to 80,
            "crowns_220" to 220,
            "crowns_600" to 600,
            "crowns_1300" to 1300,
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Deferred that resolves when the current purchase flow completes. */
    private var pendingPurchase: CompletableDeferred<PurchaseResult>? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        scope.launch {
            handlePurchasesUpdated(billingResult, purchases)
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(TriviaRoyaleApplication.appContext)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    data class PurchaseResult(
        val success: Boolean,
        val newBalance: Int = 0,
        val errorMessage: String? = null,
    )

    /** Connects to Google Play Billing. Must be called before any purchase. */
    suspend fun ensureConnected(): Boolean {
        if (billingClient.isReady) return true

        return suspendCancellableCoroutine { cont ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "Billing connected")
                        cont.resume(true)
                    } else {
                        Log.w(TAG, "Billing connection failed: ${billingResult.debugMessage}")
                        cont.resume(false)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Log.w(TAG, "Billing service disconnected")
                }
            })
        }
    }

    /** Query product details using the callback API, wrapped in a coroutine. */
    private suspend fun queryProductDetailsSuspend(
        params: QueryProductDetailsParams,
    ): Pair<BillingResult, List<ProductDetails>?> {
        return suspendCancellableCoroutine { cont ->
            billingClient.queryProductDetailsAsync(params,
                ProductDetailsResponseListener { billingResult, productDetailsList ->
                    cont.resume(Pair(billingResult, productDetailsList))
                }
            )
        }
    }

    /** Acknowledge a purchase using the callback API, wrapped in a coroutine. */
    private suspend fun acknowledgePurchaseSuspend(
        params: AcknowledgePurchaseParams,
    ): BillingResult {
        return suspendCancellableCoroutine { cont ->
            billingClient.acknowledgePurchase(params,
                AcknowledgePurchaseResponseListener { billingResult ->
                    cont.resume(billingResult)
                }
            )
        }
    }

    /**
     * Launches the purchase flow for a crown pack.
     *
     * @param activity The activity to host the purchase sheet
     * @param packId The internal pack ID (e.g. "crowns_80")
     * @return PurchaseResult with the outcome
     */
    suspend fun launchPurchaseFlow(activity: Activity, packId: String): PurchaseResult {
        val googleProductId = PRODUCT_ID_MAP[packId]
            ?: return PurchaseResult(false, errorMessage = "Unknown product: $packId")

        if (!ensureConnected()) {
            return PurchaseResult(false, errorMessage = "Could not connect to Google Play Billing.")
        }

        // Query product details
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(googleProductId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val (detailsBillingResult, productDetailsList) = queryProductDetailsSuspend(params)
        if (detailsBillingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            return PurchaseResult(false, errorMessage = "Could not query product details: ${detailsBillingResult.debugMessage}")
        }

        val productDetails = productDetailsList?.firstOrNull()
            ?: return PurchaseResult(false, errorMessage = "Product not found in Play Store: $googleProductId")

        // Set up the deferred for the async purchase callback
        pendingPurchase = CompletableDeferred()

        // Launch billing flow
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )
            )
            .build()

        val flowResult = billingClient.launchBillingFlow(activity, flowParams)
        if (flowResult.responseCode != BillingClient.BillingResponseCode.OK) {
            pendingPurchase?.complete(PurchaseResult(false, errorMessage = "Could not launch purchase: ${flowResult.debugMessage}"))
        }

        // Wait for the async callback
        return pendingPurchase!!.await()
    }

    private suspend fun handlePurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>?,
    ) {
        val deferred = pendingPurchase ?: return
        pendingPurchase = null

        if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            deferred.complete(PurchaseResult(false, errorMessage = "Purchase cancelled."))
            return
        }

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK || purchases.isNullOrEmpty()) {
            deferred.complete(PurchaseResult(false, errorMessage = "Purchase failed: ${billingResult.debugMessage}"))
            return
        }

        val purchase = purchases.first()

        // Verify with backend — send the purchase token for server-side validation
        try {
            val packId = PRODUCT_ID_MAP.entries
                .firstOrNull { it.value == purchase.products.firstOrNull() }
                ?.key ?: "unknown"

            val newBalance = cloudRepository.purchaseCrowns(
                productId = packId,
                purchaseToken = purchase.purchaseToken,
            )

            // Acknowledge the purchase after backend verification
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                !purchase.isAcknowledged
            ) {
                val ackParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                acknowledgePurchaseSuspend(ackParams)
            }

            val crowns = CROWNS_BY_PACK[packId] ?: 0
            ClanAnalytics.logCrownPackPurchased(packId, crowns)

            deferred.complete(PurchaseResult(true, newBalance = newBalance))
        } catch (e: Exception) {
            Log.e(TAG, "Backend verification failed", e)
            deferred.complete(PurchaseResult(false, errorMessage = e.message ?: "Verification failed."))
        }
    }

    /**
     * Check for any unacknowledged purchases on startup.
     * Call this from the main activity to handle purchases that weren't
     * acknowledged due to app crashes or network issues.
     */
    fun checkPendingPurchases() {
        if (!billingClient.isReady) return

        scope.launch {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
            billingClient.queryPurchasesAsync(params,
                PurchasesResponseListener { billingResult, purchasesList ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        for (purchase in purchasesList) {
                            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                                Log.w(TAG, "Found unacknowledged purchase: ${purchase.orderId}")
                                scope.launch {
                                    handlePurchasesUpdated(billingResult, listOf(purchase))
                                }
                            }
                        }
                    }
                }
            )
        }
    }

    fun destroy() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }
}
