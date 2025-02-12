/*
 * Copyright (c) 2021 Simon Johnson <simon622 AT gmail DOT com>
 *
 * Find me on GitHub:
 * https://github.com/simon622
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.slj.mqtt.sn.wire.version2_0.payload;

import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.MqttsnSpecificationValidator;
import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.spi.IMqttsnMessageValidator;
import org.slj.mqtt.sn.wire.AbstractMqttsnMessage;

public class MqttsnConnack_V2_0 extends AbstractMqttsnMessage implements IMqttsnMessageValidator {

    protected long sessionExpiryInterval;
    protected String assignedClientId;
    protected boolean sessionPresent;   //this field is not yet agreed!!

    @Override
    public int getMessageType() {
        return MqttsnConstants.CONNACK;
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {

        returnCode = readUInt8Adjusted(data, 2);
        sessionPresent = readBooleanAdjusted(data, 3);
        sessionExpiryInterval = readUInt32Adjusted(data, 4);
        if(data.length > 8){
            assignedClientId = readUTF8EncodedStringAdjusted(data, 8);
        }
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {

        int length = 8 + (assignedClientId == null ? 0 : assignedClientId.length() + 2);
        byte[] msg;
        int idx = 0;
        if ((length) > 0xFF) {
            length += 2;
            msg = new byte[length];
            msg[idx++] = (byte) 0x01;
            msg[idx++] = ((byte) (0xFF & (length >> 8)));
            msg[idx++] = ((byte) (0xFF & length));
        } else {
            msg = new byte[length];
            msg[idx++] = (byte) length;
        }

        msg[idx++] = (byte) getMessageType();
        msg[idx++] = (byte) getReturnCode();

        msg[idx++] = sessionPresent ? (byte) 1 : 0;

        writeUInt32(msg, idx, sessionExpiryInterval);
        idx += 4;

        if (assignedClientId != null) {
            writeUTF8EncodedStringData(msg, idx, assignedClientId);
        }

        return msg;
    }

    public long getSessionExpiryInterval() {
        return sessionExpiryInterval;
    }

    public void setSessionExpiryInterval(long sessionExpiryInterval) {
        this.sessionExpiryInterval = sessionExpiryInterval;
    }

    public String getAssignedClientId() {
        return assignedClientId;
    }

    public void setAssignedClientId(String assignedClientId) {
        this.assignedClientId = assignedClientId;
    }

    public boolean isSessionPresent() {
        return sessionPresent;
    }

    public void setSessionPresent(boolean sessionPresent) {
        this.sessionPresent = sessionPresent;
    }

    @Override
    public void validate() throws MqttsnCodecException {
        MqttsnSpecificationValidator.validateReturnCode(returnCode);
        MqttsnSpecificationValidator.validateSessionExpiry(sessionExpiryInterval);
        if(assignedClientId != null) MqttsnSpecificationValidator.validateClientId(assignedClientId);
    }

    @Override
    public String toString() {
        return "MqttsnConnack_V2_0{" +
                "returnCode=" + returnCode +
                ", sessionExpiryInterval=" + sessionExpiryInterval +
                ", assignedClientId='" + assignedClientId + '\'' +
                ", sessionPresent=" + sessionPresent +
                '}';
    }
}
