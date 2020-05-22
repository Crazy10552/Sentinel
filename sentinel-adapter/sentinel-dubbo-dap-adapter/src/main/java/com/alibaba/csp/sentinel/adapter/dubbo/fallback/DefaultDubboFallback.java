/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.adapter.dubbo.fallback;


import com.alibaba.csp.sentinel.adapter.dubbo.util.StringOutputFactory;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.SentinelRpcException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.fastjson.JSON;
import com.dap.api.IService;
import com.dap.param.StringOutput;
import com.hip.mhp.core.api.IHipService;
import com.hip.mhp.core.api.param.BaseOutput;
import com.hip.mhp.core.api.param.ResponseHeader;
import com.hip.mhp.core.helper.CodeContainerHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;

/**
 * @author Eric Zhao
 */
@Slf4j
public class DefaultDubboFallback implements DubboFallback {

    /**
     * -----------------------------------------------------------
     *
     * @param  invoker	请求入参
     * @param invocation	请求入参
     * @param ex 请求入参
     * @return com.alibaba.dubbo.rpc.Result	返回结果对象
     *-----------------------------modification------------------------------.
     * @author yttiany
     * @date  modify at 2020/3/31.16:16
     * 为了满足框架需求,同时做最小的改造支持,这边就针对这个DefaultDubboFallback下手
     *-----------------------------------------------------------
     */
    @Override
    public Result handle(Invoker<?> invoker, Invocation invocation, BlockException ex) {
        RpcResult result = null;

        Class interfaceClass = invoker.getInterface();
        if (interfaceClass != null) {
            //针对农信这边特殊的两个interface(返回结果是固定格式的)做出处理,其他的情况如果擅自修改接口返回结果的结构会造成dubbo框架解析结果抛出异常
            if( IHipService.class.getSimpleName().equals(interfaceClass.getSimpleName()) || IService.class.getSimpleName().equals(interfaceClass.getSimpleName()) ){
                //为了满足框架需求,同时做最小的改造支持,这边就针对这个DefaultDubboFallback下手

                if( ex instanceof ParamFlowException){
                    String msg = getErrorMsgByProperties(StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_PARAM_FLOW,StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_PARAM_FLOW_MSG);
                    result = formatDapInterfaceResult(interfaceClass.getSimpleName(),StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_PARAM_FLOW,msg);
                }

                if( ex instanceof DegradeException){
                    String msg = getErrorMsgByProperties(StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_DEGRADE,StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_DEGRADE_MSG);
                    result = formatDapInterfaceResult(interfaceClass.getSimpleName(),StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_DEGRADE,msg);
                }

                if( ex instanceof SystemBlockException){
                    String msg = getErrorMsgByProperties(StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_SYSTEM,StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_SYSTEM_MSG);
                    result = formatDapInterfaceResult(interfaceClass.getSimpleName(),StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_SYSTEM,msg);
                }

                if( ex instanceof FlowException){
                    String msg = getErrorMsgByProperties(StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_FLOW,StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_FLOW_MSG);
                    result = formatDapInterfaceResult(interfaceClass.getSimpleName(),StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_FLOW,msg);
                }

                if( ex instanceof AuthorityException){
                    String msg = getErrorMsgByProperties(StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_AUTHORITY,StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_AUTHORITY_MSG);
                    result = formatDapInterfaceResult(interfaceClass.getSimpleName(),StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_AUTHORITY,msg);
                }
            }
        }
        if(result==null){
            log.info("unfilterException",ex);
            // Just wrap and throw the exception.
            throw new SentinelRpcException(ex);
        }else{
            log.info("blocked "+JSON.toJSONString(result));
            return result;
        }
    }

    /**
     * 如果配置文件里有值则以配置文件里的错误提示为准,否则用默认值
     * @param errorCode 错误码
     * @param errorMsg 错误提示信息
     * @return 最终的错误提示语
     */
    private String getErrorMsgByProperties(String errorCode,String errorMsg){
        String msg = null;
        try {
            msg = CodeContainerHelper.getCodeDesc(errorCode);
            if(StringUtils.isBlank(msg)){
                msg = errorMsg;
            }
        } catch (Exception e) {
            log.error("",e);
            msg = errorMsg;
        }
        return msg;
    }

    /**
     * 针对两类特殊的interface来修改在熔断限流情况下的接口返回值
     * @param interfaceName api 的名称
     * @param errorCode 错误码
     * @param errorMsg 错误信息
     * @return RpcResult 对象
     */
    private RpcResult formatDapInterfaceResult(String interfaceName,String errorCode,String errorMsg){
        RpcResult result = null;
        if( IHipService.class.getSimpleName().equals(interfaceName) ){
            result = new RpcResult();
            BaseOutput baseOutput = new BaseOutput();
            ResponseHeader responseHeader = new ResponseHeader();
            responseHeader.setErrorCode(errorCode);
            responseHeader.setErrorMsg(errorMsg);
            responseHeader.setSuccess(false);
            baseOutput.setHeader(responseHeader);
            result.setValue(baseOutput);
        }else if( IService.class.getSimpleName().equals(interfaceName) ){
            result = new RpcResult();
            StringOutput baseOutput = StringOutputFactory.withFail(errorCode,errorMsg);
            result.setValue(baseOutput);
        }
        return result;
    }
}
