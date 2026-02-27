package ru.netology.nework.ui.post

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.location.Location
import com.yandex.mapkit.location.LocationListener
import com.yandex.mapkit.location.LocationStatus
import com.yandex.mapkit.map.CameraPosition
import ru.netology.nework.databinding.FragmentLocationPickerBinding

private const val LOCATION_REQUEST_KEY = "location_request"

class LocationPickerFragment : Fragment() {

    private var _binding: FragmentLocationPickerBinding? = null
    private val binding get() = _binding!!

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

        binding.btnConfirm.setOnClickListener {
            val target = binding.mapView.mapWindow.map.cameraPosition.target
            val bundle = Bundle().apply {
                putDouble("lat", target.latitude)
                putDouble("lng", target.longitude)
            }
            setFragmentResult(LOCATION_REQUEST_KEY, bundle)
            findNavController().navigateUp()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
        } else {
            moveToCurrentLocation()
        }
    }

    private fun moveToCurrentLocation() {
        val locationManager = MapKitFactory.getInstance().createLocationManager()
        locationManager.requestSingleUpdate(object : LocationListener {
            override fun onLocationUpdated(location: Location) {
                val point = location.position
                binding.mapView.map.move(
                    CameraPosition(point, 16.0f, 0.0f, 0.0f),
                    Animation(Animation.Type.SMOOTH, 1f),
                    null
                )
            }

            override fun onLocationStatusUpdated(status: LocationStatus) {
                // при необходимости обработать ошибки
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            moveToCurrentLocation()
        } else {
            Toast.makeText(requireContext(), "Нет разрешения на геолокацию", Toast.LENGTH_SHORT).show()
            val defaultPoint = Point(55.751244, 37.618423)
            binding.mapView.map.move(CameraPosition(defaultPoint, 10.0f, 0.0f, 0.0f))
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
        super.onDestroyView()
        _binding = null
    }
}