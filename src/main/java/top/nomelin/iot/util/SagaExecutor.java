package top.nomelin.iot.util;

import org.slf4j.Logger;
import top.nomelin.iot.model.enums.MessageType;
import top.nomelin.iot.service.MessageService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Saga事务执行器，用于分布式事务的最终一致性。
 * 传入一系列SagaStep，每个SagaStep包含一个正向操作和一个补偿操作，以及一个名称生成函数(用于标识步骤详细信息)。
 * 事务执行器会按照SagaStep顺序执行，如果某一步失败，则会反向开始执行补偿操作。
 * 如果所有步骤都成功，则事务成功，否则事务失败。
 * 所有步骤之间通过上下文共享数据，以便在步骤之间传递数据。
 * [注意]如果需要传递数据，必须使用上下文入参，否则后续的lambda表达式不能获取。
 * 失败时会发送站内信通知用户。
 *
 * @author nomelin
 */
public class SagaExecutor {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(SagaExecutor.class);

    private final List<SagaStep> steps = new ArrayList<>();
    private final MessageService messageService;
    private final Integer userId;
    private final String titlePrefix;

    /**
     * 生命周期为一次事务过程，也就是本类的实例化对象的生命周期。
     */
    private final Map<String, Object> context = new HashMap<>();// 事务上下文，用于在多个步骤之间传递数据

    public SagaExecutor(MessageService messageService, Integer userId, String titlePrefix) {
        this.messageService = messageService;
        this.userId = userId;
        this.titlePrefix = titlePrefix;
    }

    public void put(String key, Object value) {
        context.put(key, value);
    }

    public Object get(String key) {
        return context.get(key);
    }

    public <T> T get(String key, Class<T> clazz) {
        Object val = context.get(key);
        return clazz.isInstance(val) ? clazz.cast(val) : null;
    }

    /**
     * 添加事务步骤。
     * 为什么需要上下文？因为事务步骤之间可能需要共享数据，
     * 例如，一个步骤插入mysql，更新了自增id，下一个步骤需要用到这个id，则可以将id放入上下文，在执行下一个步骤时获取。
     * 为什么不能直接传递？因为lambda表达式是延迟执行的，在构造表达式时还没有执行上一个步骤，所以后续步骤无法获取数据。
     *
     * @param nameFn       从上下文中动态生成名称的函数
     * @param action       从上下文中执行正向操作
     * @param compensation 从上下文中执行补偿操作
     */
    public void addStep(ContextualName nameFn, ContextualAction action, ContextualAction compensation) {
        steps.add(new SagaStep(nameFn, action, compensation));
    }

    public void execute() {
        int successCount = 0;
        try {
            for (SagaStep step : steps) {
                step.getAction().execute(context);
                successCount++;
            }
        } catch (Exception e) {
            log.error("[SAGA事务失败] 第{}步执行失败: {}，异常信息: {}", successCount + 1,
                    steps.get(successCount).getName(context), e, e);

            // 拼接站内信通知内容。html格式。
            StringBuilder content = new StringBuilder();
            content.append("SAGA事务失败，操作过程如下：<br>");
            content.append("成功步骤/总步骤: ").append(successCount).append("/").append(steps.size()).append("<br>");
            for (int i = 0; i < successCount; i++) {
                content.append("✅ 成功：").append(steps.get(i).getName(context)).append("<br>");
            }
            content.append("❌ 失败：").append(steps.get(successCount).getName(context))
                    .append("，异常：").append(e).append("<br>");

            for (int i = successCount + 1; i < steps.size(); i++) {
                content.append("⏩ 跳过未执行：").append(steps.get(i).getName(context)).append("<br>");
            }
            content.append("------------------------------------------<br>");

            // 补偿执行
            for (int i = successCount - 1; i >= 0; i--) {
                try {
                    steps.get(i).getCompensation().execute(context);
                    log.warn("已补偿步骤：{}", steps.get(i).getName(context));
                    content.append("🔁 已补偿：").append(steps.get(i).getName(context)).append("<br>");
                } catch (Exception ce) {
                    log.error("补偿步骤失败：{}，异常：{}", steps.get(i).getName(context), ce.getMessage(), ce);
                    content.append("❌ 补偿失败：").append(steps.get(i).getName(context))
                            .append("，异常：").append(ce.getMessage()).append("<br>");
                }
            }

            messageService.sendSystemMessage(userId,
                    titlePrefix + "失败(注意人工检查修复)", content.toString(), MessageType.ERROR);
            throw new RuntimeException("Saga事务执行失败：" + e.getMessage(), e);
        }
    }

    /**
     * 事务步骤接口，将一个上下文作为入参，以便在多个步骤之间传递数据。
     * 例如，一个步骤插入mysql，更新了自增id，下一个步骤需要用到这个id，则可以将id放入上下文，在执行下一个步骤时获取。
     * 如果不采用上下文，则lambda之间无法共享数据。
     */
    @FunctionalInterface
    public interface ContextualAction {
        void execute(Map<String, Object> context);
    }

    /**
     * 事务步骤名称接口，通过上下文内容动态生成名称。
     */
    @FunctionalInterface
    public interface ContextualName {
        String get(Map<String, Object> context);
    }

    public static class SagaStep {
        /**
         * 事务步骤名称，根据上下文动态生成。这是为了支持延时获取名称。
         */
        private final ContextualName nameFn;
        /**
         * 正向操作
         */
        private final ContextualAction action;
        /**
         * 补偿操作
         */
        private final ContextualAction compensation;

        public SagaStep(ContextualName nameFn, ContextualAction action, ContextualAction compensation) {
            this.nameFn = nameFn;
            this.action = action;
            this.compensation = compensation;
        }

        public String getName(Map<String, Object> context) {
            return nameFn.get(context);// 根据上下文动态生成名称
        }

        public ContextualAction getAction() {
            return action;
        }

        public ContextualAction getCompensation() {
            return compensation;
        }
    }
}
