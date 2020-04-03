package app.aop;


import com.alibaba.fastjson.JSON;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.http.HttpServletRequest;


@Component
@Aspect
public class LogAspect {
    @Pointcut("execution(* app.aop.*.*(..))")
    public void pc1(){

    }

    @Before(value = "pc1()")
    public void before(JoinPoint jp){
        System.out.println(jp.getSignature().getName()+"方法开始执行！参数为:"+JSON.toJSONString(jp.getArgs()));
    }
    @After(value = "pc1()")
    public void after(JoinPoint jp){
        System.out.println(jp.getSignature().getName()+"方法执行结束！");
    }
    @AfterReturning(value = "pc1()",returning = "result")
    public void  afterReturning(JoinPoint jp,Object result){
        System.out.println(jp.getSignature().getName()+"方法的返回值是"+ JSON.toJSONString(result));
    }
    @AfterThrowing(value = "pc1()",throwing = "e")
    public void afterThrowing(JoinPoint jp,Exception e){
        System.out.println(jp.getSignature().getName()+"方法抛出异常！异常信息为:"+e.getMessage());
    }
    @Around(value = "pc1()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        return pjp.proceed();
    }
}
