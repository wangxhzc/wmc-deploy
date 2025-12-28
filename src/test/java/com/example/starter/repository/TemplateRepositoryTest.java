package com.example.starter.repository;

import com.example.starter.entity.Inventory;
import com.example.starter.entity.Project;
import com.example.starter.entity.Template;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TemplateRepository 单元测试
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("TemplateRepository 测试")
public class TemplateRepositoryTest {

    @Inject
    TemplateRepository templateRepository;

    @Inject
    ProjectRepository projectRepository;

    @Inject
    InventoryRepository inventoryRepository;

    private Project testProject;
    private Inventory testInventory;

    @BeforeEach
    @Transactional
    public void setUp() {
        // 清理之前的测试数据
        templateRepository.deleteAll();
        projectRepository.deleteAll();
        inventoryRepository.deleteAll();

        // 创建测试用的项目和清单
        testProject = new Project("test-project-" + System.currentTimeMillis(), "测试项目", "yaml内容");
        projectRepository.persist(testProject);

        testInventory = new Inventory("test-inventory-" + System.currentTimeMillis(), "测试清单");
        inventoryRepository.persist(testInventory);
    }

    @Test
    @Order(1)
    @DisplayName("查找模板 - 按名称")
    @Transactional
    public void testFindByName() {
        String name = "test-template-" + System.currentTimeMillis();
        Template template = new Template(name, "测试模板", testProject, testInventory);
        templateRepository.persist(template);

        Optional<Template> found = templateRepository.findByName(name);

        assertTrue(found.isPresent());
        assertEquals(name, found.get().getName());
    }

    @Test
    @Order(2)
    @DisplayName("查找模板 - 按名称 - 不存在")
    @Transactional
    public void testFindByNameNotFound() {
        Optional<Template> found = templateRepository.findByName("不存在的模板名称");
        assertFalse(found.isPresent());
    }

    @Test
    @Order(3)
    @DisplayName("检查名称是否存在 - 存在")
    @Transactional
    public void testExistsByNameExists() {
        String name = "exists-test-" + System.currentTimeMillis();
        Template template = new Template(name, "测试模板", testProject, testInventory);
        templateRepository.persist(template);

        assertTrue(templateRepository.existsByName(name));
    }

    @Test
    @Order(4)
    @DisplayName("检查名称是否存在 - 不存在")
    @Transactional
    public void testExistsByNameNotExists() {
        assertFalse(templateRepository.existsByName("不存在的模板名称"));
    }

    @Test
    @Order(5)
    @DisplayName("查找模板 - 按名称前缀")
    @Transactional
    public void testFindByNameStartingWith() {
        String name1 = "prefix-test-1-" + System.currentTimeMillis();
        String name2 = "prefix-test-2-" + System.currentTimeMillis();
        String name3 = "other-name-" + System.currentTimeMillis();

        templateRepository.persist(new Template(name1, "测试1", testProject, testInventory));
        templateRepository.persist(new Template(name2, "测试2", testProject, testInventory));
        templateRepository.persist(new Template(name3, "测试3", testProject, testInventory));

        List<Template> templates = templateRepository.findByNameStartingWith("prefix-test-");

        assertEquals(2, templates.size());
        assertTrue(templates.stream().anyMatch(t -> t.getName().equals(name1)));
        assertTrue(templates.stream().anyMatch(t -> t.getName().equals(name2)));
        assertFalse(templates.stream().anyMatch(t -> t.getName().equals(name3)));
    }

    @Test
    @Order(6)
    @DisplayName("检查名称是否存在 - 排除指定ID")
    @Transactional
    public void testExistsByNameExcludingId() {
        String name = "exclude-test-" + System.currentTimeMillis();
        Template template1 = new Template(name, "测试1", testProject, testInventory);
        templateRepository.persist(template1);
        Long id1 = template1.getId();

        Template template2 = new Template("other-name", "测试2", testProject, testInventory);
        templateRepository.persist(template2);

        // 同一名称，排除自己的ID - 应该返回false，因为没有其他同名的模板
        assertFalse(templateRepository.existsByNameExcludingId(name, id1));

        // 不同名称 - 应该返回false
        assertFalse(templateRepository.existsByNameExcludingId("different-name", id1));

        // 测试存在同名称的不同模板
        String duplicateName = "duplicate-test-" + System.currentTimeMillis();
        Template template3 = new Template(duplicateName, "测试3", testProject, testInventory);
        templateRepository.persist(template3);
        Long id3 = template3.getId();

        Template template4 = new Template(duplicateName, "测试4", testProject, testInventory);
        templateRepository.persist(template4);

        // 排除id3，应该能找到id4
        assertTrue(templateRepository.existsByNameExcludingId(duplicateName, id3));

        // 排除不存在的ID，应该能找到
        assertTrue(templateRepository.existsByNameExcludingId(duplicateName, 9999L));
    }

