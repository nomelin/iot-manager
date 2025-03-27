package top.nomelin.iot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.nomelin.iot.interceptor.DebugInterceptor;
import top.nomelin.iot.interceptor.TokenInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {


    private final TokenInterceptor tokenInterceptor;
    private final DebugInterceptor debugInterceptor;

    public WebConfig(TokenInterceptor tokenInterceptor, DebugInterceptor debugInterceptor) {
        this.tokenInterceptor = tokenInterceptor;
        this.debugInterceptor = debugInterceptor;
    }

    // 加自定义拦截器,设置拦截规则
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(debugInterceptor).addPathPatterns("/**");
        registry.addInterceptor(tokenInterceptor).addPathPatterns("/**")
//                .excludePathPatterns("/")
                .excludePathPatterns("/openapi/**")
                .excludePathPatterns("/connect/**");

    }
}
