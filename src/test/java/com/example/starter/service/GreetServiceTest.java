package com.example.starter.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GreetService 单元测试
 */
@QuarkusTest
@DisplayName("GreetService 测试")
public class GreetServiceTest {

    @Inject
    GreetService greetService;

    @Test
    @DisplayName("问候 - 正常名称")
    public void testGreetWithName() {
        String result = greetService.greet("World");
        assertEquals("Hello World", result);
    }

    @Test
    @DisplayName("问候 - 空字符串")
    public void testGreetWithEmptyString() {
        String result = greetService.greet("");
        assertEquals("Hello anonymous user", result);
    }

    @Test
    @DisplayName("问候 - null")
    public void testGreetWithNull() {
        String result = greetService.greet(null);
        assertEquals("Hello anonymous user", result);
    }

    @Test
    @DisplayName("问候 - 包含空格的名称")
    public void testGreetWithSpaces() {
        String result = greetService.greet("John Doe");
        assertEquals("Hello John Doe", result);
    }

    @Test
    @DisplayName("问候 - 包含特殊字符的名称")
    public void testGreetWithSpecialCharacters() {
        String result = greetService.greet("张三");
        assertEquals("Hello 张三", result);
    }

    @Test
    @DisplayName("问候 - 数字名称")
    public void testGreetWithNumber() {
        String result = greetService.greet("123");
        assertEquals("Hello 123", result);
    }
}
