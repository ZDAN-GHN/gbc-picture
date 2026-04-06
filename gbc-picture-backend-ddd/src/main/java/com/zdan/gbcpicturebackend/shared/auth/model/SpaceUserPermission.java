package com.zdan.gbcpicturebackend.shared.auth.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间成员权限
 */
@Data
public class SpaceUserPermission implements Serializable {

    private static final long serialVersionUID = -2743107559743076328L;

    /**
     * 权限键
     */
    private String key;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 权限描述
     */
    private String description;
}