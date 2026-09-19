package com.cq.maintenance.audit.aspect;
import com.cq.maintenance.audit.mapper.OperationLogMapper;
import com.cq.maintenance.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.aspectj.lang.*;
import org.aspectj.lang.annotation.*;
import org.slf4j.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.*;

@Aspect @Component
public class OperationAuditAspect {
    private static final Logger log=LoggerFactory.getLogger(OperationAuditAspect.class);private static final Set<String> READ=Set.of("GET","HEAD","OPTIONS");private final ObjectProvider<OperationLogMapper> mapperProvider;
    public OperationAuditAspect(ObjectProvider<OperationLogMapper> mapperProvider){this.mapperProvider=mapperProvider;}
    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object audit(ProceedingJoinPoint point)throws Throwable{ServletRequestAttributes attrs=(ServletRequestAttributes)RequestContextHolder.getRequestAttributes();if(attrs==null)return point.proceed();HttpServletRequest req=attrs.getRequest();if(READ.contains(req.getMethod()))return point.proceed();long started=System.nanoTime();try{Object result=point.proceed();write(req,point,"SUCCESS",null,started);return result;}catch(Throwable ex){write(req,point,"FAIL",safe(ex.getMessage(),500),started);throw ex;}}
    private void write(HttpServletRequest req,ProceedingJoinPoint point,String result,String error,long started){try{OperationLogMapper mapper=mapperProvider.getIfAvailable();if(mapper==null)return;Authentication a=SecurityContextHolder.getContext().getAuthentication();Long userId=a!=null&&a.getPrincipal() instanceof LoginUser u?u.userId():null;String uri=safe(req.getRequestURI(),255);String[] parts=uri.split("/");String module=parts.length>2?safe(parts[2],50):"system";String operation=safe(point.getSignature().getDeclaringType().getSimpleName()+"."+point.getSignature().getName(),100);String ip=clientIp(req);String summary="path="+uri;String query=req.getQueryString();if(query!=null&&!query.isBlank())summary+="; query-keys="+queryKeys(query);mapper.insert(userId,module,operation,uri,req.getMethod(),safe(ip,64),safe(summary,2000),result,error,Math.max(0,(System.nanoTime()-started)/1_000_000));}catch(Exception auditError){log.warn("Operation audit persistence failed: {}",auditError.getMessage());}}
    private String queryKeys(String query){return Arrays.stream(query.split("&")).map(v->v.split("=",2)[0]).filter(k->!k.toLowerCase(Locale.ROOT).matches(".*(token|password|secret).*" )).distinct().limit(20).reduce((a,b)->a+","+b).orElse("-");}
    private String clientIp(HttpServletRequest req){String forwarded=req.getHeader("X-Forwarded-For");return forwarded==null||forwarded.isBlank()?req.getRemoteAddr():forwarded.split(",",2)[0].trim();}
    private String safe(String value,int max){if(value==null)return null;String normalized=value.replaceAll("[\\r\\n\\t]"," ");return normalized.length()<=max?normalized:normalized.substring(0,max);}
}
