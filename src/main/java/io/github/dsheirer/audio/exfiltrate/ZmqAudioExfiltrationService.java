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

package io.github.dsheirer.audio.exfiltrate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.exfiltrate.ZmqAudioExfiltrationPreference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ.Socket;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ZeroMQ-based audio exfiltration service for real-time streaming of decoded DMR Tier 3 audio
 * with associated metadata including radio IDs, talkgroups, frequencies, and channel information.
 * 
 * Note: This is a placeholder implementation that will be completed once ZeroMQ dependencies
 * are properly resolved in the build environment.
 */
public class ZmqAudioExfiltrationService
{
    private static final Logger mLog = LoggerFactory.getLogger(ZmqAudioExfiltrationService.class);
    private static ZmqAudioExfiltrationService INSTANCE;
    
    // ZeroMQ context and socket
    private ZContext mContext;
    private Socket mPublisher;
    private final Gson mGson;
    private final AtomicBoolean mEnabled = new AtomicBoolean(false);
    private String mEndpoint;
    private UserPreferences mUserPreferences;
    private ZmqAudioExfiltrationPreference mPreference;
    private AliasModel mAliasModel;
    
    /**
     * Private constructor for singleton pattern
     */
    private ZmqAudioExfiltrationService()
    {
        mGson = new GsonBuilder().setPrettyPrinting().create();
    }
    
