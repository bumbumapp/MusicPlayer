/*
 * Copyright (c) 2021 Auxio Project
 * ContextUtil.kt is part of Auxio.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.bumbumapps.musicplayer.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.view.LayoutInflater
import android.widget.Toast
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import org.bumbumapps.musicplayer.MainActivity
import kotlin.reflect.KClass
import kotlin.system.exitProcess

const val INTENT_REQUEST_CODE = 0xA0A0

/**
 * Shortcut to get a [LayoutInflater] from a [Context]
 */
val Context.inflater: LayoutInflater get() = LayoutInflater.from(this)

/**
 * Returns whether the current UI is in night mode or not. This will work if the theme is
 * automatic as well.
 */
val Context.isNight: Boolean get() =
    resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
        Configuration.UI_MODE_NIGHT_YES

/**
 * Convenience method for getting a system service without nullability issues.
 * @param T The system service in question.
 * @param serviceClass The service's kotlin class [Java class will be used in function call]
 * @return The system service
 * @throws IllegalArgumentException If the system service cannot be retrieved.
 */
fun <T : Any> Context.getSystemServiceSafe(serviceClass: KClass<T>): T {
    return requireNotNull(ContextCompat.getSystemService(this, serviceClass.java)) {
        "System service ${serviceClass.simpleName} could not be instantiated"
    }
}

/**
 * Create a broadcast [PendingIntent]
 */
fun Context.newBroadcastIntent(what: String): PendingIntent {
    return PendingIntent.getBroadcast(
        this, INTENT_REQUEST_CODE, Intent(what),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE
        else 0
    )
}

/**
 * Create a [PendingIntent] that leads to Auxio's [MainActivity]
 */
fun Context.newMainIntent(): PendingIntent {
    return PendingIntent.getActivity(
        this, INTENT_REQUEST_CODE, Intent(this, MainActivity::class.java),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE
        else 0
    )
}

fun Context.hardRestart() {
    // Instead of having to do a ton of cleanup and horrible code changes
    // to restart this application non-destructively, I just restart the UI task [There is only
    // one, after all] and then kill the application using exitProcess. Works well enough.
    val intent = Intent(applicationContext, MainActivity::class.java).setFlags(
        Intent.FLAG_ACTIVITY_CLEAR_TASK
    )

    startActivity(intent)

    exitProcess(0)
}

/**
 * Create a toast using the provided string resource.
 */
fun Context.showToast(@StringRes str: Int) {
    Toast.makeText(applicationContext, getString(str), Toast.LENGTH_SHORT).show()
}

/**
 * Convenience method for getting a plural.
 * @param pluralsRes Resource for the plural
 * @param value Int value for the plural.
 * @return The formatted string requested
 */
fun Context.getPlural(@PluralsRes pluralsRes: Int, value: Int): String {
    return resources.getQuantityString(pluralsRes, value, value)
}

/**
 * Determine if the device is currently in landscape.
 */
fun Context.isLandscape(): Boolean {
    return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}
