package org.example.vocalchat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.vocalchat.entity.User;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM sys_user WHERE email = #{email}")
    User selectByEmail(@Param("email") String email);

    @Update("UPDATE sys_user SET password = #{password}, updated_at = NOW() WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @Update("UPDATE sys_user SET login_fail_count = #{count}, updated_at = NOW() WHERE id = #{id}")
    int updateLoginFailCount(@Param("id") Long id, @Param("count") Integer count);

    @Update("UPDATE sys_user SET status = #{status}, locked_until = #{lockedUntil}, updated_at = NOW() WHERE id = #{id}")
    int updateLockStatus(@Param("id") Long id, @Param("status") String status,
                         @Param("lockedUntil") java.time.LocalDateTime lockedUntil);

    @Update("UPDATE sys_user SET email_status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateEmailStatus(@Param("id") Long id, @Param("status") String status);
}
