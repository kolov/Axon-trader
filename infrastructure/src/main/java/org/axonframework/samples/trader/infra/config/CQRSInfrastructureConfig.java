/*
 * Copyright (c) 2010-2016. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.samples.trader.infra.config;

import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.SimpleCommandBus;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.commandhandling.gateway.DefaultCommandGateway;
import org.axonframework.config.DefaultConfigurer;
import org.axonframework.config.EventHandlingConfiguration;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.config.SagaConfiguration;
import org.axonframework.eventhandling.EventListener;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.ListenerInvocationErrorHandler;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.messaging.interceptors.BeanValidationInterceptor;
import org.axonframework.samples.trader.company.command.Company;
import org.axonframework.samples.trader.orders.command.BuyTradeManagerSaga;
import org.axonframework.samples.trader.orders.command.Portfolio;
import org.axonframework.samples.trader.orders.command.SellTradeManagerSaga;
import org.axonframework.samples.trader.orders.command.Transaction;
import org.axonframework.samples.trader.tradeengine.command.OrderBook;
import org.axonframework.samples.trader.users.command.User;
import org.axonframework.spring.config.CommandHandlerSubscriber;
import org.axonframework.spring.config.annotation.AnnotationCommandHandlerBeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Map;

@Configuration
@ComponentScan("org.axonframework.samples.trader")
@Import(CQRSInfrastructureHSQLDBConfig.class)
public class CQRSInfrastructureConfig {

    @Bean
    public CommandBus commandBus() {
        SimpleCommandBus commandBus = new SimpleCommandBus();
        commandBus.registerDispatchInterceptor(new BeanValidationInterceptor<>());

        return commandBus;
    }

    @Bean
    public CommandGateway commandGateway(CommandBus commandBus) {
        return new DefaultCommandGateway(commandBus);
    }

    @Bean
    public AnnotationCommandHandlerBeanPostProcessor annotationCommandHandlerBeanPostProcessor() {
        return new AnnotationCommandHandlerBeanPostProcessor();
    }

    @Bean
    public CommandHandlerSubscriber commandHandlerSubscriber() {
        return new CommandHandlerSubscriber();
    }

    /**
     * Ideally, this bean would be created by the axon-spring module automatically, by setting the
     * {@link org.axonframework.spring.config.EnableAxon} on one of our configuration classes.
     * This however throws 'interesting' lifecycle exceptions which didn't seem all that clear.
     * In the interest of getting this application running again for the time being, I've resorted to using the
     * {@link org.axonframework.config.Configuration} as shown below.
     * If you are interested in other forms of setting up the configuration, the
     * [Reference Guide](https://docs.axonframework.org/) would be an ideal place to start your investigation.
     */
    @Bean
    public org.axonframework.config.Configuration configuration(CommandBus commandBus,
                                                                EventStore eventStore,
                                                                ApplicationContext applicationContext) {
        EventHandlingConfiguration queryModelConfiguration =
                new EventHandlingConfiguration().registerSubscribingEventProcessor("queryModel");
        EventHandlingConfiguration commandPublisherConfiguration =
                new EventHandlingConfiguration().registerSubscribingEventProcessor("commandPublishingEventHandlers");

        ListenerInvocationErrorHandler alwaysRetryingEventHandler = (e, eventMessage, eventListener) -> {
            System.out.println("Processing error: retrying");
            eventListener.handle(eventMessage);
        };

        EventHandlingConfiguration failingPublisherConfiguration =
                new EventHandlingConfiguration().registerSubscribingEventProcessor("failingEventHandlers")
                        .configureListenerInvocationErrorHandler(conf -> alwaysRetryingEventHandler);

        EventHandlingConfiguration sagaConfiguration =
                new EventHandlingConfiguration().registerSubscribingEventProcessor("sagaEventHandlers")
                        .configureListenerInvocationErrorHandler(conf -> alwaysRetryingEventHandler);

        Map<String, Object> eventHandlingComponents = applicationContext.getBeansWithAnnotation(ProcessingGroup.class);

        eventHandlingComponents.forEach((key, value) -> {
            if (key.contains("Listener")) {
                System.out.println("Registering listener " + key);
                commandPublisherConfiguration.registerEventHandler(conf -> value);
            } else if (key.contains("Publisher")) {
                System.out.println("Registering publisher " + key);
                failingPublisherConfiguration.registerEventHandler(conf -> value);
            } else if (key.contains("Saga")) {
                System.out.println("Registering saga " + key);
                sagaConfiguration.registerEventHandler(conf -> value);
            } else {
                queryModelConfiguration.registerEventHandler(conf -> value);
            }
        });

        org.axonframework.config.Configuration configuration =
                DefaultConfigurer.defaultConfiguration()
                        .configureCommandBus(conf -> commandBus)
                        .configureEventStore(conf -> eventStore)
                        .configureAggregate(User.class)
                        .configureAggregate(Company.class)
                        .configureAggregate(Portfolio.class)
                        .configureAggregate(Transaction.class)
                        .configureAggregate(OrderBook.class)
                        .registerModule(queryModelConfiguration)
                        .registerModule(commandPublisherConfiguration)
                        .registerModule(failingPublisherConfiguration)
                        .registerModule(sagaConfiguration)
                        .registerModule(SagaConfiguration.subscribingSagaManager(SellTradeManagerSaga.class))
                        .registerModule(SagaConfiguration.subscribingSagaManager(BuyTradeManagerSaga.class))
                        .buildConfiguration();
        configuration.start();
        return configuration;
    }
}