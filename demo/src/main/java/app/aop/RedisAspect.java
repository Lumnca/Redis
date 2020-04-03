package app.aop;

import com.alibaba.fastjson.JSON;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class RedisAspect {
    @Pointcut("execution(* app.redis.*.*(..))")
    public void aop1(){

    }
    @Before(value = "aop1()")
    public void before(JoinPoint jp){
       // System.out.println(jp.getSignature().getName()+"方法开始执行！参数为:"+ JSON.toJSONString(jp.getArgs()));
    }
    @After(value = "aop1()")
    public void after(JoinPoint jp){
        //System.out.println(jp.getSignature().getName()+"方法执行结束！");
    }

}
