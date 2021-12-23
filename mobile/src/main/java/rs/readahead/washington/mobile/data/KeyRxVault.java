package rs.readahead.washington.mobile.data;

import android.content.Context;

import com.hzontal.tella_vault.Vault;
import com.hzontal.tella_vault.VaultException;
import com.hzontal.tella_vault.rx.RxVault;

import org.hzontal.tella.keys.key.LifecycleMainKey;

import io.reactivex.Observable;
import io.reactivex.subjects.AsyncSubject;
import timber.log.Timber;

public class KeyRxVault {
    private final AsyncSubject<RxVault> asyncSubject;
    private final Context context;
    private final LifecycleMainKey mainKeyHolder;
    private final Vault.Config vaultConfig;


    public KeyRxVault(Context context, LifecycleMainKey mainKeyHolder, Vault.Config vaultConfig) {
        asyncSubject = AsyncSubject.create();
        this.context = context.getApplicationContext();
        this.mainKeyHolder = mainKeyHolder;
        this.vaultConfig = vaultConfig;
    }

    public void initKeyRxVault() {
        try {
            asyncSubject.onNext(new RxVault(context, new Vault(context, mainKeyHolder, vaultConfig)));
            asyncSubject.onComplete();
        } catch (LifecycleMainKey.MainKeyUnavailableException | VaultException e) {
            Timber.d(e);
        }
    }

    public Observable<RxVault> getRxVault() {
        return asyncSubject;
    }
}
