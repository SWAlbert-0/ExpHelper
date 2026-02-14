package fjnu.edu.common.config.swagger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket createBasicDataApi() {
        return new Docket(DocumentationType.SWAGGER_2).apiInfo(apiInfo1())
                .groupName("Springboot")
                .select()
                // 方法需要有ApiOperation注解才能生存接口文档
                .apis(RequestHandlerSelectors.basePackage("com.example.algorithm.api"))
                // 路径使用any风格
                .paths(PathSelectors.any())
                .build();

    }
    private ApiInfo apiInfo1() {
        return new ApiInfoBuilder()
                .title("Springboot项目swagger：Springbootweb")
                .description("Springboot项目-api接口")
                .termsOfServiceUrl("http://www.localhost:8080")
                .version("1.0.0")
                .build();
    }


}
