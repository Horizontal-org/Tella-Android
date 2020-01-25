package rs.readahead.washington.mobile.domain.entity;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

import info.guardianproject.cacheword.ICachedSecrets;
import info.guardianproject.cacheword.PassphraseSecrets;


public class KeyBundle {
    private long version;
    private WeakReference<ICachedSecrets> reference;


    public KeyBundle(ICachedSecrets passphraseSecrets, long version) {
        this.reference = new WeakReference<>(passphraseSecrets);
        this.version = version;
    }

    @Nullable
    public byte[] getKey() {
        ICachedSecrets cachedSecrets = reference.get();

        if (cachedSecrets == null) {
            return null;
        }

        return cachedSecrets instanceof PassphraseSecrets ? ((PassphraseSecrets) cachedSecrets).getSecretKey().getEncoded() : null;
    }

    public long getVersion() {
        return version;
    }
}
