/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 */
package org.apache.dolphinscheduler.plugin.alert.voice;

import org.apache.dolphinscheduler.alert.api.AlertResult;

/**
 * VoiceService is used to abstract the external voice-calling logic
 * so we can mock it in unit tests.
 */
public interface VoiceService {

    /**
     * Sends a voice alert using the given VoiceParam.
     * @param voiceParam needed parameters such as phone number, TTS code, etc.
     * @return AlertResult indicating success or failure
     */
    AlertResult sendVoiceAlert(VoiceParam voiceParam);

}