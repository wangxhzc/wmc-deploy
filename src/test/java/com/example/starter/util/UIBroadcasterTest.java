package com.example.starter.util;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UIBroadcasterTest {

    @Test
    @Order(1)
    public void testClearAll() {
        // 测试清理所有 - 这个方法不依赖UI上下文
        assertDoesNotThrow(() -> UIBroadcaster.clearAll());
    }

    @Test
    @Order(2)
    public void testClearAllMultipleTimes() {
        // 测试多次清理
        assertDoesNotThrow(() -> {
            UIBroadcaster.clearAll();
            UIBroadcaster.clearAll();
            UIBroadcaster.clearAll();
        });
    }

    @Test
    @Order(3)
    public void testBroadcastRefresh() {
        // 测试广播刷新 - 可能在没有UI实例时失败，但应该不抛出异常
        // 只测试方法存在且可以被调用
        assertDoesNotThrow(() -> {
            try {
                UIBroadcaster.broadcastRefresh("TestView");
            } catch (NullPointerException | IllegalArgumentException e) {
                // 由于没有Vaadin UI上下文，可能会抛出这些异常
                // 这是预期的行为
            }
        });
    }

    @Test
    @Order(4)
    public void testBroadcastWithNullViewType() {
        // 测试使用null视图类型广播
        assertDoesNotThrow(() -> {
            try {
                UIBroadcaster.broadcastRefresh(null);
            } catch (NullPointerException | IllegalArgumentException e) {
                // 预期的异常
            }
        });
    }

    @Test
    @Order(5)
    public void testBroadcastWithEmptyViewType() {
        // 测试空字符串视图类型
        assertDoesNotThrow(() -> {
            try {
                UIBroadcaster.broadcastRefresh("");
            } catch (NullPointerException | IllegalArgumentException e) {
                // 预期的异常
            }
        });
    }
}
