package app.redis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin
public interface BookDao extends JpaRepository<Book,Integer> {
    @Query(value = "select b from books b where name = :name")
    Book getBookByName(@Param(value = "name") String name);
}
