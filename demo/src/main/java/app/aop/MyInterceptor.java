package app.aop;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class MyInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            if(null==request.getSession().getAttribute("admin")){
                System.out.println("未登录账户!");
                response.sendRedirect("http://127.0.0.1:8081/login.html");
            }
            else{
                System.out.println("已登录！！");
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return true;
    }
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView){
        System.out.println("MyInterceptor1>>>postHandle");
    }
    @ Override public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        System.out.println("MyInterceptor1>>>afterCompletion");
    }
}
