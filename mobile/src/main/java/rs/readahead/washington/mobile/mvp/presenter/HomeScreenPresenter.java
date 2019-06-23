package rs.readahead.washington.mobile.mvp.presenter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;

import org.javarosa.core.model.FormDef;

import info.guardianproject.cacheword.CacheWordHandler;
import io.reactivex.Completable;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.BuildConfig;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.data.database.CacheWordDataSource;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.data.sharedpref.SharedPrefs;
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.mvp.contract.IHomeScreenPresenterContract;
import rs.readahead.washington.mobile.presentation.entity.Shortcut;
import rs.readahead.washington.mobile.presentation.entity.ShortcutPosition;


public class HomeScreenPresenter implements IHomeScreenPresenterContract.IPresenter {
    private IHomeScreenPresenterContract.IView view;
    private CacheWordHandler cacheWordHandler;
    private CacheWordDataSource cacheWordDataSource;
    private CompositeDisposable disposable;
    private final Context appContext;
    //private String smsMessage;
    //private List<String> phoneNumbers;


    public HomeScreenPresenter(IHomeScreenPresenterContract.IView view, CacheWordHandler cacheWordHandler) {
        this.view = view;
        this.cacheWordHandler = cacheWordHandler;
        appContext = view.getContext().getApplicationContext();
        cacheWordDataSource = new CacheWordDataSource(appContext);
        disposable = new CompositeDisposable();
    }

    @Override
    public void executePanicMode() {
        cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .flatMapCompletable(dataSource -> {
                    /*if (phoneNumbers == null) {
                        phoneNumbers = dataSource.getTrustedPhones(); // refresh if still not here..
                    }
                    if (phoneNumbers.size() > 0) {
                        sendPanicMessagesOnMain(phoneNumbers);
                    }*/
                    if (SharedPrefs.getInstance().isEraseGalleryActive()) {
                        MediaFileHandler.destroyGallery(appContext);
                        dataSource.deleteMediaFiles();
                    }

                    if (Preferences.isDeleteServerSettingsActive()) {
                        dataSource.deleteDatabase();
                    } else {

                        //if (SharedPrefs.getInstance().isEraseContactsActive()) {
                            dataSource.deleteContacts();
                        //}

                        if (Preferences.isEraseForms()) {
                            dataSource.deleteForms();
                        }
                    }

                    clearSharedPreferences();

                    MyApplication.exit(view.getContext());

                    lockCacheWord();

                    if (Preferences.isUninstallOnPanic()) {
                        uninstallTella(view.getContext());
                    }

                    return Completable.complete();
                })
                .blockingAwait();
    }

    @Override
    public void loadPhoneList() {
        /*disposable.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle((Function<DataSource, SingleSource<List<String>>>) DataSource::getTrustedPhonesList)
                .subscribe(phoneList -> {
                    phoneNumbers = phoneList;
                    view.onPhoneListLoaded(phoneList.isEmpty());
                }, throwable -> {
                    Crashlytics.logException(throwable);
                    view.onPhoneListLoadError(throwable);
                })
        );*/
    }

    @Override
    public void destroy() {
        disposable.dispose();
        cacheWordDataSource.dispose();
        view = null;
        cacheWordHandler = null;
    }

    private void lockCacheWord() {
        if (cacheWordHandler != null && !cacheWordHandler.isLocked()) {
            cacheWordHandler.lock();
        }
    }

    private void clearSharedPreferences() {
        Preferences.setPanicMessage(null);
    }

    /*private void sendPanicMessagesOnMain(final List<String> phoneNumbers) {
        ThreadUtil.runOnMain(() -> sendPanicMessages(phoneNumbers));
    }*/

    /*private void sendPanicMessages(final List<String> phoneNumbers) {
        String panicMessage = Preferences.getPanicMessage();

        smsMessage = TextUtils.isEmpty(panicMessage) ?
                appContext.getResources().getString(R.string.default_panic_message) : panicMessage;

        if (Preferences.isPanicGeolocationActive()) {
            LocationProvider.requestSingleUpdate(appContext, location -> {
                if (location != null) {
                    smsMessage += "\n\n" + String.format(appContext.getResources().getString(R.string.location_info),
                            LocationUtil.getLocationData(location));
                }

                sendSMS(phoneNumbers);
            });
        } else {
            sendSMS(phoneNumbers);
        }
    }*/

    /*private void sendSMS(final List<String> phoneNumbers) {
        final String phoneNumber = phoneNumbers.get(0);

        final PendingIntent sentPI = PendingIntent.getBroadcast(appContext, 0,
                new Intent(C.SMS_SENT), 0);

        final PendingIntent deliveredPI = PendingIntent.getBroadcast(appContext, 0,
                new Intent(C.SMS_DELIVERED), 0);

        appContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        appContext.unregisterReceiver(this);

                        phoneNumbers.remove(phoneNumber);
                        if (phoneNumbers.size() > 0) {
                            sendSMS(phoneNumbers);
                            break;
                        } else {
                            showToast(R.string.panic_sent);
                            break;
                        }
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        showToast(R.string.panic_sent_error_generic);
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        showToast(R.string.panic_sent_error_service);
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        showToast(R.string.panic_sent_error_service);
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        showToast(R.string.panic_sent_error_service);
                        break;
                }

            }
        }, new IntentFilter(C.SMS_SENT));

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, smsMessage, sentPI, deliveredPI);
    }*/

    /*private void showToast(@StringRes int resId) {
        Toast.makeText(appContext, appContext.getString(resId), Toast.LENGTH_SHORT).show();
    }*/

    private void uninstallTella(Context context) {
        Uri packageUri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
        Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri);
        context.startActivity(intent);
    }

    @Override
    public void getCollectForm(final String formId) {
        disposable.add(cacheWordDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle(new Function<DataSource, SingleSource<FormHolder>>() {
                    @Override
                    public SingleSource<FormHolder> apply(final DataSource dataSource) {
                        return dataSource.getBlankCollectFormById(formId).flatMap(new Function<CollectForm, SingleSource<FormHolder>>() {
                            @Override
                            public SingleSource<FormHolder> apply(final CollectForm collectForm) {
                                return dataSource.getBlankFormDef(collectForm).toSingle().map(new Function<FormDef, FormHolder>() {
                                    @Override
                                    public FormHolder apply(FormDef formDef) {
                                        return new FormHolder(collectForm, formDef);
                                    }
                                });
                            }
                        });
                    }
                })
                .subscribe(formHolder -> view.getCollectFormSuccess(formHolder.collectForm, formHolder.formDef),
                        throwable -> view.onCollectFormError(throwable)
                )
        );
    }

    private class FormHolder {
        CollectForm collectForm;
        FormDef formDef;

        FormHolder(CollectForm collectForm, FormDef formDef) {
            this.collectForm = collectForm;
            this.formDef = formDef;
        }
    }
}