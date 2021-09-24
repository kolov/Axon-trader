/*
 * Copyright (c) 2012. Axon Framework
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

package org.axonframework.samples.trader.listener;

import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.samples.trader.api.portfolio.PortfolioCreatedEvent;
import org.axonframework.samples.trader.api.portfolio.PortfolioEvent;
import org.codehaus.jackson.JsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@ProcessingGroup("failingEventHandlers")
public class PortfolioPublisher {
    private static final Logger logger = LoggerFactory.getLogger(PortfolioPublisher.class);

    private JsonFactory jsonFactory = new JsonFactory();

    int counter = 0;

    @EventHandler
    public void handle(PortfolioCreatedEvent event) {

        System.out.println("---- Received PortfolioCreatedEvent for portfolio " + event.getPortfolioId().getIdentifier());
        counter++;
        if (counter == 2) {
            System.out.println("Failing on second event");
            throw new RuntimeException("Failing on second event");
        }
        if (counter == 5) {
            try {
                System.out.println("Sleeping on 5th event");
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private void doHandle(PortfolioEvent event) {
        System.out.println("---- Received event " + event.toString());
    }
}
