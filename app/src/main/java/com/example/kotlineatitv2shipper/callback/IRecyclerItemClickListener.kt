package com.example.kotlineatitv2shipper.callback

import android.view.View

interface IRecyclerItemClickListener {
    fun onItemClick(view: View, pos:Int)
}