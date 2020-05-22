package com.alibaba.csp.sentinel.adapter.dubbo.util;

import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 配置文件操作接口
 * <p><br/></p>
 *
 * @Author: lixiongxing
 * @Date: 2019-03-23
 */
public interface CodeConfigurationListener {

    void changeOfResource(Resource configResource);

    void changeOfProperties(String configKey, Properties properties);

    List<String> getConfigKeys();

    List<Resource> getResources();

    Map<String, Map<String, String>> getConfigProperties();
}
