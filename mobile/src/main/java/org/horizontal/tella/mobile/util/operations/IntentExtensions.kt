/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper_ozturk@proton.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package org.horizontal.tella.mobile.util.operations

import android.content.Intent
import android.os.Build
import android.os.Parcelable
import com.owncloud.android.lib.common.utils.Log_OC
import java.io.Serializable

@Suppress("TopLevelPropertyNaming")
private const val tag = "IntentExtension"

fun <T : Serializable?> Intent?.getSerializableArgument(key: String, type: Class<T>): T? {
    if (this == null) {
        return null
    }

    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.getSerializableExtra(key, type)
        } else {
            @Suppress("UNCHECKED_CAST", "DEPRECATION")
            if (type.isInstance(this.getSerializableExtra(key))) {
                this.getSerializableExtra(key) as T
            } else {
                null
            }
        }
    } catch (e: ClassCastException) {
        Log_OC.e(tag, e.localizedMessage)
        null
    }
}

fun <T : Parcelable?> Intent?.getParcelableArgument(key: String, type: Class<T>): T? {
    if (this == null) {
        return null
    }

    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.getParcelableExtra(key, type)
        } else {
            @Suppress("DEPRECATION")
            this.getParcelableExtra(key)
        }
    } catch (e: ClassCastException) {
        Log_OC.e(tag, e.localizedMessage)
        null
    }
}
