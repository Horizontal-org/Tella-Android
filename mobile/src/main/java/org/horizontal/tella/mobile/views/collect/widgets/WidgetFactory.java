/*
 * Copyright (C) 2009 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.horizontal.tella.mobile.views.collect.widgets;

import android.content.Context;

import androidx.annotation.NonNull;

import org.javarosa.core.model.Constants;
import org.javarosa.form.api.FormEntryPrompt;

import java.util.Locale;

import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.javarosa.FormUtils;


/**
 * Based on ODK WidgetFactory.
 */
public class WidgetFactory {

    /**
     * Returns the appropriate QuestionWidget for the given FormEntryPrompt.
     *
     * @param fep              prompt element to be rendered
     * @param context          Android context
     * @param readOnlyOverride a flag to be ORed with JR readonly attribute.
     */
    @NonNull
    public static QuestionWidget createWidgetFromPrompt(FormEntryPrompt fep, Context context, boolean readOnlyOverride) {
        if (FormUtils.doesTheFieldBeginWith(fep, context.getString(R.string.tella_location_field_prefix))) {
            return new GeoPointHiddenWidget(context, fep);
        }

        if (FormUtils.doesTheFieldBeginWith(fep, context.getString(R.string.tella_metadata_field_prefix))) {
            return new StringHiddenWidget(context, fep);
        }

        // get appearance hint and clean it up so it is lower case and never null...
        String appearance = fep.getAppearanceHint();
        if (appearance == null) {
            appearance = "";
        }
        // for now, all appearance tags are in english...
        appearance = appearance.toLowerCase(Locale.ENGLISH);
        QuestionWidget questionWidget;

        switch (fep.getControlType()) {
            case Constants.CONTROL_SELECT_ONE:
                if (appearance.startsWith("minimal")) {
                    questionWidget = new SpinnerWidget(context, fep);
                } else {
                    questionWidget = new SelectOneWidget(context, fep);
                }
                break;

            case Constants.CONTROL_INPUT:
                switch (fep.getDataType()) {
                    case Constants.DATATYPE_DATE:
                        questionWidget = new DateWidget(context, fep);
                        break;

                    case Constants.DATATYPE_TIME:
                        questionWidget = new TimeWidget(context, fep);
                        break;

                    case Constants.DATATYPE_INTEGER:
                        questionWidget = new LongWidget(context, fep, readOnlyOverride);
                        break;

                    case Constants.DATATYPE_GEOPOINT:
                        questionWidget = new GeoPointWidget(context, fep);
                        break;

                    default:
                        questionWidget = new StringWidget(context, fep, readOnlyOverride);
                        break;
                }
                break;

            case Constants.CONTROL_IMAGE_CHOOSE:
                if (appearance.equals("signature")) {
                    questionWidget = new SignatureWidget(context, fep);
                } else {
                    questionWidget = new ImageWidget(context, fep);
                }
                break;

            case Constants.CONTROL_AUDIO_CAPTURE:
                questionWidget = new AudioWidget(context, fep);
                break;

            case Constants.CONTROL_VIDEO_CAPTURE:
                questionWidget = new VideoWidget(context, fep);
                break;

            case Constants.CONTROL_SELECT_MULTI:
                questionWidget = new SelectMultiWidget(context, fep);
                break;

            case Constants.CONTROL_TRIGGER:
                questionWidget = new TriggerWidget(context, fep);
                break;

            case Constants.CONTROL_FILE_CAPTURE:
                questionWidget = new DocMediaWidget(context, fep);
                break;

            default:
                questionWidget = new StringWidget(context, fep, readOnlyOverride);
                break;
        }

        return questionWidget;
    }
}