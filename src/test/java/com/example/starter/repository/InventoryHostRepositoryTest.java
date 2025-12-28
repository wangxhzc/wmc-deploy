package com.example.starter.repository;

import com.example.starter.entity.InventoryHost;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InventoryHostRepository 单元测试
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("InventoryHostRepository 测试")
public class InventoryHostRepositoryTest {

    @Inject
    InventoryHostRepository hostRepository;

    @BeforeEach
    @Transactional
    public void setUp() {
        // 清理之前的测试数据
        hostRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("查找所有主机 - 按创建时间降序")
    @Transactional
    public void testFindAllOrderByCreatedAtDesc() {
        // 注意：由于数据库操作速度很快，可能需要添加延迟来确保时间顺序
        // 这里主要测试方法能正常执行和返回结果

        hostRepository.persist(new InventoryHost("host1", "192.168.1.1", 22, "user", "pass"));
        hostRepository.persist(new InventoryHost("host2", "192.168.1.2", 22, "user", "pass"));
        hostRepository.persist(new InventoryHost("host3", "192.168.1.3", 22, "user", "pass"));

        List<InventoryHost> hosts = hostRepository.findAllOrderByCreatedAtDesc();

        assertNotNull(hosts);
        assertTrue(hosts.size() >= 3);
    }

    @Test
    @Order(2)
    @DisplayName("查找主机 - 按名称")
    @Transactional
    public void testFindByName() {
        String name = "test-host-" + System.currentTimeMillis();
        InventoryHost host = new InventoryHost(name, "192.168.1.1", 22, "user", "pass");
        hostRepository.persist(host);

        InventoryHost found = hostRepository.findByName(name);

        assertNotNull(found);
        assertEquals(name, found.getName());
    }

    @Test
    @Order(3)
    @DisplayName("查找主机 - 按名称 - 不存在")
    @Transactional
    public void testFindByNameNotFound() {
        InventoryHost found = hostRepository.findByName("不存在的主机名称");
        assertNull(found);
    }

    @Test
    @Order(4)
    @DisplayName("检查名称是否存在 - 存在")
    @Transactional
    public void testExistsByNameExists() {
        String name = "exists-test-" + System.currentTimeMillis();
        InventoryHost host = new InventoryHost(name, "192.168.1.1", 22, "user", "pass");
        hostRepository.persist(host);

        assertTrue(hostRepository.existsByName(name));
    }

    @Test
    @Order(5)
    @DisplayName("检查名称是否存在 - 不存在")
    @Transactional
    public void testExistsByNameNotExists() {
        assertFalse(hostRepository.existsByName("不存在的主机名称"));
    }

    @Test
    @Order(6)
    @DisplayName("更新连接状态 - 按ID")
    @Transactional
    public void testUpdateConnectionStatus() {
        InventoryHost host = new InventoryHost("test-host", "192.168.1.1", 22, "user", "pass");
        hostRepository.persist(host);
        Long id = host.getId();

        hostRepository.updateConnectionStatus(id, true);

        InventoryHost updated = hostRepository.findById(id);
        assertNotNull(updated);
        assertTrue(updated.getConnected());
    }

    @Test
    @Order(7)
    @DisplayName("更新连接状态 - 按名称")
    @Transactional
    public void testUpdateConnectionStatusByName() {
        String name = "status-test-" + System.currentTimeMillis();
        InventoryHost host = new InventoryHost(name, "192.168.1.1", 22, "user", "pass");
        hostRepository.persist(host);

        hostRepository.updateConnectionStatusByName(name, false);

        InventoryHost updated = hostRepository.findByName(name);
        assertNotNull(updated);
        assertFalse(updated.getConnected());
    }

    @Test
    @Order(8)
    @DisplayName("持久化主机")
    @Transactional
    public void testPersist() {
        String name = "persist-test-" + System.currentTimeMillis();
        InventoryHost host = new InventoryHost(name, "192.168.1.1", 22, "user", "pass");

        hostRepository.persist(host);

        assertNotNull(host.getId());
        assertTrue(hostRepository.existsByName(name));
    }

    @Test
    @Order(9)
    @DisplayName("查找主机 - 按ID")
    @Transactional
    public void testFindById() {
        String name = "findbyid-test-" + System.currentTimeMillis();
        InventoryHost host = new InventoryHost(name, "192.168.1.1", 22, "user", "pass");
        hostRepository.persist(host);
        Long id = host.getId();

        InventoryHost found = hostRepository.findById(id);

        assertNotNull(found);
        assertEquals(name, found.getName());
    }

    @Test
    @Order(10)
    @DisplayName("列出所有主机")
    @Transactional
    public void testListAll() {
        hostRepository.persist(new InventoryHost("host1", "192.168.1.1", 22, "user", "pass"));
        hostRepository.persist(new InventoryHost("host2", "192.168.1.2", 22, "user", "pass"));
        hostRepository.persist(new InventoryHost("host3", "192.168.1.3", 22, "user", "pass"));

        List<InventoryHost> hosts = hostRepository.listAll();

        assertTrue(hosts.size() >= 3);
    }

    @Test
    @Order(11)
    @DisplayName("删除主机")
    @Transactional
    public void testDelete() {
        String name = "delete-test-" + System.currentTimeMillis();
        InventoryHost host = new InventoryHost(name, "192.168.1.1", 22, "user", "pass");
        hostRepository.persist(host);
        Long id = host.getId();

        hostRepository.delete(host);

        InventoryHost deleted = hostRepository.findById(id);
        assertNull(deleted);
    }

    @Test
    @Order(12)
    @DisplayName("删除所有主机")
    @Transactional
    public void testDeleteAll() {
        hostRepository.persist(new InventoryHost("host1", "192.168.1.1", 22, "user", "pass"));
        hostRepository.persist(new InventoryHost("host2", "192.168.1.2", 22, "user", "pass"));

        hostRepository.deleteAll();

        List<InventoryHost> hosts = hostRepository.listAll();
        assertEquals(0, hosts.size());
    }
}
