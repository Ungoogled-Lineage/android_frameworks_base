/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.systemui.tuner;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.View;

import com.android.systemui.plugins.PluginPrefs;
import com.android.systemui.R;

import java.util.List;
import java.util.Set;

public class PluginFragment extends PreferenceFragment {

    public static final String ACTION_PLUGIN_SETTINGS
            = "com.android.systemui.action.PLUGIN_SETTINGS";

    private PluginPrefs mPluginPrefs;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(getContext());
        screen.setOrderingAsAdded(false);
        Context prefContext = getPreferenceManager().getContext();
        mPluginPrefs = new PluginPrefs(getContext());
        Set<String> pluginActions = mPluginPrefs.getPluginList();
        for (String action : pluginActions) {
            String name = action.replace("com.android.systemui.action.PLUGIN_", "");
            PreferenceCategory category = new PreferenceCategory(prefContext);
            category.setTitle(name);

            List<ResolveInfo> result = getContext().getPackageManager().queryIntentServices(
                    new Intent(action), PackageManager.MATCH_DISABLED_COMPONENTS);
            if (result.size() > 0) {
                screen.addPreference(category);
            }
            for (ResolveInfo info : result) {
                category.addPreference(new PluginPreference(prefContext, info));
            }
        }
        setPreferenceScreen(screen);
    }

    private static class PluginPreference extends SwitchPreference {
        private final ComponentName mComponent;
        private final boolean mHasSettings;

        public PluginPreference(Context prefContext, ResolveInfo info) {
            super(prefContext);
            mComponent = new ComponentName(info.serviceInfo.packageName, info.serviceInfo.name);
            PackageManager pm = prefContext.getPackageManager();
            mHasSettings = pm.resolveActivity(new Intent(ACTION_PLUGIN_SETTINGS)
                    .setPackage(mComponent.getPackageName()), 0) != null;
            setTitle(info.serviceInfo.loadLabel(pm));
            setChecked(pm.getComponentEnabledSetting(mComponent)
                    != PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
            setWidgetLayoutResource(R.layout.tuner_widget_settings_switch);
        }

        @Override
        protected boolean persistBoolean(boolean value) {
            getContext().getPackageManager().setComponentEnabledSetting(mComponent,
                    value ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            return true;
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder holder) {
            super.onBindViewHolder(holder);
            holder.findViewById(R.id.settings).setVisibility(mHasSettings ? View.VISIBLE
                    : View.GONE);
            holder.findViewById(R.id.divider).setVisibility(mHasSettings ? View.VISIBLE
                    : View.GONE);
            holder.findViewById(R.id.settings).setOnClickListener(v -> {
                ResolveInfo result = v.getContext().getPackageManager().resolveActivity(
                        new Intent(ACTION_PLUGIN_SETTINGS).setPackage(
                                mComponent.getPackageName()), 0);
                if (result != null) {
                    v.getContext().startActivity(new Intent().setComponent(
                            new ComponentName(result.activityInfo.packageName,
                                    result.activityInfo.name)));
                }
            });
        }
    }
}
