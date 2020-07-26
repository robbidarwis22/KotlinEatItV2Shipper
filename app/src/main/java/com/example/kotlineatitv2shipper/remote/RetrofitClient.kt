package com.example.kotlineatitv2shipper.remote

import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

object RetrofitClient {
    @JvmStatic
    var instance : Retrofit?=null
        get(){
            if (field == null) field = Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
            return field
        }
    private set
}