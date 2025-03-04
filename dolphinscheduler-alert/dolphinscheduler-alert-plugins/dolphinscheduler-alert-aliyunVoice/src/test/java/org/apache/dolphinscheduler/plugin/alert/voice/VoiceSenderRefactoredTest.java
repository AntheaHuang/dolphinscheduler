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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * VoiceSenderRefactoredTest demonstrates how to test the new 'sendViaService' method
 * using a mock VoiceService, thus avoiding real external calls.
 */
public class VoiceSenderRefactoredTest {

    private VoiceParam voiceParam;
    private VoiceService mockService;
    private VoiceSender voiceSender;

    @BeforeEach
    void setUp() {
        // Prepare a sample VoiceParam
        voiceParam = new VoiceParam();
        voiceParam.setCalledNumber("12345678910");
        voiceParam.setTtsCode("TTS_TEST_CODE");

        VoiceParam.Connection conn = new VoiceParam.Connection();
        conn.setAddress("test.aliyuncs.com");
        conn.setAccessKeyId("mockKeyId");
        conn.setAccessKeySecret("mockKeySecret");
        voiceParam.setConnection(conn);

        // Initialize the VoiceSender
        voiceSender = new VoiceSender(voiceParam);

        // Create a mock VoiceService
        mockService = mock(VoiceService.class);
    }

    @Test
    void testSendViaService_Success() {
        // Arrange: mock a successful AlertResult
        AlertResult successResult = new AlertResult();
        successResult.setSuccess(true);
        successResult.setMessage("Mock call success");

        when(mockService.sendVoiceAlert(voiceParam)).thenReturn(successResult);

        // Act
        AlertResult result = voiceSender.sendViaService(mockService);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals("Mock call success", result.getMessage());
        verify(mockService, times(1)).sendVoiceAlert(voiceParam);
    }

    @Test
    void testSendViaService_Failure() {
        // Arrange: mock a failure AlertResult
        AlertResult failureResult = new AlertResult();
        failureResult.setSuccess(false);
        failureResult.setMessage("Mock call failed");

        when(mockService.sendVoiceAlert(voiceParam)).thenReturn(failureResult);

        // Act
        AlertResult result = voiceSender.sendViaService(mockService);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("Mock call failed", result.getMessage());
        verify(mockService, times(1)).sendVoiceAlert(voiceParam);
    }
}