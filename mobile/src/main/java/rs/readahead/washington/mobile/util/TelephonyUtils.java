package rs.readahead.washington.mobile.util;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;


public class TelephonyUtils {
    private static final String FORMAT = "MCC: %d, MNC: %d, Cell ID: %d";
    private static final String FORMAT_CDMA = "Net ID: %d, Cell ID: %d";
    private static final String FORMAT_INFO = "Cell ID: %d";

    public static List<String> getCellInfo(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        List<String> list = new ArrayList<>();

        if (tm == null) {
            return list;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                if (!PermissionUtil.checkPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    return list;
                }

                for (CellInfo cellInfo : tm.getAllCellInfo()) {
                    addNew(list, cellInfoToString18(cellInfo));
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                if (!PermissionUtil.checkPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    return list;
                }

                for (CellInfo cellInfo : tm.getAllCellInfo()) {
                    addNew(list, cellInfoToString17(cellInfo));
                }
            } else {
                //noinspection deprecation
                //TODO check getNeighboringCellInfo
              //  for (NeighboringCellInfo cellInfo : tm.getNeighboringCellInfo()) {
                //    addNew(list, neighboringCellInfoToString(cellInfo));
             //   }
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        return list;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static String cellInfoToString18(CellInfo cellInfo) {
        if (cellInfo instanceof CellInfoWcdma) {
            CellInfoWcdma w = (CellInfoWcdma) cellInfo;

            return String.format(Locale.ROOT, FORMAT,
                    w.getCellIdentity().getMcc(),
                    w.getCellIdentity().getMnc(),
                    w.getCellIdentity().getCid());
        }

        return cellInfoToString17(cellInfo);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private static String cellInfoToString17(CellInfo cellInfo) {
        if (cellInfo instanceof CellInfoLte) {
            CellInfoLte l = (CellInfoLte) cellInfo;

            return String.format(Locale.ROOT, FORMAT,
                    l.getCellIdentity().getMcc(),
                    l.getCellIdentity().getMnc(),
                    l.getCellIdentity().getCi());

        } else if (cellInfo instanceof CellInfoGsm) {
            CellInfoGsm g = (CellInfoGsm) cellInfo;

            return String.format(Locale.ROOT, FORMAT,
                    g.getCellIdentity().getMcc(),
                    g.getCellIdentity().getMnc(),
                    g.getCellIdentity().getCid());

        } else if (cellInfo instanceof CellInfoCdma) {
            CellInfoCdma c = (CellInfoCdma) cellInfo;

            return String.format(Locale.ROOT, FORMAT_CDMA,
                    c.getCellIdentity().getNetworkId(),
                    c.getCellIdentity().getBasestationId());
        }

        return cellInfo.toString();
    }

    private static void addNew(List<String> list, String str) {
        if (! list.contains(str)) {
            list.add(str);
        }
    }

    private static String neighboringCellInfoToString(NeighboringCellInfo cellInfo) {
        return String.format(Locale.ROOT, FORMAT_INFO,
                cellInfo.getCid());
    }
}
