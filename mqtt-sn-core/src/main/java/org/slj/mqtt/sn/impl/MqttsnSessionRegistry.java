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

import org.slj.mqtt.sn.impl.metrics.IMqttsnMetrics;
import org.slj.mqtt.sn.impl.metrics.MqttsnSnapshotMetric;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.MqttsnClientState;
import org.slj.mqtt.sn.model.session.IMqttsnSession;
import org.slj.mqtt.sn.model.session.impl.MqttsnSessionBeanImpl;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.IMqttsnSessionRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnRuntimeException;
import org.slj.mqtt.sn.utils.radix.RadixTree;
import org.slj.mqtt.sn.utils.radix.RadixTreeImpl;

import java.util.*;

/**
 * Created bean based session objects which can encapsulate the storage of session elements
 */
public class MqttsnSessionRegistry extends AbstractMqttsnSessionBeanRegistry implements IMqttsnSessionRegistry {

    protected Map<IMqttsnContext, IMqttsnSession> sessionLookup;
    protected RadixTree<String> searchTree;
    public void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        searchTree = new RadixTreeImpl<>();
        super.start(runtime);
        sessionLookup = new HashMap();
        registerMetrics(runtime);
    }

    public List<String> prefixSearch(String prefix){
        return searchTree.searchPrefix(prefix, 100);
    }

    @Override
    public IMqttsnSession createNewSession(IMqttsnContext context) {
        if(!running()) throw new MqttsnRuntimeException("unable to create session on, service not running");
        logger.info("creating new session for {}", context);
        IMqttsnSession session = new MqttsnSessionBeanImpl(context, MqttsnClientState.DISCONNECTED);
        sessionLookup.put(context, session);
        try {
            if(searchTree != null) {
                String clientId = context.getId();
                searchTree.insert(clientId, clientId);
            }
        } catch(Exception e){
            throw new MqttsnRuntimeException(e);
        }
        return session;
    }

    @Override
    public IMqttsnSession getSession(IMqttsnContext context, boolean createIfNotExists) throws MqttsnException {
        IMqttsnSession session = sessionLookup.get(context);
        if(session == null && createIfNotExists){
            synchronized (sessionLookup){
                session = sessionLookup.get(context);
                if(session == null){
                    session = createNewSession(context);
                }
            }
        }
        return session;
    }

    @Override
    public Iterator<IMqttsnSession> iterator() {
        synchronized (sessionLookup){
            return new HashSet<>(sessionLookup.values()).iterator();
        }
    }

    public boolean cleanSession(final IMqttsnContext context, final boolean deepClean) throws MqttsnException {

        IMqttsnSession session = getSession(context, false);
        if(session == null) return false;

        //clear down all prior session state
        synchronized (session){
            if(deepClean){
                //-- the queued messages
                registry.getMessageQueue().clear(session);

                //-- the subscriptions
                registry.getSubscriptionRegistry().clear(session);
            }

            //-- inflight messages & protocol messages
            registry.getMessageStateService().clear(session.getContext());

            //-- topic registrations
            registry.getTopicRegistry().clear(session);

            //-- will data
            registry.getWillRegistry().clear(session);
        }

        logger.info("cleaning session state {}, deepClean ? {}, queueSize after clean {}",
                session, deepClean, registry.getMessageQueue().queueSize(session));
        return true;
    }

    @Override
    public void clear(IMqttsnSession session, boolean clearNetworking) throws MqttsnException {
        try {
            if(session == null) throw new NullPointerException("cannot clear <null> session");
            logger.info("removing session for {}, clear networking ? {}",
                    session.getContext(), clearNetworking);
            if(clearNetworking){
                getRegistry().getNetworkRegistry().
                        removeExistingClientId(session.getContext().getId());
            }
            cleanSession(session.getContext(), true);
        } finally {
            sessionLookup.remove(session.getContext());
            try {
                if(searchTree != null) searchTree.delete(session.getContext().getId());
            } catch(Exception e){
                throw new MqttsnRuntimeException(e);
            }
        }
    }

    @Override
    public void clearAll() {
        Iterator<IMqttsnSession> itr = iterator();
        while(itr.hasNext()){
            clear(itr.next());
        }
    }

    @Override
    public void clear(IMqttsnSession session) {
        try {
            clear(session, true);
        } catch(MqttsnException e){
            throw new MqttsnRuntimeException(e);
        }
    }

    @Override
    public Optional<IMqttsnContext> lookupClientIdSession(String clientId){
        synchronized (sessionLookup){
            Iterator<IMqttsnContext> itr = sessionLookup.keySet().iterator();
            while(itr.hasNext()){
                IMqttsnContext c = itr.next();
                if(c != null && c.getId().equals(clientId))
                    return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    @Override
    public long countSessions(MqttsnClientState state) {
        synchronized (sessionLookup){
            return sessionLookup.values().stream().filter(s ->
                    s.getClientState() == state).count();
        }
    }

    @Override
    public long countTotalSessions() {
        synchronized (sessionLookup){
            return sessionLookup.size();
        }
    }

    @Override
    public boolean hasSession(IMqttsnContext context) {
        return sessionLookup.containsKey(context);
    }

    @Override
    public void modifyClientState(IMqttsnSession session, MqttsnClientState state){
        getSessionBean(session).setClientState(state);
        logger.info("setting session-state as '{}' {}", state, session);
    }

    @Override
    public void modifyLastSeen(IMqttsnSession session) {
        Date lastSeen = new Date();
        getSessionBean(session).setLastSeen(lastSeen);
        logger.info("setting session-lastSeen as '{}' {}", lastSeen, session);
    }

    @Override
    public void modifyKeepAlive(IMqttsnSession session, int keepAlive) {
        getSessionBean(session).setKeepAlive(keepAlive);
        logger.info("setting session-keepAlive as '{}' {}", keepAlive, session);
    }

    @Override
    public void modifySessionExpiryInterval(IMqttsnSession session, long sessionExpiryInterval) {
        getSessionBean(session).setSessionExpiryInterval(sessionExpiryInterval);
        logger.info("setting session-sessionExpiryInterval as '{}' {}", sessionExpiryInterval, session);
    }

    @Override
    public void modifyMaxPacketSize(IMqttsnSession session, int maxPacketSize) {
        getSessionBean(session).setMaxPacketSize(maxPacketSize);
        logger.info("setting session-maxPacketSize as '{}}' {}", maxPacketSize, session);
    }

    protected void registerMetrics(IMqttsnRuntimeRegistry runtime){
        if(runtime.getMetrics() != null){
            runtime.getMetrics().registerMetric(new MqttsnSnapshotMetric(IMqttsnMetrics.SESSION_ACTIVE_REGISTRY_COUNT, "A count of the number of sessions marked in the 'ACTIVE' state resident in the runtime.",
                    IMqttsnMetrics.DEFAULT_MAX_SAMPLES, IMqttsnMetrics.DEFAULT_SNAPSHOT_TIME_MILLIS, () -> countSessions(MqttsnClientState.ACTIVE)));
            runtime.getMetrics().registerMetric(new MqttsnSnapshotMetric(IMqttsnMetrics.SESSION_DISCONNECTED_REGISTRY_COUNT, "A count of the number of sessions marked in the 'DISCONNECTED' state resident in the runtime.",
                    IMqttsnMetrics.DEFAULT_MAX_SAMPLES, IMqttsnMetrics.DEFAULT_SNAPSHOT_TIME_MILLIS, () -> countSessions(MqttsnClientState.DISCONNECTED)));
            runtime.getMetrics().registerMetric(new MqttsnSnapshotMetric(IMqttsnMetrics.SESSION_LOST_REGISTRY_COUNT, "A count of the number of sessions marked in the 'LOST' state resident in the runtime.",
                    IMqttsnMetrics.DEFAULT_MAX_SAMPLES, IMqttsnMetrics.DEFAULT_SNAPSHOT_TIME_MILLIS, () -> countSessions(MqttsnClientState.LOST)));
            runtime.getMetrics().registerMetric(new MqttsnSnapshotMetric(IMqttsnMetrics.SESSION_AWAKE_REGISTRY_COUNT, "A count of the number of sessions marked in the 'AWAKE' state resident in the runtime.",
                    IMqttsnMetrics.DEFAULT_MAX_SAMPLES, IMqttsnMetrics.DEFAULT_SNAPSHOT_TIME_MILLIS, () -> countSessions(MqttsnClientState.AWAKE)));
            runtime.getMetrics().registerMetric(new MqttsnSnapshotMetric(IMqttsnMetrics.SESSION_ASLEEP_REGISTRY_COUNT, "A count of the number of sessions marked in the 'ASLEEP' state resident in the runtime.",
                    IMqttsnMetrics.DEFAULT_MAX_SAMPLES, IMqttsnMetrics.DEFAULT_SNAPSHOT_TIME_MILLIS, () -> countSessions(MqttsnClientState.ASLEEP)));
        }
    }
}
