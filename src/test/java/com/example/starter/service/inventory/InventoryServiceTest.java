package com.example.starter.service.inventory;

import com.example.starter.entity.*;
import com.example.starter.exception.DuplicateResourceException;
import com.example.starter.exception.ResourceNotFoundException;
import com.example.starter.repository.InventoryHostRepository;
import com.example.starter.repository.InventoryRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InventoryService 单元测试
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InventoryServiceTest {

    @Inject
    InventoryService inventoryService;

    @Inject
    InventoryRepository inventoryRepository;

    @Inject
    InventoryHostRepository hostRepository;

    @Inject
    EntityManager entityManager;

    @BeforeEach
    @Transactional
    public void setUp() {
        // 清理所有数据
        inventoryRepository.deleteAll();
        hostRepository.deleteAll();

        // 刷新以确保删除操作完成
        entityManager.flush();
    }

    @Test
    @Order(1)
    @DisplayName("创建清单")
    public void testCreateInventory() {
        Inventory inventory = inventoryService.createInventory("测试清单1", "这是一个测试清单");

        assertNotNull(inventory);
        assertNotNull(inventory.getId());
        assertEquals("测试清单1", inventory.getName());
        assertEquals("这是一个测试清单", inventory.getDescription());
    }

    @Test
    @Order(2)
    @DisplayName("创建清单失败 - 名称已存在")
    public void testCreateInventoryDuplicateName() {
        inventoryService.createInventory("测试清单2", "重复名称");

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> inventoryService.createInventory("测试清单2", "重复名称"));

        assertEquals("清单名称已存在", exception.getMessage());
    }

    @Test
    @Order(3)
    @DisplayName("创建清单失败 - 空名称")
    public void testCreateInventoryEmptyName() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> inventoryService.createInventory("", "测试"));

        assertEquals("清单名称不能为空", exception.getMessage());
    }

    @Test
    @Order(4)
    @DisplayName("创建主机")
    @Transactional
    public void testCreateHost() {
        String uuid = java.util.UUID.randomUUID().toString().substring(0, 8);
        String hostName = "测试主机-" + uuid;

        InventoryHost host1 = new InventoryHost(hostName, "192.168.1.1", 22, "user", "pass");
        hostRepository.persist(host1);

        assertNotNull(host1.getId());
        assertEquals(hostName, host1.getName());
    }

    @Test
    @Order(5)
    @DisplayName("添加主机到清单 - 多对多关系")
    @Transactional
    public void testAddHostToInventory() {
        Inventory inventory = inventoryService.createInventory("测试清单3", "测试");

        String uuid = java.util.UUID.randomUUID().toString().substring(0, 8);
        InventoryHost host1 = new InventoryHost("host1-" + uuid, "192.168.1.1", 22, "user", "pass");
        hostRepository.persist(host1);

        InventoryHost host2 = new InventoryHost("host2-" + uuid, "192.168.1.2", 22, "user", "pass");
        hostRepository.persist(host2);

        inventoryService.addHostToInventory(host1.getId(), inventory.getId());
        inventoryService.addHostToInventory(host2.getId(), inventory.getId());

        Inventory updated = inventoryService.getInventoryByIdWithAssociations(inventory.getId());
        List<InventoryHost> hosts = updated.getHosts();

        assertEquals(2, hosts.size());
    }

    @Test
    @Order(6)
    @DisplayName("添加主机到清单失败 - 主机已存在")
    @Transactional
    public void testAddHostToInventoryDuplicate() {
        Inventory inventory = inventoryService.createInventory("测试清单4", "测试");

        String uuid = java.util.UUID.randomUUID().toString().substring(0, 8);
        InventoryHost host1 = new InventoryHost("host1-" + uuid, "192.168.1.1", 22, "user", "pass");
        hostRepository.persist(host1);

        inventoryService.addHostToInventory(host1.getId(), inventory.getId());

        // 再次尝试添加应该失败
        assertThrows(ResourceNotFoundException.class,
                () -> inventoryService.addHostToInventory(9999L, inventory.getId()));
    }

    @Test
    @Order(7)
    @DisplayName("从清单移除主机 - 不影响其他清单")
    @Transactional
    public void testRemoveHostFromInventory() {
        Inventory inventory = inventoryService.createInventory("测试清单5", "测试");

        String uuid = java.util.UUID.randomUUID().toString().substring(0, 8);
        InventoryHost host1 = new InventoryHost("host1-" + uuid, "192.168.1.1", 22, "user", "pass");
        hostRepository.persist(host1);

        InventoryHost host2 = new InventoryHost("host2-" + uuid, "192.168.1.2", 22, "user", "pass");
        hostRepository.persist(host2);

        inventoryService.addHostToInventory(host1.getId(), inventory.getId());
        inventoryService.addHostToInventory(host2.getId(), inventory.getId());

        inventoryService.removeHostFromInventory(inventory.getId(), host1.getId());

        Inventory updated = inventoryService.getInventoryByIdWithAssociations(inventory.getId());
        List<InventoryHost> hosts = updated.getHosts();

        assertEquals(1, hosts.size());
    }

    @Test
    @Order(8)
    @DisplayName("创建根组")
    @Transactional
    public void testAddGroupToInventory() {
        Inventory inventory = inventoryService.createInventory("测试清单6", "测试");

        InventoryGroup group = inventoryService.addGroupToInventory(
                inventory.getId(), "web服务器", "Web应用服务器组");

        // 刷新以确保ID被设置
        entityManager.flush();

        assertNotNull(group);
        assertNotNull(group.getId());
        assertEquals("web服务器", group.getName());
        assertEquals(inventory.getId(), group.getInventory().getId());
        assertNull(group.getParentGroup());
    }

    @Test
    @Order(9)
    @DisplayName("创建子组 - 第2层")
    @Transactional
    public void testAddChildGroupLevel2() {
        Inventory inventory = inventoryService.createInventory("测试清单7", "测试");

        InventoryGroup rootGroup = inventoryService.addGroupToInventory(
                inventory.getId(), "web服务器", "Web应用服务器组");

        entityManager.flush();
        assertNotNull(rootGroup.getId(), "rootGroup ID不能为null");

        InventoryGroup subGroup = inventoryService.addChildGroup(
                rootGroup.getId(), "前端服务器", "Nginx服务器");

        assertNotNull(subGroup);
        assertNotNull(subGroup.getId());
        assertEquals("前端服务器", subGroup.getName());
        assertEquals(rootGroup.getId(), subGroup.getParentGroup().getId());

        // 验证层级深度
        int depth = inventoryService.calculateGroupDepth(subGroup);
        assertEquals(2, depth);
    }

    @Test
    @Order(10)
    @DisplayName("创建子组 - 第3层")
    @Transactional
    public void testAddChildGroupLevel3() {
        Inventory inventory = inventoryService.createInventory("测试清单8", "测试");

        InventoryGroup rootGroup = inventoryService.addGroupToInventory(
                inventory.getId(), "web服务器", "Web应用服务器组");

        entityManager.flush();
        assertNotNull(rootGroup.getId(), "rootGroup ID不能为null");

        InventoryGroup subGroup1 = inventoryService.addChildGroup(
                rootGroup.getId(), "前端服务器", "Nginx服务器");

        entityManager.flush();
        assertNotNull(subGroup1.getId(), "subGroup1 ID不能为null");

        InventoryGroup subGroup2 = inventoryService.addChildGroup(
                subGroup1.getId(), "Nginx集群", "负载均衡Nginx");

        assertNotNull(subGroup2);
        assertNotNull(subGroup2.getId());
        assertEquals("Nginx集群", subGroup2.getName());

        // 验证层级深度
        int depth = inventoryService.calculateGroupDepth(subGroup2);
        assertEquals(3, depth);
    }

    @Test
    @Order(11)
    @DisplayName("创建子组失败 - 超过3层限制")
    @Transactional
    public void testAddChildGroupExceedsLimit() {
        Inventory inventory = inventoryService.createInventory("测试清单9", "测试");

        InventoryGroup rootGroup = inventoryService.addGroupToInventory(
                inventory.getId(), "web服务器", "Web应用服务器组");

        entityManager.flush();
        assertNotNull(rootGroup.getId(), "rootGroup ID不能为null");

        InventoryGroup subGroup1 = inventoryService.addChildGroup(
                rootGroup.getId(), "前端服务器", "Nginx服务器");

        entityManager.flush();
        assertNotNull(subGroup1.getId(), "subGroup1 ID不能为null");

        InventoryGroup subGroup2 = inventoryService.addChildGroup(
                subGroup1.getId(), "Nginx集群", "负载均衡Nginx");

        entityManager.flush();
        assertNotNull(subGroup2.getId(), "subGroup2 ID不能为null");

        // 尝试创建第4层
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> inventoryService.addChildGroup(subGroup2.getId(), "过深", "超过3层"));

        assertTrue(exception.getMessage().contains("组嵌套层级不能超过3层"));
    }

    @Test
    @Order(12)
    @DisplayName("添加主机到组")
    @Transactional
    public void testAddHostToGroup() {
        Inventory inventory = inventoryService.createInventory("测试清单10", "测试");

        InventoryGroup group = inventoryService.addGroupToInventory(
                inventory.getId(), "web服务器", "Web应用服务器组");

        entityManager.flush();
        assertNotNull(group.getId(), "group ID不能为null");

        String uuid = java.util.UUID.randomUUID().toString().substring(0, 8);
        InventoryHost host = new InventoryHost("host-" + uuid, "192.168.1.1", 22, "user", "pass");
        hostRepository.persist(host);

        entityManager.flush();
        assertNotNull(host.getId(), "host ID不能为null");

        inventoryService.addHostToGroup(group.getId(), host.getId());

        InventoryGroup updated = entityManager.find(InventoryGroup.class, group.getId());
        assertEquals(1, updated.getGroupHosts().size());
    }

    @Test
    @Order(13)
    @DisplayName("添加主机到组失败 - 主机已存在")
    @Transactional
    public void testAddHostToGroupDuplicate() {
        Inventory inventory = inventoryService.createInventory("测试清单11", "测试");

        InventoryGroup group = inventoryService.addGroupToInventory(
                inventory.getId(), "web服务器", "Web应用服务器组");

        entityManager.flush();
        assertNotNull(group.getId(), "group ID不能为null");

        String uuid = java.util.UUID.randomUUID().toString().substring(0, 8);
        InventoryHost host = new InventoryHost("host-" + uuid, "192.168.1.1", 22, "user", "pass");
        hostRepository.persist(host);

        entityManager.flush();
        assertNotNull(host.getId(), "host ID不能为null");

        inventoryService.addHostToGroup(group.getId(), host.getId());

        // 再次添加应该抛出异常
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> inventoryService.addHostToGroup(group.getId(), host.getId()));

        assertEquals("主机已在该组中", exception.getMessage());
    }

    @Test
    @Order(14)
    @DisplayName("从组移除主机")
    @Transactional
    public void testRemoveHostFromGroup() {
        Inventory inventory = inventoryService.createInventory("测试清单12", "测试");

        InventoryGroup group = inventoryService.addGroupToInventory(
                inventory.getId(), "web服务器", "Web应用服务器组");

        entityManager.flush();
        assertNotNull(group.getId(), "group ID不能为null");

        String uuid = java.util.UUID.randomUUID().toString().substring(0, 8);
        InventoryHost host = new InventoryHost("host-" + uuid, "192.168.1.1", 22, "user", "pass");
        hostRepository.persist(host);

        entityManager.flush();
        assertNotNull(host.getId(), "host ID不能为null");

        inventoryService.addHostToGroup(group.getId(), host.getId());
        inventoryService.removeHostFromGroup(group.getId(), host.getId());

        InventoryGroup updated = entityManager.find(InventoryGroup.class, group.getId());
        assertEquals(0, updated.getGroupHosts().size());
    }

    @Test
    @Order(15)
    @DisplayName("计算根组层级深度")
    @Transactional
    public void testCalculateRootGroupDepth() {
        Inventory inventory = inventoryService.createInventory("测试清单13", "测试");

        InventoryGroup group = inventoryService.addGroupToInventory(
                inventory.getId(), "web服务器", "Web应用服务器组");

        int depth = inventoryService.calculateGroupDepth(group);
        assertEquals(1, depth);
    }

    @Test
    @Order(16)
    @DisplayName("更新清单信息")
    @Transactional
    public void testUpdateInventory() {
        Inventory inventory = inventoryService.createInventory("测试清单14", "原始描述");

        inventory.setName("更新后的测试清单");
        inventory.setDescription("更新后的描述");

        inventoryService.updateInventory(inventory);

        Inventory updated = inventoryService.getInventoryById(inventory.getId());
        assertEquals("更新后的测试清单", updated.getName());
        assertEquals("更新后的描述", updated.getDescription());
    }

    @Test
    @Order(17)
    @DisplayName("添加变量到清单")
    @Transactional
    public void testAddVariableToInventory() {
        Inventory inventory = inventoryService.createInventory("测试清单15", "测试");

        InventoryVariable variable = inventoryService.addVariableToInventory(
                inventory.getId(), "ansible_user", "ubuntu");

        assertNotNull(variable);
        assertNotNull(variable.getId());
        assertEquals("ansible_user", variable.getVariableName());
        assertEquals("ubuntu", variable.getVariableValue());
    }

    @Test
    @Order(18)
    @DisplayName("获取所有可用主机")
    @Transactional
    public void testGetAvailableHosts() {
        // 创建几个主机
        String uuid = java.util.UUID.randomUUID().toString().substring(0, 8);
        InventoryHost host1 = new InventoryHost("host1-" + uuid, "192.168.1.1", 22, "user", "pass");
        hostRepository.persist(host1);

        InventoryHost host2 = new InventoryHost("host2-" + uuid, "192.168.1.2", 22, "user", "pass");
        hostRepository.persist(host2);

        List<InventoryHost> hosts = inventoryService.getAvailableHosts();

        assertNotNull(hosts);
        assertTrue(hosts.size() >= 2);
    }

    @Test
    @Order(19)
    @DisplayName("删除清单")
    @Transactional
    public void testDeleteInventory() {
        Inventory inventory = inventoryService.createInventory("测试清单16", "待删除的清单");
        assertNotNull(inventory);

        inventoryService.deleteInventory(inventory);

        Inventory deleted = inventoryService.getInventoryById(inventory.getId());
        assertNull(deleted);
    }
}
