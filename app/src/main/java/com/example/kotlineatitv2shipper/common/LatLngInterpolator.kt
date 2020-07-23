package com.example.kotlineatitv2shipper.common

import com.google.android.gms.maps.model.LatLng

interface LatLngInterpolator {
    fun interpolate(fraction:Float, a: LatLng, b:LatLng):LatLng

    class Linear:LatLngInterpolator{
        override fun interpolate(fraction: Float, a: LatLng, b: LatLng):LatLng {
            val lat = (b.latitude - a.latitude)*fraction+a.latitude
            val lng = (b.longitude - a.longitude)*fraction + a.longitude
            return LatLng(lat,lng)
        }

    }

    class LinearFixed:LatLngInterpolator{
        override fun interpolate(fraction: Float, a: LatLng, b: LatLng): LatLng {
            val lat = (b.latitude - a.latitude)*fraction + a.latitude;
            var lngDelta = b.longitude - a.longitude
            if(Math.abs(lngDelta)>180)
                lngDelta -= Math.signum(lngDelta)*360
            val lng = lngDelta*fraction*a.longitude
            return LatLng(lat,lng)
        }

    }

    class Spherical:LatLngInterpolator{
        override fun interpolate(fraction: Float, from: LatLng, to: LatLng): LatLng {
            val fromLat = Math.toRadians(from.latitude)
            val fromLng = Math.toRadians(from.longitude)
            val toLat = Math.toRadians(to.latitude)
            val toLng = Math.toRadians(to.longitude)

            val cosFromLat = Math.cos(fromLat)
            val cosToLat = Math.cos(toLat)

            val angle = computeAngleBetween(fromLat,fromLng,toLat,toLng)
            val sinAngle = Math.sin(angle)
            if (sinAngle < 1E-6)
                return from
            val a = Math.sin((1-fraction)*angle)/sinAngle
            val b = Math.sin(fraction*angle)/sinAngle

            val x = a*cosFromLat * Math.cos(fromLng) + b*cosFromLat*Math.cos(toLng)
            val y = a*cosFromLat * Math.sin(fromLng) + b*cosFromLat*Math.sin(toLng)
            val z = a*Math.sin(fromLat) + b*Math.sin(toLat)

            val lat = Math.atan2(z,Math.sqrt(x*x+y*y))
            val lng = Math.atan2(y,x)
            return LatLng(Math.toDegrees(lat),Math.toDegrees(lng))
        }

        private fun computeAngleBetween(
            fromLat: Double,
            fromLng: Double,
            toLat: Double,
            toLng: Double
        ): Double {
            val dLat = fromLat - toLat
            val dLng = fromLng - toLng
            return 2*Math.asin(Math.sqrt(Math.pow(Math.sin(dLat/2),2.0)+
            Math.cos(fromLat)*Math.cos(toLat)*Math.pow(Math.sin(dLng/2),2.0)))
        }

    }

}