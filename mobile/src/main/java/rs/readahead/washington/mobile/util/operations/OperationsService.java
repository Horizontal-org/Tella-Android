/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018-2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2021 TSI-mc
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2017-2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2015 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2014 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package rs.readahead.washington.mobile.util.operations;

import android.accounts.Account;
import android.accounts.AccountsException;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Pair;

import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.RestoreFileVersionRemoteOperation;
import com.owncloud.android.lib.resources.files.model.FileVersion;
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

public class OperationsService extends Service {

    private static final String TAG = OperationsService.class.getSimpleName();

    public static final String EXTRA_ACCOUNT = "ACCOUNT";
    public static final String EXTRA_SERVER_URL = "SERVER_URL";
    public static final String EXTRA_REMOTE_PATH = "REMOTE_PATH";
    public static final String EXTRA_NEWNAME = "NEWNAME";
    public static final String EXTRA_REMOVE_ONLY_LOCAL = "REMOVE_LOCAL_COPY";
    public static final String EXTRA_SYNC_FILE_CONTENTS = "SYNC_FILE_CONTENTS";
    public static final String EXTRA_NEW_PARENT_PATH = "NEW_PARENT_PATH";
    public static final String EXTRA_FILE = "FILE";
    public static final String EXTRA_FILE_VERSION = "FILE_VERSION";
    public static final String EXTRA_SHARE_PASSWORD = "SHARE_PASSWORD";
    public static final String EXTRA_SHARE_TYPE = "SHARE_TYPE";
    public static final String EXTRA_SHARE_WITH = "SHARE_WITH";
    public static final String EXTRA_SHARE_EXPIRATION_DATE_IN_MILLIS = "SHARE_EXPIRATION_YEAR";
    public static final String EXTRA_SHARE_PERMISSIONS = "SHARE_PERMISSIONS";
    public static final String EXTRA_SHARE_PUBLIC_LABEL = "SHARE_PUBLIC_LABEL";
    public static final String EXTRA_SHARE_HIDE_FILE_DOWNLOAD = "HIDE_FILE_DOWNLOAD";
    public static final String EXTRA_SHARE_ID = "SHARE_ID";
    public static final String EXTRA_SHARE_NOTE = "SHARE_NOTE";
    public static final String EXTRA_IN_BACKGROUND = "IN_BACKGROUND";

    public static final String ACTION_CREATE_SHARE_VIA_LINK = "CREATE_SHARE_VIA_LINK";
    public static final String ACTION_CREATE_SECURE_FILE_DROP = "CREATE_SECURE_FILE_DROP";
    public static final String ACTION_CREATE_SHARE_WITH_SHAREE = "CREATE_SHARE_WITH_SHAREE";
    public static final String ACTION_UNSHARE = "UNSHARE";
    public static final String ACTION_UPDATE_PUBLIC_SHARE = "UPDATE_PUBLIC_SHARE";
    public static final String ACTION_UPDATE_USER_SHARE = "UPDATE_USER_SHARE";
    public static final String ACTION_UPDATE_SHARE_NOTE = "UPDATE_SHARE_NOTE";
    public static final String ACTION_UPDATE_SHARE_INFO = "UPDATE_SHARE_INFO";
    public static final String ACTION_GET_SERVER_INFO = "GET_SERVER_INFO";
    public static final String ACTION_GET_USER_NAME = "GET_USER_NAME";
    public static final String ACTION_RENAME = "RENAME";
    public static final String ACTION_REMOVE = "REMOVE";
    public static final String ACTION_CREATE_FOLDER = "CREATE_FOLDER";
    public static final String ACTION_SYNC_FILE = "SYNC_FILE";
    public static final String ACTION_SYNC_FOLDER = "SYNC_FOLDER";
    public static final String ACTION_MOVE_FILE = "MOVE_FILE";
    public static final String ACTION_COPY_FILE = "COPY_FILE";
    public static final String ACTION_CHECK_CURRENT_CREDENTIALS = "CHECK_CURRENT_CREDENTIALS";
    public static final String ACTION_RESTORE_VERSION = "RESTORE_VERSION";

    private ServiceHandler mOperationsHandler;
    private OperationsServiceBinder mOperationsBinder;

