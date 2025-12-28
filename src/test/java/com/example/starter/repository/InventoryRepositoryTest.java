package com.example.starter.repository;

import com.example.starter.entity.Inventory;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InventoryRepository 单元测试
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("InventoryRepository 测试")
public class InventoryRepositoryTest {

    @Inject
    InventoryRepository inventoryRepository;

    @BeforeEach
    @Transactional
    public void setUp() {
        // 清理之前的测试数据
        inventoryRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("查找所有活跃清单")
    @Transactional
    public void testFindAllActive() {
        inventoryRepository.persist(new Inventory("测试清单1", "描述1"));
        inventoryRepository.persist(new Inventory("测试清单2", "描述2"));
        inventoryRepository.persist(new Inventory("测试清单3", "描述3"));

        var inventories = inventoryRepository.findAllActive();

        assertNotNull(inventories);
        assertTrue(inventories.size() >= 3);
    }

    @Test
    @Order(2)
    @DisplayName("检查名称是否存在 - 存在")
    @Transactional
    public void testExistsByNameExists() {
        String name = "exists-test-" + System.currentTimeMillis();
        Inventory inventory = new Inventory(name, "测试清单");
        inventoryRepository.persist(inventory);

        assertTrue(inventoryRepository.existsByName(name));
    }

    @Test
    @Order(3)
    @DisplayName("检查名称是否存在 - 不存在")
    @Transactional
    public void testExistsByNameNotExists() {
        assertFalse(inventoryRepository.existsByName("不存在的清单名称"));
    }

    @Test
    @Order(4)
    @DisplayName("持久化清单")
    @Transactional
    public void testPersist() {
        String name = "persist-test-" + System.currentTimeMillis();
        Inventory inventory = new Inventory(name, "测试清单");

        inventoryRepository.persist(inventory);

        assertNotNull(inventory.getId());
        assertTrue(inventoryRepository.existsByName(name));
    }

    @Test
    @Order(5)
    @DisplayName("查找清单 - 按ID")
    @Transactional
    public void testFindById() {
        String name = "findbyid-test-" + System.currentTimeMillis();
        Inventory inventory = new Inventory(name, "测试清单");
        inventoryRepository.persist(inventory);
        Long id = inventory.getId();

        Inventory found = inventoryRepository.findById(id);

        assertNotNull(found);
        assertEquals(name, found.getName());
    }

    @Test
    @Order(6)
    @DisplayName("列出所有清单")
    @Transactional
    public void testListAll() {
        inventoryRepository.persist(new Inventory("清单1", "描述1"));
        inventoryRepository.persist(new Inventory("清单2", "描述2"));
        inventoryRepository.persist(new Inventory("清单3", "描述3"));

        var inventories = inventoryRepository.listAll();

        assertTrue(inventories.size() >= 3);
    }

    @Test
    @Order(7)
    @DisplayName("删除清单")
    @Transactional
    public void testDelete() {
        String name = "delete-test-" + System.currentTimeMillis();
        Inventory inventory = new Inventory(name, "测试清单");
        inventoryRepository.persist(inventory);
        Long id = inventory.getId();

        inventoryRepository.delete(inventory);

        Inventory deleted = inventoryRepository.findById(id);
        assertNull(deleted);
    }

    @Test
    @Order(8)
    @DisplayName("删除所有清单")
    @Transactional
    public void testDeleteAll() {
        inventoryRepository.persist(new Inventory("清单1", "描述1"));
        inventoryRepository.persist(new Inventory("清单2", "描述2"));

        inventoryRepository.deleteAll();

        var inventories = inventoryRepository.listAll();
        assertEquals(0, inventories.size());
    }
}
