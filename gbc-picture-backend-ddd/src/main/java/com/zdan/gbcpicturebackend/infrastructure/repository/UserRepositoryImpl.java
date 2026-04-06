package com.zdan.gbcpicturebackend.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zdan.gbcpicturebackend.domain.user.entity.User;
import com.zdan.gbcpicturebackend.infrastructure.mapper.UserMapper;
import org.springframework.stereotype.Repository;

/**
 * 用户仓储实现
 */
@Repository
public class UserRepositoryImpl extends ServiceImpl<UserMapper, User>
        implements com.zdan.gbcpicturebackend.domain.user.repository.UserRepository {
}
