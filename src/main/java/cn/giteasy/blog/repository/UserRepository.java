package cn.giteasy.blog.repository;

import cn.giteasy.blog.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;


/**
 * Created by Axin in 2019/12/16 20:02
 *
 * 不用写实现类，JPA已经有实现类了
 */

public interface UserRepository extends JpaRepository<User,Long> {

    /**
     * 根据用户姓名分页查询用户列表
     * @param name
     * @param pageable
     * @return
     */
    Page<User> findByNameLike(String name, Pageable pageable);


    /**
     * 根据用户账号查询用户
     * @param username
     * @return
     */
    User findByUsername(String username);


    /**
     * 根据名称列表查询
     * @param usernames
     * @return
     */
    List<User> findByUsernameIn(Collection<String> usernames);

}
