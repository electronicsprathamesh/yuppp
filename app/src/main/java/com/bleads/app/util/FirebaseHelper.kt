package com.bleads.app.util

import android.util.Log
import com.bleads.app.data.Campaign
import com.bleads.app.data.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirebaseHelper {
    private val db = FirebaseFirestore.getInstance()
    private val campaignsCollection = db.collection("campaigns")
    private val logsCollection = db.collection("logs")

    companion object {
        private const val TAG = "FirebaseHelper"
    }

    /**
     * Check if a UUID has an active campaign
     */
    suspend fun getCampaignByUuid(uuid: String): Campaign? {
        return try {
            val normalizedUuid = uuid.lowercase().trim()
            val querySnapshot = campaignsCollection
                .whereEqualTo("uuid", normalizedUuid)
                .whereEqualTo("isActive", true)
                .limit(1)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val document = querySnapshot.documents[0]
                Campaign(
                    id = document.id,
                    name = document.getString("name") ?: "",
                    description = document.getString("description") ?: "",
                    website = document.getString("website") ?: "",
                    uuid = document.getString("uuid") ?: "",
                    isActive = document.getBoolean("isActive") ?: true
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting campaign by UUID: ${e.message}", e)
            null
        }
    }

    /**
     * Log notification event to Firestore
     */
    suspend fun logNotification(
        user: User,
        beaconName: String,
        campaignName: String,
        distance: Double? = null
    ): Boolean {
        return try {
            val logData = hashMapOf(
                "userName" to user.name,
                "userPhone" to user.phone,
                "beaconName" to beaconName,
                "campaignName" to campaignName,
                "timestamp" to com.google.firebase.Timestamp.now(),
                "distance" to (distance ?: 0.0)
            )

            logsCollection.add(logData).await()
            Log.d(TAG, "Notification logged successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error logging notification: ${e.message}", e)
            false
        }
    }
}
