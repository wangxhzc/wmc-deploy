package com.example.starter.repository;

import com.example.starter.entity.Project;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProjectRepository 单元测试
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("ProjectRepository 测试")
public class ProjectRepositoryTest {

    @Inject
    ProjectRepository projectRepository;

    @BeforeEach
    @Transactional
    public void setUp() {
        // 清理之前的测试数据
        projectRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("查找项目 - 按名称")
    @Transactional
    public void testFindByName() {
        String name = "test-project-" + System.currentTimeMillis();
        Project project = new Project(name, "测试项目", "yaml内容");
        projectRepository.persist(project);

        Optional<Project> found = projectRepository.findByName(name);

        assertTrue(found.isPresent());
        assertEquals(name, found.get().getName());
    }

    @Test
    @Order(2)
    @DisplayName("查找项目 - 按名称 - 不存在")
    @Transactional
    public void testFindByNameNotFound() {
        Optional<Project> found = projectRepository.findByName("不存在的项目名称");
        assertFalse(found.isPresent());
    }

    @Test
    @Order(3)
    @DisplayName("检查名称是否存在 - 存在")
    @Transactional
    public void testExistsByNameExists() {
        String name = "exists-test-" + System.currentTimeMillis();
        Project project = new Project(name, "测试项目", "yaml内容");
        projectRepository.persist(project);

        assertTrue(projectRepository.existsByName(name));
    }

    @Test
    @Order(4)
    @DisplayName("检查名称是否存在 - 不存在")
    @Transactional
    public void testExistsByNameNotExists() {
        assertFalse(projectRepository.existsByName("不存在的项目名称"));
    }

    @Test
    @Order(5)
    @DisplayName("查找项目 - 按前缀")
    @Transactional
    public void testFindByNameStartingWith() {
        String name1 = "prefix-test-1-" + System.currentTimeMillis();
        String name2 = "prefix-test-2-" + System.currentTimeMillis();
        String name3 = "other-name-" + System.currentTimeMillis();

        projectRepository.persist(new Project(name1, "测试1", "yaml"));
        projectRepository.persist(new Project(name2, "测试2", "yaml"));
        projectRepository.persist(new Project(name3, "测试3", "yaml"));

        List<Project> projects = projectRepository.findByNameStartingWith("prefix-test-");

        assertEquals(2, projects.size());
        assertTrue(projects.stream().anyMatch(p -> p.getName().equals(name1)));
        assertTrue(projects.stream().anyMatch(p -> p.getName().equals(name2)));
        assertFalse(projects.stream().anyMatch(p -> p.getName().equals(name3)));
    }

    @Test
    @Order(6)
    @DisplayName("检查名称是否存在 - 排除指定ID")
    @Transactional
    public void testExistsByNameExcludingId() {
        String name = "exclude-test-" + System.currentTimeMillis();
        Project project1 = new Project(name, "测试1", "yaml");
        projectRepository.persist(project1);
        Long id1 = project1.getId();

        Project project2 = new Project("other-name", "测试2", "yaml");
        projectRepository.persist(project2);

        // 同一名称，排除自己的ID - 应该返回false，因为没有其他同名的项目
        assertFalse(projectRepository.existsByNameExcludingId(name, id1));

        // 不同名称 - 应该返回false
        assertFalse(projectRepository.existsByNameExcludingId("different-name", id1));

        // 测试存在同名称的不同项目
        String duplicateName = "duplicate-test-" + System.currentTimeMillis();
        Project project3 = new Project(duplicateName, "测试3", "yaml");
        projectRepository.persist(project3);
        Long id3 = project3.getId();

        Project project4 = new Project(duplicateName, "测试4", "yaml");
        projectRepository.persist(project4);

        // 排除id3，应该能找到id4
        assertTrue(projectRepository.existsByNameExcludingId(duplicateName, id3));

        // 排除不存在的ID，应该能找到
        assertTrue(projectRepository.existsByNameExcludingId(duplicateName, 9999L));
    }

    @Test
    @Order(7)
    @DisplayName("持久化项目")
    @Transactional
    public void testPersist() {
        String name = "persist-test-" + System.currentTimeMillis();
        Project project = new Project(name, "测试项目", "yaml内容");

        projectRepository.persist(project);

        assertNotNull(project.getId());

        Optional<Project> found = projectRepository.findByName(name);
        assertTrue(found.isPresent());
    }

    @Test
    @Order(8)
    @DisplayName("查找项目 - 按ID")
    @Transactional
    public void testFindById() {
        String name = "findbyid-test-" + System.currentTimeMillis();
        Project project = new Project(name, "测试项目", "yaml内容");
        projectRepository.persist(project);
        Long id = project.getId();

        Project found = projectRepository.findById(id);

        assertNotNull(found);
        assertEquals(name, found.getName());
    }

    @Test
    @Order(9)
    @DisplayName("列出所有项目")
    @Transactional
    public void testListAll() {
        projectRepository.persist(new Project("project-1", "测试1", "yaml"));
        projectRepository.persist(new Project("project-2", "测试2", "yaml"));
        projectRepository.persist(new Project("project-3", "测试3", "yaml"));

        List<Project> projects = projectRepository.listAll();

        assertTrue(projects.size() >= 3);
    }

    @Test
    @Order(10)
    @DisplayName("删除项目")
    @Transactional
    public void testDelete() {
        String name = "delete-test-" + System.currentTimeMillis();
        Project project = new Project(name, "测试项目", "yaml内容");
        projectRepository.persist(project);
        Long id = project.getId();

        projectRepository.delete(project);

        Project deleted = projectRepository.findById(id);
        assertNull(deleted);
    }

    @Test
    @Order(11)
    @DisplayName("删除所有项目")
    @Transactional
    public void testDeleteAll() {
        projectRepository.persist(new Project("project-1", "测试1", "yaml"));
        projectRepository.persist(new Project("project-2", "测试2", "yaml"));

        projectRepository.deleteAll();

        List<Project> projects = projectRepository.listAll();
        assertEquals(0, projects.size());
    }
}