    @Test
    @Order(7)
    @DisplayName("查找模板 - 按项目ID")
    @Transactional
    public void testFindByProjectId() {
        // 创建额外的项目
        Project project2 = new Project("project-2-" + System.currentTimeMillis(), "项目2", "yaml");
        projectRepository.persist(project2);

        // 为项目1创建2个模板
        Template template1 = new Template("template-1-" + System.currentTimeMillis(), "测试1", testProject,
                testInventory);
        Template template2 = new Template("template-2-" + System.currentTimeMillis(), "测试2", testProject,
                testInventory);
        templateRepository.persist(template1);
        templateRepository.persist(template2);

        // 为项目2创建1个模板
        Template template3 = new Template("template-3-" + System.currentTimeMillis(), "测试3", project2, testInventory);
        templateRepository.persist(template3);

        // 查询项目1的模板
        List<Template> templates = templateRepository.findByProjectId(testProject.getId());

        assertEquals(2, templates.size());
        assertTrue(templates.stream().anyMatch(t -> t.getName().equals(template1.getName())));
        assertTrue(templates.stream().anyMatch(t -> t.getName().equals(template2.getName())));
        assertFalse(templates.stream().anyMatch(t -> t.getName().equals(template3.getName())));
    }

    @Test
    @Order(8)
    @DisplayName("查找模板 - 按清单ID")
    @Transactional
    public void testFindByInventoryId() {
        // 创建额外的清单
        Inventory inventory2 = new Inventory("inventory-2-" + System.currentTimeMillis(), "清单2");
        inventoryRepository.persist(inventory2);

        // 为清单1创建2个模板
        Template template1 = new Template("template-1-" + System.currentTimeMillis(), "测试1", testProject,
                testInventory);
        Template template2 = new Template("template-2-" + System.currentTimeMillis(), "测试2", testProject,
                testInventory);
        templateRepository.persist(template1);
        templateRepository.persist(template2);

        // 为清单2创建1个模板
        Template template3 = new Template("template-3-" + System.currentTimeMillis(), "测试3", testProject, inventory2);
        templateRepository.persist(template3);

        // 查询清单1的模板
        List<Template> templates = templateRepository.findByInventoryId(testInventory.getId());

        assertEquals(2, templates.size());
        assertTrue(templates.stream().anyMatch(t -> t.getName().equals(template1.getName())));
        assertTrue(templates.stream().anyMatch(t -> t.getName().equals(template2.getName())));
        assertFalse(templates.stream().anyMatch(t -> t.getName().equals(template3.getName())));
    }

    @Test
    @Order(9)
    @DisplayName("持久化模板")
    @Transactional
    public void testPersist() {
        String name = "persist-test-" + System.currentTimeMillis();
        Template template = new Template(name, "测试模板", testProject, testInventory);

        templateRepository.persist(template);

        assertNotNull(template.getId());

        Optional<Template> found = templateRepository.findByName(name);
        assertTrue(found.isPresent());
    }

    @Test
    @Order(10)
    @DisplayName("查找模板 - 按ID")
    @Transactional
    public void testFindById() {
        String name = "findbyid-test-" + System.currentTimeMillis();
        Template template = new Template(name, "测试模板", testProject, testInventory);
        templateRepository.persist(template);
        Long id = template.getId();

        Template found = templateRepository.findById(id);

        assertNotNull(found);
        assertEquals(name, found.getName());
    }

    @Test
    @Order(11)
    @DisplayName("列出所有模板")
    @Transactional
    public void testListAll() {
        templateRepository.persist(new Template("template-1", "测试1", testProject, testInventory));
        templateRepository.persist(new Template("template-2", "测试2", testProject, testInventory));
        templateRepository.persist(new Template("template-3", "测试3", testProject, testInventory));

        List<Template> templates = templateRepository.listAll();

        assertTrue(templates.size() >= 3);
    }

    @Test
    @Order(12)
    @DisplayName("删除模板")
    @Transactional
    public void testDelete() {
        String name = "delete-test-" + System.currentTimeMillis();
        Template template = new Template(name, "测试模板", testProject, testInventory);
        templateRepository.persist(template);
        Long id = template.getId();

        templateRepository.delete(template);

        Template deleted = templateRepository.findById(id);
        assertNull(deleted);
    }

    @Test
    @Order(13)
    @DisplayName("删除所有模板")
    @Transactional
    public void testDeleteAll() {
        templateRepository.persist(new Template("template-1", "测试1", testProject, testInventory));
        templateRepository.persist(new Template("template-2", "测试2", testProject, testInventory));

        templateRepository.deleteAll();

        List<Template> templates = templateRepository.listAll();
        assertEquals(0, templates.size());
    }

    @Test
    @Order(14)
    @DisplayName("查找所有模板并关联加载")
    @Transactional
    public void testFindAllWithAssociations() {
        Template template = new Template("association-test", "测试", testProject, testInventory);
        templateRepository.persist(template);

        List<Template> templates = templateRepository.findAllWithAssociations();

        assertFalse(templates.isEmpty());
        templates.forEach(t -> {
            assertNotNull(t.getProject());
            assertNotNull(t.getInventory());
        });
    }
}
