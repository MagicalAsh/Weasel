package us.magicalash.weasel.plugin;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

@Component
public class PluginTaskService {
    @Setter
    @Value("#{${weasel.plugin.thread.default_thread_count}}")
    private int defaultThreadCount;

    @Setter
//    @Value("#{${weasel.plugin.threads.plugin_specific}}")
    private Map<String, String> threadCounts = new HashMap<>();

    private Map<String, ThreadPoolTaskExecutor> pluginExecutors;
    private ThreadPoolTaskExecutor others;

    public PluginTaskService() {
        this.pluginExecutors = new HashMap<>();
    }

    public <T> Future<T> submit(PluginTask<T> task) {
        final ThreadPoolTaskExecutor executor;
        if (pluginExecutors.get(task.getPluginName()) != null) {
            executor = pluginExecutors.get(task.getPluginName());
        } else {
            executor = others;
        }

        synchronized (executor) {
            return executor.submit(task.getTask());
        }
    }

    @PostConstruct
    private void setPostConstruct() {
        others = new ThreadPoolTaskExecutor();
        others.setMaxPoolSize(defaultThreadCount);
        others.initialize();
        for (String key : threadCounts.keySet()) {
            try {
                int threadCount = Integer.parseInt(threadCounts.get(key));
                ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
                taskExecutor.setMaxPoolSize(threadCount);
                taskExecutor.initialize();
                pluginExecutors.put(key, taskExecutor);
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Plugin thread pool thread count must be an integer, \""
                        + key + "\" has " + threadCounts.get(key));
            }
        }
    }
}
