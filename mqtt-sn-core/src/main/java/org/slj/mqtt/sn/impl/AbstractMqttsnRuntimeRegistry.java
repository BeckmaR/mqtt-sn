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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slj.mqtt.sn.model.MqttsnContext;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.net.NetworkAddress;
import org.slj.mqtt.sn.net.NetworkContext;
import org.slj.mqtt.sn.spi.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The base runtime registry provides support for simple fluent construction and encapsulates
 * both the controllers and the configuration for a runtime. Each controller has access to the
 * registry and uses it to access other parts of the application and config.
 *
 * During startup, the registry will be validated to ensure all required components are available.
 * Extending implementations should provide convenience methods for out-of-box runtimes.
 */
public abstract class AbstractMqttsnRuntimeRegistry implements IMqttsnRuntimeRegistry {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    protected MqttsnOptions options;
    protected AbstractMqttsnRuntime runtime;
    protected INetworkAddressRegistry networkAddressRegistry;
    protected IMqttsnStorageService storageService;
    protected IMqttsnCodec codec;
    protected IMqttsnMessageFactory factory;
    protected List<IMqttsnService> services;

    public AbstractMqttsnRuntimeRegistry(IMqttsnStorageService storageService, MqttsnOptions options){
        this.options = options;
        this.storageService = storageService;
        services = new ArrayList<>();
    }

    @Override
    public void init() {
        validateOnStartup();
        initNetworkRegistry();
        factory = codec.createMessageFactory();

        //ensure the storage system is added to managed lifecycle
        withService(storageService);
    }

    protected void initNetworkRegistry(){
        //-- ensure initial definitions exist in the network registry
        if(options.getNetworkAddressEntries() != null && !options.getNetworkAddressEntries().isEmpty()){
            Iterator<String> itr = options.getNetworkAddressEntries().keySet().iterator();
            while(itr.hasNext()){
                String key = itr.next();
                NetworkAddress address = options.getNetworkAddressEntries().get(key);
                NetworkContext networkContext = new NetworkContext(address);
                MqttsnContext sessionContext = new MqttsnContext(key);
                sessionContext.setProtocolVersion(getCodec().getProtocolVersion());
                networkAddressRegistry.bindContexts(networkContext, sessionContext);
            }
        }
    }

    public AbstractMqttsnRuntimeRegistry withQueueProcessorStateCheck(IMqttsnQueueProcessorStateService queueProcessorStateCheckService){
        withService(queueProcessorStateCheckService);
        return this;
    }