    /**
     * Gets the singleton instance of the ZMQ audio exfiltration service
     */
    public static ZmqAudioExfiltrationService getInstance()
    {
        if(INSTANCE == null)
        {
            synchronized(ZmqAudioExfiltrationService.class)
            {
                if(INSTANCE == null)
                {
                    INSTANCE = new ZmqAudioExfiltrationService();
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * Initializes the service with user preferences and alias model
     */
    public void initialize(UserPreferences userPreferences, AliasModel aliasModel)
    {
        mUserPreferences = userPreferences;
        mAliasModel = aliasModel;
        mPreference = userPreferences.getZmqAudioExfiltrationPreference();
        
        mLog.info("ZMQ Audio Exfiltration Service initialized");
        
        // Load settings from preferences
        updateFromPreferences();
    }
    
    /**
     * Updates service configuration from user preferences
     */
    public void updateFromPreferences()
    {
        if(mPreference != null)
        {
            boolean enabled = mPreference.isEnabled();
            String endpoint = mPreference.getEndpoint();
            
            mLog.info("ZMQ Audio Exfiltration preference update - Enabled: {}, Endpoint: {}", enabled, endpoint);
            
            // Set endpoint first, then enable (so endpoint is available when start() is called)
            setEndpoint(endpoint);
            setEnabled(enabled);
        }
        else
        {
            mLog.warn("ZMQ Audio Exfiltration preference is null - service will remain disabled");
        }
    }
    
    /**
     * Enables or disables the exfiltration service
     */
    public void setEnabled(boolean enabled)
    {
        if(enabled && !mEnabled.get())
        {
            start();
        }
        else if(!enabled && mEnabled.get())
        {
            stop();
        }
    }
    
    /**
     * Sets the ZeroMQ endpoint
     */
    public void setEndpoint(String endpoint)
    {
        if(endpoint != null && !endpoint.equals(mEndpoint))
        {
            mEndpoint = endpoint;
            if(mEnabled.get())
            {
                // Restart with new endpoint
                stop();
                start();
            }
        }
    }
    
    /**
     * Gets the current endpoint
     */
    public String getEndpoint()
    {
        return mEndpoint;
    }
    
    /**
     * Indicates if the service is enabled
     */
    public boolean isEnabled()
    {
        return mEnabled.get();
    }
    
    /**
     * Starts the ZeroMQ publisher
     */
    private void start()
    {
        try
        {
            mContext = new ZContext();
            mPublisher = mContext.createSocket(SocketType.PUB);
            
            // Configure socket options for better reliability
            mPublisher.setLinger(1000); // Wait max 1 second on close
            mPublisher.setSndHWM(1000);  // High water mark for send queue
            
            if(mEndpoint != null)
            {
                mPublisher.bind(mEndpoint);
                mEnabled.set(true);
                mLog.info("ZMQ Audio Exfiltration Service started on endpoint: " + mEndpoint);
            }
            else
            {
                mLog.error("Cannot start ZMQ Audio Exfiltration Service - no endpoint configured");
            }
        }
        catch(Exception e)
        {
            mLog.error("Failed to start ZMQ Audio Exfiltration Service", e);
            stop();
        }
    }
    
    /**
     * Stops the ZeroMQ publisher
     */
    private void stop()
    {
        mEnabled.set(false);
        
        if(mPublisher != null)
        {
            mPublisher.close();
            mPublisher = null;
        }
        
        if(mContext != null)
        {
            mContext.close();
            mContext = null;
        }
        
        mLog.info("ZMQ Audio Exfiltration Service stopped");
    }
    
    /**
     * Publishes audio data with metadata to ZeroMQ
     */
    public void publishAudio(float[] audioSamples, IdentifierCollection identifierCollection, 
                           String protocol, int timeslot, long timestamp, long frequency)
    {
        if(!mEnabled.get() || mPublisher == null)
        {
            return;
        }
        
        try
        {
            // Convert float samples to 16-bit PCM
            byte[] pcmData = convertToPCM16(audioSamples);
            
            // Extract metadata in the format expected by the consumer
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("protocol", protocol);
            metadata.put("timeslot", timeslot);
            metadata.put("frequency", frequency);
            
            // Extract identifier information
            extractIdentifierInformation(metadata, identifierCollection);
            
            // Create complete message in the format expected by the consumer
            Map<String, Object> message = new HashMap<>();
            message.put("audio", Arrays.toString(pcmData));  // Convert to string representation
            message.put("metadata", metadata);
            message.put("timestamp", timestamp);
            
            // Serialize and publish
            String json = mGson.toJson(message);
            
            try 
            {
                mPublisher.send(json.getBytes("UTF-8"), 0);
            }
            catch(IndexOutOfBoundsException e)
            {
                // ZMQ library bug when no subscribers are connected - ignore this error
                mLog.debug("ZMQ send failed (no subscribers): {}", e.getMessage());
            }
            catch(Exception zmqException)
            {
                // Other ZMQ-related errors
                mLog.warn("ZMQ send error: {}", zmqException.getMessage());
            }
            
        }
        catch(Exception e)
        {
            mLog.error("Failed to prepare audio data for ZMQ", e);
        }
    }
    
    /**
     * Converts float audio samples to 16-bit PCM
     */
    private byte[] convertToPCM16(float[] samples)
    {
        ByteBuffer buffer = ByteBuffer.allocate(samples.length * 2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        for(float sample : samples)
        {
            // Clamp to [-1.0, 1.0] and convert to 16-bit
            sample = Math.max(-1.0f, Math.min(1.0f, sample));
            short pcmSample = (short)(sample * 32767);
            buffer.putShort(pcmSample);
        }
        
        return buffer.array();
    }
    
    /**
     * Extracts identifier information and adds it to the metadata map
     */
    private void extractIdentifierInformation(Map<String, Object> metadata, IdentifierCollection identifierCollection)
    {
        if(identifierCollection != null)
        {
            // Extract FROM information (Radio/User)
            List<Identifier> fromIdentifiers = identifierCollection.getIdentifiers(Role.FROM);
            if(!fromIdentifiers.isEmpty())
            {
                Identifier fromId = fromIdentifiers.get(0);
                Map<String, Object> fromInfo = new HashMap<>();
                fromInfo.put("id", extractIdValue(fromId));
                
                String alias = getAlias(fromId, identifierCollection);
                if(alias != null)
                {
                    fromInfo.put("alias", alias);
                }
                
                metadata.put("from", fromInfo);
            }
            
            // Extract TO information (Talkgroup)
            List<Identifier> toIdentifiers = identifierCollection.getIdentifiers(Role.TO);
            if(!toIdentifiers.isEmpty())
            {
                Identifier toId = toIdentifiers.get(0);
                Map<String, Object> toInfo = new HashMap<>();
                toInfo.put("id", extractIdValue(toId));
                
                String alias = getAlias(toId, identifierCollection);
                if(alias != null)
                {
                    toInfo.put("alias", alias);
                }
                
                metadata.put("to", toInfo);
            }
            
            // Extract LCN (Logical Channel Number) if available
            for(Identifier id : identifierCollection.getIdentifiers())
            {
                String idString = id.toString().toLowerCase();
                if(idString.contains("lcn") || idString.contains("logical"))
                {
                    Object value = extractIdValue(id);
                    if(value != null)
                    {
                        metadata.put("lcn", value);
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Extracts the actual ID value from an identifier
     */
    private Object extractIdValue(Identifier identifier)
    {
        if(identifier != null && identifier.getValue() != null)
        {
            return identifier.getValue();
        }
        
        // Fallback to string parsing if getValue() doesn't work
        return extractIntegerFromString(identifier.toString());
    }
    
    /**
     * Extracts integer value from identifier string representation
     */
    private Integer extractIntegerFromString(String text)
    {
        if(text == null) return null;
        
        // Look for numeric patterns in the string
        String[] parts = text.split("\\s+");
        for(String part : parts)
        {
            try
            {
                // Try to parse as integer
                String numbers = part.replaceAll("[^0-9]", "");
                if(!numbers.isEmpty())
                {
                    return Integer.parseInt(numbers);
                }
            }
            catch(NumberFormatException e)
            {
                // Continue to next part
            }
        }
        return null;
    }
    
    /**
     * Get alias for an identifier
     */
    private String getAlias(Identifier identifier, IdentifierCollection identifierCollection)
    {
        if(mAliasModel != null && identifier != null && identifierCollection != null)
        {
            try
            {
                AliasList aliasList = mAliasModel.getAliasList(identifierCollection);
                if(aliasList != null)
                {
                    List<Alias> aliases = aliasList.getAliases(identifier);
                    if(aliases != null && !aliases.isEmpty())
                    {
                        return aliases.get(0).getName();
                    }
                }
            }
            catch(Exception e)
            {
                // Ignore alias lookup errors
            }
        }
        return null;
    }
    
    /**
     * Shuts down the service
     */
    public void shutdown()
    {
        stop();
    }
    
    /**
     * Message structure for audio exfiltration data
     */
    public static class AudioExfiltrationMessage
    {
        public long timestamp;
        public String protocol;
        public Integer timeslot;
        public Long frequency;
        public String audioFormat;
        public Integer sampleRate;
        public String audioData; // Base64 encoded PCM data
        
        // From identifier (Radio)
        public Integer fromId;
        public String fromAlias;
        
        // To identifier (Talkgroup)
        public Integer toId;
        public String toAlias;
        public Boolean compressed; // DMR talkgroup compression
        
        // Channel information
        public Integer lcn; // Logical Channel Number
        public Integer channelId;
        
        // Additional metadata
        public Integer colorCode;
        public Boolean encrypted;
    }
}
