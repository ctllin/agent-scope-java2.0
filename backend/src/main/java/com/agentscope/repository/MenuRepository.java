package com.agentscope.repository;

import com.agentscope.model.entity.Menu;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 菜单数据访问层
 */
@Repository
public interface MenuRepository extends MongoRepository<Menu, String> {

    /**
     * 根据父菜单ID查找子菜单列表
     * @param parentId 父菜单ID
     * @return 子菜单列表
     */
    List<Menu> findByParentIdOrderBySort(String parentId);

    /**
     * 根据父菜单ID查找子菜单列表（包含排序）
     * @param parentId 父菜单ID
     * @return 子菜单列表
     */
    List<Menu> findByParentId(String parentId);

    /**
     * 查找所有顶级菜单
     * @return 顶级菜单列表
     */
    List<Menu> findByParentIdIsNullOrderBySort();
}
