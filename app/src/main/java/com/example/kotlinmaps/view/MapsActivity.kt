package com.example.kotlinmaps.view

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.example.kotlinmaps.R

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.kotlinmaps.databinding.ActivityMapsBinding
import com.example.kotlinmaps.model.Place
import com.example.kotlinmaps.roomdb.PlaceDao
import com.example.kotlinmaps.roomdb.PlaceDatabase
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationManager : LocationManager
    private lateinit var locationListener : LocationListener
    private lateinit var permissionLauncher : ActivityResultLauncher<String>
    private lateinit var sharedPreferences: SharedPreferences
    private var trackBoolean : Boolean? = null
    private var selectedLatitude : Double? = null
    private var selectedLongitude : Double? = null
    private lateinit var db : PlaceDatabase
    private lateinit var placeDao: PlaceDao
    val compositeDisposable = CompositeDisposable()
    var placeFromMain : Place? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


        registerLauncher()

        sharedPreferences = this.getSharedPreferences("com.example.kotlinmaps", MODE_PRIVATE)
        trackBoolean = false

        selectedLatitude = 0.0
        selectedLongitude = 0.0

        db = Room.databaseBuilder(applicationContext, PlaceDatabase::class.java, "Places")

            .build()
        placeDao = db.placeDao()

        binding.saveButton.isEnabled = false

    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapLongClickListener(this)

        val intent = intent
        val info = intent.getStringExtra("info")

        //checking info is new or not, yeni save edilmis bir seydir, yoxsa yenisini yaratmaq isteyirik
        if (info == "new") {

            binding.saveButton.visibility = View.VISIBLE
            binding.deleteButton.visibility = View.GONE

            //casting
            locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager

            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    //bunu etdikde onLocationChanged 1defe cagirilir ve xeritede gezinende geriye qayitmir

                    trackBoolean = sharedPreferences.getBoolean("trackBoolean", false)
                    if (trackBoolean == false) {
                        val userLocation = LatLng(location.latitude, location.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                        sharedPreferences.edit().putBoolean("trackBoolean", true).apply()
                    }

                }

            }

//PERMISSION PART
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //rationale
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                    //request permission
                    Snackbar.make(binding.root, "Permission needed for location!", Snackbar.LENGTH_INDEFINITE).setAction("Give permission") {
                        //request permission from snacbar button
                        permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)

                    }.show()
                } else {
                    //request permission
                    permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)

                }


            } else {

                //permission granted
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
                val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastLocation != null) {
                    val lastUserLocation = LatLng(lastLocation.latitude, lastLocation.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation, 15f))
                }
                mMap.isMyLocationEnabled = true
            }

//PERMISSION PART

        } else {
            //old info ise, tikladiqda evvelki bilgiler gorsenir
            mMap.clear()

            placeFromMain = intent.getSerializableExtra("selectedPlace") as? Place

            placeFromMain?.let {

                val latlng = LatLng(it.latitude, it.longitude)
                mMap.addMarker(MarkerOptions().position(latlng).title(it.name))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 15f))

                binding.placeText.setText(it.name)
                binding.saveButton.visibility = View.GONE
                binding.deleteButton.visibility = View.VISIBLE

            }

        }


    }


    private fun registerLauncher() {

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if (result) {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    //permission granted
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
                    val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (lastLocation != null) {
                        val lastUserLocation = LatLng(lastLocation.latitude, lastLocation.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation, 15f))
                    }
                    mMap.isMyLocationEnabled = true

                }
            } else {
                //permission denied
                Toast.makeText(this@MapsActivity, "Permission needed!", Toast.LENGTH_LONG).show()
            }
        }
    }

    //Long click listener
    override fun onMapLongClick(p0: LatLng) {

        mMap.clear()
        mMap.addMarker(MarkerOptions().position(p0))

        selectedLatitude = p0.latitude
        selectedLongitude = p0.longitude

        binding.saveButton.isEnabled = true

    }


    fun save(view : View) {

        if (selectedLatitude != null && selectedLongitude != null) {
            val place = Place(binding.placeText.text.toString(), selectedLatitude!!, selectedLongitude!!)
            //RXJava part // handle response save etdikden sonra MainActivity'e intent eletdirir
            compositeDisposable.add(
                placeDao.insert(place)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponse)
            )
        }

    }

    private fun handleResponse() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    fun delete(view : View) {

        //eger yer null deyilse silinir, her ehtimala qarsi null safety edilir
        placeFromMain?.let {
            compositeDisposable.add(
                placeDao.delete(it)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponse)
            )
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }
}