package com.xudasong.project.model;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@Data
public class GateWayProperties {

    @ApiModelProperty("直接放行")
    @Value("#{'${gateWayUrlType.release}'.split(';')}")
    private Set<String> release;

    @ApiModelProperty("第三方文档开环境直接放行")
    @Value("#{'${gatewayUrlType.third.doc}'.split(';')}")
    private Set<String> thirdDocRelease;

    @ApiModelProperty("认证")
    @Value("#{'${gatewayUrlType.auth}'.split(';')}")
    private Set<String> auth;

    @ApiModelProperty("环境")
    @Value("${spring.profiles.active}")
    private String env;

    @ApiModelProperty("白名单")
    private Set<String> white = new HashSet<>();

    @ApiOperation("获取白名单url集合")
    public Set<String> getWhite(){
        white.addAll(release);
        white.addAll(auth);
        return white;
    }

}
