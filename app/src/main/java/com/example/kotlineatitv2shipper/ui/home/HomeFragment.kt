package com.example.kotlineatitv2shipper.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlineatitv2shipper.adapter.MyShippingOrderAdapter
import com.example.kotlineatitv2shipper.R
import com.example.kotlineatitv2shipper.common.Common
import com.example.kotlineatitv2shipper.model.ShippingOrderModel
import com.example.kotlineatitv2shipper.model.eventbus.UpdateShippingOrderEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class HomeFragment : Fragment() {
    
    var recycler_order: RecyclerView?=null
    var layoutAnimationController : LayoutAnimationController?= null
    var adapter:MyShippingOrderAdapter?=null

    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
            ViewModelProviders.of(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        initViews(root)
        homeViewModel!!.messageError.observe(this, Observer { s:String-> Toast.makeText(context,s,Toast.LENGTH_SHORT).show() })
        homeViewModel!!.getOrderModelMutableLiveData(Common.currentShipperUser!!.phone!!)
            .observe(this, Observer { shippingOrderModels:List<ShippingOrderModel> ->
                adapter = MyShippingOrderAdapter(context!!,shippingOrderModels)
                recycler_order!!.adapter = adapter
                recycler_order!!.layoutAnimation = layoutAnimationController
            })
        return root
    }

    private fun initViews(root: View?) {
        recycler_order = root!!.findViewById(R.id.recycler_order) as RecyclerView
        recycler_order!!.setHasFixedSize(true)
        recycler_order!!.layoutManager = LinearLayoutManager(context)
        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context,R.anim.layout_item_from_left)
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        if (EventBus.getDefault().hasSubscriberForEvent(UpdateShippingOrderEvent::class.java))
            EventBus.getDefault().removeStickyEvent(UpdateShippingOrderEvent::class.java)
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public fun pnUpdateShippingOrder(event:UpdateShippingOrderEvent)
    {
        homeViewModel.getOrderModelMutableLiveData(Common.currentShipperUser!!.phone!!)
    }
}