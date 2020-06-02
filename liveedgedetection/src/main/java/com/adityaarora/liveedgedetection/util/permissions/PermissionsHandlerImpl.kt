package com.adityaarora.liveedgedetection.util.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.adityaarora.liveedgedetection.R
import com.adityaarora.liveedgedetection.util.permissions.PermissionsDelegate
import com.adityaarora.liveedgedetection.util.permissions.PermissionsHandler

class PermissionsHandlerImpl(
        private var permissionsDelegate: PermissionsDelegate? = null
) : PermissionsHandler {

    private val PERM_REQUEST_CODE = 9999

    /**
     * Example permissionsType : Manifest.permission.CAMERA
     */
    override fun hasPermissions(context: Context, permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(
                context,
                permissionType
        ) != PackageManager.PERMISSION_DENIED
    }

    /**
     * Example permissionsType : Manifest.permission.CAMERA
     */
    override fun requestPermissions(activity: Activity, permissionType: String) {
        ActivityCompat.requestPermissions(
                activity,
                arrayOf(permissionType),
                PERM_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
            activity: Activity,
            permission: String,
            requestCode: Int,
            grantResults: IntArray
    ) {
        if (requestCode == PERM_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionsDelegate?.permissionsAccepted()
            } else {
                permissionsDelegate?.permissionsDenied()
            }
        }
    }
}