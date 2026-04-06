package com.zdan.gbcpicturebackend.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zdan.gbcpicturebackend.domain.space.entity.Space;
import com.zdan.gbcpicturebackend.domain.space.repository.SpaceRepository;
import com.zdan.gbcpicturebackend.infrastructure.mapper.SpaceMapper;
import org.springframework.stereotype.Repository;

/**
 * 空间仓储实现
 */
@Repository
public class SpaceRepositoryImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceRepository {
}
