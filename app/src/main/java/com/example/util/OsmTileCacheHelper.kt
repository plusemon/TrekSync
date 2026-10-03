package com.example.util

import android.content.Context
import android.util.Log
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView

/**
 * Offline Tile Caching Guide & Helper for Remote Expeditions.
 *
 * This utility manages offline tile pre-fetching using osmdroid's [CacheManager].
 * It allows expedition leaders to download full topographic map tiles for a designated
 * trail bounding box prior to departing for zero-connectivity backcountry zones.
 */
object OsmTileCacheHelper {

    private const val TAG = "OsmTileCacheHelper"

    interface CacheDownloadCallback {
        fun onProgress(percentage: Int, current: Int, total: Int)
        fun onComplete(totalTilesDownloaded: Int)
        fun onError(errorMessage: String)
    }

    /**
     * Calculates a bounding box around a center coordinate with a given radius in kilometers.
     */
    fun createBoundingBox(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double = 10.0
    ): BoundingBox {
        val latOffset = radiusKm / 111.0
        val lonOffset = radiusKm / (111.0 * kotlin.math.cos(Math.toRadians(centerLat)))
        return BoundingBox(
            centerLat + latOffset,
            centerLon + lonOffset,
            centerLat - latOffset,
            centerLon - lonOffset
        )
    }

    /**
     * Estimates the total number of tiles required for a given bounding box across zoom levels.
     *
     * @param mapView Active MapView instance with loaded tile source
     * @param boundingBox Geographical boundary of expedition area
     * @param minZoom Minimum overview zoom (e.g. 12)
     * @param maxZoom Maximum detailed contour zoom (e.g. 16)
     */
    fun estimateTileCount(
        mapView: MapView,
        boundingBox: BoundingBox,
        minZoom: Int = 12,
        maxZoom: Int = 16
    ): Int {
        return try {
            val cacheManager = CacheManager(mapView)
            cacheManager.possibleTilesInArea(boundingBox, minZoom, maxZoom)
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating tile count: ${e.message}")
            0
        }
    }

    /**
     * Asynchronously downloads and caches map tiles for offline expedition use.
     *
     * @param context Application context
     * @param mapView Active MapView instance
     * @param boundingBox Geographical bounding box to download
     * @param minZoom Minimum zoom level
     * @param maxZoom Maximum zoom level
     * @param callback Progress and completion callback
     */
    fun downloadBoundingBoxOffline(
        context: Context,
        mapView: MapView,
        boundingBox: BoundingBox,
        minZoom: Int = 12,
        maxZoom: Int = 16,
        callback: CacheDownloadCallback
    ) {
        val cacheManager = CacheManager(mapView)
        val totalTiles = cacheManager.possibleTilesInArea(boundingBox, minZoom, maxZoom)

        if (totalTiles <= 0) {
            callback.onError("No tiles to download for the selected region.")
            return
        }

        cacheManager.downloadAreaAsync(
            context,
            boundingBox,
            minZoom,
            maxZoom,
            object : CacheManager.CacheManagerCallback {
                override fun downloadStarted() {
                    Log.i(TAG, "Tile download started")
                }

                override fun setPossibleTilesInArea(total: Int) {
                    Log.d(TAG, "Possible tiles to download: $total")
                }

                override fun onTaskComplete() {
                    Log.i(TAG, "Offline tile download complete ($totalTiles tiles)")
                    callback.onComplete(totalTiles)
                }

                override fun updateProgress(progress: Int, current: Int, total: Int, errors: Int) {
                    val pct = if (total > 0) ((current.toFloat() / total) * 100).toInt() else progress
                    callback.onProgress(pct, current, total)
                }

                override fun onTaskFailed(errors: Int) {
                    Log.e(TAG, "Tile download failed with $errors errors.")
                    callback.onError("Tile caching failed ($errors errors).")
                }
            }
        )
    }

    /**
     * Clears all cached tiles in a designated bounding box.
     */
    fun cleanBoundingBoxCache(
        context: Context,
        mapView: MapView,
        boundingBox: BoundingBox,
        minZoom: Int = 12,
        maxZoom: Int = 16
    ) {
        try {
            val cacheManager = CacheManager(mapView)
            cacheManager.cleanAreaAsync(context, boundingBox, minZoom, maxZoom)
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning area: ${e.message}")
        }
    }
}
