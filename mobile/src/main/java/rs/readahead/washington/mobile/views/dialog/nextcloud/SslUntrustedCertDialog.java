/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2018-2022 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2020 Stefan Niedermann <info@niedermann.it>
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2014 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2014 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package rs.readahead.washington.mobile.views.dialog.nextcloud;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.SslErrorHandler;
import android.widget.Button;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.owncloud.android.lib.common.network.CertificateCombinedException;
import com.owncloud.android.lib.common.network.NetworkUtils;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.databinding.SslUntrustedCertLayoutBinding;

/**
 * Dialog to show information about an untrusted certificate and allow the user to decide trust on it or not.
 * Abstract implementation of common functionality for different dialogs that get the information about the error and
 * the certificate from different classes.
 */
public class SslUntrustedCertDialog extends DialogFragment {

    private final static String TAG = SslUntrustedCertDialog.class.getSimpleName();

    // ViewThemeUtils viewThemeUtils;

    protected SslUntrustedCertLayoutBinding binding;
    protected SslErrorHandler mHandler;
    protected X509Certificate m509Certificate;

    private ErrorViewAdapter mErrorViewAdapter;
    private CertificateViewAdapter mCertificateViewAdapter;
    private String serverUrl;

    public static SslUntrustedCertDialog newInstanceForEmptySslError(SslError error, SslErrorHandler handler) {
        if (error == null) {
            throw new IllegalArgumentException("Trying to create instance with parameter error == null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Trying to create instance with parameter handler == null");
        }
        SslUntrustedCertDialog dialog = new SslUntrustedCertDialog();
        dialog.mHandler = handler;
        dialog.mErrorViewAdapter = new SslErrorViewAdapter(error);
        dialog.mCertificateViewAdapter = new SslCertificateViewAdapter(error.getCertificate());
        return dialog;
    }

    public static SslUntrustedCertDialog newInstanceForFullSslError(CertificateCombinedException sslException, String serverUrl) {
        if (sslException == null) {
            throw new IllegalArgumentException("Trying to create instance with parameter sslException == null");
        }
        SslUntrustedCertDialog dialog = new SslUntrustedCertDialog();
        dialog.m509Certificate = sslException.getServerCertificate();
        dialog.mErrorViewAdapter = new CertificateCombinedExceptionViewAdapter(sslException);
        dialog.mCertificateViewAdapter = new X509CertificateViewAdapter(sslException.getServerCertificate());
        dialog.serverUrl = serverUrl;
        return dialog;
    }

    public static SslUntrustedCertDialog newInstanceForFullSslError(X509Certificate cert, SslError error, SslErrorHandler handler) {
        if (cert == null) {
            throw new IllegalArgumentException("Trying to create instance with parameter cert == null");
        }
        if (error == null) {
            throw new IllegalArgumentException("Trying to create instance with parameter error == null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Trying to create instance with parameter handler == null");
        }
        SslUntrustedCertDialog dialog = new SslUntrustedCertDialog();
        dialog.m509Certificate = cert;
        dialog.mHandler = handler;
        dialog.mErrorViewAdapter = new SslErrorViewAdapter(error);
        dialog.mCertificateViewAdapter = new X509CertificateViewAdapter(cert);
        return dialog;
    }

    @Override
    public void onAttach(@NonNull Activity activity) {
        Log_OC.d(TAG, "onAttach");
        super.onAttach(activity);
        if (!(activity instanceof OnSslUntrustedCertListener)) {
            throw new IllegalArgumentException("The host activity must implement " + OnSslUntrustedCertListener.class.getCanonicalName());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreate, savedInstanceState is " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setRetainInstance(true);    // force to keep the state of the fragment on configuration changes (such as device rotations)
        setCancelable(false);
        binding = null;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreateDialog, savedInstanceState is " + savedInstanceState);

        binding = SslUntrustedCertLayoutBinding.inflate(getLayoutInflater(), null, false);
        binding.detailsScroll.setVisibility(View.GONE);
        mErrorViewAdapter.updateErrorView(binding);

        binding.ok.setOnClickListener(new OnCertificateTrusted());
        binding.cancel.setOnClickListener(new OnCertificateNotTrusted());
        binding.detailsBtn.setOnClickListener(v -> {
            if (binding.detailsScroll.getVisibility() == View.VISIBLE) {
                binding.detailsScroll.setVisibility(View.GONE);
                ((Button) v).setText(R.string.ssl_validator_btn_details_see);
            } else {
                binding.detailsScroll.setVisibility(View.VISIBLE);
                ((Button) v).setText(R.string.ssl_validator_btn_details_hide);
                mCertificateViewAdapter.updateCertificateView(binding);
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(binding.getRoot());
        builder.setCancelable(false);

        final Dialog dialog = builder.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onDestroyView() {
        Log_OC.d(TAG, "onDestroyView");
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

    private class OnCertificateNotTrusted implements OnClickListener {

        @Override
        public void onClick(View v) {
            getDialog().cancel();
            if (mHandler != null) {
                mHandler.cancel();
            }
        }
    }

    private class OnCertificateTrusted implements OnClickListener {

        @Override
        public void onClick(View v) {
            dismiss();
            if (mHandler != null) {
                mHandler.proceed();
            }
            if (m509Certificate != null) {
                Activity activity = getActivity();
                try {
                    assert activity != null;
                    NetworkUtils.addCertToKnownServersStore(m509Certificate, activity);   // TODO make this asynchronously, it can take some time
                    ((OnSslUntrustedCertListener)activity).onSavedCertificate(serverUrl);
                } catch (GeneralSecurityException | IOException e) {
                    ((OnSslUntrustedCertListener)activity).onFailedSavingCertificate();
                    Log_OC.e(TAG, "Server certificate could not be saved in the known-servers trust store ", e);
                }
            }
        }
    }

    public interface OnSslUntrustedCertListener {
        void onSavedCertificate(String uri);
        void onFailedSavingCertificate();
    }

    public interface ErrorViewAdapter {
        void updateErrorView(SslUntrustedCertLayoutBinding binding);
    }

    public interface CertificateViewAdapter {
        void updateCertificateView(SslUntrustedCertLayoutBinding binding);
    }
}
