package com.example.starter.service.inventory;

import com.example.starter.entity.Inventory;
import com.example.starter.entity.InventoryGroup;
import com.example.starter.entity.InventoryHost;
import com.example.starter.entity.InventoryVariable;
import com.example.starter.exception.DuplicateResourceException;
import com.example.starter.exception.ResourceNotFoundException;
import com.example.starter.repository.InventoryHostRepository;
import com.example.starter.repository.InventoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * 清单服务类，处理所有清单相关的业务逻辑
 */
@ApplicationScoped
public class InventoryService {

    @Inject
    InventoryRepository inventoryRepository;

    @Inject
    InventoryHostRepository hostRepository;

    @Inject
    EntityManager entityManager;

    /**
     * 获取所有清单
     */
    @Transactional
    public List<Inventory> getAllInventories() {
        return inventoryRepository.listAll();
    }

    /**
     * 根据ID获取清单（包含所有关联数据）
     */
    @Transactional
    public Inventory getInventoryByIdWithAssociations(Long id) {
        Inventory inventory = entityManager.find(Inventory.class, id);

        // 手动初始化集合以确保在事务外也能访问
        if (inventory != null) {
            inventory.getHosts().size();
            inventory.getGroups().size();
            inventory.getVariables().size();
        }

        return inventory;
    }

    /**
     * 根据ID获取清单
     */
    @Transactional
    public Inventory getInventoryById(Long id) {
        return inventoryRepository.findById(id);
    }

    /**
     * 检查清单名称是否存在
     */
    @Transactional
    public boolean existsByName(String name) {
        return inventoryRepository.existsByName(name);
    }

    /**
     * 创建新清单
     */
    @Transactional
    public Inventory createInventory(String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("清单名称不能为空");
        }

        if (existsByName(name.trim())) {
            throw new DuplicateResourceException("清单名称已存在");
        }

