package com.alibaba.csp.sentinel.adapter.dubbo.util;

import com.alibaba.fastjson.serializer.ValueFilter;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class BigDecimalValueFilter implements ValueFilter {
    @Override
    public Object process(Object object, String name, Object value) {
        if (value instanceof BigDecimal) {
            if (value == null) {
                return new BigDecimal("0.00");
            } else {
                return NumberUtils.toScaledBigDecimal(String.valueOf(value), 2, RoundingMode.HALF_UP);
            }
        }

        return value;
    }
}