package com.alibaba.csp.sentinel.adapter.dubbo.util;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;

import java.util.*;
import java.util.stream.Collectors;


/**
 * code描述获取工具助手
 * <p><br/></p>
 *
 * @Author: lixiongxing
 * @Date: 2019-03-22
 */
@Slf4j
public class CodeContainerHelper implements CodeConfigurationListener, InitializingBean {
    private static final Integer MAP_DEFAULT_CAPACITY = 16;

    private static Resource[] locations;

    private static final String DEFAULT_MSG = null;

    private static final String DEFAULT_CODE_CONFIG_PATH = "classpath*:config/*_code.properties";

    private static final Map<String, Map<String, String>> CODE_MAPPINGS = new HashMap<String, Map<String, String>>(MAP_DEFAULT_CAPACITY);

    private static boolean inited = false;

    private static List<String> configKeys;

    @Override
    public void changeOfResource(Resource configResource) {
        log.debug("resource changed for configkey:{0}", configResource.getFilename());
        loadCodeConfig(configResource);
    }

    @Override
    public void changeOfProperties(String configKey, Properties properties) {
        log.debug("propeties changed for configkey:{0}", configKey);
        if (properties == null) {
            CODE_MAPPINGS.put(configKey, new HashMap<String, String>(MAP_DEFAULT_CAPACITY));
            return;
        }
        Map<String, String> map = properties.keySet().stream().collect(
                Collectors.toMap(value -> (String) value, value -> properties.getProperty((String) value)));
        CODE_MAPPINGS.put(configKey, map);
    }

    @Override
    public List<String> getConfigKeys() {
        return configKeys;
    }

    @Override
    public List<Resource> getResources() {
        return Arrays.asList(locations);
    }

    @Override
    public Map<String, Map<String, String>> getConfigProperties() {
        return CODE_MAPPINGS;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

    public static String getCodeDesc(String code, Object... pattern) {
        if (!inited) {
            init();
        }
        String message = null;
        Optional<Map<String, String>> optional =
                CODE_MAPPINGS.values().stream().filter(codeMap -> codeMap.containsKey(code)).findFirst();
        if (optional.isPresent()) {
            message = optional.get().get(code);
        }
        if (StringUtils.isEmpty(message)) {
            log.warn("未找到code[{0}"+code+"]的定义", code);
            message = DEFAULT_MSG;
        }
        return MessageFormatter.arrayFormat(message, pattern).getMessage();
    }

    private static void init() {
        if (null == locations) {
            try {
                locations = ResourceUtils.readResource(DEFAULT_CODE_CONFIG_PATH);
            } catch (Exception e) {
                log.error("初始化错误码配置文件失败",e);
            }
        }
        if (ArrayUtils.isEmpty(locations)) {
            inited = true;
            return;
        }

        configKeys = new ArrayList();

        Resource[] resources = locations;
        Arrays.stream(resources).forEach(resource -> {
            configKeys.add(resource.getFilename());
            loadCodeConfig(resource);

        });
        inited = true;
    }

    private static void loadCodeConfig(Resource resource) {
        String file = resource.getFilename();
        HashMap<String, String> codeMap = loadNewConfig(resource);
        CODE_MAPPINGS.put(file, codeMap);
    }


    private static HashMap<String, String> loadNewConfig(Resource resource) {
        HashMap<String, String> map = new HashMap<String, String>(MAP_DEFAULT_CAPACITY);
        Properties prop = ResourceUtils.toProperties(resource);
        if (prop != null) {
            prop.keySet().forEach(key -> {
                map.put((String) key, prop.getProperty((String) key));
            });
        }
        return map;
    }


}
