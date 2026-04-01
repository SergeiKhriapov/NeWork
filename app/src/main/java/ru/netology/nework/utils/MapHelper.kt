package ru.netology.nework.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.ContextCompat
import com.yandex.mapkit.Animation
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import ru.netology.nework.R

private const val TAG = "MapHelper"
private const val DEFAULT_ZOOM = 16f

/**
 * Вспомогательный класс для работы с Яндекс Картами
 */
object MapHelper {

    /**
     * Создаёт карту и добавляет маркер в указанную точку
     */
    fun createStaticMap(
        context: Context,
        mapContainer: android.view.ViewGroup,
        lat: Double,
        lng: Double,
        zoom: Float = DEFAULT_ZOOM
    ): MapView? {
        mapContainer.removeAllViews()

        val mapView = MapView(context).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            // Отключаем взаимодействие с картой
            isClickable = false
            isFocusable = false
            isEnabled = false
        }

        mapContainer.addView(mapView)

        val point = Point(lat, lng)

        mapView.map.move(
            CameraPosition(point, zoom, 0f, 0f),
            Animation(Animation.Type.SMOOTH, 0.5f),
            null
        )

        addMarker(mapView.map, point, context)

        return mapView
    }

    /**
     * Добавляет маркер на карту
     */
    fun addMarker(map: Map, point: Point, context: Context): PlacemarkMapObject? {
        return try {
            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_map_pin)
            val bitmap = drawableToBitmap(drawable)
            val imageProvider = ImageProvider.fromBitmap(bitmap)
            val placemark = map.mapObjects.addPlacemark(point, imageProvider)
            placemark?.setOpacity(1.0f)
            placemark
        } catch (e: Exception) {
            Log.e(TAG, "Error creating marker", e)
            map.mapObjects.addPlacemark(point)
        }
    }

    /**
     * Добавляет маркер пользователя (синий кружок)
     */
    fun addUserMarker(map: Map, point: Point, context: Context): PlacemarkMapObject? {
        val userMarkerBitmap = createUserLocationBitmap(context)
        val imageProvider = ImageProvider.fromBitmap(userMarkerBitmap)
        val placemark = map.mapObjects.addPlacemark(point, imageProvider)
        placemark?.setOpacity(1.0f)
        return placemark
    }

    /**
     * Создаёт коллекцию для маркеров
     */
    fun createMapObjectsCollection(map: Map): MapObjectCollection {
        return map.mapObjects.addCollection()
    }

    /**
     * Очищает коллекцию маркеров
     */
    fun clearMapObjects(mapObjects: MapObjectCollection?) {
        mapObjects?.clear()
    }

    /**
     * Конвертирует Drawable в Bitmap
     */
    private fun drawableToBitmap(drawable: Drawable?): Bitmap {
        val width = drawable?.intrinsicWidth?.takeIf { it > 0 } ?: 48
        val height = drawable?.intrinsicHeight?.takeIf { it > 0 } ?: 48
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable?.setBounds(0, 0, canvas.width, canvas.height)
        drawable?.draw(canvas)
        return bitmap
    }

    /**
     * Создаёт иконку для маркера пользователя (синий кружок)
     */
    private fun createUserLocationBitmap(context: Context): Bitmap {
        val size = 40
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = Color.parseColor("#2196F3")
        paint.style = Paint.Style.FILL
        canvas.drawCircle((size / 2).toFloat(), (size / 2).toFloat(), (size / 2 - 2).toFloat(), paint)

        paint.color = Color.WHITE
        canvas.drawCircle((size / 2).toFloat(), (size / 2).toFloat(), (size / 2 - 6).toFloat(), paint)

        paint.color = Color.parseColor("#2196F3")
        canvas.drawCircle((size / 2).toFloat(), (size / 2).toFloat(), (size / 2 - 10).toFloat(), paint)

        return bitmap
    }

    /**
     * Создаёт маркер с галочкой (фиолетовый)
     */
    fun createCheckMarkerBitmap(context: Context): Bitmap {
        val size = 56
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Круг
        paint.color = ContextCompat.getColor(context, R.color.purple_primary)
        paint.style = Paint.Style.FILL
        canvas.drawCircle((size / 2).toFloat(), (size / 2).toFloat(), (size / 2 - 4).toFloat(), paint)

        // Галочка
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND

        val startX = size / 2 - 10f
        val startY = size / 2 + 4f
        val midX = size / 2f
        val midY = size / 2 + 12f
        val endX = size / 2 + 12f
        val endY = size / 2 - 6f

        val path = android.graphics.Path()
        path.moveTo(startX, startY)
        path.lineTo(midX, midY)
        path.lineTo(endX, endY)
        canvas.drawPath(path, paint)

        // Обводка
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawCircle((size / 2).toFloat(), (size / 2).toFloat(), (size / 2 - 4).toFloat(), paint)

        return bitmap
    }
}