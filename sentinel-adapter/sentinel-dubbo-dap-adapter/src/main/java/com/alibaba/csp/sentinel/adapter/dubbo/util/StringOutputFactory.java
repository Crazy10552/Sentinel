package com.alibaba.csp.sentinel.adapter.dubbo.util;

import com.alibaba.csp.sentinel.adapter.dubbo.enums.DelimiterEnums;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.dap.param.StringOutput;


import java.util.Date;

/**
 * RPC层-rpc接口返回结果封装
 * <p><br/></p>
 *
 * @author: lixiongxing
 * @date: 2019-03-25
 */
public class StringOutputFactory {

    /**
     * 无返回实体，请求成功结果封装方法
     *
     * @return
     */
    public static StringOutput withSuccess() {
        return withSuccess(null);
    }

    /**
     * 有返回实体，请求成功结果封装方法
     *
     * @param result 返回结果
     * @return
     */
    public static <T> StringOutput withSuccess(T result) {
        StringOutput output = new StringOutput();
        output.getRespData().setCode(ResponseCode.SUCCESS);
        output.getRespData().setMessage(ResponseCode.SUCCESS_MSG);
        output.getRespData().setReturnTime(new Date());
        output.getRespData().setType(ResponseType.SUCCESS);
        output.getRespData().setServerIp(RpcContext.getContext().getLocalHost());
        if (result == null) {
            output.setBody(DelimiterEnums.BRACE.getCode());
        } else {
            output.setBody(JSONObject.toJSONString(result, new BigDecimalValueFilter(), SerializerFeature.WriteDateUseDateFormat));
        }
        return output;
    }

    /**
     * 有返回实体,返回结果为分页对象，请求成功结果封装方法
     *
     * @param result      返回结果
     * @param recordCount 分页数据记录总数
     * @return
     */
    public static <T> StringOutput withSuccess(T result, Long recordCount) {
        StringOutput output = withSuccess(result);
        output.getRespData().setRecordCount(recordCount != null ? recordCount.intValue() : 0);
        return output;
    }

    /**
     * 请求失败结果封装方法
     *
     * @param errorCode 错误编码
     * @return
     */
    public static StringOutput withFail(String errorCode) {
        return withFail(errorCode, CodeContainerHelper.getCodeDesc(errorCode));
    }

    /**
     * 请求失败结果封装方法
     *
     * @param errorCode 错误状态码
     * @param errorDesc 错误描述
     * @return
     */
    public static StringOutput withFail(String errorCode, String errorDesc) {
        StringOutput output = new StringOutput();
        output.getRespData().setType(ResponseType.FAIL);
        output.getRespData().setCode(errorCode);
        output.getRespData().setMessage(errorDesc);
        output.getRespData().setReturnTime(new Date());
        output.getRespData().setServerIp(RpcContext.getContext().getLocalHost());
        return output;
    }

    /**
     * 请求失败结果封装方法
     *
     * @param errorCode   错误状态码
     * @param errorParams 错误描述占位符替换参数
     * @return
     */
    public static StringOutput withFail(String errorCode, Object[] errorParams) {
        StringOutput output = new StringOutput();
        output.getRespData().setCode(errorCode);
        output.getRespData().setType(ResponseType.FAIL);
        output.getRespData().setMessage(CodeContainerHelper.getCodeDesc(errorCode, errorParams));
        output.getRespData().setReturnTime(new Date());
        output.getRespData().setServerIp(RpcContext.getContext().getLocalHost());
        return output;
    }

    public static class ResponseType {
        public static final String SUCCESS = "S";
        public static final String SUCCESS_MSG = "交易成功";
        public static final String FAIL = "F";
        public static final String FAIL_MSG = "交易失败";
        public static final String WAIT = "M";
        public static final String WAIT_MSG = "交易处理中";

        public ResponseType() {
        }
    }

    public static class ResponseCode {
        public static final String SUCCESS = "00000000";
        public static final String SUCCESS_MSG = "请求成功";
        public static final String SENTINEL_BLOCKED_PARAM_FLOW = "SEN00001";
        public static final String SENTINEL_BLOCKED_PARAM_FLOW_MSG = "Blocked by Sentinel (ParamFlow limiting)";
        public static final String SENTINEL_BLOCKED_DEGRADE = "SEN00002";
        public static final String SENTINEL_BLOCKED_DEGRADE_MSG = "Blocked by Sentinel (Degrade limiting)";
        public static final String SENTINEL_BLOCKED_FLOW = "SEN00003";
        public static final String SENTINEL_BLOCKED_FLOW_MSG = "Blocked by Sentinel (Flow limiting)";
        public static final String SENTINEL_BLOCKED_SYSTEM = "SEN00004";
        public static final String SENTINEL_BLOCKED_SYSTEM_MSG = "Blocked by Sentinel (System limiting)";
        public static final String SENTINEL_BLOCKED_AUTHORITY = "SEN00005";
        public static final String SENTINEL_BLOCKED_AUTHORITY_MSG = "Blocked by Sentinel (Authority limiting)";

        public ResponseCode() {
        }
    }
}
