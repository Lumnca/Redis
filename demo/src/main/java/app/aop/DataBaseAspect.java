package app.aop;


import app.course.CourseMapper;
import app.logger.Log;
import app.logger.LogRep;
import app.redis.BookServer;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
@Aspect

public class DataBaseAspect {
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    @Autowired
    CourseMapper logs;
    @Autowired
    LogRep logRep;
    @Pointcut("execution(* app.redis.BookDao.*(..))")
    public void aop2(){

    }
    @Before(value = "aop2()")
    public void before(JoinPoint jp){
       synchronized (this){
           Log log = new Log();
           log.setId(logRep.count()+1);
           log.setDate(simpleDateFormat.format(new Date()));
           log.setType("数据库操作");
           log.setUser("admin");
           log.setState(1);
           log.setInfo("操作方法："+jp.getSignature().getName());
           logs.addLogs(log);
           System.out.println(jp.getSignature().getName()+"方法被执行！-------------> " + simpleDateFormat.format(new Date()));
       }

    }
    @After(value = "aop2()")
    public void after(JoinPoint jp){
        //System.out.println(jp.getSignature().getName()+"方法执行结束！");
    }
}
