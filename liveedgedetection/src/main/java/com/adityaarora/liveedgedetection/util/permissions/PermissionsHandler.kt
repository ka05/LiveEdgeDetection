package com.adityaarora.liveedgedetection.util.permissions

import android.app.Activity
import android.content.Context

interface PermissionsHandler {

    fun hasPermissions(
            context: Context,
            permissionType: String
    ): Boolean

    fun requestPermissions(
            activity: Activity,
            permissionType: String
    )

    fun onRequestPermissionsResult(
            activity: Activity,
            permission: String,
            requestCode: Int,
            grantResults: IntArray
    )
}