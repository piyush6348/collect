/*
 * Copyright (C) 2017 Shobhit
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

package org.odk.collect.android.utilities;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;
import org.odk.collect.android.R;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.preferences.AdminSharedPreferences;
import org.odk.collect.android.preferences.GeneralSharedPreferences;
import org.odk.collect.android.preferences.PreferenceKeys;

import java.util.Collection;
import java.util.HashSet;

import timber.log.Timber;

import static org.odk.collect.android.preferences.AdminKeys.ALL_KEYS;
import static org.odk.collect.android.preferences.AdminKeys.KEY_ADMIN_PW;
import static org.odk.collect.android.preferences.PreferenceKeys.GENERAL_KEYS;
import static org.odk.collect.android.preferences.PreferenceKeys.KEY_PASSWORD;


public class SharedPreferencesUtils {

    private final Context context = Collect.getInstance();

    String getJSONFromPreferences(Collection<String> keys) throws JSONException {
        keys.addAll(GENERAL_KEYS.keySet());

        SharedPreferencesUtils obj = new SharedPreferencesUtils();
        JSONObject sharedPrefJson = obj.getModifiedPrefs(keys);
        Timber.i(sharedPrefJson.toString());
        return sharedPrefJson.toString();
    }

    private JSONObject getModifiedPrefs(Collection<String> keys) throws JSONException {
        JSONObject prefs = new JSONObject();
        JSONObject adminPrefs = new JSONObject();
        JSONObject generalPrefs = new JSONObject();

        //checking for admin password
        if (keys.contains(KEY_ADMIN_PW)) {
            String password = (String) AdminSharedPreferences.getInstance().get(KEY_ADMIN_PW);
            if (!password.equals("")) {
                adminPrefs.put(KEY_ADMIN_PW, password);
            }
            keys.remove(KEY_ADMIN_PW);
        }

        for (String key : keys) {
            Object defaultValue = GENERAL_KEYS.get(key);
            Object value = GeneralSharedPreferences.getInstance().get(key);

            if (value == null) {
                value = "";
            }
            if (defaultValue == null) {
                defaultValue = "";
            }

            if (!defaultValue.equals(value)) {
                generalPrefs.put(key, value);
            }
        }
        prefs.put("general", generalPrefs);

        for (String key : ALL_KEYS) {

            Object defaultValue = AdminSharedPreferences.getInstance().getDefault(key);
            Object value = AdminSharedPreferences.getInstance().get(key);
            if (defaultValue != value) {
                adminPrefs.put(key, value);
            }
        }
        prefs.put("admin", adminPrefs);

        return prefs;
    }

    public void savePreferencesFromJSON(JSONObject settingsJson) throws JSONException {

        JSONObject generalPrefsJson = settingsJson.getJSONObject("general");
        JSONObject adminPrefsJson = settingsJson.getJSONObject("admin");

        for (String key : getAllGeneralKeys()) {

            if (generalPrefsJson.has(key)) {
                Object value = generalPrefsJson.get(key);
                GeneralSharedPreferences.getInstance().save(key, value);
            } else {
                GeneralSharedPreferences.getInstance().reset(key);
            }
        }

        for (String key : getAllAdminKeys()) {

            if (adminPrefsJson.has(key)) {
                Object value = adminPrefsJson.get(key);
                AdminSharedPreferences.getInstance().save(key, value);
            } else {
                AdminSharedPreferences.getInstance().reset(key);
            }
        }

        checkQRCodeForRemovedSettings(generalPrefsJson);
        AuthDialogUtility.setWebCredentialsFromPreferences(context);

        //settings import confirmation toast
        ToastUtils.showLongToast(context.getString(R.string.successfully_imported_settings));
    }

    /**
     * This method is to provide backward compatibility with v1.7.0 and below
     * Autosend was originally set into separate wifi and cellular autosend settings
     */
    private void checkQRCodeForRemovedSettings(JSONObject generalPrefsJson) {

        String[] preferences = {
                PreferenceKeys.KEY_AUTOSEND_WIFI,
                PreferenceKeys.KEY_AUTOSEND_NETWORK
        };

        String autosend = "";
        if (generalPrefsJson.has(preferences[0]) && generalPrefsJson.has(preferences[1])) {
            autosend = "wifi_and_cellular";
        } else if (generalPrefsJson.has(preferences[0])) {
            autosend = "wifi_only";
        } else if (generalPrefsJson.has(preferences[1])) {
            autosend = "cellular_only";
        }

        if (!autosend.equals("")) {
            GeneralSharedPreferences.getInstance().save(PreferenceKeys.KEY_AUTOSEND, autosend);
        }
    }

    private Collection<String> getAllGeneralKeys() {
        Collection<String> keys = new HashSet<>(GENERAL_KEYS.keySet());
        keys.add(KEY_PASSWORD);
        return keys;
    }

    private Collection<String> getAllAdminKeys() {
        Collection<String> keys = new HashSet<>(ALL_KEYS);
        keys.add(KEY_ADMIN_PW);
        return keys;
    }
}

