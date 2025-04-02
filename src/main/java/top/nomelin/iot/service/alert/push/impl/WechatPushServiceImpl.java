package top.nomelin.iot.service.alert.push.impl;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import top.nomelin.iot.config.PushPlusConfig;
import top.nomelin.iot.service.alert.push.WechatPushService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


@Service
public class WechatPushServiceImpl implements WechatPushService {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(WechatPushServiceImpl.class);
    private final PushPlusConfig config;
    private final AtomicInteger globalRemaining;
    private final Map<Integer, UserBucket> userBuckets = new ConcurrentHashMap<>();
    private final String pushplusToken;

    @Autowired
    public WechatPushServiceImpl(PushPlusConfig config) {
        this.config = config;
        this.globalRemaining = new AtomicInteger(config.getDailyGlobalLimit());
        this.pushplusToken = config.getToken();
        log.info("WechatPushService 初始化, 全局每日限制: {}, 用户令牌桶大小: {}, 用户令牌刷新间隔: {}秒",
                config.getDailyGlobalLimit(), config.getUserTokenBucketSize(), config.getUserTokenRefreshInterval());
    }

    @Scheduled(cron = "0 0 1 * * ?", zone = "Asia/Shanghai") // 每天凌晨1点执行
    public void resetGlobalRemaining() {
        int newLimit = config.getDailyGlobalLimit();
        //无需CAS.
        log.info("已重置pushplus全局推送次数限制，旧值: {} → 新值: {}",
                globalRemaining.getAndSet(newLimit), newLimit);
    }

    @Override
    public boolean send(String title, String content, int fromUserId, List<String> friendTokens) {
        // 乐观锁控制全局每日限制次数
        int currentGlobal;
        do {
            currentGlobal = globalRemaining.get();
            if (currentGlobal <= 0) {
                log.warn("全局每日推送次数已用完.title: {}, content: {}, fromUserId: {}", title, content, fromUserId);
                return false;
            }
        } while (!globalRemaining.compareAndSet(currentGlobal, currentGlobal - 1));

        UserBucket userBucket = userBuckets.computeIfAbsent(fromUserId, this::createUserBucket);
        userBucket.getQueue().add(new Message(title, content, friendTokens));
        processQueue(fromUserId);//有新消息,尝试处理队列
        return true;
    }

