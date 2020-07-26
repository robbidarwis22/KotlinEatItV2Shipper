package com.example.kotlineatitv2shipper

import android.Manifest
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.example.kotlineatitv2shipper.common.Common
import com.example.kotlineatitv2shipper.model.ShippingOrderModel
import com.example.kotlineatitv2shipper.remote.IGoogleApi
import com.example.kotlineatitv2shipper.remote.RetrofitClient
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
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_shipping.*
import org.json.JSONObject
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

    private var handler: Handler?=null
    private var index:Int = -1
    private var next:Int=0
    private var startPosition:LatLng?= LatLng(0.0,0.0)
    private var endPosition:LatLng?=LatLng(0.0,0.0)
    private var v:Float = 0f
    private var lat:Double=-1.0
    private var lng:Double=-1.0

    private var blackPolyline:Polyline?=null
    private var greyPolyline:Polyline?=null
    private var polylineOptions:PolylineOptions?=null
    private var blackPolylineOptions:PolylineOptions?=null

    private var polylineList:List<LatLng> = ArrayList<LatLng>()
    private var iGoogleApi:IGoogleApi?=null
    private var compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shipping)

        iGoogleApi = RetrofitClient.instance!!.create(IGoogleApi::class.java)

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

                    mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper,18f))
                }


                if (isInit && previousLocation != null)
                {
                    val from = StringBuilder()
                        .append(previousLocation!!.latitude)
                        .append(",")
                        .append(previousLocation!!.longitude)
                    val to = StringBuilder()
                        .append(locationShipper.latitude)
                        .append(",")
                        .append(locationShipper.longitude)

                    moveMarkerAnimation(shipperMarker,from,to)

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

    private fun moveMarkerAnimation(
        marker: Marker?,
        from: java.lang.StringBuilder,
        to: java.lang.StringBuilder
    ) {
        compositeDisposable.add(iGoogleApi!!.getDirections("driving",
        "less_driving",
        from.toString(),
        to.toString(),
        getString(R.string.google_maps_key))!!
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ s->
                Log.d("DEBUG",s)
                try {
                    val jsonObject = JSONObject(s)
                    val jsonArray = jsonObject.getJSONArray("routes")
                    for (i in 0 until jsonArray.length())
                    {
                        val route = jsonArray.getJSONObject(i)
                        val poly = route.getJSONObject("overview_polyline")
                        val polyline = poly.getString("points")
                        polylineList = Common.decodePoly(polyline)
                    }

                    polylineOptions = PolylineOptions()
                    polylineOptions!!.color(Color.GRAY)
                    polylineOptions!!.width(5.0f)
                    polylineOptions!!.startCap(SquareCap())
                    polylineOptions!!.endCap(SquareCap())
                    polylineOptions!!.jointType(JointType.ROUND)
                    polylineOptions!!.addAll(polylineList)
                    greyPolyline = mMap.addPolyline(polylineOptions)

                    blackPolylineOptions = PolylineOptions()
                    blackPolylineOptions!!.color(Color.GRAY)
                    blackPolylineOptions!!.width(5.0f)
                    blackPolylineOptions!!.startCap(SquareCap())
                    blackPolylineOptions!!.endCap(SquareCap())
                    blackPolylineOptions!!.jointType(JointType.ROUND)
                    blackPolylineOptions!!.addAll(polylineList)
                    blackPolyline = mMap.addPolyline(blackPolylineOptions)

                    //Animator
                    val polylineAnimator = ValueAnimator.ofInt(0,100)
                    polylineAnimator.setDuration(2000)
                    polylineAnimator.setInterpolator(LinearInterpolator())
                    polylineAnimator.addUpdateListener { valueAnimator ->
                        val points = greyPolyline!!.points
                        val percentValue = Integer.parseInt(valueAnimator.animatedValue.toString())
                        val size = points.size
                        val newPoints = (size*(percentValue / 100.0f)).toInt()
                        val p = points.subList(0,newPoints)
                        blackPolyline!!.points = p

                    }
                    polylineAnimator.start()

                    //Car moving
                    index = -1
                    next = 1
                    val r = object: Runnable {
                        override fun run() {
                            if (index < polylineList.size - 1)
                            {
                                index++
                                next = index + 1
                                startPosition = polylineList[index]
                                endPosition = polylineList[next]
                            }

                            val valueAnimator = ValueAnimator.ofInt(0,1)
                            valueAnimator.setDuration(1500)
                            valueAnimator.setInterpolator(LinearInterpolator())
                            valueAnimator.addUpdateListener { valueAnimator ->
                                v = valueAnimator.animatedFraction
                                lat = v * endPosition!!.latitude + (1-v) * startPosition!!.latitude
                                lng = v * endPosition!!.longitude + (1-v) * startPosition!!.longitude

                                val newPos = LatLng(lat,lng)
                                marker!!.position = newPos
                                marker!!.setAnchor(0.5f,0.5f)
                                marker!!.rotation = Common.getBearing(startPosition!!,newPos)

                                mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.position))

                            }

                            valueAnimator.start()
                            if (index < polylineList.size - 2)
                                handler!!.postDelayed(this,1500)
                        }

                    }

                    handler = Handler()
                    handler!!.postDelayed(r,1500)



                }catch (e:Exception)
                {
                    Log.d("DEBUG",e.message)
                }

            },{throwable ->
                Toast.makeText(this@ShippingActivity,""+throwable.message,Toast.LENGTH_SHORT).show()
            }))
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
        compositeDisposable.clear()
        super.onDestroy()
    }
}