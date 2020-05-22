package com.alibaba.csp.sentinel.adapter.dubbo.util;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 数字操作工具类
 * <p><br/></p>
 *
 * @author: lixiongxing
 * @date: 2019-03-29
 */
public class NumberUtils extends org.apache.commons.lang3.math.NumberUtils {

    public static String leftFillZero(String numValue, int maxLenth) {
        if (StringUtils.isBlank(numValue)) {
            return getZero(maxLenth);
        }
        if (numValue.length() > maxLenth) {
            return numValue.substring(numValue.length() - maxLenth);
        }
        StringBuffer buffer = new StringBuffer();
        while (buffer.length() < maxLenth - numValue.length()) {
            buffer.append("0");
        }
        return getZero(maxLenth - numValue.length()).concat(numValue);
    }

    public static String leftFillZero(long num, int maxLenth) {
        return leftFillZero(String.valueOf(num), maxLenth);
    }

    public static String rightFillZero(String numValue, int maxLenth) {
        if (StringUtils.isBlank(numValue)) {
            return getZero(maxLenth);
        }
        if (numValue.length() > maxLenth) {
            return numValue.substring(0, maxLenth);
        }
        return numValue.concat(getZero(maxLenth - numValue.length()));
    }

    public static String rightFillZero(long num, int maxLenth) {
        return rightFillZero(String.valueOf(num), maxLenth);
    }


    /**
     * bigDecimal 类型数据大小比较 0 相等 1 var1 -1 var2 大
     * @param var1 第一个参数
     * @param var2 第二个参数
     * @return 返回结果
     */
    public static int compareTo(BigDecimal var1, BigDecimal var2) {
        if( var1 == null && var2 == null ){
            return 0;
        }else if( var1==null ){
            return -1;
        }else if( var2==null ){
            return 1;
        }else{
            return var1.setScale(BigDecimal.ROUND_CEILING, RoundingMode.HALF_UP)
                    .compareTo(var2.setScale(BigDecimal.ROUND_CEILING, RoundingMode.HALF_UP));
        }

    }

    public static String toString(BigDecimal var1) {
        if (var1 == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP).toString();
        }
        return var1.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
    }


    private static String getZero(int length) {
        if (length < 1) {
            return StringUtils.EMPTY;
        }
        StringBuilder buffer = new StringBuilder();
        while (buffer.length() < length) {
            buffer.append("0");
        }
        return buffer.toString();
    }
}
