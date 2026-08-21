package org.hzontal.tella.keys.wrapper;

import android.os.Build;

import org.hzontal.tella.keys.key.MainKey;
import org.hzontal.tella.keys.key.WrappedMainKey;

import java.security.spec.KeySpec;

import javax.crypto.spec.SecretKeySpec;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.M)
public class AndroidKeyStoreWrapper implements IMainKeyWrapper {
    @Override
    public String getName() {
        return AndroidKeyStoreWrapper.class.getName();
    }

    @Override
    public void unwrap(WrappedMainKey wrappedMainKey, KeySpec keySpec, IUnwrapCallback callback) {
        try {
            byte[] key = AndroidKeyStoreHelper.unwrap(wrappedMainKey);

            MainKey mainKey = new MainKey(new SecretKeySpec(key, "AES"));

            callback.onReady(mainKey);
        } catch (Throwable throwable) {
            callback.onError(throwable);
        }
    }

    @Override
    public void wrap(MainKey mainKey, KeySpec keySpec, IWrapCallback callback) {
        try {
            WrappedMainKey wrappedMainKey = AndroidKeyStoreHelper.wrap(mainKey.getKey().getEncoded(), this);

            callback.onReady(wrappedMainKey);
        } catch (Throwable throwable) {
            callback.onError(throwable);
        }
    }
}
