package org.hzontal.tella.keys;

import android.content.Context;
import android.text.TextUtils;

import org.hzontal.tella.keys.key.MainKey;
import org.hzontal.tella.keys.key.WrappedMainKey;
import org.hzontal.tella.keys.util.Preferences;
import org.hzontal.tella.keys.wrapper.IMainKeyWrapper;

import java.security.spec.KeySpec;

public class MainKeyStore {
    private final String KEY_WRAPPER_NAME = "wrapperName";
    private final String KEY_DATA = "data";
    private final String KEY_IV = "iv";
    private final String KEY_SALT = "salt";
    private final String KEY_ITERATION_COUNT = "iterationCount";

    private final Context context;

    public MainKeyStore(Context context) {
        this.context = context;
    }

    public void load(IMainKeyWrapper wrapper, KeySpec keySpec, IMainKeyLoadCallback callback) {
        try {
            WrappedMainKey current = loadWrappedKey();

            wrapper.unwrap(current, keySpec, new IMainKeyWrapper.IUnwrapCallback() {
                @Override
                public void onReady(MainKey mainKey) {
                    callback.onReady(mainKey);
                }

                @Override
                public void onError(Throwable error) {
                    callback.onError(error);
                }
            });
        } catch (Throwable throwable) {
            callback.onError(throwable);
        }
    }

    public void store(MainKey mainKey, IMainKeyWrapper wrapper, KeySpec keySpec, IMainKeyStoreCallback callback) {
        wrapper.wrap(mainKey, keySpec, new IMainKeyWrapper.IWrapCallback() {
            @Override
            public void onReady(WrappedMainKey wrappedMainKey) {
                storeWrappedMainKey(wrappedMainKey);
                callback.onSuccess(mainKey);
            }

            @Override
            public void onError(Throwable error) {
                callback.onError(error);
            }
        });
    }

    public void remove() {
        storeWrappedMainKey(new WrappedMainKey(null));
    }

    public boolean isStored() {
        return isWrappedKeyInitialized();
    }

    private WrappedMainKey loadWrappedKey() {
        String wrapperName = Preferences.load(context, getPreferenceKey(KEY_WRAPPER_NAME), null);

        if (TextUtils.isEmpty(wrapperName)) {
            throw new IllegalStateException();
        }

        WrappedMainKey wrappedMainKey = new WrappedMainKey(wrapperName);
        wrappedMainKey.data = Preferences.load(context, getPreferenceKey(KEY_DATA));
        wrappedMainKey.iv = Preferences.load(context, getPreferenceKey(KEY_IV));
        wrappedMainKey.salt = Preferences.load(context, getPreferenceKey(KEY_SALT));
        wrappedMainKey.iterationCount = Preferences.load(context, getPreferenceKey(KEY_ITERATION_COUNT), 0);

        return wrappedMainKey;
    }

    private void storeWrappedMainKey(WrappedMainKey wrappedMainKey) {
        Preferences.store(context, getPreferenceKey(KEY_WRAPPER_NAME), wrappedMainKey.getWrapperName());
        Preferences.store(context, getPreferenceKey(KEY_DATA), wrappedMainKey.data);
        Preferences.store(context, getPreferenceKey(KEY_IV), wrappedMainKey.iv);
        Preferences.store(context, getPreferenceKey(KEY_SALT), wrappedMainKey.salt);
        Preferences.store(context, getPreferenceKey(KEY_ITERATION_COUNT), wrappedMainKey.iterationCount);
    }

    private boolean isWrappedKeyInitialized() {
        return Preferences.contains(context, getPreferenceKey(KEY_WRAPPER_NAME));
    }

    private String getPreferenceKey(String key) {
        return MainKey.class.getName() + "_" + key;
    }

    public interface IMainKeyLoadCallback {
        void onReady(MainKey mainKey);
        void onError(Throwable throwable);
    }

    public interface IMainKeyStoreCallback {
        void onSuccess(MainKey mainKey);
        void onError(Throwable throwable);
    }
}
