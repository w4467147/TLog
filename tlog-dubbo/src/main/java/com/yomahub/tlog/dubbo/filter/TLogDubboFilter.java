package com.yomahub.tlog.dubbo.filter;

import cn.hutool.core.net.NetUtil;
import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.SpanIdGenerator;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.context.TLogLabelGenerator;
import com.yomahub.tlog.core.context.AspectLogContext;
import com.yomahub.tlog.id.UniqueIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * dubbo的调用拦截器
 *
 * @author Bryan.Zhang
 * @since 2020/9/11
 */
@Activate(group = {CommonConstants.PROVIDER, CommonConstants.CONSUMER}, order = -10000)
public class TLogDubboFilter implements Filter {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        Result result;
        String side = invoker.getUrl().getParameter(CommonConstants.SIDE_KEY);

        if (side.equals(CommonConstants.PROVIDER_SIDE)) {
            String preIvkApp = invocation.getAttachment(TLogConstants.PRE_IVK_APP_KEY);
            String preIvkHost = invocation.getAttachment(TLogConstants.PRE_IVK_APP_HOST);
            String preIp = invocation.getAttachment(TLogConstants.PRE_IP_KEY);
            String traceId = invocation.getAttachment(TLogConstants.TLOG_TRACE_KEY);
            String spanId = invocation.getAttachment(TLogConstants.TLOG_SPANID_KEY);

            if (StringUtils.isBlank(preIvkApp)) {
                preIvkApp = TLogConstants.UNKNOWN;
            }
            if (StringUtils.isBlank(preIvkHost)) {
                preIvkHost = TLogConstants.UNKNOWN;
            }
            if (StringUtils.isBlank(preIp)) {
                preIp = TLogConstants.UNKNOWN;
            }

            TLogContext.putPreIvkApp(preIvkApp);
            TLogContext.putPreIvkHost(preIvkHost);
            TLogContext.putPreIp(preIp);

            //如果从隐式传参里没有获取到，则重新生成一个traceId
            if (StringUtils.isBlank(traceId)) {
                traceId = UniqueIdGenerator.generateStringId();
                log.debug("[TLOG]可能上一个节点[{}]没有没有正确传递traceId,重新生成traceId[{}]", preIvkApp, traceId);
            }

            //往TLog上下文里放当前获取到的spanId，如果spanId为空，会放入初始值
            TLogContext.putSpanId(spanId);

            //往TLog上下文里放一个当前的traceId
            TLogContext.putTraceId(traceId);

            //生成日志标签
            String tlogLabel = TLogLabelGenerator.generateTLogLabel(preIvkApp, preIvkHost, preIp, traceId, TLogContext.getSpanId());

            //往日志切面器里放一个日志前缀
            AspectLogContext.putLogValue(tlogLabel);

            //如果有MDC，则往MDC中放入日志标签
            if (TLogContext.hasTLogMDC()) {
                MDC.put(TLogConstants.MDC_KEY, tlogLabel);
            }

            try {
                //调用dubbo
                result = invoker.invoke(invocation);
            } finally {
                //移除ThreadLocal里的数据
                TLogContext.removePreIvkApp();
                TLogContext.removePreIvkHost();
                TLogContext.removePreIp();
                TLogContext.removeTraceId();
                TLogContext.removeSpanId();
                AspectLogContext.remove();
                if (TLogContext.hasTLogMDC()) {
                    MDC.remove(TLogConstants.MDC_KEY);
                }
            }

            return result;
        } else if (side.equals(CommonConstants.CONSUMER_SIDE)) {
            String traceId = TLogContext.getTraceId();

            if (StringUtils.isNotBlank(traceId)) {
                String appName = invoker.getUrl().getParameter(CommonConstants.APPLICATION_KEY);
                String ip = NetUtil.getLocalhostStr();
                String hostName = NetUtil.getLocalhost().getHostName();

                RpcContext.getContext().setAttachment(TLogConstants.TLOG_TRACE_KEY, traceId);
                RpcContext.getContext().setAttachment(TLogConstants.PRE_IVK_APP_KEY, appName);
                RpcContext.getContext().setAttachment(TLogConstants.PRE_IVK_APP_HOST, hostName);
                RpcContext.getContext().setAttachment(TLogConstants.PRE_IP_KEY, ip);
                RpcContext.getContext().setAttachment(TLogConstants.TLOG_SPANID_KEY, SpanIdGenerator.generateNextSpanId());
            } else {
                log.warn("[TLOG]本地threadLocal变量没有正确传递traceId,本次调用不传递traceId");
            }
            result = invoker.invoke(invocation);
        } else {
            result = null;
        }
        return result;
    }
}
