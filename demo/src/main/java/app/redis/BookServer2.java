package app.redis;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 本地缓存
 */
@Service
@CacheConfig(cacheNames = "book_cache",cacheManager = "eCacheCacheManager")
public class BookServer2 {
    @Autowired
    BookDao bookDao;
    @Cacheable(value = "book_cache")
    public String getBookById(Integer id){
        System.out.println("========从本地缓存写入数据==========");
        return JSON.toJSONString( bookDao.getOne(id));
    }
}
