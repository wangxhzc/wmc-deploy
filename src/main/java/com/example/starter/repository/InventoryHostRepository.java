package com.example.starter.repository;

import com.example.starter.entity.InventoryHost;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * InventoryHost数据访问层
 */
@ApplicationScoped
public class InventoryHostRepository implements PanacheRepository<InventoryHost> {

    /**
     * 查询所有主机，按创建时间降序
     */
    public List<InventoryHost> findAllOrderByCreatedAtDesc() {
        return find("ORDER BY createdAt DESC").list();
    }

    /**
     * 根据名称查询主机
     */
    public InventoryHost findByName(String name) {
        return find("name", name).firstResult();
    }

    /**
     * 检查名称是否存在
     */
    public boolean existsByName(String name) {
        return count("name", name) > 0;
    }

    /**
     * 更新主机连接状态
     */
    @Transactional
    public void updateConnectionStatus(Long hostId, Boolean connected) {
        InventoryHost host = findById(hostId);
        if (host != null) {
            host.setConnected(connected);
            host.setLastChecked(java.time.LocalDateTime.now());
            getEntityManager().merge(host);
        }
    }

    /**
     * 根据名称更新主机连接状态
     */
    @Transactional
    public void updateConnectionStatusByName(String name, Boolean connected) {
        InventoryHost host = findByName(name);
        if (host != null) {
            host.setConnected(connected);
            host.setLastChecked(java.time.LocalDateTime.now());
            getEntityManager().merge(host);
        }
    }
}
