/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.service.task;

import static java.lang.String.format;

import org.apache.dolphinscheduler.plugin.task.api.TaskChannel;
import org.apache.dolphinscheduler.plugin.task.api.TaskChannelFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TaskPluginManager {
    private static final Logger logger = LoggerFactory.getLogger(TaskPluginManager.class);

    private final Map<String, TaskChannel> taskChannelMap = new ConcurrentHashMap<>();

    public Map<String, TaskChannel> getTaskChannelMap() {
        return Collections.unmodifiableMap(taskChannelMap);
    }

    public TaskChannel getTaskChannel(String type) {
        return this.getTaskChannelMap().get(type);
    }

    @EventListener
    public void installPlugin(ApplicationReadyEvent readyEvent) {
        final Set<String> names = new HashSet<>();

        ServiceLoader.load(TaskChannelFactory.class).forEach(factory -> {
            final String name = factory.getName();

            logger.info("Registering task plugin: {}", name);

            if (!names.add(name)) {
                throw new IllegalStateException(format("Duplicate task plugins named '%s'", name));
            }

            loadTaskChannel(factory);

            logger.info("Registered task plugin: {}", name);
        });
    }

    private void loadTaskChannel(TaskChannelFactory taskChannelFactory) {
        TaskChannel taskChannel = taskChannelFactory.create();
        taskChannelMap.put(taskChannelFactory.getName(), taskChannel);
    }
}
