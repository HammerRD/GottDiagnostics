package com.gottdiagnostics

import android.content.Context
import android.graphics.ImageDecoder
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Images stay on the phone; their URI is never included in diagnostic exports or AI requests. */
@Composable internal fun VehiclePhoto(vehicle: Vehicle, uri: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(uri) {
        bitmap = withContext(Dispatchers.IO) {
            if(uri == null) null else runCatching { loadVehiclePhoto(context, Uri.parse(uri)) }.getOrNull()
        }
    }
    Box(modifier.background(RacePanel), contentAlignment = Alignment.Center) {
        val image = bitmap
        if(image != null) {
            Image(image.asImageBitmap(), "${vehicle.paint} ${vehicle.title}",
                Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        } else {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RaceEyebrow("${vehicle.paint} / ${vehicle.make.uppercase(java.util.Locale.US)}", vehicleAccent(vehicle.id))
                Text(if(uri == null) "Add your car photo in Garage" else "Photo unavailable — choose it again in Garage", color = RaceMuted)
            }
        }
    }
}

private fun loadVehiclePhoto(context: Context, uri: Uri): android.graphics.Bitmap {
    if(Build.VERSION.SDK_INT >= 28) {
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            val scale = (1280f / maxOf(info.size.width, info.size.height)).coerceAtMost(1f)
            decoder.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1), (info.size.height * scale).toInt().coerceAtLeast(1))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
    require(bounds.outWidth > 0 && bounds.outHeight > 0)
    val options = BitmapFactory.Options().apply {
        inSampleSize = 1
        while(maxOf(bounds.outWidth, bounds.outHeight) / inSampleSize > 1280) inSampleSize *= 2
    }
    return context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
        ?: error("Image could not be decoded")
}
