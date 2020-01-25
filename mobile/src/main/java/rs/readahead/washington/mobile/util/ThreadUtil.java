package rs.readahead.washington.mobile.util;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;


public class ThreadUtil {
    private static Handler handler = new Handler(Looper.getMainLooper());


    public static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public static void runOnMain(final @NonNull Runnable runnable) {
        if (isMainThread()) {
            runnable.run();
        } else {
            handler.post(runnable);
        }
    }
}
