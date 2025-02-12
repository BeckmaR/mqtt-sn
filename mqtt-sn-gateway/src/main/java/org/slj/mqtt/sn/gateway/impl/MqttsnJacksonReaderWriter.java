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

package org.slj.mqtt.sn.gateway.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.slj.mqtt.sn.model.IMqttsnDataRef;
import org.slj.mqtt.sn.model.IntegerDataRef;
import org.slj.mqtt.sn.spi.IMqttsnObjectReaderWriter;
import org.slj.mqtt.sn.spi.MqttsnException;

import java.io.IOException;
import java.io.Serializable;

public class MqttsnJacksonReaderWriter implements IMqttsnObjectReaderWriter {

    protected ObjectMapper mapper;

    public MqttsnJacksonReaderWriter() {
        initializeMapper();
    }

    protected void initializeMapper(){
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule()
                .addAbstractTypeMapping(IMqttsnDataRef.class, IntegerDataRef.class);
        mapper.registerModule(module);
    }

    @Override
    public byte[] write(Serializable o) throws MqttsnException {
        try {
            return mapper.writeValueAsBytes(o);
        } catch (JsonProcessingException e) {
            throw new MqttsnException(e);
        }
    }

    @Override
    public <T extends Serializable> T load(Class<? extends T> clz, byte[] arr)
            throws MqttsnException {
        try {
            return mapper.readValue(arr, clz);
        } catch (IOException e) {
            throw new MqttsnException(e);
        }
    }
}
