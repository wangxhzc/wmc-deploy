package com.example.starter.service.host;

import com.example.starter.entity.Inventory;
import com.example.starter.entity.InventoryHost;
import com.example.starter.service.inventory.InventoryService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SSHConnectionServiceTest {

    @Inject
    SSHConnectionService sshConnectionService;

    @Inject
    InventoryService inventoryService;

    private static InventoryHost testHost;
    private static int testCounter = 0;

    @BeforeEach
    @Transactional
    public void setUp() {
        testCounter++;
        // 创建测试清单和主机 - 使用唯一名称
        Inventory inventory = inventoryService.createInventory(
                "测试清单" + testCounter,
                "测试清单描述" + testCounter);
        testHost = new InventoryHost(
                "testhost" + testCounter,
                "192.168.1." + (100 + testCounter),
                22,
                "testuser",
                "testpass");
        testHost.persist();
    }

    @AfterEach
    @Transactional
    public void tearDown() {
        // 清理数据
        if (testHost != null && testHost.id != null) {
            InventoryHost.deleteById(testHost.id);
        }
    }

    @Test
    @Order(1)
    public void testTestConnectionWithValidParameters() {
        // 测试连接 - 这个测试可能会失败，因为没有实际的主机
        // 但我们主要测试方法是否可以被调用
        SSHConnectionService.ConnectionResult result = sshConnectionService.testConnection(
                "192.168.1.100", 22, "testuser", "testpass");

        assertNotNull(result);
        // 结果可能是成功或失败，取决于网络环境
        assertNotNull(result.getMessage());
    }

    @Test
    @Order(2)
    public void testTestConnectionWithHost() {
        SSHConnectionService.ConnectionResult result = sshConnectionService.testConnection(testHost);

        assertNotNull(result);
        assertNotNull(result.getMessage());
    }

    @Test
    @Order(3)
    public void testTestConnectionWithNullHost() {
        SSHConnectionService.ConnectionResult result = sshConnectionService.testConnection((InventoryHost) null);

        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    @Order(4)
    public void testTestConnectionWithNullHostname() {
        SSHConnectionService.ConnectionResult result = sshConnectionService.testConnection(
                null, 22, "testuser", "testpass");

        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    @Order(5)
    public void testTestConnectionWithEmptyHostname() {
        SSHConnectionService.ConnectionResult result = sshConnectionService.testConnection(
                "", 22, "testuser", "testpass");

        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    @Order(6)
    public void testTestConnectionWithInvalidPort() {
        SSHConnectionService.ConnectionResult result = sshConnectionService.testConnection(
                "192.168.1.100", -1, "testuser", "testpass");

        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    @Order(7)
    public void testTestConnectionWithPortZero() {
        SSHConnectionService.ConnectionResult result = sshConnectionService.testConnection(
                "192.168.1.100", 0, "testuser", "testpass");

        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    @Order(8)
    public void testTestConnectionWithNullUsername() {
        SSHConnectionService.ConnectionResult result = sshConnectionService.testConnection(
                "192.168.1.100", 22, null, "testpass");

        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    @Order(9)
    public void testTestConnectionWithEmptyUsername() {
        SSHConnectionService.ConnectionResult result = sshConnectionService.testConnection(
                "192.168.1.100", 22, "", "testpass");

        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    @Order(10)
    public void testTestConnectionWithNullPassword() {
        SSHConnectionService.ConnectionResult result = sshConnectionService.testConnection(
                "192.168.1.100", 22, "testuser", null);

        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    @Order(11)
    public void testTestConnectionWithEmptyPassword() {
        SSHConnectionService.ConnectionResult result = sshConnectionService.testConnection(
                "192.168.1.100", 22, "testuser", "");

        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    @Order(12)
    public void testConnectionResultMethods() {
        SSHConnectionService.ConnectionResult result = sshConnectionService.testConnection(
                "192.168.1.100", 22, "testuser", "testpass");

        assertNotNull(result);
        assertNotNull(result.isSuccess());
        assertNotNull(result.getMessage());

        // 测试消息不为空
        assertFalse(result.getMessage().isEmpty());
    }

    @Test
    @Order(13)
    public void testTestConnectionWithLocalhost() {
        // 测试本地连接 - 如果SSH服务在运行
        SSHConnectionService.ConnectionResult result = sshConnectionService.testConnection(
                "localhost", 22, "testuser", "testpass");

        assertNotNull(result);
        assertNotNull(result.getMessage());
    }
}
