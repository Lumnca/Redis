package app.aop;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AopTest {
    @Autowired
    AopTestMethod aopTestMethod;
    @GetMapping("/aop1")
    public String getData(){
        return aopTestMethod.getUserById(1);
    }
    @GetMapping("/aop2")
    public void delData(){
        aopTestMethod.deleteUserById(1);
    }
}
