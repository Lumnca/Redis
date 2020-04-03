package app.redis;

import app.reponseData.Response;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@CrossOrigin
@ConfigurationProperties(prefix = "spring.redis.cluster")
public class RedisTest {
    @Autowired
    BookDao bookService;
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    BookServer bookServer;
    @Autowired
    BookServer2 bookServer2;

    /**
     * Redis节点测试
     * @param host
     * @param post
     * @return
     */
    @GetMapping("connect")
    public Response getConnect(String host,int post){
        Response response = new Response(0,"");
        try {
            Jedis jedis = new Jedis(host,post);
            jedis.auth("chuan.868");
            if(jedis.isConnected()){
                response.setCode(200);
                response.setMessage("运行正常");
            }
            else{
                response.setCode(400);
                response.setMessage("无连接");
            }
        }
        catch (Exception e){
            response.setCode(500);
            response.setMessage(e.getMessage());
        }
        return response;
    }

    /**
     * 有效时间根据访问量增加
     * @param id
     * @return
     */
    @GetMapping("getBook/{id}")
    public String getBookById(@PathVariable("id")Integer id) {
        String key = "sang:"+id;
        if(stringRedisTemplate.hasKey(key)){
            stringRedisTemplate.expire(key,stringRedisTemplate.getExpire(key,TimeUnit.SECONDS)+1800,TimeUnit.SECONDS);
        }
        return bookServer.getBookById(id);
    }

    /**
     * 数据库互斥锁访问
     * @param id
     * @return
     */
    @GetMapping("getBook2/{id}")
    public String getBook2ById(@PathVariable("id")Integer id) {
        String book;
        if(stringRedisTemplate.hasKey("sang:"+id)){
            return bookServer.getBookById(id);
        }
        synchronized (this){
            book = bookServer.getBookById(id);
        }
        return book;
    }

    /**
     * 更新缓存数据
     * @param book
     * @return
     */
    @PostMapping("updatebook")
    public Response updateBookById(@RequestBody Book book){
        if(bookServer.updateBookById(book)!=null){
            return new Response(200,"修改成功！");
        }
        else{
            return new Response(400,"修改失败！");
        }
    }

    /**
     * 删除缓存数据
     * @param id
     * @return
     */
    @DeleteMapping("deleteBookById/{id}")
    public String deleteBookBuId(@PathVariable("id") Integer id){
        return bookServer.deleteBookById(id);
    }

    /**
     * 获取redis缓存数据
     * @param key
     * @return
     */
    @GetMapping("cache/{key}")
    public Response getCacheValue(@PathVariable("key")String key){
        Response response = new Response(0,"");
        JSONObject object = new JSONObject();
        if(stringRedisTemplate.hasKey(key)){
            response.setCode(200);
            object.put("key",key);
            object.put("value",stringRedisTemplate.opsForValue().get(key));
            object.put("time",stringRedisTemplate.getExpire(key,TimeUnit.SECONDS));
            response.setMessage(JSONObject.toJSONString(object));
        }
        else{
            response.setCode(404);
            response.setMessage("无数据");
        }
        return  response;
    }

    /**
     * 重置缓存过期时间
     * @param object
     * @return
     */
    @PostMapping("setexpire")
    public Response setExpire(@RequestBody JSONObject object){
        if(stringRedisTemplate.expire(object.getString("key"),object.getInteger("sec"),TimeUnit.SECONDS)){
            return new Response(200,"重置成功！");
        }
        else{
            return new Response(500,"重置失败");
        }
    }

    /**
     * 获取数据库访问数量
     * @return
     */
    @GetMapping("getcount")
    public Integer getCount(){
        return bookServer.getCount();
    }

    /**
     * 重置数据库访问数量
     * @return
     */
    @PostMapping("setcount")
    public Response setCount(){
        bookServer.setCount(0);
        return new Response(200,"重置成功！");
    }

    /**
     * 获取热点数据0-20的索引号
     * @return
     */
    @GetMapping("data")
    public List<JSONObject> getHotDataList(){
        List<JSONObject> list = new ArrayList<>();
        for (int i = 1; i < 20; i++) {
            if(stringRedisTemplate.hasKey("sang:"+i)){
                JSONObject object = new JSONObject();
                object.put("id",i);
                object.put("key","sang:"+i);
                object.put("timeout",stringRedisTemplate.getExpire("sang:"+i,TimeUnit.SECONDS));
                list.add(object);
            }
        }
        return list;
    }

    /**
     * 通过数据库访问
     * @param name
     * @return
     */
    @GetMapping("bookName/{name}")
    public String getBookByName(@PathVariable("name") String name){
        return bookServer.getBookByName(name);
    }
}