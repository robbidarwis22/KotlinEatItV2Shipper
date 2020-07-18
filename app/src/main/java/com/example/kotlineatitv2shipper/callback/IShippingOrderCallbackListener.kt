package com.example.kotlineatitv2shipper.callback

import com.example.kotlineatitv2shipper.model.ShippingOrderModel

interface IShippingOrderCallbackListener {
    fun onShippingOrderLoadSuccess(shippingOrders:List<ShippingOrderModel>)
    fun onShippingOrderLoadFailed(message:String)
}