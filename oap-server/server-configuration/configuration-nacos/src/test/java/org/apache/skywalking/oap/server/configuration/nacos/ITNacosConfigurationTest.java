/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.oap.server.configuration.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.util.Map;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.util.PropertyPlaceholderHelper;
import org.apache.skywalking.oap.server.library.module.ApplicationConfiguration;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.library.util.CollectionUtils;
import org.apache.skywalking.oap.server.library.util.ResourceUtils;
import org.junit.Before;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@Slf4j
public class ITNacosConfigurationTest {
    private final Yaml yaml = new Yaml();

    private NacosConfigurationTestProvider provider;

    @Before
    public void setUp() throws Exception {
        final ApplicationConfiguration applicationConfiguration = new ApplicationConfiguration();
        loadConfig(applicationConfiguration);

        final ModuleManager moduleManager = new ModuleManager();
        moduleManager.init(applicationConfiguration);

        provider = (NacosConfigurationTestProvider) moduleManager.find(NacosConfigurationTestModule.NAME).provider();

        assertNotNull(provider);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Test(timeout = 20000)
    public void shouldReadUpdated() throws NacosException {
        assertNull(provider.watcher.value());

        final Properties properties = new Properties();
        final String nacosHost = System.getProperty("nacos.host");
        final String nacosPort = System.getProperty("nacos.port");
        log.info("nacosHost: {}, nacosPort: {}", nacosHost, nacosPort);
        properties.put("serverAddr", nacosHost + ":" + nacosPort);

        final ConfigService configService = NacosFactory.createConfigService(properties);
        assertTrue(configService.publishConfig("test-module.default.testKey", "skywalking", "500"));

        for (String v = provider.watcher.value(); v == null; v = provider.watcher.value()) {
        }

        assertEquals("500", provider.watcher.value());

        assertTrue(configService.removeConfig("test-module.default.testKey", "skywalking"));

        for (String v = provider.watcher.value(); v != null; v = provider.watcher.value()) {
        }

        assertNull(provider.watcher.value());
    }

    @Test(timeout = 20000)
    public void shouldReadUpdatedGroup() throws NacosException {
        assertEquals("{}", provider.groupWatcher.groupItems().toString());

        final Properties properties = new Properties();
        final String nacosHost = System.getProperty("nacos.host");
        final String nacosPort = System.getProperty("nacos.port");
        log.info("nacosHost: {}, nacosPort: {}", nacosHost, nacosPort);
        properties.put("serverAddr", nacosHost + ":" + nacosPort);

        final ConfigService configService = NacosFactory.createConfigService(properties);
        //test add group key and item1 item2
        assertTrue(configService.publishConfig("test-module.default.testKeyGroup", "skywalking", "item1\n item2"));
        assertTrue(configService.publishConfig("item1", "skywalking", "100"));
        assertTrue(configService.publishConfig("item2", "skywalking", "200"));
        for (String v = provider.groupWatcher.groupItems().get("item1"); v == null; v = provider.groupWatcher.groupItems().get("item1")) {
        }
        for (String v = provider.groupWatcher.groupItems().get("item2"); v == null; v = provider.groupWatcher.groupItems().get("item2")) {
        }
        assertEquals("100", provider.groupWatcher.groupItems().get("item1"));
        assertEquals("200", provider.groupWatcher.groupItems().get("item2"));

        //test remove item1
        assertTrue(configService.removeConfig("item1", "skywalking"));
        for (String v = provider.groupWatcher.groupItems().get("item1"); v != null; v = provider.groupWatcher.groupItems().get("item1")) {
        }
        assertNull(provider.groupWatcher.groupItems().get("item1"));

        //test modify item1
        assertTrue(configService.publishConfig("item1", "skywalking", "300"));
        for (String v = provider.groupWatcher.groupItems().get("item1"); v == null; v = provider.groupWatcher.groupItems().get("item1")) {
        }
        assertEquals("300", provider.groupWatcher.groupItems().get("item1"));

        //test remove group key
        assertTrue(configService.removeConfig("test-module.default.testKeyGroup", "skywalking"));
        for (String v = provider.groupWatcher.groupItems().get("item2"); v != null; v = provider.groupWatcher.groupItems().get("item2")) {
        }
        assertNull(provider.groupWatcher.groupItems().get("item2"));
        //chean
        assertTrue(configService.removeConfig("item1", "skywalking"));
        assertTrue(configService.removeConfig("item2", "skywalking"));
    }

    @SuppressWarnings("unchecked")
    private void loadConfig(ApplicationConfiguration configuration) throws FileNotFoundException {
        Reader applicationReader = ResourceUtils.read("application.yml");
        Map<String, Map<String, Map<String, ?>>> moduleConfig = yaml.loadAs(applicationReader, Map.class);
        if (CollectionUtils.isNotEmpty(moduleConfig)) {
            moduleConfig.forEach((moduleName, providerConfig) -> {
                if (providerConfig.size() > 0) {
                    ApplicationConfiguration.ModuleConfiguration moduleConfiguration = configuration.addModule(moduleName);
                    providerConfig.forEach((name, propertiesConfig) -> {
                        Properties properties = new Properties();
                        if (propertiesConfig != null) {
                            propertiesConfig.forEach((key, value) -> {
                                properties.put(key, value);
                                final Object replaceValue = yaml.load(PropertyPlaceholderHelper.INSTANCE.replacePlaceholders(value + "", properties));
                                if (replaceValue != null) {
                                    properties.replace(key, replaceValue);
                                }
                            });
                        }
                        moduleConfiguration.addProviderConfiguration(name, properties);
                    });
                }
            });
        }
    }
}
