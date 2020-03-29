package com.xudasong.project.utils;

import com.google.common.base.Splitter;
import com.xudasong.project.model.GateWayProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Collectors;

@Component
public class PathUtils {

    @Autowired
    private GateWayProperties gateWayProperties;

    private static PathUtils pathUtils;

    public void setGateWayProperties(GateWayProperties gateWayProperties){
        this.gateWayProperties = gateWayProperties;
    }

    @PostConstruct
    public void init(){
        pathUtils = this;
        pathUtils.gateWayProperties = this.gateWayProperties;
    }

    public static boolean checkWhite(String path, HttpMethod method){
        if ("dev".equals(pathUtils.gateWayProperties.getEnv())){
            boolean matchResult = match(pathUtils.gateWayProperties.getThirdDocRelease(),path,method);
            if (matchResult){
                return matchResult;
            }
        }
        return match(pathUtils.gateWayProperties.getWhite(), path, method);
    }

    public static boolean checkRelease(String path, HttpMethod method){
        if ("dev".equals(pathUtils.gateWayProperties.getEnv())){
            boolean matchResult = match(pathUtils.gateWayProperties.getThirdDocRelease(),path,method);
            if (matchResult){
                return matchResult;
            }
        }
        return match(pathUtils.gateWayProperties.getRelease(), path, method);
    }

    public static boolean checkAuth(String path, HttpMethod method){
        return match(pathUtils.gateWayProperties.getAuth(), path, method);
    }

    private static Boolean match(Set<String> pathMethodSet, String reqPath, HttpMethod reqMethod){
        Boolean match = false;
        if (CollectionUtils.isEmpty(pathMethodSet)){
            return match;
        }
        AntPathMatcher pathMatcher = new AntPathMatcher();
        for (String pathMethodStr : pathMethodSet){
            List<String> pathMethodList = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(pathMethodStr);
            String pathStr = pathMethodList.get(0);
            //不匹配请求方式，只匹配路径
            if (pathMethodList.size() == 1 && pathMatcher.match(pathStr,reqPath)){
                return true;
            }
            //匹配请求方法和路径
            if (pathMethodList.size() >= 2){
                List<String> methodList = Splitter.on("|").omitEmptyStrings().trimResults().splitToList(pathMethodList.get(1));
                //转换为大写
                List<String> uppercaseMethodList = methodList.stream().map(String::toUpperCase)
                        .collect(Collectors.toList());
                if (pathMatcher.match(pathStr,reqPath) && (uppercaseMethodList.contains(reqMethod.name()))){
                    return true;
                }
            }
        }
        return match;
    }

}
