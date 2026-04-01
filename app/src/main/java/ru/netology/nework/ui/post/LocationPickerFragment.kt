package ru.netology.nework.ui.post

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.location.Location
import com.yandex.mapkit.location.LocationListener
import com.yandex.mapkit.location.LocationManager
import com.yandex.mapkit.location.LocationStatus
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.runtime.image.ImageProvider
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentLocationPickerBinding

private const val LOCATION_REQUEST_KEY = "location_request"
private const val LOCATION_PERMISSION_REQUEST_CODE = 100

class LocationPickerFragment : Fragment() {

    private var _binding: FragmentLocationPickerBinding? = null
    private val binding: FragmentLocationPickerBinding
        get() = _binding ?: error("Binding accessed after view destroyed")

    private var locationManager: LocationManager? = null
    private var centerMarker: PlacemarkMapObject? = null
    private var userLocationMarker: PlacemarkMapObject? = null
    private var mapObjects: MapObjectCollection? = null
    private var currentUserLocation: Point? = null

    private val locationListener = object : LocationListener {
        override fun onLocationUpdated(location: Location) {
            val point = location.position
            currentUserLocation = point

            addUserLocationMarker(point)

            // Просто центрируем камеру на точке
            binding.mapView.map.move(
                CameraPosition(point, 16.0f, 0.0f, 0.0f),
                Animation(Animation.Type.SMOOTH, 1f),
                null
            )

            locationManager?.unsubscribe(this)
        }

        override fun onLocationStatusUpdated(status: LocationStatus) {
            when (status) {
                LocationStatus.AVAILABLE -> {
                    Log.d("LocationPicker", "Location available")
                }
                LocationStatus.NOT_AVAILABLE -> {
                    Toast.makeText(requireContext(), "Местоположение недоступно", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // RESET или другое состояние
                    Log.d("LocationPicker", "Other status: $status")
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMapObjects()
        setupZoomButtons()
        setupCameraListener()

        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnConfirm.setOnClickListener {
            val target = binding.mapView.mapWindow.map.cameraPosition.target
            val bundle = Bundle().apply {
                putDouble("lat", target.latitude)
                putDouble("lng", target.longitude)
            }
            setFragmentResult(LOCATION_REQUEST_KEY, bundle)
            findNavController().navigateUp()
        }

        binding.btnMyLocation.setOnClickListener {
            moveToCurrentLocation()
        }

        checkLocationPermission()
    }

    override fun onResume() {
        super.onResume()
        // Скрываем FAB при входе в фрагмент
        try {
            requireActivity().findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_create)?.hide()
            Log.d("LocationPickerFragment", "FAB hidden")
        } catch (e: Exception) {
            Log.e("LocationPickerFragment", "Error hiding FAB: ${e.message}")
        }
    }

    override fun onPause() {
        super.onPause()
        // Показываем FAB обратно при выходе
        try {
            requireActivity().findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_create)?.show()
            Log.d("LocationPickerFragment", "FAB shown")
        } catch (e: Exception) {
            Log.e("LocationPickerFragment", "Error showing FAB: ${e.message}")
        }
    }

    private fun setupMapObjects() {
        mapObjects = binding.mapView.map.mapObjects.addCollection()
    }

    private fun setupZoomButtons() {
        binding.btnZoomIn.setOnClickListener {
            val currentZoom = binding.mapView.map.cameraPosition.zoom
            binding.mapView.map.move(
                CameraPosition(
                    binding.mapView.map.cameraPosition.target,
                    currentZoom + 1,
                    0f,
                    0f
                ),
                Animation(Animation.Type.SMOOTH, 0.3f),
                null
            )
        }

        binding.btnZoomOut.setOnClickListener {
            val currentZoom = binding.mapView.map.cameraPosition.zoom
            binding.mapView.map.move(
                CameraPosition(
                    binding.mapView.map.cameraPosition.target,
                    currentZoom - 1,
                    0f,
                    0f
                ),
                Animation(Animation.Type.SMOOTH, 0.3f),
                null
            )
        }
    }

    private fun setupCameraListener() {
        binding.mapView.map.addCameraListener(object : com.yandex.mapkit.map.CameraListener {
            override fun onCameraPositionChanged(
                map: com.yandex.mapkit.map.Map,
                cameraPosition: CameraPosition,
                cameraUpdateReason: com.yandex.mapkit.map.CameraUpdateReason,
                finished: Boolean
            ) {
                if (finished) {
                    centerMarker?.geometry = cameraPosition.target
                }
            }
        })
    }

    private fun addUserLocationMarker(point: Point) {
        userLocationMarker?.let { mapObjects?.remove(it) }

        userLocationMarker = mapObjects?.addPlacemark(point)

        val userMarkerBitmap = createUserLocationBitmap()
        val imageProvider = ImageProvider.fromBitmap(userMarkerBitmap)

        userLocationMarker?.setIcon(imageProvider)
    }

    private fun createMarkerBitmap(): Bitmap {
        val size = 56
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = ContextCompat.getColor(requireContext(), R.color.purple_primary)
        paint.style = Paint.Style.FILL
        canvas.drawCircle((size / 2).toFloat(), (size / 2).toFloat(), (size / 2 - 4).toFloat(), paint)

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

        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawCircle((size / 2).toFloat(), (size / 2).toFloat(), (size / 2 - 4).toFloat(), paint)

        return bitmap
    }

    private fun createUserLocationBitmap(): Bitmap {
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

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            moveToCurrentLocation()
        }
    }

    private fun moveToCurrentLocation() {
        if (currentUserLocation != null) {
            // Просто центрируем камеру на текущем местоположении
            binding.mapView.map.move(
                CameraPosition(currentUserLocation!!, 16.0f, 0.0f, 0.0f),
                Animation(Animation.Type.SMOOTH, 0.5f),
                null
            )
        } else {
            locationManager = MapKitFactory.getInstance().createLocationManager()
            locationManager?.requestSingleUpdate(locationListener)
            Toast.makeText(requireContext(), "Определяем местоположение...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            moveToCurrentLocation()
        } else {
            if (_binding == null) return

            Toast.makeText(
                requireContext(),
                "Нет разрешения на геолокацию",
                Toast.LENGTH_SHORT
            ).show()

            val defaultPoint = Point(55.751244, 37.618423)
            binding.mapView.map.move(
                CameraPosition(defaultPoint, 10.0f, 0.0f, 0.0f)
            )
        }
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        binding.mapView.onStart()
    }

    override fun onStop() {
        binding.mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onDestroyView() {
        locationManager?.unsubscribe(locationListener)
        locationManager = null
        mapObjects?.clear()
        mapObjects = null
        centerMarker = null
        userLocationMarker = null
        _binding = null
        super.onDestroyView()
    }

}