    private ConcurrentMap<Integer, Pair<RemoteOperation, RemoteOperationResult>>
        mUndispatchedFinishedOperations = new ConcurrentHashMap<>();


    private static class Target {
        public Uri mServerUrl;
        public Account mAccount;

        public Target(Account account, Uri serverUrl) {
            mAccount = account;
            mServerUrl = serverUrl;
        }
    }

    /**
     * Service initialization
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log_OC.d(TAG, "Creating service");

        // First worker thread for most of operations
        HandlerThread thread = new HandlerThread("Operations thread",
                                                 Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mOperationsHandler = new ServiceHandler(thread.getLooper(), this);
        mOperationsBinder = new OperationsServiceBinder(mOperationsHandler);

        // Separated worker thread for download of folders (WIP)
        thread = new HandlerThread("Syncfolder thread", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
    }

    /**
     * Entry point to add a new operation to the queue of operations.
     * <p/>
     * New operations are added calling to startService(), resulting in a call to this method. This ensures the service
     * will keep on working although the caller activity goes away.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log_OC.d(TAG, "Starting command with id " + startId);

        // WIP: for the moment, only SYNC_FOLDER is expected here;
        // the rest of the operations are requested through the Binder
        if (intent != null && ACTION_SYNC_FOLDER.equals(intent.getAction())) {

            if (!intent.hasExtra(EXTRA_ACCOUNT) || !intent.hasExtra(EXTRA_REMOTE_PATH)) {
                Log_OC.e(TAG, "Not enough information provided in intent");
                return START_NOT_STICKY;
            }

            Account account = IntentExtensionsKt.getParcelableArgument(intent, EXTRA_ACCOUNT, Account.class);
            String remotePath = intent.getStringExtra(EXTRA_REMOTE_PATH);

            Pair<Account, String> itemSyncKey = new Pair<>(account, remotePath);

            Pair<Target, RemoteOperation> itemToQueue = newOperation(intent);
            if (itemToQueue != null) {
            }

        } else {
            Message msg = mOperationsHandler.obtainMessage();
            msg.arg1 = startId;
            mOperationsHandler.sendMessage(msg);
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log_OC.v(TAG, "Destroying service");
        // Saving cookies
    /*    OwnCloudClientManagerFactory.getDefaultSingleton()
            .saveAllClients(this);*/

        mUndispatchedFinishedOperations.clear();

        mOperationsBinder = null;

        mOperationsHandler.getLooper().quit();
        mOperationsHandler = null;

