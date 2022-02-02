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
           /*
            case UwaziConstants.CONTROL_SELECT_ONE:
                questionWidget = new SelectOneWidget(context, fep);

            case UwaziConstants.CONTROL_INPUT:
                switch (fep.getDataType()) {
                    case UwaziConstants.DATATYPE_DATE:
                        questionWidget = new DateWidget(context, fep);
                        break;

                    case UwaziConstants.DATATYPE_TIME:
                        questionWidget = new TimeWidget(context, fep);
                        break;

                    case UwaziConstants.DATATYPE_INTEGER:
                        questionWidget = new LongWidget(context, fep, readOnlyOverride);
                        break;

                    case UwaziConstants.DATATYPE_GEOPOINT:
                        questionWidget = new GeoPointWidget(context, fep);
                        break;

                    default:
                        questionWidget = new UwaziStringWidget(context, fep, readOnlyOverride);
                        break;
                }
                break;

            case UwaziConstants.CONTROL_IMAGE_CHOOSE:
                questionWidget = new ImageWidget(context, fep);
                break;

            case UwaziConstants.CONTROL_AUDIO_CAPTURE:
                questionWidget = new AudioWidget(context, fep);
                break;

            case UwaziConstants.CONTROL_VIDEO_CAPTURE:
                questionWidget = new VideoWidget(context, fep);
                break;

            case UwaziConstants.CONTROL_SELECT_MULTI:
                questionWidget = new SelectMultiWidget(context, fep);
                break;

            case UwaziConstants.CONTROL_TRIGGER:
                questionWidget = new TriggerWidget(context, fep);
                break;
*/
            case UwaziConstants.UWAZI_DATATYPE_MEDIA:
                questionWidget = new UwaziMediaWidget(context, fep);
                break;

            case UwaziConstants.UWAZI_DATATYPE_DATE:
                questionWidget = new UwaziDateWidget(context, fep);
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