    public AbstractMqttsnRuntimeRegistry withMetrics(IMqttsnMetricsService metricsService){
        withService(metricsService);
        return this;
    }
    public AbstractMqttsnRuntimeRegistry withQueueProcessor(IMqttsnMessageQueueProcessor queueProcessor){
        withService(queueProcessor);
        return this;
    }
    public AbstractMqttsnRuntimeRegistry withTopicModifier(IMqttsnTopicModifier topicModifier){
        withService(topicModifier);
        return this;
    }
    public AbstractMqttsnRuntimeRegistry withContextFactory(IMqttsnContextFactory contextFactory){
        withService(contextFactory);
        return this;
    }
    public AbstractMqttsnRuntimeRegistry withMessageRegistry(IMqttsnMessageRegistry messageRegistry){
        withService(messageRegistry);
        return this;
    }
    public AbstractMqttsnRuntimeRegistry withMessageStateService(IMqttsnMessageStateService messageStateService){
        withService(messageStateService);
        return this;
    }
    public AbstractMqttsnRuntimeRegistry withSubscriptionRegistry(IMqttsnSubscriptionRegistry subscriptionRegistry){
        withService(subscriptionRegistry);
        return this;
    }
    public AbstractMqttsnRuntimeRegistry withTopicRegistry(IMqttsnTopicRegistry topicRegistry){
        withService(topicRegistry);
        return this;
    }
    public AbstractMqttsnRuntimeRegistry withMessageQueue(IMqttsnMessageQueue messageQueue){
        withService(messageQueue);
        return this;
    }
    public AbstractMqttsnRuntimeRegistry withCodec(IMqttsnCodec codec){
        this.codec = codec;
        return this;
    }
    public AbstractMqttsnRuntimeRegistry withMessageHandler(IMqttsnMessageHandler handler){
        withService(handler);
        return this;
    }
    public AbstractMqttsnRuntimeRegistry withTransport(IMqttsnTransport transport){
        withService(transport);
        return this;
    }
    public AbstractMqttsnRuntimeRegistry withNetworkAddressRegistry(INetworkAddressRegistry networkAddressRegistry){
        this.networkAddressRegistry = networkAddressRegistry;
        return this;
    }
    public AbstractMqttsnRuntimeRegistry withSecurityService(IMqttsnSecurityService securityService){
        withService(securityService);
        return this;
    }
    public AbstractMqttsnRuntimeRegistry withSessionRegistry(IMqttsnSessionRegistry sessionRegistry){
        withService(sessionRegistry);
        return this;
    }
    public AbstractMqttsnRuntimeRegistry withAuthenticationService(IMqttsnAuthenticationService authenticationService){
        withService(authenticationService);
        return this;
    }
    public AbstractMqttsnRuntimeRegistry withAuthorizationService(IMqttsnAuthorizationService authorizationService){
        withService(authorizationService);
        return this;
    }
    public AbstractMqttsnRuntimeRegistry withClientIdFactory(IMqttsnClientIdFactory clientIdFactory){
        withService(clientIdFactory);
        return this;
    }
    public AbstractMqttsnRuntimeRegistry withWillRegistry(IMqttsnWillRegistry willRegistry){
        withService(willRegistry);
        return this;
    }
    public AbstractMqttsnRuntimeRegistry withDeadLetterQueue(IMqttsnDeadLetterQueue deadLetterQueue){
        withService(deadLetterQueue);
        return this;
    }

    @Override
    public MqttsnOptions getOptions() {
        return options;
    }

    @Override
    public INetworkAddressRegistry getNetworkRegistry() {
        return networkAddressRegistry;
    }

