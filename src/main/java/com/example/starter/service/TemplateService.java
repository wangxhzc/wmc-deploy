package com.example.starter.service;

import com.example.starter.entity.Inventory;
import com.example.starter.entity.Project;
import com.example.starter.entity.Template;
import com.example.starter.entity.TemplateVariable;
import com.example.starter.exception.DuplicateResourceException;
import com.example.starter.exception.ResourceNotFoundException;
import com.example.starter.repository.TemplateRepository;
import com.example.starter.service.inventory.InventoryService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * 模板服务类 - 处理模板的CRUD操作
 * 类似于Ansible AWX的Job Template
 */
@ApplicationScoped
public class TemplateService {

    @Inject
    TemplateRepository templateRepository;

    @Inject
    ProjectService projectService;

    @Inject
    InventoryService inventoryService;

    /**
     * 创建新模板
     * 
     * @param name        模板名称
     * @param description 模板描述
     * @param projectId   项目ID
     * @param inventoryId 清单ID
     * @return 创建的模板
     */
    @Transactional
    public Template createTemplate(String name, String description, Long projectId, Long inventoryId) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("模板名称不能为空");
        }

        if (templateRepository.existsByName(name)) {
            throw new DuplicateResourceException("模板名称已存在");
        }

        // 验证项目是否存在
        Project project = projectService.getProjectById(projectId);
        if (project == null) {
            throw new ResourceNotFoundException("项目不存在");
        }

        // 验证清单是否存在
        Inventory inventory = inventoryService.getInventoryById(inventoryId);
        if (inventory == null) {
            throw new ResourceNotFoundException("清单不存在");
        }

        Template template = new Template(name, description, project, inventory);
        templateRepository.persist(template);
        return template;
    }

    /**
     * 更新模板
     * 
     * @param template 要更新的模板
     */
    @Transactional
    public void updateTemplate(Template template) {
        if (template == null || template.getId() == null) {
            throw new IllegalArgumentException("模板或模板ID不能为空");
        }

        // 验证模板名称
        if (template.getName() == null || template.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("模板名称不能为空");
        }

        Template existing = templateRepository.findById(template.getId());
        if (existing == null) {
            throw new ResourceNotFoundException("模板不存在");
        }

        // 如果名称被修改，检查新名称是否与其他模板冲突
        if (!existing.getName().equals(template.getName())) {
            if (templateRepository.existsByNameExcludingId(template.getName(), template.getId())) {
                throw new DuplicateResourceException("模板名称已存在");
            }
        }

        // 验证项目和清单是否存在
        if (template.getProject() != null) {
            Project project = projectService.getProjectById(template.getProject().getId());
            if (project == null) {
                throw new ResourceNotFoundException("项目不存在");
            }
        }

        if (template.getInventory() != null) {
            Inventory inventory = inventoryService.getInventoryById(template.getInventory().getId());
            if (inventory == null) {
                throw new ResourceNotFoundException("清单不存在");
            }
        }

        templateRepository.getEntityManager().merge(template);
    }

    /**
     * 删除模板
     * 
     * @param id 模板ID
     */
    @Transactional
    public void deleteTemplate(Long id) {
        Template template = templateRepository.findById(id);
        if (template != null) {
            templateRepository.delete(template);
        }
    }

    /**
     * 根据ID获取模板
     * 
     * @param id 模板ID
     * @return 模板对象
     */
    public Template getTemplateById(Long id) {
        return templateRepository.findById(id);
    }

    /**
     * 获取所有模板
     * 
     * @return 模板列表
     */
    public List<Template> getAllTemplates() {
        return templateRepository.findAllWithAssociations();
    }

    /**
     * 根据项目ID查找模板
     * 
     * @param projectId 项目ID
     * @return 模板列表
     */
    public List<Template> getTemplatesByProjectId(Long projectId) {
        return templateRepository.findByProjectId(projectId);
    }

    /**
     * 根据清单ID查找模板
     * 
     * @param inventoryId 清单ID
     * @return 模板列表
     */
    public List<Template> getTemplatesByInventoryId(Long inventoryId) {
        return templateRepository.findByInventoryId(inventoryId);
    }

    /**
     * 为模板添加变量
     * 
     * @param templateId    模板ID
     * @param variableName  变量名
     * @param variableValue 变量值
     */
    @Transactional
    public void addVariable(Long templateId, String variableName, String variableValue) {
        Template template = getTemplateById(templateId);
        if (template == null) {
            throw new ResourceNotFoundException("模板不存在");
        }

        TemplateVariable variable = new TemplateVariable(variableName, variableValue);
        variable.setTemplate(template);
        template.getVariables().add(variable);
        templateRepository.getEntityManager().merge(template);
    }

    /**
     * 从模板删除变量
     * 
     * @param templateId   模板ID
     * @param variableName 变量名
     */
    @Transactional
    public void removeVariable(Long templateId, String variableName) {
        Template template = getTemplateById(templateId);
        if (template == null) {
            throw new ResourceNotFoundException("模板不存在");
        }

        template.getVariables().removeIf(v -> v.getVariableName().equals(variableName));
        templateRepository.getEntityManager().merge(template);
    }
}
