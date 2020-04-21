package app.redis;


import com.alibaba.fastjson.JSON;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;


@RestController
public class DemoAPI {

    private static Lock lock;
    @Autowired
    BookDao bookDao;
    @Autowired
    BookServer bookServer;
    @Autowired
    BookServer2 bookServer2;

    @GetMapping("_book/{id}")
    public String getBook1ById(@PathVariable("id")  Integer id) {
        return bookServer.getBookById(id);
    }
    @GetMapping("_books/{id}")
    public String getBookById(@PathVariable("id")  Integer id) {
        String book;
        try {
            book = bookServer.getBookById(id);
            System.out.println("缓存获取");
        }
        //也可以直接捕捉对应的错误
        catch (Exception e){
            //直接从数据库获取
            System.out.println("数据库获取");
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

    @HystrixCommand(fallbackMethod = "fallback",commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "8000"),
            @HystrixProperty(name = "execution.timeout.enabled", value = "false"),
    },
            threadPoolProperties = {
                    @HystrixProperty(name = "coreSize", value = "2"),
                    @HystrixProperty(name = "maxQueueSize", value = "1"),
                    @HystrixProperty(name = "keepAliveTimeMinutes", value = "2"),
                    @HystrixProperty(name = "queueSizeRejectionThreshold", value = "15"),
                    @HystrixProperty(name = "metrics.rollingStats.numBuckets", value = "12"),
                    @HystrixProperty(name = "metrics.rollingStats.timeInMilliseconds", value = "1440")
            })
    @GetMapping("_3books/{id}")
    public String getBook3ById(@PathVariable("id")  Integer id) {
        return bookServer.getBookById(id);
    }
    //执行的回退方法
    public String fallback(@PathVariable("id")  Integer id){
        return "服务繁忙！";
    }


    @HystrixCommand(fallbackMethod = "fallback2")
    @GetMapping("_4books/{id}")
    public String getBook4ById(@PathVariable("id")Integer id) {
        return bookServer.getBookById(id);
    }
    //回退方法：
    public String fallback2(@PathVariable("id")Integer id){
        System.out.println("出现了异常！已从本地缓存获取！");
        String data = bookServer2.getBookById(id);
        System.out.println(data);
        return data;
    }



    @GetMapping("getBookByName")
    public String getBookByName(String name){
        return bookServer.getBookByName(name);
    }
    @PostMapping("sa")
    public List<String> assa(@RequestBody List<String> list){
        return  list;
    }


    @GetMapping("lock")
    public void lock(){
            try {
                if(Lock.tryLock()){
                    System.out.println("获取成功！"+"当前数量:"+Lock.get());
                    Thread.sleep(1000);
                    Lock.unLock();
                }
                else {
                    Thread.sleep(3000);
                    lock();
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }

    }

}
