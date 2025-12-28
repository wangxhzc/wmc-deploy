package com.example.starter.service;

import com.example.starter.entity.Project;
import com.example.starter.exception.DuplicateResourceException;
import com.example.starter.repository.ProjectRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProjectService 单元测试
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProjectServiceTest {

    @Inject
    ProjectService projectService;

    @Inject
    ProjectRepository projectRepository;

    private static String testYamlContent = "---\n- name: Test playbook\n  hosts: all\n  tasks:\n    - name: Echo\n      debug:\n        msg: 'Hello World'\n";

    @BeforeEach
    @Transactional
    public void setUp() {
        // 清理之前的测试数据 - 删除所有测试创建的项目
        projectRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("创建项目 - 成功")
    public void testCreateProject() {
        String name = "test-project-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000);

        Project project = projectService.createProject(name, "测试项目", testYamlContent);

        assertNotNull(project);
        assertNotNull(project.getId());
        assertEquals(name, project.getName());
        assertEquals("测试项目", project.getDescription());
        assertNotNull(project.getYamlContent());

        // 验证YAML内容已编码
        assertNotEquals(testYamlContent, project.getYamlContent());

        // 验证编码内容可以解码
        String decoded = projectService.getDecodedYamlContent(project);
        assertEquals(testYamlContent, decoded);
    }

    @Test
    @Order(2)
    @DisplayName("创建项目 - 空名称 - 失败")
    public void testCreateProjectEmptyName() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> projectService.createProject("", "测试", testYamlContent));

        assertEquals("项目名称不能为空", exception.getMessage());
    }

    @Test
    @Order(3)
    @DisplayName("创建项目 - 重复名称 - 失败")
    public void testCreateProjectDuplicateName() {
        String name = "duplicate-test-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000);
        projectService.createProject(name, "测试项目1", testYamlContent);

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> projectService.createProject(name, "测试项目2", testYamlContent));

        assertEquals("项目名称已存在", exception.getMessage());
    }

    @Test
    @Order(4)
    @DisplayName("创建项目 - 空YAML内容 - 成功")
    public void testCreateProjectEmptyYaml() {
        String name = "empty-yaml-test-" + System.currentTimeMillis();
        Project project = projectService.createProject(name, "空YAML测试", "");

        assertNotNull(project);
        assertEquals("", projectService.getDecodedYamlContent(project));
    }

    @Test
    @Order(5)
    @DisplayName("根据ID获取项目")
    public void testGetProjectById() {
        String name = "get-by-id-test-" + System.currentTimeMillis();
        Project created = projectService.createProject(name, "测试", testYamlContent);

        Project found = projectService.getProjectById(created.getId());

        assertNotNull(found);
        assertEquals(created.getId(), found.getId());
        assertEquals(name, found.getName());
    }

    @Test
    @Order(6)
    @DisplayName("根据ID获取项目 - 不存在")
    public void testGetProjectByIdNotFound() {
        Project found = projectService.getProjectById(999999L);
        assertNull(found);
    }

    @Test
    @Order(7)
    @DisplayName("获取所有项目")
    public void testGetAllProjects() {
        String name1 = "all-test-1-" + System.currentTimeMillis();
        String name2 = "all-test-2-" + System.currentTimeMillis();
        projectService.createProject(name1, "测试1", testYamlContent);
        projectService.createProject(name2, "测试2", testYamlContent);

        List<Project> projects = projectService.getAllProjects();

        assertNotNull(projects);
        assertTrue(projects.size() >= 2);
        assertTrue(projects.stream().anyMatch(p -> p.getName().equals(name1)));
        assertTrue(projects.stream().anyMatch(p -> p.getName().equals(name2)));
    }

    @Test
    @Order(8)
    @DisplayName("获取project开头的项目资源")
    public void testGetProjectResources() {
        projectService.createProject("project-resource-" + System.currentTimeMillis(), "资源1", testYamlContent);
        projectService.createProject("other-name-" + System.currentTimeMillis(), "其他项目", testYamlContent);

        List<Project> projects = projectService.getProjectResources();

        assertNotNull(projects);
        // 只返回以"project"开头的项目
        assertTrue(projects.size() >= 1);
        assertTrue(projects.stream().allMatch(p -> p.getName().startsWith("project")));
    }

    @Test
    @Order(9)
    @DisplayName("更新项目 - 成功")
    public void testUpdateProject() {
        String name = "update-test-" + System.currentTimeMillis();
        Project created = projectService.createProject(name, "原始描述", testYamlContent);

        String newYaml = "---\n- name: Updated playbook\n  hosts: localhost\n  tasks:\n    - name: Task1\n      command: echo 'updated'";

        created.setDescription("更新后的描述");
        created.setYamlContent(newYaml);

        projectService.updateProject(created);

        Project updated = projectService.getProjectById(created.getId());
        assertEquals("更新后的描述", updated.getDescription());
        assertEquals(newYaml, projectService.getDecodedYamlContent(updated));
    }

    @Test
    @Order(10)
    @DisplayName("更新项目 - 修改名称 - 成功")
    @Transactional
    public void testUpdateProjectRename() {
        String oldName = "rename-old-" + System.currentTimeMillis();
        String newName = "rename-new-" + System.currentTimeMillis();
        Project created = projectService.createProject(oldName, "测试", testYamlContent);

        created.setName(newName);
        projectService.updateProject(created);

        Project updated = projectService.getProjectById(created.getId());
        assertEquals(newName, updated.getName());
    }

    @Test
    @Order(11)
    @DisplayName("更新项目 - 新名称重复 - 失败")
    public void testUpdateProjectDuplicateNewName() {
        String name1 = "dup-name-1-" + System.currentTimeMillis();
        String name2 = "dup-name-2-" + System.currentTimeMillis();
        projectService.createProject(name1, "测试1", testYamlContent);
        Project project2 = projectService.createProject(name2, "测试2", testYamlContent);

        project2.setName(name1);

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> projectService.updateProject(project2));

        assertEquals("项目名称已存在", exception.getMessage());
    }

    @Test
    @Order(12)
    @DisplayName("更新项目 - 空名称 - 失败")
    public void testUpdateProjectEmptyName() {
        String name = "empty-name-update-" + System.currentTimeMillis();
        Project created = projectService.createProject(name, "测试", testYamlContent);

        created.setName("");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> projectService.updateProject(created));

        assertEquals("项目名称不能为空", exception.getMessage());
    }

    @Test
    @Order(13)
    @DisplayName("更新项目 - 传入明文YAML - 自动编码")
    public void testUpdateProjectWithPlainTextYaml() {
        String name = "plain-yaml-update-" + System.currentTimeMillis();
        String plainYaml = "---\n- name: Plain YAML\n  hosts: all";
        Project created = projectService.createProject(name, "测试", testYamlContent);

        // 设置明文YAML（模拟用户在界面上输入的）
        created.setYamlContent(plainYaml);
        projectService.updateProject(created);

        // 验证已编码
        Project updated = projectService.getProjectById(created.getId());
        assertNotEquals(plainYaml, updated.getYamlContent());
        assertEquals(plainYaml, projectService.getDecodedYamlContent(updated));
    }

    @Test
    @Order(14)
    @DisplayName("删除项目 - 成功")
    public void testDeleteProject() {
        String name = "delete-test-" + System.currentTimeMillis();
        Project created = projectService.createProject(name, "测试", testYamlContent);
        Long id = created.getId();

        projectService.deleteProject(id);

        Project deleted = projectService.getProjectById(id);
        assertNull(deleted);
    }

    @Test
    @Order(15)
    @DisplayName("删除项目 - 不存在的ID - 无异常")
    public void testDeleteProjectNotFound() {
        // 删除不存在的项目ID，应该静默成功
        assertDoesNotThrow(() -> projectService.deleteProject(999999L));
    }

    @Test
    @Order(16)
    @DisplayName("解码YAML内容 - Base64编码")
    public void testGetDecodedYamlContent() {
        String name = "decode-test-" + System.currentTimeMillis();
        String yaml = "---\n- name: Decode test\n  hosts: localhost";
        Project created = projectService.createProject(name, "测试", yaml);

        String decoded = projectService.getDecodedYamlContent(created);

        assertEquals(yaml, decoded);
    }

    @Test
    @Order(17)
    @DisplayName("解码YAML内容 - 空内容")
    public void testGetDecodedYamlContentEmpty() {
        Project project = new Project();
        project.setYamlContent("");

        String decoded = projectService.getDecodedYamlContent(project);
        assertEquals("", decoded);
    }

    @Test
    @Order(18)
    @DisplayName("解码YAML内容 - null内容")
    public void testGetDecodedYamlContentNull() {
        Project project = new Project();
        project.setYamlContent(null);

        String decoded = projectService.getDecodedYamlContent(project);
        assertEquals("", decoded);
    }

    @Test
    @Order(19)
    @DisplayName("解码YAML内容 - null项目")
    public void testGetDecodedYamlContentNullProject() {
        String decoded = projectService.getDecodedYamlContent(null);
        assertEquals("", decoded);
    }

    @Test
    @Order(20)
    @DisplayName("Base64编解码 - 包含中文字符")
    public void testBase64ChineseCharacters() {
        String name = "chinese-test-" + System.currentTimeMillis();
        String yamlWithChinese = "---\n# 这是一个中文注释\n- name: 中文任务\n  debug:\n    msg: '你好世界'";

        Project created = projectService.createProject(name, "中文测试", yamlWithChinese);
        String decoded = projectService.getDecodedYamlContent(created);

        assertEquals(yamlWithChinese, decoded);
    }

    @Test
    @Order(21)
    @DisplayName("Base64编解码 - 包含特殊字符")
    public void testBase64SpecialCharacters() {
        String name = "special-test-" + System.currentTimeMillis();
        String yamlWithSpecial = "---\n- name: Special chars\n  debug:\n    msg: 'Test @#$%^&*()_+-={}[]|\\\\:\";''<>,.?/~`\n'";

        Project created = projectService.createProject(name, "特殊字符测试", yamlWithSpecial);
        String decoded = projectService.getDecodedYamlContent(created);

        assertEquals(yamlWithSpecial, decoded);
    }
}
