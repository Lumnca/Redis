package app.aop;

import org.springframework.stereotype.Service;

@Service
public class AopTestMethod {
    public String getUserById(Integer id){
        System.out.println("get user id:"+id);
        return "patty";
    }
    public void  deleteUserById(Integer id){
        System.out.println("delete user id:"+id);
    }
}