    private UserBucket createUserBucket(int userId) {
        UserBucket userBucket = new UserBucket();
        userBucket.setTokenBucket(new AtomicInteger(config.getUserTokenBucketSize()));
        userBucket.setQueue(new ConcurrentLinkedQueue<>());

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> refillToken(userId),
                0, config.getUserTokenRefreshInterval(), TimeUnit.SECONDS);
        userBucket.setScheduler(scheduler);
        log.info("创建用户令牌桶,并且启动添加令牌的定时任务, userId: {}, 令牌桶大小: {}, 刷新间隔: {}秒",
                userId, config.getUserTokenBucketSize(), config.getUserTokenRefreshInterval());
        return userBucket;
    }

    private void refillToken(int userId) {
        UserBucket userBucket = userBuckets.get(userId);
        if (userBucket == null) {
            return;
        }
        AtomicInteger tokenBucket = userBucket.getTokenBucket();
        int current = tokenBucket.get();
        if (current < config.getUserTokenBucketSize()) {
            tokenBucket.compareAndSet(current, current + 1);
            processQueue(userId);//有新令牌,尝试处理队列
        }
    }

    /**
     * 尝试处理某个用户缓冲区队列中的消息, 直到队列为空或令牌桶耗尽
     */
    private void processQueue(int userId) {
        UserBucket userBucket = userBuckets.get(userId);
        if (userBucket == null) {
            return;
        }
        if (globalRemaining.get() <= 0) {
            return;//double check
        }
        while (true) {
            int tokens = userBucket.getTokenBucket().get();
            if (tokens <= 0) {
                break;//没有令牌,不会循环,直接退出
            }
            Message msg = userBucket.getQueue().poll();
            if (msg == null) {
                break;//队列为空,不会循环,直接退出
            }
            if (userBucket.getTokenBucket().compareAndSet(tokens, tokens - 1)) {
                sendToPushPlus(msg.title, msg.content, msg.friendTokens);
            }
            //发送完一条消息后会继续循环,直到队列为空或令牌桶耗尽
        }
    }

    private boolean sendToPushPlus(String title, String content, List<String> friendTokens) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("token", pushplusToken);
            body.put("title", title);
            body.put("content", content);
            body.put("template", "html");
            body.put("channel", "wechat");
            if (friendTokens != null && !friendTokens.isEmpty()) {
                body.put("to", String.join(",", friendTokens));
            }
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "http://www.pushplus.plus/send",
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            log.info("推送消息到pushplus成功, title: {}, content: {}, friendTokens: {}, response.body: {}",
                    title, content, friendTokens, response.getBody());

            return response.getStatusCode() == HttpStatus.OK &&
                    "200".equals(String.valueOf(response.getBody().get("code")));
        } catch (RestClientException e) {
            return false;
        }
    }

    @PreDestroy
    public void shutdown() {
        userBuckets.values().forEach(bucket -> bucket.getScheduler().shutdownNow());
        log.info("WechatPushService 已关闭并释放资源.");
    }

    // 管理接口
    @Override
    public int getGlobalRemaining() {
        return globalRemaining.get();
    }

    @Override
    public void addGlobalRemaining(int delta) {
        int prev = globalRemaining.getAndAdd(delta);
        log.info("增加pushplus全局推送次数, 原值: {},增量: {}, 新值: {}", prev, delta, globalRemaining.get());
    }

    @Override
    public Map<String, Integer> getUserStatus(int userId) {
        UserBucket bucket = userBuckets.get(userId);
        if (bucket == null) {
            return Map.of(
                    "tokensLeft", -1,
                    "tokensCapacity", -1,
                    "tokensRefillInterval", -1,
                    "queueSize", -1
            );
        }
        return Map.of(
                "tokensLeft", bucket.getTokenBucket().get(),
                "tokensCapacity", config.getUserTokenBucketSize(),
                "tokensRefillInterval", config.getUserTokenRefreshInterval(),
                "queueSize", bucket.getQueue().size()
        );
    }

    @Override
    public void clearUserQueue(int userId) {
        UserBucket bucket = userBuckets.get(userId);
        if (bucket != null) {
            bucket.getQueue().clear();
        }
        log.info("清空用户{}的wechat消息队列", userId);
    }


    /**
     * 对于每个发送用户，维护一个令牌桶和缓冲区队列，令牌桶用于控制发送速率，缓冲区队列用于存储待发送的消息。
     * 之所以需要令牌桶,是因为微信推送次数很少,需要严格控制使用,否则可能会被封禁。
     */
    private static class UserBucket {
        private AtomicInteger tokenBucket;// 令牌桶
        private Queue<Message> queue;// 缓冲区队列
        private ScheduledExecutorService scheduler;

        // getters and setters
        public AtomicInteger getTokenBucket() {
            return tokenBucket;
        }

        public void setTokenBucket(AtomicInteger tokenBucket) {
            this.tokenBucket = tokenBucket;
        }

        public Queue<Message> getQueue() {
            return queue;
        }

        public void setQueue(Queue<Message> queue) {
            this.queue = queue;
        }

        public ScheduledExecutorService getScheduler() {
            return scheduler;
        }

        public void setScheduler(ScheduledExecutorService scheduler) {
            this.scheduler = scheduler;
        }
    }

    private static class Message {
        final String title;
        final String content;
        final List<String> friendTokens;

        Message(String title, String content, List<String> friendTokens) {
            this.title = title;
            this.content = content;
            this.friendTokens = friendTokens;
        }
    }
}

