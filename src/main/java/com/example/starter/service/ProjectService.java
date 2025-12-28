package com.example.starter.service;

import com.example.starter.entity.Project;
import com.example.starter.exception.DuplicateResourceException;
import com.example.starter.exception.ResourceNotFoundException;
import com.example.starter.repository.ProjectRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * 项目服务类 - 处理项目的CRUD操作
 */
@ApplicationScoped
public class ProjectService {

    @Inject
    ProjectRepository projectRepository;

    /**
     * 创建新项目
     * 
     * @param name        项目名称
     * @param description 项目描述
     * @param yamlContent YAML内容（明文，会自动编码为Base64）
     * @return 创建的项目
     */
    @Transactional
    public Project createProject(String name, String description, String yamlContent) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("项目名称不能为空");
        }

        if (projectRepository.existsByName(name)) {
            throw new DuplicateResourceException("项目名称已存在");
        }

        // 将YAML内容编码为Base64
        String encodedYaml = encodeToBase64(yamlContent);

        Project project = new Project(name, description, encodedYaml);
        projectRepository.persist(project);
        return project;
    }

    /**
     * 更新项目
     * 
     * @param project 要更新的项目
     */
    @Transactional
    public void updateProject(Project project) {
        if (project == null || project.getId() == null) {
            throw new IllegalArgumentException("项目或项目ID不能为空");
        }

        // 验证项目名称
        if (project.getName() == null || project.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("项目名称不能为空");
        }

        Project existing = projectRepository.findById(project.getId());
        if (existing == null) {
            throw new ResourceNotFoundException("项目不存在");
        }

        // 如果名称被修改，检查新名称是否与其他项目冲突
        if (!existing.getName().equals(project.getName())) {
            if (projectRepository.existsByNameExcludingId(project.getName(), project.getId())) {
                throw new DuplicateResourceException("项目名称已存在");
            }
        }

        // 确保YAML内容是Base64编码的
        if (project.getYamlContent() != null && !project.getYamlContent().isEmpty()) {
            try {
                // 尝试解码验证是否为有效的Base64
                decodeFromBase64(project.getYamlContent());
            } catch (IllegalArgumentException e) {
                // 如果不是有效的Base64，则进行编码
                project.setYamlContent(encodeToBase64(project.getYamlContent()));
            }
        }

        projectRepository.getEntityManager().merge(project);
    }

    /**
     * 删除项目
     * 
     * @param id 项目ID
     */
    @Transactional
    public void deleteProject(Long id) {
        Project project = projectRepository.findById(id);
        if (project != null) {
            projectRepository.delete(project);
        }
    }

    /**
     * 根据ID获取项目
     * 
     * @param id 项目ID
     * @return 项目对象
     */
    public Project getProjectById(Long id) {
        return projectRepository.findById(id);
    }

    /**
     * 获取所有项目
     * 
     * @return 项目列表
     */
    public List<Project> getAllProjects() {
        return projectRepository.listAll();
    }

    /**
     * 获取以"project"开头的所有项目
     * 
     * @return 项目列表
     */
    public List<Project> getProjectResources() {
        return projectRepository.findByNameStartingWith("project");
    }

    /**
     * 解码YAML内容（从Base64转为明文）
     * 
     * @param project 项目对象
     * @return 明文的YAML内容
     */
    public String getDecodedYamlContent(Project project) {
        if (project == null || project.getYamlContent() == null || project.getYamlContent().isEmpty()) {
            return "";
        }
        return decodeFromBase64(project.getYamlContent());
    }

    /**
     * 将字符串编码为Base64
     */
    private String encodeToBase64(String input) {
        if (input == null) {
            return "";
        }
        return Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 从Base64解码字符串
     */
    private String decodeFromBase64(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return "";
        }
        byte[] decodedBytes = Base64.getDecoder().decode(encoded);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }
}
