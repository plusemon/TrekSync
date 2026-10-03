package com.example.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import java.io.File
import kotlin.math.cos

/**
 * State representing an active or completed offline tile cache operation.
 */
sealed class OfflineCacheState {
    object Idle : OfflineCacheState()
    data class Downloading(val progressPercent: Int, val currentTiles: Int, val totalTiles: Int) : OfflineCacheState()
    data class Success(val totalTilesDownloaded: Int, val boundingBox: BoundingBox) : OfflineCacheState()
    data class Error(val message: String, val errorsCount: Int = 0) : OfflineCacheState()
}

/**
 * OfflineMapManager handles tile caching, storage estimation, and bounding-box
 * pre-fetching for remote backcountry expeditions with zero cellular connectivity.
 */
class OfflineMapManager(
    private val context: Context
) {

    companion object {
        private const val TAG = "OfflineMapManager"
        private const val AVERAGE_TILE_SIZE_BYTES = 25 * 1024L // Approx 25 KB per tile

        @Volatile
        private var INSTANCE: OfflineMapManager? = null

        fun getInstance(context: Context): OfflineMapManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OfflineMapManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val _cacheState = MutableStateFlow<OfflineCacheState>(OfflineCacheState.Idle)
    val cacheState: StateFlow<OfflineCacheState> = _cacheState.asStateFlow()

    private var activeCacheTask: CacheManager.CacheManagerTask? = null

    /**
     * Builds a geographical [BoundingBox] around a center coordinate with a specified radius.
     */
    fun createBoundingBox(centerLat: Double, centerLon: Double, radiusKm: Double): BoundingBox {
        val latOffset = radiusKm / 111.0
        val lonOffset = radiusKm / (111.0 * cos(Math.toRadians(centerLat)))
        return BoundingBox(
            centerLat + latOffset,
            centerLon + lonOffset,
            centerLat - latOffset,
            centerLon - lonOffset
        )
    }

    /**
     * Estimates the total number of tiles within the specified bounding box for zoom range.
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
            Log.e(TAG, "Failed to calculate tile count estimate: ${e.message}")
            0
        }
    }

    /**
     * Estimates disk storage in MegaBytes for a given tile count.
     */
    fun estimateStorageSizeMb(tileCount: Int): Double {
        val bytes = tileCount * AVERAGE_TILE_SIZE_BYTES
        return bytes / (1024.0 * 1024.0)
    }

    /**
     * Asynchronously pre-caches an expedition bounding box for offline navigation.
     *
     * @param mapView Active MapView containing current tile source
     * @param boundingBox Geographical area to pre-cache
     * @param minZoom Minimum overview zoom (default 12)
     * @param maxZoom Maximum detailed contour zoom (default 16)
     * @param onComplete Optional callback on download completion
     */
    fun preCacheBoundingBox(
        mapView: MapView,
        boundingBox: BoundingBox,
        minZoom: Int = 12,
        maxZoom: Int = 16,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val cacheManager = CacheManager(mapView)
        val totalTiles = cacheManager.possibleTilesInArea(boundingBox, minZoom, maxZoom)

        if (totalTiles <= 0) {
            _cacheState.value = OfflineCacheState.Error("No tiles available to cache in this region.")
            onComplete?.invoke(false)
            return
        }

        _cacheState.value = OfflineCacheState.Downloading(0, 0, totalTiles)

        activeCacheTask = cacheManager.downloadAreaAsync(
            context,
            boundingBox,
            minZoom,
            maxZoom,
            object : CacheManager.CacheManagerCallback {
                override fun downloadStarted() {
                    Log.i(TAG, "Offline pre-caching task started for $totalTiles tiles.")
                }

                override fun setPossibleTilesInArea(total: Int) {
                    _cacheState.value = OfflineCacheState.Downloading(0, 0, total)
                }

                override fun onTaskComplete() {
                    Log.i(TAG, "Offline pre-caching complete ($totalTiles tiles).")
                    _cacheState.value = OfflineCacheState.Success(totalTiles, boundingBox)
                    activeCacheTask = null
                    onComplete?.invoke(true)
                }

                override fun updateProgress(progress: Int, current: Int, total: Int, errors: Int) {
                    val pct = if (total > 0) ((current.toFloat() / total) * 100).toInt() else progress
                    _cacheState.value = OfflineCacheState.Downloading(pct, current, total)
                }

                override fun onTaskFailed(errors: Int) {
                    Log.e(TAG, "Offline pre-caching task failed with $errors errors.")
                    _cacheState.value = OfflineCacheState.Error("Pre-cache interrupted with $errors failed tiles.", errors)
                    activeCacheTask = null
                    onComplete?.invoke(false)
                }
            }
        )
    }

    /**
     * Pre-caches an area defined by center GPS coordinates and a radius in kilometers.
     */
    fun preCacheRadius(
        mapView: MapView,
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double = 10.0,
        minZoom: Int = 12,
        maxZoom: Int = 16,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val bbox = createBoundingBox(centerLat, centerLon, radiusKm)
        preCacheBoundingBox(mapView, bbox, minZoom, maxZoom, onComplete)
    }

    /**
     * Cancels an ongoing pre-cache operation.
     */
    fun cancelActivePreCache() {
        activeCacheTask?.let {
            try {
                it.cancel(true)
                _cacheState.value = OfflineCacheState.Idle
                Log.i(TAG, "Offline pre-caching cancelled by user.")
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling pre-cache task: ${e.message}")
            }
        }
        activeCacheTask = null
    }

    /**
     * Calculates the total size of cached osmdroid tiles currently stored on disk in bytes.
     */
    fun getCachedStorageBytes(): Long {
        val cacheDir = Configuration.getInstance().osmdroidTileCache ?: File(context.cacheDir, "osmdroid/tiles")
        return calculateDirSize(cacheDir)
    }

    /**
     * Clears all cached offline map tiles.
     */
    fun clearAllOfflineTiles() {
        val cacheDir = Configuration.getInstance().osmdroidTileCache ?: File(context.cacheDir, "osmdroid/tiles")
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
        }
        _cacheState.value = OfflineCacheState.Idle
    }

    private fun calculateDirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) calculateDirSize(file) else file.length()
        }
        return size
    }
}
