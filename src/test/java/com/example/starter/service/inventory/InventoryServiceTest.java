package com.example.starter.service.inventory;

import com.example.starter.entity.*;
import com.example.starter.exception.DuplicateResourceException;
import com.example.starter.repository.InventoryHostRepository;
import com.example.starter.repository.InventoryRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
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

    private static Long testInventoryId;
    private static Long testHostId1;
    private static Long testHostId2;
    private static Long testRootGroupId;
    private static Long testSubGroupId1;
    private static Long testSubGroupId2;
    private static int testRunCounter = 0;
    private static String host1Name = "";
    private static String host2Name = "";

    @BeforeEach
    public void setUp() {
        testRunCounter++;
    }

    @Test
    @Order(1)
    @DisplayName("创建清单")
    public void testCreateInventory() {
        Inventory inventory = inventoryService.createInventory("测试清单" + testRunCounter, "这是一个测试清单");

        assertNotNull(inventory);
        assertNotNull(inventory.getId());
        assertEquals("测试清单" + testRunCounter, inventory.getName());
        assertEquals("这是一个测试清单", inventory.getDescription());

        testInventoryId = inventory.getId();
    }

    @Test
    @Order(2)
    @DisplayName("创建清单失败 - 名称已存在")
    public void testCreateInventoryDuplicateName() {
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> inventoryService.createInventory("测试清单1", "重复名称"));

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
    @jakarta.transaction.Transactional
    public void testCreateHost() {
        // 使用UUID确保主机名唯一
        String uuid = java.util.UUID.randomUUID().toString().substring(0, 8);

        host1Name = "测试主机1-" + uuid;
        host2Name = "测试主机2-" + uuid;

        InventoryHost host1 = new InventoryHost(host1Name, "192.168.1.1", 22, "user", "pass");
        hostRepository.persist(host1);
        testHostId1 = host1.getId();

        InventoryHost host2 = new InventoryHost(host2Name, "192.168.1.2", 22, "user", "pass");
        hostRepository.persist(host2);
        testHostId2 = host2.getId();

        assertNotNull(host1.getId());
        assertNotNull(host2.getId());
        assertEquals(host1Name, host1.getName());
        assertEquals(host2Name, host2.getName());
    }

    @Test
    @Order(5)
    @DisplayName("添加主机到清单 - 多对多关系")
    @jakarta.transaction.Transactional
    public void testAddHostToInventory() {
        inventoryService.addHostToInventory(testHostId1, testInventoryId);
        inventoryService.addHostToInventory(testHostId2, testInventoryId);

        Inventory inventory = inventoryService.getInventoryByIdWithAssociations(testInventoryId);
        List<InventoryHost> hosts = inventory.getHosts();

        assertEquals(2, hosts.size());
        assertTrue(hosts.stream().anyMatch(h -> h.getName().equals(host1Name)));
        assertTrue(hosts.stream().anyMatch(h -> h.getName().equals(host2Name)));
    }

    @Test
    @Order(6)
    @DisplayName("添加主机到清单失败 - 主机已存在")
    @jakarta.transaction.Transactional
    public void testAddHostToInventoryDuplicate() {
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> inventoryService.addHostToInventory(testHostId1, testInventoryId));

        assertEquals("主机已在该清单中", exception.getMessage());
    }

    @Test
    @Order(7)
    @DisplayName("从清单移除主机 - 不影响其他清单")
    @jakarta.transaction.Transactional
    public void testRemoveHostFromInventory() {
        inventoryService.removeHostFromInventory(testInventoryId, testHostId1);

        Inventory inventory = inventoryService.getInventoryByIdWithAssociations(testInventoryId);
        List<InventoryHost> hosts = inventory.getHosts();

        assertEquals(1, hosts.size());
        assertEquals(host2Name, hosts.get(0).getName());
    }

    @Test
    @Order(8)
    @DisplayName("创建根组")
    public void testAddGroupToInventory() {
        InventoryGroup group = inventoryService.addGroupToInventory(
                testInventoryId, "web服务器", "Web应用服务器组");

        assertNotNull(group);
        assertNotNull(group.getId());
        assertEquals("web服务器", group.getName());
        assertEquals(testInventoryId, group.getInventory().getId());
        assertNull(group.getParentGroup());

        testRootGroupId = group.getId();
    }

    @Test
    @Order(9)
    @DisplayName("创建子组 - 第2层")
    public void testAddChildGroupLevel2() {
        InventoryGroup subGroup = inventoryService.addChildGroup(
                testRootGroupId, "前端服务器", "Nginx服务器");

        assertNotNull(subGroup);
        assertNotNull(subGroup.getId());
        assertEquals("前端服务器", subGroup.getName());
        assertEquals(testRootGroupId, subGroup.getParentGroup().getId());

        // 验证层级深度
        int depth = inventoryService.calculateGroupDepth(subGroup);
        assertEquals(2, depth);

        testSubGroupId1 = subGroup.getId();
    }

    @Test
    @Order(10)
    @DisplayName("创建子组 - 第3层")
    public void testAddChildGroupLevel3() {
        InventoryGroup subGroup = inventoryService.addChildGroup(
                testSubGroupId1, "Nginx集群", "负载均衡Nginx");

        assertNotNull(subGroup);
        assertNotNull(subGroup.getId());
        assertEquals("Nginx集群", subGroup.getName());

        // 验证层级深度
        int depth = inventoryService.calculateGroupDepth(subGroup);
        assertEquals(3, depth);

        testSubGroupId2 = subGroup.getId();
    }

    @Test
    @Order(11)
    @DisplayName("创建子组失败 - 超过3层限制")
    public void testAddChildGroupExceedsLimit() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> inventoryService.addChildGroup(testSubGroupId2, "过深", "超过3层"));

        assertTrue(exception.getMessage().contains("组嵌套层级不能超过3层"));
    }

    @Test
    @Order(12)
    @DisplayName("添加主机到组")
    @jakarta.transaction.Transactional
    public void testAddHostToGroup() {
        // 先把主机重新添加到清单
        inventoryService.addHostToInventory(testHostId1, testInventoryId);

        InventoryGroup group = entityManager.find(InventoryGroup.class, testRootGroupId);
        inventoryService.addHostToGroup(testRootGroupId, testHostId1);

        group = entityManager.find(InventoryGroup.class, testRootGroupId);
        assertEquals(1, group.getGroupHosts().size());
        assertEquals(host1Name, group.getGroupHosts().get(0).getHost().getName());
    }

    @Test
    @Order(13)
    @DisplayName("添加主机到组失败 - 主机已存在")
    @jakarta.transaction.Transactional
    public void testAddHostToGroupDuplicate() {
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> inventoryService.addHostToGroup(testRootGroupId, testHostId1));

        assertEquals("主机已在该组中", exception.getMessage());
    }

    @Test
    @Order(14)
    @DisplayName("从组移除主机")
    @jakarta.transaction.Transactional
    public void testRemoveHostFromGroup() {
        inventoryService.removeHostFromGroup(testRootGroupId, testHostId1);

        InventoryGroup group = entityManager.find(InventoryGroup.class, testRootGroupId);
        assertEquals(0, group.getGroupHosts().size());
    }

    @Test
    @Order(15)
    @DisplayName("计算根组层级深度")
    @jakarta.transaction.Transactional
    public void testCalculateRootGroupDepth() {
        InventoryGroup rootGroup = entityManager.find(InventoryGroup.class, testRootGroupId);
        int depth = inventoryService.calculateGroupDepth(rootGroup);
        assertEquals(1, depth);
    }

    @Test
    @Order(16)
    @DisplayName("更新清单信息")
    @jakarta.transaction.Transactional
    public void testUpdateInventory() {
        Inventory inventory = inventoryService.getInventoryById(testInventoryId);
        inventory.setName("更新后的测试清单");
        inventory.setDescription("更新后的描述");

        inventoryService.updateInventory(inventory);

        Inventory updated = inventoryService.getInventoryById(testInventoryId);
        assertEquals("更新后的测试清单", updated.getName());
        assertEquals("更新后的描述", updated.getDescription());
    }

    @Test
    @Order(17)
    @DisplayName("添加变量到清单")
    @jakarta.transaction.Transactional
    public void testAddVariableToInventory() {
        InventoryVariable variable = inventoryService.addVariableToInventory(
                testInventoryId, "ansible_user", "ubuntu");

        assertNotNull(variable);
        assertNotNull(variable.getId());
        assertEquals("ansible_user", variable.getVariableName());
        assertEquals("ubuntu", variable.getVariableValue());
    }

    @Test
    @Order(18)
    @DisplayName("获取所有可用主机")
    @jakarta.transaction.Transactional
    public void testGetAvailableHosts() {
        List<InventoryHost> hosts = inventoryService.getAvailableHosts();

        assertNotNull(hosts);
        assertTrue(hosts.size() >= 2); // 至少有我们创建的两个测试主机
    }

    @Test
    @Order(19)
    @DisplayName("删除清单")
    @jakarta.transaction.Transactional
    public void testDeleteInventory() {
        Inventory inventory = inventoryService.getInventoryById(testInventoryId);
        assertNotNull(inventory);

        inventoryService.deleteInventory(inventory);

        Inventory deleted = inventoryService.getInventoryById(testInventoryId);
        assertNull(deleted);
    }
}
