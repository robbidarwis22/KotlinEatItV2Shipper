package com.example.kotlineatitv2shipper

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.example.kotlineatitv2shipper.common.Common
import com.example.kotlineatitv2shipper.common.LatLngInterpolator
import com.example.kotlineatitv2shipper.common.MarkerAnimation
import com.example.kotlineatitv2shipper.model.ShippingOrderModel
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import io.paperdb.Paper
import kotlinx.android.synthetic.main.activity_shipping.*
import java.text.SimpleDateFormat

class ShippingActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    
    private var shipperMarker: Marker?=null
    private var shippingOrderModel: ShippingOrderModel?=null
    
    var isInit = false
    var previousLocation: Location?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shipping)

        buildLocationRequest()
        buildLocationCallback()

        setShippingOrderModel()

        Dexter.withActivity(this)
            .withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object:PermissionListener{
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                    val mapFragment = supportFragmentManager
                        .findFragmentById(R.id.map) as SupportMapFragment
                    mapFragment.getMapAsync(this@ShippingActivity)

                    fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this@ShippingActivity)
                    if (ActivityCompat.checkSelfPermission(
                            this@ShippingActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            this@ShippingActivity,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return
                    }
                    fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback,
                        Looper.myLooper())
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest?,
                    token: PermissionToken?
                ) {

                }

                override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                    Toast.makeText(this@ShippingActivity,"You must enable this permission",Toast.LENGTH_SHORT).show()
                }

            }).check()


    }

    private fun buildLocationCallback() {
        locationCallback = object:LocationCallback(){
            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)
                val locationShipper = LatLng(p0!!.lastLocation.latitude,
                    p0!!.lastLocation.longitude)
                if (shipperMarker == null)
                {
                    val height = 80
                    val width = 80
                    val bitmapDrawable = ContextCompat.getDrawable(this@ShippingActivity,
                        R.drawable.shipper)
                    val b = bitmapDrawable!!.toBitmap()
                    val smallMarker = Bitmap.createScaledBitmap(b,width,height,false)
                    shipperMarker = mMap!!.addMarker(MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromBitmap(smallMarker))
                        .position(locationShipper)
                        .title("You"))

                    mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper,15f))
                }
                else
                {
                    shipperMarker!!.position = locationShipper
                }

                if (isInit && previousLocation != null)
                {
                    val previousLocationLatLng = LatLng(previousLocation!!.latitude,previousLocation!!.longitude)
                    MarkerAnimation.animateMarkerToGB(shipperMarker!!,locationShipper,LatLngInterpolator.Spherical())
                    shipperMarker!!.rotation = Common.getBearing(previousLocationLatLng,locationShipper)
                    mMap!!.animateCamera(CameraUpdateFactory.newLatLng(locationShipper))

                    previousLocation = p0.lastLocation
                }
                if (!isInit)
                {
                    isInit = true
                    previousLocation = p0.lastLocation
                }
            }
        }
    }

    private fun setShippingOrderModel() {
        Paper.init(this)
        val data = Paper.book().read<String>(Common.SHIPPING_DATA)
        if (!TextUtils.isEmpty(data))
        {
            shippingOrderModel = Gson()
                .fromJson<ShippingOrderModel>(data,object:TypeToken<ShippingOrderModel>(){}.type)

            if (shippingOrderModel != null)
            {
                Common.setSpanStringColor("Name: ",
                    shippingOrderModel!!.orderModel!!.userName,
                    txt_name,
                    Color.parseColor("#333639"))

                Common.setSpanStringColor("Address: ",
                    shippingOrderModel!!.orderModel!!.shippingAddress,
                    txt_address,
                    Color.parseColor("#673ab7"))

                Common.setSpanStringColor("No.: ",
                    shippingOrderModel!!.orderModel!!.key,
                    txt_order_number,
                    Color.parseColor("#795548"))

                txt_date!!.text = StringBuilder().append(SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(
                    shippingOrderModel!!.orderModel!!.createDate
                ))

                Glide.with(this)
                    .load(shippingOrderModel!!.orderModel!!.cartItemList!![0]!!.foodImage)
                    .into(img_food_image)
            }
        }
        else
        {
            Toast.makeText(this,"Shipping Order Model is null",Toast.LENGTH_SHORT).show()
        }
    }



    private fun buildLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.setInterval(15000); //15sec
        locationRequest.setFastestInterval(10000) //10 sec
        locationRequest.setSmallestDisplacement(20f)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap!!.uiSettings.isZoomControlsEnabled = true
        try {
            val success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this,R.raw.uber_light_with_label))
            if (!success)
                Log.d("Darwis","Failed to load map style")
        }catch (ex:Resources.NotFoundException)
        {
            Log.d("Darwis","Not found json string for map style")
        }
    }

    override fun onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }
}