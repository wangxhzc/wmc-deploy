package com.example.starter.service;

import com.example.starter.entity.Inventory;
import com.example.starter.entity.Project;
import com.example.starter.entity.Template;
import com.example.starter.exception.DuplicateResourceException;
import com.example.starter.exception.ResourceNotFoundException;
import com.example.starter.repository.ProjectRepository;
import com.example.starter.repository.TemplateRepository;
import com.example.starter.service.inventory.InventoryService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TemplateService 单元测试
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TemplateServiceTest {

    @Inject
    TemplateService templateService;

    @Inject
    ProjectService projectService;

    @Inject
    InventoryService inventoryService;

    @Inject
    TemplateRepository templateRepository;

    @Inject
    ProjectRepository projectRepository;

    private Project testProject;
    private Inventory testInventory;

    @BeforeEach
    @Transactional
    public void setUp() {
        // 清理之前的测试数据
        templateRepository.deleteAll();
        projectRepository.deleteAll();
        inventoryService.getAllInventories().forEach(inventoryService::deleteInventory);

        // 创建测试用的项目和清单
        testProject = projectService.createProject("test-project-" + System.currentTimeMillis(), "测试项目",
                "---\n- name: Test\n  hosts: all");
        testInventory = inventoryService.createInventory("test-inventory-" + System.currentTimeMillis(), "测试清单");
    }

    @Test
    @Order(1)
    @DisplayName("创建模板 - 成功")
    public void testCreateTemplate() {
        String name = "test-template-" + System.currentTimeMillis();

        Template template = templateService.createTemplate(name, "测试模板", testProject.getId(), testInventory.getId());

        assertNotNull(template);
        assertNotNull(template.getId());
        assertEquals(name, template.getName());
        assertEquals("测试模板", template.getDescription());
        assertNotNull(template.getProject());
        assertNotNull(template.getInventory());
        assertEquals(testProject.getId(), template.getProject().getId());
        assertEquals(testInventory.getId(), template.getInventory().getId());
    }

    @Test
    @Order(2)
    @DisplayName("创建模板 - 空名称 - 失败")
    public void testCreateTemplateEmptyName() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> templateService.createTemplate("", "测试", testProject.getId(), testInventory.getId()));

        assertEquals("模板名称不能为空", exception.getMessage());
    }

    @Test
    @Order(3)
    @DisplayName("创建模板 - 重复名称 - 失败")
    public void testCreateTemplateDuplicateName() {
        String name = "duplicate-test-" + System.currentTimeMillis();
        templateService.createTemplate(name, "测试模板1", testProject.getId(), testInventory.getId());

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> templateService.createTemplate(name, "测试模板2", testProject.getId(), testInventory.getId()));

        assertEquals("模板名称已存在", exception.getMessage());
    }

    @Test
    @Order(4)
    @DisplayName("创建模板 - 项目不存在 - 失败")
    public void testCreateTemplateProjectNotFound() {
        String name = "project-not-found-" + System.currentTimeMillis();

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> templateService.createTemplate(name, "测试", 999999L, testInventory.getId()));

        assertEquals("项目不存在", exception.getMessage());
    }

    @Test
    @Order(5)
    @DisplayName("创建模板 - 清单不存在 - 失败")
    public void testCreateTemplateInventoryNotFound() {
        String name = "inventory-not-found-" + System.currentTimeMillis();

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> templateService.createTemplate(name, "测试", testProject.getId(), 999999L));

        assertEquals("清单不存在", exception.getMessage());
    }

    @Test
    @Order(6)
    @DisplayName("根据ID获取模板")
    public void testGetTemplateById() {
        String name = "get-by-id-test-" + System.currentTimeMillis();
        Template created = templateService.createTemplate(name, "测试", testProject.getId(), testInventory.getId());

        Template found = templateService.getTemplateById(created.getId());

        assertNotNull(found);
        assertEquals(created.getId(), found.getId());
        assertEquals(name, found.getName());
    }

    @Test
    @Order(7)
    @DisplayName("根据ID获取模板 - 不存在")
    public void testGetTemplateByIdNotFound() {
        Template found = templateService.getTemplateById(999999L);
        assertNull(found);
    }

    @Test
    @Order(8)
    @DisplayName("获取所有模板")
    public void testGetAllTemplates() {
        String name1 = "all-test-1-" + System.currentTimeMillis();
        String name2 = "all-test-2-" + System.currentTimeMillis();
        templateService.createTemplate(name1, "测试1", testProject.getId(), testInventory.getId());
        templateService.createTemplate(name2, "测试2", testProject.getId(), testInventory.getId());

        List<Template> templates = templateService.getAllTemplates();

        assertNotNull(templates);
        assertTrue(templates.size() >= 2);
        assertTrue(templates.stream().anyMatch(t -> t.getName().equals(name1)));
        assertTrue(templates.stream().anyMatch(t -> t.getName().equals(name2)));
    }

    @Test
    @Order(9)
    @DisplayName("根据项目ID查找模板")
    public void testGetTemplatesByProjectId() {
        // 创建额外的项目
        Project project2 = projectService.createProject("project-2-" + System.currentTimeMillis(), "项目2",
                "---\n- name: Test2");

        // 为项目1创建2个模板
        Template template1 = templateService.createTemplate("template-1-" + System.currentTimeMillis(), "测试1",
                testProject.getId(), testInventory.getId());
        Template template2 = templateService.createTemplate("template-2-" + System.currentTimeMillis(), "测试2",
                testProject.getId(), testInventory.getId());

        // 为项目2创建1个模板
        Template template3 = templateService.createTemplate("template-3-" + System.currentTimeMillis(), "测试3",
                project2.getId(), testInventory.getId());

        // 查询项目1的模板
        List<Template> templates = templateService.getTemplatesByProjectId(testProject.getId());

        assertEquals(2, templates.size());
        assertTrue(templates.stream().anyMatch(t -> t.getName().equals(template1.getName())));
        assertTrue(templates.stream().anyMatch(t -> t.getName().equals(template2.getName())));
        assertFalse(templates.stream().anyMatch(t -> t.getName().equals(template3.getName())));
    }

    @Test
    @Order(10)
    @DisplayName("根据清单ID查找模板")
    public void testGetTemplatesByInventoryId() {
        // 创建额外的清单
        Inventory inventory2 = inventoryService.createInventory("inventory-2-" + System.currentTimeMillis(), "清单2");

        // 为清单1创建2个模板
        Template template1 = templateService.createTemplate("template-1-" + System.currentTimeMillis(), "测试1",
                testProject.getId(), testInventory.getId());
        Template template2 = templateService.createTemplate("template-2-" + System.currentTimeMillis(), "测试2",
                testProject.getId(), testInventory.getId());

        // 为清单2创建1个模板
        Template template3 = templateService.createTemplate("template-3-" + System.currentTimeMillis(), "测试3",
                testProject.getId(), inventory2.getId());

        // 查询清单1的模板
        List<Template> templates = templateService.getTemplatesByInventoryId(testInventory.getId());

        assertEquals(2, templates.size());
        assertTrue(templates.stream().anyMatch(t -> t.getName().equals(template1.getName())));
        assertTrue(templates.stream().anyMatch(t -> t.getName().equals(template2.getName())));
        assertFalse(templates.stream().anyMatch(t -> t.getName().equals(template3.getName())));
    }

    @Test
    @Order(11)
    @DisplayName("更新模板 - 成功")
    public void testUpdateTemplate() {
        String name = "update-test-" + System.currentTimeMillis();
        Template created = templateService.createTemplate(name, "原始描述", testProject.getId(), testInventory.getId());

        created.setDescription("更新后的描述");

        templateService.updateTemplate(created);

        Template updated = templateService.getTemplateById(created.getId());
        assertEquals("更新后的描述", updated.getDescription());
    }

    @Test
    @Order(12)
    @DisplayName("更新模板 - 修改名称 - 成功")
    @Transactional
    public void testUpdateTemplateRename() {
        String oldName = "rename-old-" + System.currentTimeMillis();
        String newName = "rename-new-" + System.currentTimeMillis();
        Template created = templateService.createTemplate(oldName, "测试", testProject.getId(), testInventory.getId());

        created.setName(newName);
        templateService.updateTemplate(created);

        Template updated = templateService.getTemplateById(created.getId());
        assertEquals(newName, updated.getName());
    }

    @Test
    @Order(13)
    @DisplayName("更新模板 - 新名称重复 - 失败")
    public void testUpdateTemplateDuplicateNewName() {
        String name1 = "dup-name-1-" + System.currentTimeMillis();
        String name2 = "dup-name-2-" + System.currentTimeMillis();
        templateService.createTemplate(name1, "测试1", testProject.getId(), testInventory.getId());
        Template template2 = templateService.createTemplate(name2, "测试2", testProject.getId(), testInventory.getId());

        template2.setName(name1);

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> templateService.updateTemplate(template2));

        assertEquals("模板名称已存在", exception.getMessage());
    }

    @Test
    @Order(14)
    @DisplayName("更新模板 - 空名称 - 失败")
    public void testUpdateTemplateEmptyName() {
        String name = "empty-name-update-" + System.currentTimeMillis();
        Template created = templateService.createTemplate(name, "测试", testProject.getId(), testInventory.getId());

        created.setName("");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> templateService.updateTemplate(created));

        assertEquals("模板名称不能为空", exception.getMessage());
    }

    @Test
    @Order(15)
    @DisplayName("更新模板 - 项目不存在 - 失败")
    public void testUpdateTemplateProjectNotFound() {
        String name = "update-project-not-found-" + System.currentTimeMillis();
        Template created = templateService.createTemplate(name, "测试", testProject.getId(), testInventory.getId());

        // 设置不存在的项目
        Project fakeProject = new Project();
        fakeProject.setId(999999L);
        created.setProject(fakeProject);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> templateService.updateTemplate(created));

        assertEquals("项目不存在", exception.getMessage());
    }

    @Test
    @Order(16)
    @DisplayName("更新模板 - 清单不存在 - 失败")
    public void testUpdateTemplateInventoryNotFound() {
        String name = "update-inventory-not-found-" + System.currentTimeMillis();
        Template created = templateService.createTemplate(name, "测试", testProject.getId(), testInventory.getId());

        // 设置不存在的清单
        Inventory fakeInventory = new Inventory();
        fakeInventory.setId(999999L);
        created.setInventory(fakeInventory);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> templateService.updateTemplate(created));

        assertEquals("清单不存在", exception.getMessage());
    }

    @Test
    @Order(17)
    @DisplayName("删除模板 - 成功")
    public void testDeleteTemplate() {
        String name = "delete-test-" + System.currentTimeMillis();
        Template created = templateService.createTemplate(name, "测试", testProject.getId(), testInventory.getId());
        Long id = created.getId();

        templateService.deleteTemplate(id);

        Template deleted = templateService.getTemplateById(id);
        assertNull(deleted);
    }

    @Test
    @Order(18)
    @DisplayName("删除模板 - 不存在的ID - 无异常")
    public void testDeleteTemplateNotFound() {
        // 删除不存在的模板ID，应该静默成功
        assertDoesNotThrow(() -> templateService.deleteTemplate(999999L));
    }

    @Test
    @Order(19)
    @DisplayName("为模板添加变量 - 成功")
    public void testAddVariable() {
        String name = "add-var-test-" + System.currentTimeMillis();
        Template template = templateService.createTemplate(name, "测试", testProject.getId(), testInventory.getId());

        templateService.addVariable(template.getId(), "var1", "value1");
        templateService.addVariable(template.getId(), "var2", "value2");

        Template updated = templateService.getTemplateById(template.getId());
        assertNotNull(updated.getVariables());
        assertEquals(2, updated.getVariables().size());
        assertTrue(updated.getVariables().stream().anyMatch(v -> v.getVariableName().equals("var1")));
        assertTrue(updated.getVariables().stream().anyMatch(v -> v.getVariableName().equals("var2")));
    }

    @Test
    @Order(20)
    @DisplayName("为模板添加变量 - 模板不存在 - 失败")
    public void testAddVariableTemplateNotFound() {
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> templateService.addVariable(999999L, "var1", "value1"));

        assertEquals("模板不存在", exception.getMessage());
    }

    @Test
    @Order(21)
    @DisplayName("从模板删除变量 - 成功")
    @Transactional
    public void testRemoveVariable() {
        String name = "remove-var-test-" + System.currentTimeMillis();
        Template template = templateService.createTemplate(name, "测试", testProject.getId(), testInventory.getId());

        templateService.addVariable(template.getId(), "var1", "value1");
        templateService.addVariable(template.getId(), "var2", "value2");
        templateService.addVariable(template.getId(), "var3", "value3");

        // 删除var2
        templateService.removeVariable(template.getId(), "var2");

        // 重新从数据库获取模板以确保读取最新数据
        Template updated = templateRepository.findById(template.getId());
        assertEquals(2, updated.getVariables().size());
        assertTrue(updated.getVariables().stream().anyMatch(v -> v.getVariableName().equals("var1")));
        assertTrue(updated.getVariables().stream().anyMatch(v -> v.getVariableName().equals("var3")));
        assertFalse(updated.getVariables().stream().anyMatch(v -> v.getVariableName().equals("var2")));
    }

    @Test
    @Order(22)
    @DisplayName("从模板删除变量 - 模板不存在 - 失败")
    public void testRemoveVariableTemplateNotFound() {
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> templateService.removeVariable(999999L, "var1"));

        assertEquals("模板不存在", exception.getMessage());
    }

    @Test
    @Order(23)
    @DisplayName("创建模板 - 同一项目不同清单的多个模板")
    public void testCreateMultipleTemplatesSameProjectDifferentInventories() {
        Inventory inventory2 = inventoryService.createInventory("inventory-3-" + System.currentTimeMillis(), "清单3");

        String name1 = "template-diff-inv-1-" + System.currentTimeMillis();
        String name2 = "template-diff-inv-2-" + System.currentTimeMillis();

        Template template1 = templateService.createTemplate(name1, "测试1", testProject.getId(), testInventory.getId());
        Template template2 = templateService.createTemplate(name2, "测试2", testProject.getId(), inventory2.getId());

        assertNotNull(template1);
        assertNotNull(template2);
        assertEquals(testProject.getId(), template1.getProject().getId());
        assertEquals(testProject.getId(), template2.getProject().getId());
        assertNotEquals(template1.getInventory().getId(), template2.getInventory().getId());
    }

    @Test
    @Order(24)
    @DisplayName("创建模板 - 不同项目同一清单的多个模板")
    public void testCreateMultipleTemplatesDifferentProjectsSameInventory() {
        Project project2 = projectService.createProject("project-3-" + System.currentTimeMillis(), "项目3",
                "---\n- name: Test3");

        String name1 = "template-diff-proj-1-" + System.currentTimeMillis();
        String name2 = "template-diff-proj-2-" + System.currentTimeMillis();

        Template template1 = templateService.createTemplate(name1, "测试1", testProject.getId(), testInventory.getId());
        Template template2 = templateService.createTemplate(name2, "测试2", project2.getId(), testInventory.getId());

        assertNotNull(template1);
        assertNotNull(template2);
        assertEquals(testInventory.getId(), template1.getInventory().getId());
        assertEquals(testInventory.getId(), template2.getInventory().getId());
        assertNotEquals(template1.getProject().getId(), template2.getProject().getId());
    }
}
