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

public class MqttsnUnsuback_V2_0 extends AbstractMqttsnMessage implements IMqttsnMessageValidator {

    @Override
    public int getMessageType() {
        return MqttsnConstants.UNSUBACK;
    }

    public boolean needsId() {
        return true;
    }

    @Override
    public void decode(byte[] data) throws MqttsnCodecException {
        id = readUInt16Adjusted(data, 3);
        returnCode = readUInt8Adjusted(data, 7);
    }

    @Override
    public byte[] encode() throws MqttsnCodecException {

        int length = 5;
        int idx = 0;
        byte[] msg = new byte[length];
        msg[idx++] = (byte) length;
        msg[idx++] = (byte) getMessageType();

        msg[idx++] = (byte) ((id >> 8) & 0xFF);
        msg[idx++] = (byte) (id & 0xFF);

        msg[idx++] = (byte) (returnCode);

        return msg;
    }

    @Override
    public String toString() {
        return "MqttsnUnsuback_V2_0{" +
                "id=" + id +
                ", returnCode=" + returnCode +
                '}';
    }

    @Override
    public void validate() throws MqttsnCodecException {
        MqttsnSpecificationValidator.validatePacketIdentifier(id);
        MqttsnSpecificationValidator.validateReturnCode(returnCode);
    }
}