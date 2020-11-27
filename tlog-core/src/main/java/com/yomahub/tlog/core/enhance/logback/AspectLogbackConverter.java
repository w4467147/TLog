package com.yomahub.tlog.core.enhance.logback;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.core.context.AspectLogContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bryan.Zhang
 */
public class AspectLogbackConverter extends ClassicConverter {

    private static Logger log = LoggerFactory.getLogger(AspectLogbackConverter.class);

    @Override
    public String convert(ILoggingEvent event) {
        //只有在MDC没有设置的情况下才加到message里
        if(!TLogContext.hasTLogMDC()){
            String logValue = AspectLogContext.getLogValue();

            log.info("[TLOG_DEBUG][{}]logback适配模式取到的标签为：{}",Thread.currentThread().getId(),logValue);

            if(StringUtils.isBlank(logValue)){
                return event.getFormattedMessage();
            }else{
                return logValue + " " + event.getFormattedMessage();
            }
        }else{
            return event.getFormattedMessage();
        }
    }
}
