package com.alibaba.csp.sentinel.adapter.dubbo.filter;

import com.alibaba.csp.sentinel.adapter.dubbo.util.CodeContainerHelper;
import com.alibaba.csp.sentinel.adapter.dubbo.util.StringOutputFactory;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.*;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.alibaba.fastjson.JSON;
import com.dap.api.IService;
import com.dap.param.StringOutput;
import com.hip.mhp.core.api.IHipService;
import com.hip.mhp.core.api.param.BaseOutput;
import com.hip.mhp.core.api.param.ResponseHeader;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

/**
 * 参考dubbo框架自带的com.alibaba.dubbo.rpc.filter.ExceptionFilter 来做实现
 * @author yttiany
 * @date Create on 2020/5/7.14:36
 * -----------------------------modification------------------------------.
 * @date modify at 2020/5/7.14:36
 * <p>
 * -----------------------------------------------------------
 */
@Slf4j
@Data
@Activate(group = Constants.CONSUMER)
public class DubboDapSentinelBlockExceptionFilter implements Filter{


    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        try {
            Result result = invoker.invoke(invocation);
            if (result.hasException() && GenericService.class != invoker.getInterface()) {
                try {
                    Throwable exception = result.getException();
                    // 如果是checked异常，直接抛出
                    if (! (exception instanceof RuntimeException) && (exception instanceof Exception)) {
                        return result;
                    }
                    // 在方法签名上有声明，直接抛出
                    try {
                        Method method = invoker.getInterface().getMethod(invocation.getMethodName(), invocation.getParameterTypes());
                        Class<?>[] exceptionClassses = method.getExceptionTypes();
                        for (Class<?> exceptionClass : exceptionClassses) {
                            if (exception.getClass().equals(exceptionClass)) {
                                return result;
                            }
                        }
                    } catch (NoSuchMethodException e) {
                        return result;
                    }

                    // 未在方法签名上定义的异常，在服务器端打印ERROR日志
//                    log.error("Got unchecked and undeclared exception which called by " + RpcContext.getContext().getRemoteHost()+ ". service: " + invoker.getInterface().getName() + ", method: " + invocation.getMethodName() + ", exception: " + exception.getClass().getName() + ": " + exception.getMessage(), exception);

                    // 异常类和接口类在同一jar包里，直接抛出
                    String serviceFile = ReflectUtils.getCodeBase(invoker.getInterface());
                    String exceptionFile = ReflectUtils.getCodeBase(exception.getClass());
                    if (serviceFile == null || exceptionFile == null || serviceFile.equals(exceptionFile)){
                        return result;
                    }
                    // 是JDK自带的异常，直接抛出
                    String className = exception.getClass().getName();
                    if (className.startsWith("java.") || className.startsWith("javax.")) {
                        return result;
                    }
                    // 是Dubbo本身的异常，直接抛出
                    if (exception instanceof RpcException) {
                        RpcException rpcException = (RpcException) exception;
                        Class interfaceClass = invoker.getInterface();
                        if( interfaceClass!=null && org.apache.commons.lang3.StringUtils.isNotBlank(interfaceClass.getSimpleName()) ){
                            if( IHipService.class.getSimpleName().equals(interfaceClass.getSimpleName()) || IService.class.getSimpleName().equals(interfaceClass.getSimpleName()) ){
                                //为了满足框架需求,同时做最小的改造支持,这边就针对这个DefaultDubboFallback下手

                                int startIndex = rpcException.getMessage().lastIndexOf("Caused by:")+11;
                                int endIndex = rpcException.getMessage().length()-2;
                                String resultStr = rpcException.getMessage().substring(startIndex,endIndex);
                                if( resultStr.equals(SystemBlockException.class.getCanonicalName()) ){
                                    String msg = getErrorMsgByProperties(StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_SYSTEM,StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_SYSTEM_MSG);
                                    result = formatDapInterfaceResult(interfaceClass.getSimpleName(),StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_SYSTEM,msg);
                                }else if( resultStr.equals(AuthorityException.class.getCanonicalName()) ){
                                    String msg = getErrorMsgByProperties(StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_AUTHORITY,StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_AUTHORITY_MSG);
                                    result = formatDapInterfaceResult(interfaceClass.getSimpleName(),StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_AUTHORITY,msg);
                                }else if( resultStr.equals(FlowException.class.getCanonicalName()) ){
                                    String msg = getErrorMsgByProperties(StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_FLOW,StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_FLOW_MSG);
                                    result = formatDapInterfaceResult(interfaceClass.getSimpleName(),StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_FLOW,msg);
                                }else if( resultStr.equals(DegradeException.class.getCanonicalName()) ){
                                    String msg = getErrorMsgByProperties(StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_DEGRADE,StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_DEGRADE_MSG);
                                    result = formatDapInterfaceResult(interfaceClass.getSimpleName(),StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_DEGRADE,msg);
                                }else if( resultStr.equals(ParamFlowException.class.getCanonicalName()) ){
                                    String msg = getErrorMsgByProperties(StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_PARAM_FLOW,StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_PARAM_FLOW_MSG);
                                    result = formatDapInterfaceResult(interfaceClass.getSimpleName(),StringOutputFactory.ResponseCode.SENTINEL_BLOCKED_PARAM_FLOW,msg);
                                }
                                log.info("blocked "+JSON.toJSONString(result));
                            }
                        }
                    }

                    if( result==null){
                        // 否则，包装成RuntimeException抛给客户端
                        return new RpcResult(new RuntimeException(StringUtils.toString(exception)));
                    }
                } catch (Throwable e) {
                    log.warn("Fail to ExceptionFilter when called by " + RpcContext.getContext().getRemoteHost()+ ". service: " + invoker.getInterface().getName() + ", method: " + invocation.getMethodName() + ", exception: " + e.getClass().getName() + ": " + e.getMessage(), e);
                    return result;
                }
            }
            return result;
        } catch (RuntimeException e) {
            log.error("Got unchecked and undeclared exception which called by " + RpcContext.getContext().getRemoteHost()
                    + ". service: " + invoker.getInterface().getName() + ", method: " + invocation.getMethodName()
                    + ", exception: " + e.getClass().getName() + ": " + e.getMessage(), e);
            throw e;
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
            if(org.apache.commons.lang3.StringUtils.isBlank(msg)){
                msg = errorMsg;
            }
        } catch (Exception e) {
            log.error("",e);
            msg = errorMsg;
        }
        msg = "请稍候再试(医疗云：["+msg+"])";
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