    @Override
    public void setRuntime(AbstractMqttsnRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public AbstractMqttsnRuntime getRuntime() {
        return runtime;
    }

    @Override
    public IMqttsnAuthenticationService getAuthenticationService() {
        return getOptionalService(IMqttsnAuthenticationService.class).orElse(null);
    }

    @Override
    public IMqttsnAuthorizationService getAuthorizationService() {
        return getOptionalService(IMqttsnAuthorizationService.class).orElse(null);
    }

    public IMqttsnQueueProcessorStateService getQueueProcessorStateCheckService() {
        return getOptionalService(IMqttsnQueueProcessorStateService.class).orElse(null);
    }

    @Override
    public IMqttsnCodec getCodec() {
        return codec;
    }

    @Override
    public IMqttsnMessageHandler getMessageHandler() {
        return getService(IMqttsnMessageHandler.class);
    }

    @Override
    public IMqttsnTransport getTransport() {
        return getService(IMqttsnTransport.class);
    }

    @Override
    public IMqttsnMessageFactory getMessageFactory(){
        return factory;
    }

    @Override
    public IMqttsnMessageQueue getMessageQueue() {
        return getService(IMqttsnMessageQueue.class);
    }

    @Override
    public IMqttsnTopicRegistry getTopicRegistry() {
        return getService(IMqttsnTopicRegistry.class);
    }

    @Override
    public IMqttsnSubscriptionRegistry getSubscriptionRegistry() {
        return getService(IMqttsnSubscriptionRegistry.class);
    }

    @Override
    public IMqttsnMessageStateService getMessageStateService() {
        return getService(IMqttsnMessageStateService.class);
    }

    @Override
    public IMqttsnMessageRegistry getMessageRegistry(){
        return getService(IMqttsnMessageRegistry.class);
    }

    @Override
    public IMqttsnWillRegistry getWillRegistry(){
        return getService(IMqttsnWillRegistry.class);
    }

    @Override
    public IMqttsnContextFactory getContextFactory() {
        return getService(IMqttsnContextFactory.class);
    }

    @Override
    public IMqttsnMessageQueueProcessor getQueueProcessor() {
        return getService(IMqttsnMessageQueueProcessor.class);
    }

    @Override
    public IMqttsnSecurityService getSecurityService() {
        return getOptionalService(IMqttsnSecurityService.class).orElse(null);
    }

    @Override
    public IMqttsnSessionRegistry getSessionRegistry() {
        return getService(IMqttsnSessionRegistry.class);
    }

    @Override
    public IMqttsnTopicModifier getTopicModifier() {
        return getOptionalService(IMqttsnTopicModifier.class).orElse(null);
    }

    @Override
    public IMqttsnMetricsService getMetrics() {
        return getOptionalService(IMqttsnMetricsService.class).orElse(null);
    }

    @Override
    public IMqttsnStorageService getStorageService() {
        return storageService;
    }

    @Override
    public IMqttsnClientIdFactory getClientIdFactory() {
        return getService(IMqttsnClientIdFactory.class);
    }

    @Override
    public IMqttsnDeadLetterQueue getDeadLetterQueue() {
        return getOptionalService(IMqttsnDeadLetterQueue.class).orElse(null);
    }

    @Override
    public List<IMqttsnService> getServices() {
        synchronized (services){
            ArrayList sorted = new ArrayList(services);
            Collections.sort(sorted, new ServiceSort());
            return Collections.unmodifiableList(sorted);
        }
    }

    @Override
    public AbstractMqttsnRuntimeRegistry withService(IMqttsnService service){
        synchronized (services){
            services.add(service);
        }
        return this;
    }

    @Override
    public <T extends IMqttsnService> AbstractMqttsnRuntimeRegistry withServiceReplaceIfExists(Class<T> serviceInterface, IMqttsnService serviceInstance){
        synchronized (services){
            Optional<IMqttsnService> existing =
                    getOptionalService((Class<IMqttsnService>) serviceInterface);
            if(existing.isPresent()){
                logger.info("found existing instance of {} in runtime, removing",
                        serviceInterface.getName());
                services.remove(existing.get());
            }
            services.add(serviceInstance);
        }
        return this;
    }

    @Override
    public <T extends IMqttsnService> T getService(Class<T> clz){
        synchronized (services){
            List<IMqttsnService> all = services.stream().filter(s ->
                            clz.isAssignableFrom(s.getClass())).
                    collect(Collectors.toList());
            if(all.size() > 1){
                throw new MqttsnRuntimeException("more than a single instance of "+clz+" service found");
            }
            else if(all.size() == 0){
                throw new MqttsnRuntimeException("unable to find instance of "+clz+" service found");
            }
            return (T) all.get(0);
        }
    }

    @Override
    public <T extends IMqttsnService> List<T> getServices(Class<T> clz){
        synchronized (services){
            List<IMqttsnService> all = services.stream().filter(s ->
                    clz.isAssignableFrom(s.getClass())).
                    collect(Collectors.toList());
            return (List<T>) all;
        }
    }

    @Override
    public <T extends IMqttsnService> Optional<T> getOptionalService(Class<T> clz){
        synchronized (services){
            return (Optional<T>) services.stream().filter(s ->
                            clz.isAssignableFrom(s.getClass())).findFirst();
        }
    }


    protected void validateOnStartup() throws MqttsnRuntimeException {
        if(storageService == null) throw new MqttsnRuntimeException("storage service must be found for a valid runtime");
        if(networkAddressRegistry == null) throw new MqttsnRuntimeException("network-registry must be bound for valid runtime");
        if(codec == null) throw new MqttsnRuntimeException("codec must be bound for valid runtime");
    }
}