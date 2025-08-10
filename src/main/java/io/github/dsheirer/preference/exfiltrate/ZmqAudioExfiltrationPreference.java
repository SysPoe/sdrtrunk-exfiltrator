/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.preference.exfiltrate;

import io.github.dsheirer.preference.Preference;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.sample.Listener;

import java.util.prefs.Preferences;

/**
 * User preferences for ZMQ audio exfiltration
 */
public class ZmqAudioExfiltrationPreference extends Preference
{
    private final Preferences mPreferences = Preferences.userNodeForPackage(ZmqAudioExfiltrationPreference.class);
    
    private static final String ENABLED = "enabled";
    private static final String ENDPOINT = "endpoint";
    
    private Boolean mEnabled;
    private String mEndpoint;

    /**
     * Constructs this preference with an update listener
     * @param updateListener to receive notifications whenever these preferences change
     */
    public ZmqAudioExfiltrationPreference(Listener<PreferenceType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public PreferenceType getPreferenceType()
    {
        return PreferenceType.ZMQ_AUDIO_EXFILTRATION;
    }

    /**
     * Indicates if ZMQ audio exfiltration is enabled
     */
    public boolean isEnabled()
    {
        if(mEnabled == null)
        {
            mEnabled = mPreferences.getBoolean(ENABLED, false);
        }

        return mEnabled;
    }

    /**
     * Sets the enabled state for ZMQ audio exfiltration
     */
    public void setEnabled(boolean enabled)
    {
        mEnabled = enabled;
        mPreferences.putBoolean(ENABLED, enabled);
        notifyPreferenceUpdated();
    }

    /**
     * Gets the ZMQ endpoint (address:port)
     */
    public String getEndpoint()
    {
        if(mEndpoint == null)
        {
            mEndpoint = mPreferences.get(ENDPOINT, "tcp://*:15023");
        }

        return mEndpoint;
    }

    /**
     * Sets the ZMQ endpoint (address:port)
     */
    public void setEndpoint(String endpoint)
    {
        mEndpoint = endpoint;
        mPreferences.put(ENDPOINT, endpoint);
        notifyPreferenceUpdated();
    }
}
