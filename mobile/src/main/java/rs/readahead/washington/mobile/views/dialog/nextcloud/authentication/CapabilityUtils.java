/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2017 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package rs.readahead.washington.mobile.views.dialog.nextcloud.authentication;

import android.accounts.Account;
import android.content.Context;
import android.os.Build;

import com.owncloud.android.lib.resources.status.OCCapability;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class CapabilityUtils {

    private static final Map<String, OCCapability> cachedCapabilities = new HashMap<>();

    public static OCCapability getCapability(Context context) {
        User user = null;
        if (context != null) {
            // TODO: refactor when dark theme work is completed
           //  user = UserAccountManagerImpl.fromContext(context).getUser();
        }

        if (user != null) {
            return getCapability(user, context);
        } else {
            return new OCCapability();
        }
    }

    /**
     * @deprecated use {@link #getCapability(User, Context)} instead
     */
    @Deprecated
    public static OCCapability getCapability(Account acc, Context context) {
        Optional<User> user = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            user = Optional.empty();
        }

        if (acc != null) {
            //  user = UserAccountManagerImpl.fromContext(context).getUser(acc.name);
        } else if (context != null) {
            // TODO: refactor when dark theme work is completed
            //  user = Optional.of(UserAccountManagerImpl.fromContext(context).getUser());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (user.isPresent()) {
                return getCapability(user.get(), context);
            } else {
                return new OCCapability();
            }
        }
        return null;
    }

    public static OCCapability getCapability(User user, Context context) {
        OCCapability capability = cachedCapabilities.get(user.getAccountName());

        if (capability == null) {
        //  FileDataStorageManager storageManager = new FileDataStorageManager(user, context.getContentResolver());
          //  capability = storageManager.getCapability(user.getAccountName());

            cachedCapabilities.put(capability.getAccountName(), capability);
        }

        return capability;
    }
}
