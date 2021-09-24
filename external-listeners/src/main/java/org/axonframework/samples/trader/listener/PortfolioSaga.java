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
import org.axonframework.eventhandling.saga.SagaEventHandler;
import org.axonframework.eventhandling.saga.StartSaga;
import org.axonframework.samples.trader.api.portfolio.PortfolioCreatedEvent;
import org.axonframework.samples.trader.api.portfolio.PortfolioEvent;
import org.codehaus.jackson.JsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@ProcessingGroup("sagaEventHandlers")
public class PortfolioSaga {
    private static final Logger logger = LoggerFactory.getLogger(PortfolioSaga.class);

    private JsonFactory jsonFactory = new JsonFactory();


    @SagaEventHandler(associationProperty = "portfolioId")
    @StartSaga
    public void handle(PortfolioCreatedEvent event) {
        String firstChar = event.getPortfolioId().getIdentifier().substring(0, 1);
        if ("01234567".contains(firstChar)) {
            System.out.println("Failing saga startign with " + firstChar);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            throw new RuntimeException("Boom");
        }
        System.out.println("---- Started saga for portfolio " + event.getPortfolioId().getIdentifier());

    }

    private void doHandle(PortfolioEvent event) {
        System.out.println("---- Received event " + event.toString());
    }
}