        Inventory inventory = new Inventory(name.trim(), description);
        inventoryRepository.persist(inventory);
        return inventory;
    }

    /**
     * 更新清单
     */
    @Transactional
    public void updateInventory(Inventory inventory) {
        if (inventory == null || inventory.getId() == null) {
            throw new IllegalArgumentException("清单不能为空");
        }

        Inventory existing = getInventoryById(inventory.getId());
        if (existing == null) {
            throw new ResourceNotFoundException("清单不存在");
        }

        // 使用 merge 而不是 persist，因为这是更新现有实体
        entityManager.merge(inventory);
    }

    /**
     * 删除清单
     */
    @Transactional
    public void deleteInventory(Inventory inventory) {
        if (inventory == null || inventory.getId() == null) {
            throw new IllegalArgumentException("清单不能为空");
        }

        Inventory existing = getInventoryById(inventory.getId());
        if (existing == null) {
            throw new ResourceNotFoundException("清单不存在");
        }

        inventoryRepository.delete(inventory);
    }

    /**
     * 添加组到清单
     */
    @Transactional
    public InventoryGroup addGroupToInventory(Long inventoryId, String groupName, String description) {
        if (groupName == null || groupName.trim().isEmpty()) {
            throw new IllegalArgumentException("组名称不能为空");
        }

        Inventory inventory = getInventoryById(inventoryId);
        if (inventory == null) {
            throw new ResourceNotFoundException("清单不存在");
        }

        InventoryGroup group = new InventoryGroup(groupName.trim(), description);
        group.setInventory(inventory);
        inventory.getGroups().add(group);
        inventoryRepository.persist(inventory);

        return group;
    }

    /**
     * 从清单删除组
     */
    @Transactional
    public void removeGroupFromInventory(Long inventoryId, InventoryGroup group) {
        Inventory inventory = getInventoryById(inventoryId);
        if (inventory == null) {
            throw new ResourceNotFoundException("清单不存在");
        }

        inventory.getGroups().remove(group);
        inventoryRepository.persist(inventory);
    }

    /**
     * 添加变量到清单
     */
    @Transactional
    public InventoryVariable addVariableToInventory(Long inventoryId, String variableName, String variableValue) {
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new IllegalArgumentException("变量名称不能为空");
        }

        Inventory inventory = getInventoryById(inventoryId);
        if (inventory == null) {
            throw new ResourceNotFoundException("清单不存在");
        }

        InventoryVariable variable = new InventoryVariable(variableName.trim(), variableValue);
        variable.setInventory(inventory);
        inventory.getVariables().add(variable);
        entityManager.persist(variable);

        return variable;
    }

    /**
     * 从清单删除变量
     */
    @Transactional
    public void removeVariableFromInventory(Long inventoryId, InventoryVariable variable) {
        Inventory inventory = getInventoryById(inventoryId);
        if (inventory == null) {
            throw new ResourceNotFoundException("清单不存在");
        }

        inventory.getVariables().remove(variable);
        inventoryRepository.persist(inventory);
    }

    /**
     * 添加主机到清单（支持多对多关系）
     * 主机可以同时属于多个清单
     */
    @Transactional
    public void addHostToInventory(Long hostId, Long inventoryId) {
        InventoryHost host = hostRepository.findById(hostId);
        if (host == null) {
            throw new ResourceNotFoundException("主机不存在");
        }

        Inventory inventory = getInventoryById(inventoryId);
        if (inventory == null) {
            throw new ResourceNotFoundException("清单不存在");
        }

        // 检查主机是否已经在该清单中
        boolean alreadyExists = inventory.getHostInventories().stream()
                .anyMatch(hi -> hi.getHost().getId().equals(hostId));
        if (alreadyExists) {
            throw new DuplicateResourceException("主机已在该清单中");
        }

        // 创建关联关系
        com.example.starter.entity.InventoryHostInventory hostInventory = new com.example.starter.entity.InventoryHostInventory(
                inventory, host);
        inventory.getHostInventories().add(hostInventory);
        inventoryRepository.persist(inventory);
    }

    /**
     * 从清单移除主机（支持多对多关系）
     * 只从指定清单中移除主机，主机可以属于其他清单
     */
    @Transactional
    public void removeHostFromInventory(Long inventoryId, Long hostId) {
        Inventory inventory = getInventoryById(inventoryId);
        if (inventory == null) {
            throw new ResourceNotFoundException("清单不存在");
        }

        // 从清单中移除主机
        inventory.getHostInventories().removeIf(hi -> hi.getHost().getId().equals(hostId));
        inventoryRepository.persist(inventory);
    }

    /**
     * 获取所有可用主机（用于添加到清单或组）
     * 返回所有主机，因为主机可以同时属于多个清单
     */
    @Transactional
    public List<InventoryHost> getAvailableHosts() {
        return hostRepository.findAllOrderByCreatedAtDesc();
    }

    /**
     * 计算组的层级深度（从根组开始计算）
     * 
     * @param group 要计算的组
     * @return 层级深度，根组为1
     */
    @Transactional
    public int calculateGroupDepth(InventoryGroup group) {
        int depth = 1;
        InventoryGroup current = group;
        while (current.getParentGroup() != null) {
            depth++;
            current = current.getParentGroup();
        }
        return depth;
    }

    /**
     * 添加子组到父组（支持嵌套，最多3层）
     * 
     * @param parentGroupId  父组ID
     * @param childGroupName 子组名称
     * @param description    子组描述
     * @return 创建的子组
     */
    @Transactional
    public InventoryGroup addChildGroup(Long parentGroupId, String childGroupName, String description) {
        if (childGroupName == null || childGroupName.trim().isEmpty()) {
            throw new IllegalArgumentException("子组名称不能为空");
        }

        InventoryGroup parentGroup = entityManager.find(InventoryGroup.class, parentGroupId);
        if (parentGroup == null) {
            throw new ResourceNotFoundException("父组不存在");
        }

        // 检查父组层级，确保添加子组后不超过3层
        int parentDepth = calculateGroupDepth(parentGroup);
        if (parentDepth >= 3) {
            throw new IllegalArgumentException("组嵌套层级不能超过3层，当前父组已达到第" + parentDepth + "层");
        }

        // 检查子组名称在当前Inventory中是否唯一
        Inventory existingGroup = parentGroup.getInventory();
        boolean nameExists = existingGroup.getGroups().stream()
                .anyMatch(g -> g.getName().equals(childGroupName.trim()));
        if (nameExists) {
            throw new DuplicateResourceException("当前清单中已存在名为'" + childGroupName + "'的组");
        }

        InventoryGroup childGroup = new InventoryGroup(childGroupName.trim(), description);
        childGroup.setInventory(parentGroup.getInventory());
        childGroup.setParentGroup(parentGroup);
        parentGroup.getChildGroups().add(childGroup);
        entityManager.persist(childGroup);

        return childGroup;
    }

    /**
     * 添加主机到组
     * 
     * @param groupId 组ID
     * @param hostId  主机ID
     */
    @Transactional
    public void addHostToGroup(Long groupId, Long hostId) {
        InventoryGroup group = entityManager.find(InventoryGroup.class, groupId);
        if (group == null) {
            throw new ResourceNotFoundException("组不存在");
        }

        InventoryHost host = hostRepository.findById(hostId);
        if (host == null) {
            throw new ResourceNotFoundException("主机不存在");
        }

        // 检查主机是否已经在这个组中
        boolean alreadyExists = group.getGroupHosts().stream()
                .anyMatch(gh -> gh.getHost().getId().equals(hostId));
        if (alreadyExists) {
            throw new DuplicateResourceException("主机已在该组中");
        }

        com.example.starter.entity.InventoryGroupHost groupHost = new com.example.starter.entity.InventoryGroupHost(
                group, host);
        group.getGroupHosts().add(groupHost);
        entityManager.persist(groupHost);
    }

    /**
     * 从组中移除主机
     * 
     * @param groupId 组ID
     * @param hostId  主机ID
     */
    @Transactional
    public void removeHostFromGroup(Long groupId, Long hostId) {
        InventoryGroup group = entityManager.find(InventoryGroup.class, groupId);
        if (group == null) {
            throw new ResourceNotFoundException("组不存在");
        }

        group.getGroupHosts().removeIf(gh -> gh.getHost().getId().equals(hostId));
        entityManager.persist(group);
    }
}
