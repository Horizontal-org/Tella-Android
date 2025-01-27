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
package org.horizontal.tella.mobile.javarosa;

import org.javarosa.core.services.IPropertyManager;
import org.javarosa.core.services.properties.IPropertyRules;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.horizontal.tella.mobile.domain.entity.collect.CollectFormInstance;
import org.horizontal.tella.mobile.odk.FormController;


/**
 * Based on OdkCollect PropertyManager
 */
public class PropertyManager implements IPropertyManager {
    public static final String PROPMGR_DEVICE_ID        = "deviceid";
    public static final String PROPMGR_SUBSCRIBER_ID    = "subscriberid";
    public static final String PROPMGR_SIM_SERIAL       = "simserial";
    public static final String PROPMGR_PHONE_NUMBER     = "phonenumber";
    public static final String PROPMGR_USERNAME         = "username";
    public static final String PROPMGR_EMAIL            = "email";

    private final Map<String, String> properties = new HashMap<>();


    public PropertyManager() {
        //properties.put(PROPMGR_DEVICE_ID, "not supported");
    }

    @Override
    public List<String> getProperty(String propertyName) {
        return null;
    }

    @Override
    public void setProperty(String propertyName, String propertyValue) {
    }

    @Override
    public void setProperty(String propertyName, List<String> propertyValue) {
    }

    @Override
    public String getSingularProperty(String propertyName) {
        String propertyValue = null;

        if (PROPMGR_USERNAME.equals(propertyName)) {
            FormController fc = FormController.getActive();
            if (fc != null) {
                CollectFormInstance instance = fc.getCollectFormInstance();
                if (instance != null) {
                    propertyValue = instance.getUsername();
                }
            }
        } else {
            // for now, all property names are in english...
            propertyValue = properties.get(propertyName.toLowerCase(Locale.ENGLISH));
        }

        return propertyValue;
    }

    @Override
    public void addRules(IPropertyRules rules) {
    }

    @Override
    public List<IPropertyRules> getRules() {
        return null;
    }
}
