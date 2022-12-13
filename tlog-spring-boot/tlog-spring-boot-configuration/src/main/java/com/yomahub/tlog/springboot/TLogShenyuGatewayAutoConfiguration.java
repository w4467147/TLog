package com.yomahub.tlog.springboot;

import com.yomahub.tlog.webflux.aop.ShenyuPluginLogAop;
import com.yomahub.tlog.webflux.filter.TLogWebFluxFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Copy from @author naah
 * @author timgise
 * @since 1.5.0
 */
@Configuration
@ConditionalOnClass(name = "org.apache.shenyu.web.handler.ShenyuWebHandler")
public class TLogShenyuGatewayAutoConfiguration {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public TLogWebFluxFilter traceFilter() {
        return new TLogWebFluxFilter();
    }

    @Bean
    public ShenyuPluginLogAop pluginLogAop() { return new ShenyuPluginLogAop(); }

}
