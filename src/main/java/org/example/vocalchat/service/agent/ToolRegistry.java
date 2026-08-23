package org.example.vocalchat.service.agent;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 工具注册表。
 *
 * <p>启动阶段自动扫描所有 Spring Bean 中带 {@link Tool @Tool} 注解的方法，
 * 借助 LangChain4j 的 {@link ToolSpecifications} 生成 {@link ToolSpecification}（工具说明书），
 * 并生成 {@link ToolExecutor}（反射执行器）。运行时按工具名查找并执行工具。</p>
 *
 * <p>实现 {@link SmartInitializingSingleton} 而非 {@code @PostConstruct}，确保扫描发生在
 * 所有单例 Bean 实例化完成后，避免提前触发其他 Bean 的初始化。</p>
 */
@Slf4j
@Component
public class ToolRegistry implements SmartInitializingSingleton {

    private final ApplicationContext applicationContext;

    /** 工具名 -> (工具说明书, 反射执行器) */
    private final Map<String, ToolEntry> tools = new ConcurrentHashMap<>();

    public ToolRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public record ToolEntry(ToolSpecification specification, ToolExecutor executor) {
    }

    /** 工具执行结果：success 标记是否成功，text 是给 LLM / 前端看的文本。 */
    public record ToolResult(boolean success, String text) {
        public static ToolResult ok(String text) {
            return new ToolResult(true, text);
        }

        public static ToolResult fail(String text) {
            return new ToolResult(false, text);
        }
    }

    @Override
    public void afterSingletonsInstantiated() {
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            Object bean;
            try {
                bean = applicationContext.getBean(beanName);
            } catch (Exception e) {
                // 忽略无法实例化的 Bean（懒加载 / 依赖外部环境的 Bean），避免拖垮整个注册表初始化
                log.debug("跳过无法实例化的 Bean {}: {}", beanName, e.getMessage());
                continue;
            }
            // 用 AopUtils 还原被代理前的真实类，避免 CGLIB/JDK 代理导致方法注解丢失
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            for (Method method : targetClass.getMethods()) {
                Tool tool = method.getAnnotation(Tool.class);
                if (tool == null) {
                    continue;
                }
                ToolSpecification specification = ToolSpecifications.toolSpecificationFrom(method);
                ToolExecutor executor = new DefaultToolExecutor(bean, method);
                tools.put(specification.name(), new ToolEntry(specification, executor));
                log.info("已注册 Agent 工具: {} - {}", specification.name(), toolDescription(tool));
            }
        }
    }

    /** 提供给 LLM 的全部工具说明书，用于 Function Calling。 */
    public List<ToolSpecification> allSpecifications() {
        return tools.values().stream().map(ToolEntry::specification).toList();
    }

    /**
     * 执行一个工具调用请求，返回带 success 标记的结果。
     *
     * <p>任何异常都在此捕获并降级为失败结果（success=false），绝不上抛——否则会中断整个 Agent Loop。
     * 调用方可根据 {@link ToolResult#success()} 区分成功/失败，分别告知前端与 LLM。</p>
     */
    public ToolResult execute(ToolExecutionRequest request) {
        ToolEntry entry = tools.get(request.name());
        if (entry == null) {
            log.warn("LLM 请求了未注册的工具: {}", request.name());
            return ToolResult.fail("未知工具: " + request.name());
        }
        try {
            return ToolResult.ok(entry.executor().execute(request, null));
        } catch (Exception e) {
            log.error("工具 {} 执行失败", request.name(), e);
            return ToolResult.fail("工具 " + request.name() + " 执行失败: " + e.getMessage());
        }
    }

    private String toolDescription(Tool tool) {
        String[] value = tool.value();
        return value.length == 0 ? "" : String.join(" ", value);
    }
}
