package rs.readahead.washington.mobile.views.fragment.uwazi.widgets;

import android.content.Context;

import androidx.annotation.NonNull;

import rs.readahead.washington.mobile.data.uwazi.UwaziConstants;
import rs.readahead.washington.mobile.views.fragment.uwazi.entry.UwaziEntryPrompt;

public class UwaziWidgetFactory {

    /**
     * Returns the appropriate UwaziQuestionWidget for the given UwaziEntryPrompt.
     *
     * @param fep              prompt element to be rendered
     * @param context          Android context
     * @param readOnlyOverride a flag to be ORed with JR readonly attribute.
     */
    @NonNull
    public static UwaziQuestionWidget createWidgetFromPrompt(UwaziEntryPrompt fep, Context context, boolean readOnlyOverride) {

        UwaziQuestionWidget questionWidget;

        switch (fep.getDataType()) {

            case UwaziConstants.UWAZI_DATATYPE_NUMERIC:
                questionWidget = new UwaziNumericWidget(context, fep, readOnlyOverride);
                break;

            case UwaziConstants.UWAZI_DATATYPE_MEDIA:
                questionWidget = new UwaziMediaWidget(context, fep);
                break;

            case UwaziConstants.UWAZI_DATATYPE_IMAGE:
                questionWidget = new UwaziImageWidget(context, fep);
                break;

            case UwaziConstants.UWAZI_DATATYPE_DATE:
                questionWidget = new UwaziDateWidget(context, fep);
                break;

            case UwaziConstants.UWAZI_DATATYPE_DATERANGE:
                questionWidget = new UwaziDateRangeWidget(context, fep);
                break;

            case UwaziConstants.UWAZI_DATATYPE_GEOLOCATION:
                questionWidget = new UwaziGeoPointWidget(context, fep);
                break;

            default:
                questionWidget = new UwaziStringWidget(context, fep, readOnlyOverride);
                break;
        }

        return questionWidget;
    }
}