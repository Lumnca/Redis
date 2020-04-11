package app.redis;


import app.logger.Log;
import app.reponseData.Response;
import com.alibaba.fastjson.JSON;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;



@RestController
public class DemoAPI {

    @Autowired
    BookDao bookDao;
    @Autowired
    BookServer bookServer;
    @Autowired
    BookServer2 bookServer2;

    @GetMapping("_books/{id}")
    public String getBookById(@PathVariable("id")  Integer id) {
        String book;
        try {
            book = bookServer.getBookById(id);
        }
        //也可以直接捕捉对应的错误
        catch (Exception e){
            //直接从数据库获取
            book = JSON.toJSONString(bookDao.getOne(id));
        }
        return book;
    }
    @GetMapping("_2books/{id}")
    public String getBook2ById(@PathVariable("id")  Integer id) {
        String book;
        try {
            //redis获取
            book = bookServer.getBookById(id);
        }
        catch (Exception e){
            //本地获取
            book = bookServer2.getBookById(id);
        }
        return book;
    }
    @GetMapping("_3books/{id}")
    @HystrixCommand(defaultFallback = "fallback",commandProperties = {
            @HystrixProperty(name="execution.isolation.thread.timeoutInMilliseconds", value = "5000"),   //最大响应时间5000ms
    })
    public String getBook3ById(@PathVariable("id")  Integer id) {
        String book;
        try {
            book = bookServer.getBookById(id);
        }
        catch (Exception e){
            book = bookServer2.getBookById(id);
        }
        return book;
    }
    //执行的回退方法
    public String fallback(){
        return "请稍后再访问！";
    }

    @GetMapping("_4books/{id}")
    @HystrixCommand(fallbackMethod = "fallback2")
    public String getBook4ById(@PathVariable("id")Integer id) {
        return bookServer.getBookById(id);
    }
    //回退方法：
    public String fallback2(@PathVariable("id")Integer id){
        System.out.println("出现了异常！");
        return bookServer2.getBookById(id);
    }

    @GetMapping("getBookByName")
    public String getBookByName(String name){
        return bookServer.getBookByName(name);
    }



}