        super.onDestroy();
    }

    /**
     * Provides a binder object that clients can use to perform actions on the queue of operations, except the addition
     * of new operations.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mOperationsBinder;
    }


    /**
     * Called when ALL the bound clients were unbound.
     */
    @Override
    public boolean onUnbind(Intent intent) {
        mOperationsBinder.clearListeners();
        return false;   // not accepting rebinding (default behaviour)
    }


    /**
     * Binder to let client components to perform actions on the queue of operations.
     * <p/>
     * It provides by itself the available operations.
     */
    public class OperationsServiceBinder extends Binder /* implements OnRemoteOperationListener */ {

        /**
         * Map of listeners that will be reported about the end of operations from a {@link OperationsServiceBinder}
         * instance
         */
        private final ConcurrentMap<OnRemoteOperationListener, Handler> mBoundListeners = new ConcurrentHashMap<>();

        private ServiceHandler mServiceHandler;

        public OperationsServiceBinder(ServiceHandler serviceHandler) {
            mServiceHandler = serviceHandler;
        }


        /**
         * Cancels a pending or current synchronization.
         *
         * @param account ownCloud account where the remote folder is stored.
         * @param file    A folder in the queue of pending synchronizations
         */
        public void cancel(Account account, OCFile file) {
           // mSyncFolderHandler.cancel(account, file);
        }


        public void clearListeners() {

            mBoundListeners.clear();
        }


        /**
         * Adds a listener interested in being reported about the end of operations.
         *
         * @param listener        Object to notify about the end of operations.
         * @param callbackHandler {@link Handler} to access the listener without breaking Android threading protection.
         */
        public void addOperationListener(OnRemoteOperationListener listener,
                                         Handler callbackHandler) {
            synchronized (mBoundListeners) {
                mBoundListeners.put(listener, callbackHandler);
            }
        }


        /**
         * Removes a listener from the list of objects interested in the being reported about the end of operations.
         *
         * @param listener Object to notify about progress of transfer.
         */
        public void removeOperationListener(OnRemoteOperationListener listener) {
            synchronized (mBoundListeners) {
                mBoundListeners.remove(listener);
            }
        }


        /**
         * TODO - IMPORTANT: update implementation when more operations are moved into the service
         *
         * @return 'True' when an operation that enforces the user to wait for completion is in process.
         */
        public boolean isPerformingBlockingOperation() {
            return !mServiceHandler.mPendingOperations.isEmpty();
        }


        /**
         * Creates and adds to the queue a new operation, as described by operationIntent.
         * <p>
         * Calls startService to make the operation is processed by the ServiceHandler.
         *
         * @param operationIntent Intent describing a new operation to queue and execute.
         * @return Identifier of the operation created, or null if failed.
         */
        public long queueNewOperation(Intent operationIntent) {
            Pair<Target, RemoteOperation> itemToQueue = newOperation(operationIntent);
            if (itemToQueue != null) {
                mServiceHandler.mPendingOperations.add(itemToQueue);
                startService(new Intent(OperationsService.this, OperationsService.class));
                return itemToQueue.second.hashCode();

            } else {
                return Long.MAX_VALUE;
            }
        }

        public boolean dispatchResultIfFinished(int operationId,
                                                OnRemoteOperationListener listener) {
            Pair<RemoteOperation, RemoteOperationResult> undispatched =
                mUndispatchedFinishedOperations.remove(operationId);
            if (undispatched != null) {
                listener.onRemoteOperationFinish(undispatched.first, undispatched.second);
                return true;
            } else {
                return !mServiceHandler.mPendingOperations.isEmpty();
            }
        }

    }


    /**
     * Operations worker. Performs the pending operations in the order they were requested.
     * <p>
     * Created with the Looper of a new thread, started in {@link OperationsService#onCreate()}.
     */
    private static class ServiceHandler extends Handler {
        // don't make it a final class, and don't remove the static ; lint will warn about a possible memory leak

        OperationsService mService;


        private ConcurrentLinkedQueue<Pair<Target, RemoteOperation>> mPendingOperations =
            new ConcurrentLinkedQueue<>();
        private RemoteOperation mCurrentOperation;
        private Target mLastTarget;
        private OwnCloudClient mOwnCloudClient;

        public ServiceHandler(Looper looper, OperationsService service) {
            super(looper);
            if (service == null) {
                throw new IllegalArgumentException("Received invalid NULL in parameter 'service'");
            }
            mService = service;
        }

        @Override
        public void handleMessage(Message msg) {
            nextOperation();
            Log_OC.d(TAG, "Stopping after command with id " + msg.arg1);
            mService.stopSelf(msg.arg1);
        }

        /**
         * Performs the next operation in the queue
         */
        private void nextOperation() {

            //Log_OC.e(TAG, "nextOperation init" );

            Pair<Target, RemoteOperation> next;
            synchronized (mPendingOperations) {
                next = mPendingOperations.peek();
            }

            if (next != null) {
                mCurrentOperation = next.second;
                RemoteOperationResult result;
                try {
                    /// prepare client object to send the request to the ownCloud server
                    if (mLastTarget == null || !mLastTarget.equals(next.first)) {
                        mLastTarget = next.first;
                        OwnCloudAccount ocAccount;
                        if (mLastTarget.mAccount != null) {
                            ocAccount = new OwnCloudAccount(mLastTarget.mAccount, mService);
                        } else {
                            ocAccount = new OwnCloudAccount(mLastTarget.mServerUrl, null);
                        }
                        mOwnCloudClient = OwnCloudClientManagerFactory.getDefaultSingleton().
                            getClientFor(ocAccount, mService);
                    }

                    /// perform the operation
                    result = mCurrentOperation.execute(mOwnCloudClient);
                } catch (AccountsException e) {
                    if (mLastTarget.mAccount == null) {
                        Log_OC.e(TAG, "Error while trying to get authorization for a NULL account",
                                 e);
                    } else {
                        Log_OC.e(TAG, "Error while trying to get authorization for " +
                            mLastTarget.mAccount.name, e);
                    }
                    result = new RemoteOperationResult(e);

                } catch (IOException e) {
                    if (mLastTarget.mAccount == null) {
                        Log_OC.e(TAG, "Error while trying to get authorization for a NULL account",
                                 e);
                    } else {
                        Log_OC.e(TAG, "Error while trying to get authorization for " +
                            mLastTarget.mAccount.name, e);
                    }
                    result = new RemoteOperationResult(e);
                } catch (Exception e) {
                    if (mLastTarget.mAccount == null) {
                        Log_OC.e(TAG, "Unexpected error for a NULL account", e);
                    } else {
                        Log_OC.e(TAG, "Unexpected error for " + mLastTarget.mAccount.name, e);
                    }
                    result = new RemoteOperationResult(e);

                } finally {
                    synchronized (mPendingOperations) {
                        mPendingOperations.poll();
                    }
                }

                //sendBroadcastOperationFinished(mLastTarget, mCurrentOperation, result);
                mService.dispatchResultToOperationListeners(mCurrentOperation, result);
            }
        }
    }


    /**
     * Creates a new operation, as described by operationIntent.
     * <p>
     * TODO - move to ServiceHandler (probably)
     *
     * @param operationIntent Intent describing a new operation to queue and execute.
     * @return Pair with the new operation object and the information about its target server.
     */
    private Pair<Target, RemoteOperation> newOperation(Intent operationIntent) {
        RemoteOperation operation = null;
        Target target = null;
        try {
            if (!operationIntent.hasExtra(EXTRA_ACCOUNT) &&
                !operationIntent.hasExtra(EXTRA_SERVER_URL)) {
                Log_OC.e(TAG, "Not enough information provided in intent");

            } else {
                Account account = IntentExtensionsKt.getParcelableArgument(operationIntent, EXTRA_ACCOUNT, Account.class);
                String serverUrl = operationIntent.getStringExtra(EXTRA_SERVER_URL);
                target = new Target(account, (serverUrl == null) ? null : Uri.parse(serverUrl));

                String action = operationIntent.getAction();

                switch (action) {
                    case ACTION_GET_SERVER_INFO:
                        operation = new GetServerInfoOperation(serverUrl, this);
                        break;

                    case ACTION_GET_USER_NAME:
                        operation = new GetUserInfoRemoteOperation();
                        break;


                    case ACTION_RESTORE_VERSION:
                        FileVersion fileVersion = IntentExtensionsKt.getParcelableArgument(operationIntent, EXTRA_FILE_VERSION, FileVersion.class);
                        operation = new RestoreFileVersionRemoteOperation(fileVersion.getLocalId(),
                                                                          fileVersion.getFileName());
                        break;

                    default:
                        // do nothing
                        break;
                }
            }

        } catch (IllegalArgumentException e) {
            Log_OC.e(TAG, "Bad information provided in intent: " + e.getMessage());
            operation = null;
        }

        if (operation != null) {
            return new Pair<>(target, operation);
        } else {
            return null;
        }
    }


    /**
     * Notifies the currently subscribed listeners about the end of an operation.
     *
     * @param operation Finished operation.
     * @param result    Result of the operation.
     */
    protected void dispatchResultToOperationListeners(
        final RemoteOperation operation, final RemoteOperationResult result
                                                     ) {
        int count = 0;
        Iterator<OnRemoteOperationListener> listeners = mOperationsBinder.mBoundListeners.keySet().iterator();
        while (listeners.hasNext()) {
            final OnRemoteOperationListener listener = listeners.next();
            final Handler handler = mOperationsBinder.mBoundListeners.get(listener);
            if (handler != null) {
                handler.post(() -> listener.onRemoteOperationFinish(operation, result));
                count += 1;
            }
        }
        if (count == 0) {
            Pair<RemoteOperation, RemoteOperationResult> undispatched = new Pair<>(operation, result);
            mUndispatchedFinishedOperations.put(operation.hashCode(), undispatched);
        }
        Log_OC.d(TAG, "Called " + count + " listeners");
    }
}
