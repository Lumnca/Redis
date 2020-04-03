package app.redis;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Redis缓存
 */
@Service
@CacheConfig(cacheNames = "c1",cacheManager = "redisCacheManager")
    public class BookServer{

    private static int count = 0;

    @Autowired
    BookDao bookDao;

    @Cacheable(value = "c1")
    public String getBookById(Integer id){
        System.out.println("==================从redis缓存写入数据==================");
        count++;
        return JSON.toJSONString(bookDao.getOne(id));
    }

    @CachePut(key = "#book.id")
    public String updateBookById(Book book){
        count++;
        bookDao.save(book);
        System.out.println("==================updateBook=====================");
        return JSON.toJSONString(book);
    }
    @CacheEvict(key = "#id")
    public String deleteBookById(Integer id){
        System.out.println("==================deleteBook=====================");
        return "delete";
    }

    public String getBookByName(String name){
        count++;
        return JSON.toJSONString(bookDao.getBookByName(name));
    }
    public String getBook (Integer id){
        count++;
        return JSON.toJSONString(bookDao.getOne(id));
    }
    public Integer getCount(){
        return count;
    }
    public void setCount(int i){
        count = i;
    }
}
