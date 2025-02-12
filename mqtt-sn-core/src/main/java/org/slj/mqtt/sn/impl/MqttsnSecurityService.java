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

package org.slj.mqtt.sn.impl;

import org.slj.mqtt.sn.model.INetworkContext;
import org.slj.mqtt.sn.model.MqttsnSecurityOptions;
import org.slj.mqtt.sn.spi.IMqttsnSecurityService;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnSecurityException;
import org.slj.mqtt.sn.spi.AbstractMqttsnService;
import org.slj.mqtt.sn.utils.Security;

import java.nio.charset.StandardCharsets;

public class MqttsnSecurityService
        extends AbstractMqttsnService implements IMqttsnSecurityService {

    public boolean protocolIntegrityEnabled(){
        MqttsnSecurityOptions securityOptions = registry.getOptions().getSecurityOptions();
        if(securityOptions != null) {
            return MqttsnSecurityOptions.INTEGRITY_TYPE.none != securityOptions.getIntegrityType() &&
                    MqttsnSecurityOptions.INTEGRITY_POINT.protocol_messages == securityOptions.getIntegrityPoint();
        }
        return false;
    }

    public boolean payloadIntegrityEnabled(){
        MqttsnSecurityOptions securityOptions = registry.getOptions().getSecurityOptions();
        if(securityOptions != null) {
            return MqttsnSecurityOptions.INTEGRITY_TYPE.none != securityOptions.getIntegrityType() &&
                    MqttsnSecurityOptions.INTEGRITY_POINT.publish_data == securityOptions.getIntegrityPoint();
        }
        return false;
    }

    public byte[] readVerified(INetworkContext context, byte[] data) throws MqttsnSecurityException {
        MqttsnSecurityOptions securityOptions = registry.getOptions().getSecurityOptions();
        int beforeSize = data.length;
        if(securityOptions != null) {
            MqttsnSecurityOptions.INTEGRITY_TYPE type = securityOptions.getIntegrityType();
            if (type != MqttsnSecurityOptions.INTEGRITY_TYPE.none) {
                int length;
                boolean verified;
                try {
                    if(securityOptions.getIntegrityType() == MqttsnSecurityOptions.INTEGRITY_TYPE.hmac){
                        verified = Security.verifyHMac(securityOptions.getIntegrityHmacAlgorithm(),
                                securityOptions.getIntegrityKey().getBytes(StandardCharsets.UTF_8), data);
                        length = securityOptions.getIntegrityHmacAlgorithm().getSize();
                    } else {
                        verified = Security.verifyChecksum(securityOptions.getIntegrityChecksumAlgorithm(), data);
                        length = securityOptions.getIntegrityHmacAlgorithm().getSize();
                    }
                    if(verified){
                        data = Security.readOriginalData(length, data);
                        logger.info("integrity check verified {} bytes of data becomes {} bytes", beforeSize, data.length);
                        return data;
                    } else {
                        throw new MqttsnSecurityException("message integrity check failed");
                    }
                } catch(MqttsnException e){
                    throw new MqttsnSecurityException("security configuration error;", e);
                }
            }
        }
        return data;
    }

    public byte[] writeVerified(INetworkContext context, byte[] data) throws MqttsnSecurityException {
        MqttsnSecurityOptions securityOptions = registry.getOptions().getSecurityOptions();
        int beforeSize = data.length;
        if(securityOptions != null) {
            MqttsnSecurityOptions.INTEGRITY_TYPE type = securityOptions.getIntegrityType();
            if (type != MqttsnSecurityOptions.INTEGRITY_TYPE.none) {
                try {
                    if(securityOptions.getIntegrityType() == MqttsnSecurityOptions.INTEGRITY_TYPE.hmac){
                        data = Security.createHmacdData(securityOptions.getIntegrityHmacAlgorithm(),
                                securityOptions.getIntegrityKey().getBytes(StandardCharsets.UTF_8), data);
                    } else {
                        data = Security.createChecksumdData(securityOptions.getIntegrityChecksumAlgorithm(), data);
                    }

                    logger.info("integrity process {} bytes of data becomes {} bytes", beforeSize, data.length);

                } catch(MqttsnException e){
                    throw new MqttsnSecurityException("security configuration error;", e);
                }
            }
        }
        return data;
    }
}
