package com.zdan.gbcpicturebackend.infrastructure.repository;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zdan.gbcpicturebackend.domain.picture.entity.Picture;
import com.zdan.gbcpicturebackend.domain.picture.repository.PictureRepository;
import com.zdan.gbcpicturebackend.infrastructure.mapper.PictureMapper;
import org.springframework.stereotype.Repository;

/**
 * 图片仓储实现
 */
@Repository
public class PictureRepositoryImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureRepository {
}
