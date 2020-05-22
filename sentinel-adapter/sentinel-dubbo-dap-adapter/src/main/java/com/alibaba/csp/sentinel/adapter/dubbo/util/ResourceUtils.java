package com.alibaba.csp.sentinel.adapter.dubbo.util;


import com.alibaba.csp.sentinel.adapter.dubbo.enums.DelimiterEnums;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;


/**
 * 类描述
 * <p><br/></p>
 *
 * @Author: lixiongxing
 * @Date: 2019-03-22
 */
@Slf4j
public class ResourceUtils {

    public ResourceUtils() {
    }

    /**
     * 路径通配符加载Resource
     *
     * @param path
     * @return
     * @throws Exception
     */
    public static Resource[] readResource(String path) throws Exception {
        Resource[] locations = (new PathMatchingResourcePatternResolver()).getResources(path);
        return locations;
    }

    /**
     * 根据路径加载Resource
     *
     * @param paths
     * @return
     */
    public static Resource[] readResource(List<String> paths) {
        if (CollectionUtils.isEmpty(paths)) {
            return null;
        }
        Resource[] locations = new Resource[paths.size()];

        try {
            return paths.stream().map(path -> {
                if (path.endsWith(DelimiterEnums.NUMBER.getCode()) && StringUtils.isNotBlank(path)) {
                    path = path.substring(0, path.length() - 1);
                }
                return new PathMatchingResourcePatternResolver().getResource(path);
            }).collect(Collectors.toList()).toArray(locations);
        } catch (Exception e) {
            log.error("Read configuration file exception",e);
        }
        return null;
    }

    /**
     * resource 转properties
     *
     * @param res
     * @return
     */
    public static Properties toProperties(Resource res) {
        InputStream is = null;
        InputStreamReader isr = null;
        Properties prop = null;
        try {
            is = res.getInputStream();
            isr = new InputStreamReader(is);
            prop = new Properties();
            prop.load(isr);
        } catch (Exception e) {
            log.error("Read configuration file exception",e);
        } finally {
            if (isr != null) {
                try {
                    isr.close();
                } catch (IOException e) {
                    log.error("InputStreamReader close exception",e);
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    log.error("InputStream close exception",e);
                }
            }
        }
        return prop;
    }
}
