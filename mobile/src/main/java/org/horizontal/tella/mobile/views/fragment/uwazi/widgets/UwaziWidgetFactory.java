package org.horizontal.tella.mobile.views.fragment.uwazi.widgets;

import android.content.Context;

import androidx.annotation.NonNull;

import org.horizontal.tella.mobile.data.uwazi.UwaziConstants;
import org.horizontal.tella.mobile.views.fragment.uwazi.entry.UwaziEntryPrompt;

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

            case UwaziConstants.UWAZI_DATATYPE_MULTIDATE:
                questionWidget = new UwaziMultiDateWidget(context, fep);
                break;

            case UwaziConstants.UWAZI_DATATYPE_MULTIDATERANGE:
                questionWidget = new UwaziMultiDateRangeWidget(context, fep);
                break;

            case UwaziConstants.UWAZI_DATATYPE_DATERANGE:
                questionWidget = new UwaziDateRangeWidget(context, fep);
                break;

            case UwaziConstants.UWAZI_DATATYPE_GEOLOCATION:
                questionWidget = new UwaziGeoPointWidget(context, fep);
                break;

            case UwaziConstants.UWAZI_DATATYPE_SELECT:
                questionWidget = new UwaziSelectOneWidget(context, fep);
                break;

            case UwaziConstants.UWAZI_DATATYPE_MULTISELECT:
                questionWidget = new UwaziMultiSelectWidget(context, fep);
                break;

            case UwaziConstants.UWAZI_DATATYPE_MULTIFILES:
                questionWidget = new UwaziMultiFileWidget(context, fep, false);
                break;

            case UwaziConstants.UWAZI_DATATYPE_MULTIPDFFILES:
                questionWidget = new UwaziMultiFileWidget(context, fep, true);
                break;

            case UwaziConstants.UWAZI_DATATYPE_RELATIONSHIP:
                questionWidget = new UwaziRelationShipWidget(context, fep);
                break;

            case UwaziConstants.UWAZI_DATATYPE_LINK:
                questionWidget = new UwaziLinkWidget(context, fep);
                break;

            case UwaziConstants.UWAZI_DATATYPE_MARKDOWN:
                questionWidget = new UwaziStringWidget(context, fep, readOnlyOverride, true);
                break;

            case UwaziConstants.UWAZI_DATATYPE_GENERATEDID:
                questionWidget = new UwaziGeneratedIdWidget(context, fep, readOnlyOverride);
                break;

            default:
                questionWidget = new UwaziStringWidget(context, fep, readOnlyOverride, false);
                break;
        }

        return questionWidget;
    }
}