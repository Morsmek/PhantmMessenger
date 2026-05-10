package com.stagic.phantm.mesh

import android.content.Context

actual fun createMeshNetwork(localId: String): MeshNetwork =
    throw UnsupportedOperationException(
        "Use createMeshNetwork(localId, context) on Android — Context is required for BLE/WiFi Direct"
    )

/** Android-specific factory requiring an Android [Context]. */
fun createMeshNetwork(localId: String, context: Context): MeshNetwork =
    AndroidMeshNetwork(context, localId)
