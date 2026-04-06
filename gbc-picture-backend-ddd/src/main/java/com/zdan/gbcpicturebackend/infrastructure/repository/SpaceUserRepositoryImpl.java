package com.zdan.gbcpicturebackend.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zdan.gbcpicturebackend.domain.space.entity.SpaceUser;
import com.zdan.gbcpicturebackend.domain.space.repository.SpaceUserRepository;
import com.zdan.gbcpicturebackend.infrastructure.mapper.SpaceUserMapper;
import org.springframework.stereotype.Repository;

/**
 * 空间成员仓储实现
 */
@Repository
public class SpaceUserRepositoryImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
        implements SpaceUserRepository {
}
