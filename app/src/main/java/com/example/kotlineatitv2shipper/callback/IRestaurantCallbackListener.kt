package com.example.kotlineatitv2shipper.callback

import com.example.kotlineatitv2shipper.model.RestaurantModel

interface IRestaurantCallbackListener {
    fun onRestaurantLoadSuccess(restaurantList: List<RestaurantModel>)
    fun onRestaurantLoadFailed(message:String)
}