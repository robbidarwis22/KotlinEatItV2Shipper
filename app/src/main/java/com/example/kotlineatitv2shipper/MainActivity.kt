package com.example.kotlineatitv2shipper

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kotlineatitv2shipper.common.Common
import com.example.kotlineatitv2shipper.model.RestaurantModel
import com.example.kotlineatitv2shipper.model.ShipperUserModel
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dmax.dialog.SpotsDialog
import io.paperdb.Paper
import java.util.*

class MainActivity : AppCompatActivity() {

    private var firebaseAuth: FirebaseAuth? = null
    private var listener: FirebaseAuth.AuthStateListener? = null
    private var dialog: AlertDialog? = null
    private var serverRef: DatabaseReference? = null
    private var providers : List<AuthUI.IdpConfig>? = null

    companion object{
        private val APP_REQUEST_CODE = 7171
    }

    override fun onStart() {
        super.onStart()
        firebaseAuth!!.addAuthStateListener(listener!!)
    }

    override fun onStop() {
        firebaseAuth!!.removeAuthStateListener(listener!!)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()

        //Delete data
        Paper.init(this);
//        Paper.book().delete(Common.TRIP_START)
//        Paper.book().delete(Common.SHIPPING_DATA)
    }

    private fun init() {
        providers = Arrays.asList<AuthUI.IdpConfig>(AuthUI.IdpConfig.PhoneBuilder().build(),
        AuthUI.IdpConfig.EmailBuilder().build())

        serverRef = FirebaseDatabase.getInstance().getReference(Common.SHIPPER_REF)
        firebaseAuth = FirebaseAuth.getInstance()
        dialog = SpotsDialog.Builder().setContext(this).setCancelable(false).build()
        listener = object:FirebaseAuth.AuthStateListener{
            override fun onAuthStateChanged(firebaseAuth: FirebaseAuth) {
                val user = firebaseAuth.currentUser
                if (user != null)
                {
                    Paper.init(this@MainActivity)
                    val jsonEncode = Paper.book().read<String>(Common.RESTAURANT_SAVE)
                    val restaurantModel = Gson().fromJson<RestaurantModel>(jsonEncode,
                    object:TypeToken<RestaurantModel>(){}.type)
                    if (restaurantModel != null)
                        checkServerUserFromFirebase(user,restaurantModel!!)
                    else
                    {
                        startActivity(Intent(this@MainActivity,RestaurantListActivity::class.java))
                        finish()
                    }
                }
                else
                {
                    phoneLogin()
                }
            }

        }
    }

    private fun checkServerUserFromFirebase(user: FirebaseUser, restaurantModel: RestaurantModel) {
        dialog!!.show()
        //Init serverRef
        serverRef = FirebaseDatabase.getInstance().getReference(Common.RESTAURANT_REF)
            .child(restaurantModel.uid)
            .child(Common.SHIPPER_REF)
        serverRef!!.child(user.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    dialog!!.dismiss()
                    Toast.makeText(this@MainActivity,""+p0.message,Toast.LENGTH_SHORT).show()
                }

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists())
                    {
                        val userModel = dataSnapshot.getValue(ShipperUserModel::class.java)
                        if (userModel!!.isActive)
                        {
                            goToHomeActivity(userModel,restaurantModel)
                        }
                        else{
                            dialog!!.dismiss()
                            Toast.makeText(this@MainActivity,"You must be allowed from Admin to access this app",Toast.LENGTH_SHORT).show()

                        }
                    }

                }

            })
    }

    private fun goToHomeActivity(userModel: ShipperUserModel, restaurantModel: RestaurantModel) {
        dialog!!.dismiss()
        Common.currentRestaurant = restaurantModel
        Common.currentShipperUser = userModel
        startActivity(Intent(this,HomeActivity::class.java))
        finish()
    }

    private fun phoneLogin() {
        startActivityForResult(AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers!!)
            .setTheme(R.style.LoginTheme)
            .setLogo(R.drawable.logo)
            .build(),APP_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == APP_REQUEST_CODE)
        {

            if (resultCode == Activity.RESULT_OK)
            {
                val user = FirebaseAuth.getInstance().currentUser
            }
            else
            {
                Toast.makeText(this,"Failed to sign in",Toast.LENGTH_SHORT).show()
            }
        }
    }
}
