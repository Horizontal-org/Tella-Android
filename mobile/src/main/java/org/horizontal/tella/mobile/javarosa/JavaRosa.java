/*
 * Copyright (C) 2009 JavaRosa
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
package org.horizontal.tella.mobile.javarosa;

import org.javarosa.core.services.IPropertyManager;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.model.xform.XFormsModule;


/**
 * Code taken from: org.odk.collect.android.logic.FormController
 */
public class JavaRosa {
    /**
     * Classes needed to serialize objects. Need to put anything from JR in here.
     */
    private static final String[] SERIALIABLE_CLASSES = {
            "org.javarosa.core.model.SubmissionProfile",
            "org.javarosa.core.model.FormDef",
            "org.javarosa.core.model.QuestionDef",
            "org.javarosa.core.model.RangeQuestion",
            "org.javarosa.core.model.GroupDef",
            "org.javarosa.core.model.instance.TreeReference",
            "org.javarosa.core.model.instance.FormInstance",
            "org.javarosa.core.model.instance.ExternalDataInstance",
            "org.javarosa.core.model.data.BooleanData",
            "org.javarosa.core.model.data.DateData",
            "org.javarosa.core.model.data.DateTimeData",
            "org.javarosa.core.model.data.DecimalData",
            "org.javarosa.core.model.data.GeoPointData",
            "org.javarosa.core.model.data.GeoShapeData",
            "org.javarosa.core.model.data.GeoTraceData",
            "org.javarosa.core.model.data.IntegerData",
            "org.javarosa.core.model.data.LongData",
            "org.javarosa.core.model.data.MultiPointerAnswerData",
            "org.javarosa.core.model.data.PointerAnswerData",
            "org.javarosa.core.model.data.SelectMultiData",
            "org.javarosa.core.model.data.MultipleItemsData",
            "org.javarosa.core.model.data.SelectOneData",
            "org.javarosa.core.model.data.StringData",
            "org.javarosa.core.model.data.TimeData",
            "org.javarosa.core.model.data.UncastData",
            "org.javarosa.core.model.data.helper.BasicDataPointer",
            "org.javarosa.core.model.actions.SetValueAction",
            "org.javarosa.core.model.actions.setgeopoint.StubSetGeopointAction",
            "org.javarosa.core.model.actions.recordaudio.RecordAudioAction"
    };

    private static boolean isJavaRosaInitialized = false;

    /**
     * Isolate the initialization of JavaRosa into one method, called first
     * by the Collect Application.  Called subsequently whenever the Preferences
     * dialogs are exited (to potentially update username and email fields).
     */
    public static synchronized void initializeJavaRosa(IPropertyManager mgr) {
        if (!isJavaRosaInitialized) {
            // need a list of classes that formdef uses
            // unfortunately, the JR registerModule() functions do more than this.
            // register just the classes that would have been registered by:
            // new JavaRosaCoreModule().registerModule();
            // new CoreModelModule().registerModule();
            // replace with direct call to PrototypeManager
            PrototypeManager.registerPrototypes(SERIALIABLE_CLASSES);
            new XFormsModule().registerModule();

            isJavaRosaInitialized = true;
        }

        // needed to override rms property manager
        org.javarosa.core.services.PropertyManager
                .setPropertyManager(mgr);
    }
}
