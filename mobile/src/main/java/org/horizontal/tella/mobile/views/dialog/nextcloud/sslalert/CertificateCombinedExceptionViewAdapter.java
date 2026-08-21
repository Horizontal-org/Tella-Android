/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Stefan Niedermann <info@niedermann.it>
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2015 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2014 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package org.horizontal.tella.mobile.views.dialog.nextcloud.sslalert;

import android.view.View;

import com.owncloud.android.lib.common.network.CertificateCombinedException;

import org.horizontal.tella.mobile.databinding.SslUntrustedCertLayoutBinding;

public class CertificateCombinedExceptionViewAdapter implements SslUntrustedCertDialog.ErrorViewAdapter {

    //private final static String TAG = CertificateCombinedExceptionViewAdapter.class.getSimpleName();

    private CertificateCombinedException mSslException;

    public CertificateCombinedExceptionViewAdapter(CertificateCombinedException sslException) {
        mSslException = sslException;
    }

    @Override
    public void updateErrorView(SslUntrustedCertLayoutBinding binding) {
        /// clean
        binding.reasonNoInfoAboutError.setVisibility(View.GONE);

        /// refresh
        if (mSslException.getCertPathValidatorException() != null) {
            binding.reasonCertNotTrusted.setVisibility(View.VISIBLE);
        } else {
            binding.reasonCertNotTrusted.setVisibility(View.GONE);
        }

        if (mSslException.getCertificateExpiredException() != null) {
            binding.reasonCertExpired.setVisibility(View.VISIBLE);
        } else {
            binding.reasonCertExpired.setVisibility(View.GONE);
        }

        if (mSslException.getCertificateNotYetValidException() != null) {
            binding.reasonCertNotYetValid.setVisibility(View.VISIBLE);
        } else {
            binding.reasonCertNotYetValid.setVisibility(View.GONE);
        }

        if (mSslException.getSslPeerUnverifiedException() != null) {
            binding.reasonHostnameNotVerified.setVisibility(View.VISIBLE);
        } else {
            binding.reasonHostnameNotVerified.setVisibility(View.GONE);
        }

    }
}
