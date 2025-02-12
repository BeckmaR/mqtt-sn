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

package org.slj.mqtt.sn.spi;

import org.slj.mqtt.sn.model.IMqttsnContext;

/**
 * Bind in a listener to be notified of CONNECTION events from the various sub-systems
 */
public interface IMqttsnConnectionStateListener {

    /**
     * The context has successfully exchanged CONNECT messages with the remote.
     * Generated by the message handler
     * @param context - the origin context
     */
    void notifyConnected(IMqttsnContext context);

    /**
     * An unsolicited DISCONNECT message was received from the context
     * Generated by the message handler
     * @param context - the origin context
     */
    void notifyRemoteDisconnect(IMqttsnContext context);

    /**
     * Where applicable, the runtime has not exchanged any active messages (CONNECT, PUBLISH, SUBSCRIBE, UNSUBSCRIBE)
     * with the remote.
     *
     * NB: This is different from keepAlive, and is a timing around ACTIVE rather than PASSIVE messages. ie. excluding PING
     *
     * Generated by the message state service
     *
     * @param context - the origin context
     */
    void notifyActiveTimeout(IMqttsnContext context);

    /**
     * An error occurred in the local runtime which caused an unsolicited DISCONNECT operation to be sent
     * to the remote
     *
     * NB: this is only called for DISCONNECT operations not created by the application
     *
     * Generated by the message handler
     *
     * @param context - the origin context
     * @param t - the exception that caused the local error
     */
    void notifyLocalDisconnect(IMqttsnContext context, Throwable t);

    /**
     * When stateful connections are held by the transport layer (ie. TCP sockets)
     * when connections are lost (e.g. socket timeout on read, EOF received, write IO)
     * the method can propogate the event up to the application
     * @param context - the origin context
     * @param t - the exception that caused the connection loss
     */
    void notifyConnectionLost(IMqttsnContext context, Throwable t);

}
