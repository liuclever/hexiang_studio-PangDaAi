package com.back_hexiang_studio.pangDaAi.service.assistant;

import com.back_hexiang_studio.pangDaAi.tool.api.NewsApiToolService;
import com.back_hexiang_studio.pangDaAi.tool.api.WeatherToolService;
import com.back_hexiang_studio.pangDaAi.tool.workflow.AttendanceManagementTools;
import com.back_hexiang_studio.pangDaAi.tool.workflow.CourseManagementTools;
import com.back_hexiang_studio.pangDaAi.tool.workflow.MaterialManagementTools;
import com.back_hexiang_studio.pangDaAi.tool.workflow.ModelManagementTools;
import com.back_hexiang_studio.pangDaAi.tool.workflow.NoticeManagementTools;
import com.back_hexiang_studio.pangDaAi.tool.workflow.StudioManagementTools;
import com.back_hexiang_studio.pangDaAi.tool.workflow.TaskManagementTools;
import com.back_hexiang_studio.pangDaAi.tool.workflow.UserManagementTools;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;


import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *    智能工具选择器 -
 * 自动扫描、加载并根据用户查询动态选择相关的【工作流工具实例】。
 */
@Component
@Slf4j
public class SmartToolSelector {

    @Autowired
    private ApplicationContext applicationContext;

    // 存储所有扫描到的工作流工具实例
    private List<Object> allWorkflowTools;
    // 缓存工具方法和其所属类的映射
    private Map<String, Object> toolMethodToInstanceMap;

    /**
     * 初始化时，自动扫描并加载所有的工作流工具
     */
    @PostConstruct
    public void initialize() {
        allWorkflowTools = new ArrayList<>();
        toolMethodToInstanceMap = new HashMap<>();
        // 从Spring容器中获取所有定义的工作流工具Bean
        // 并解包任何AOP代理，以获取原始对象
        allWorkflowTools.add(unwrapProxy(applicationContext.getBean(UserManagementTools.class)));
        allWorkflowTools.add(unwrapProxy(applicationContext.getBean(CourseManagementTools.class)));
        allWorkflowTools.add(unwrapProxy(applicationContext.getBean(MaterialManagementTools.class)));
        allWorkflowTools.add(unwrapProxy(applicationContext.getBean(ModelManagementTools.class)));
        allWorkflowTools.add(unwrapProxy(applicationContext.getBean(NoticeManagementTools.class)));
        allWorkflowTools.add(unwrapProxy(applicationContext.getBean(TaskManagementTools.class)));
        allWorkflowTools.add(unwrapProxy(applicationContext.getBean(AttendanceManagementTools.class)));
        allWorkflowTools.add(unwrapProxy(applicationContext.getBean(StudioManagementTools.class)));
        allWorkflowTools.add(unwrapProxy(applicationContext.getBean(WeatherToolService.class)));
        allWorkflowTools.add(unwrapProxy(applicationContext.getBean(NewsApiToolService.class)));
        // 如果未来有新的工具类，在这里添加即可

        // 缓存每个@Tool方法和它所属的实例
        for (Object toolInstance : allWorkflowTools) {
            for (Method method : toolInstance.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(Tool.class)) {
                    toolMethodToInstanceMap.put(method.getName(), toolInstance);
                }
            }
        }
        log.info("  智能工具选择器初始化完成，共加载 {} 个工作流工具类。", allWorkflowTools.size());
    }

    /**
     *  根据用户查询智能选择相关的【工具实例】
     * @param userQuery 用户查询内容
     * @return 相关的工具实例列表
     */
    public List<Object> selectRelevantTools(String userQuery) {
        if (userQuery == null || userQuery.trim().isEmpty()) {
            log.debug("   空查询，不选择任何工具。");
            return new ArrayList<>();
        }



        // 我们可以暂时将所有工具都提供给AI，让AI自行选择。
        // LangChain4j的内部机制会根据工具的描述（@Tool注解）来决定调用哪个。
        // 这种方式在工具数量可控（例如少于20-30个）的情况下，效果非常好，


        // 未来的优化方向：如果工具数量继续增长，我们可以重新引入基于关键词或语义的预筛选，
        // 但筛选的对象将是整个工具类实例，而不是零散的方法名。

        log.info("   为查询 '{}' 选择了所有 {} 个工作流工具。", userQuery, allWorkflowTools.size());
        return allWorkflowTools;
    }

    /**
     *    获取所有已加载的工作流工具实例
     * @return 所有工具实例的列表
     */
    public List<Object> getAllWorkflowTools() {
        return allWorkflowTools;
    }

    /**
     * 检查并解包Spring AOP代理对象，以返回原始的目标对象。
     * Langchain4j的工具扫描是基于反射的，无法穿透Spring的代理类。
     * @param bean 可能被代理的Spring Bean
     * @return 原始的目标对象
     */
    private Object unwrapProxy(Object bean) {
        try {
            //Advised 是 Spring 内部接口：
            //只有 AOP 代理对象才会实现它。
            //它提供了访问代理元数据、增强、目标对象的接口，例如：
            // 检查bean是否为Spring AOP代理
            if (AopUtils.isAopProxy(bean) && bean instanceof Advised) {
                // 如果是代理，则获取其背后的原始目标对象
                Object target = ((Advised) bean).getTargetSource().getTarget();
                log.info(" 成功解包Spring AOP代理: {} -> {}", bean.getClass().getName(), target.getClass().getName());
                return target;
            }
        } catch (Exception e) {
            log.error(" 解包AOP代理失败: {}", bean.getClass().getName(), e);
        }
        log.debug(" 工具 {} 不是AOP代理，直接使用。", bean.getClass().getSimpleName());
        return bean;
    }
} 