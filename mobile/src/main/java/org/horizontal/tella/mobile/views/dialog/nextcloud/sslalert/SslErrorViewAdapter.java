/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Stefan Niedermann <info@niedermann.it>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2014 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package org.horizontal.tella.mobile.views.dialog.nextcloud.sslalert;

import android.net.http.SslError;
import android.view.View;


import org.horizontal.tella.mobile.databinding.SslUntrustedCertLayoutBinding;

/**
 * Dialog to show an Untrusted Certificate
 */
public class SslErrorViewAdapter implements SslUntrustedCertDialog.ErrorViewAdapter {

    //private final static String TAG = SslErrorViewAdapter.class.getSimpleName();

    private SslError mSslError;

    public SslErrorViewAdapter(SslError sslError) {
        mSslError = sslError;
    }

    @Override
    public void updateErrorView(SslUntrustedCertLayoutBinding binding) {
        /// clean
        binding.reasonNoInfoAboutError.setVisibility(View.GONE);

        /// refresh
        if (mSslError.hasError(SslError.SSL_UNTRUSTED)) {
            binding.reasonCertNotTrusted.setVisibility(View.VISIBLE);
        } else {
            binding.reasonCertNotTrusted.setVisibility(View.GONE);
        }

        if (mSslError.hasError(SslError.SSL_EXPIRED)) {
            binding.reasonCertExpired.setVisibility(View.VISIBLE);
        } else {
            binding.reasonCertExpired.setVisibility(View.GONE);
        }

        if (mSslError.getPrimaryError() == SslError.SSL_NOTYETVALID) {
            binding.reasonCertNotYetValid.setVisibility(View.VISIBLE);
        } else {
            binding.reasonCertNotYetValid.setVisibility(View.GONE);
        }

        if (mSslError.getPrimaryError() == SslError.SSL_IDMISMATCH) {
            binding.reasonHostnameNotVerified.setVisibility(View.VISIBLE);
        } else {
            binding.reasonHostnameNotVerified.setVisibility(View.GONE);
        }
    }

}
