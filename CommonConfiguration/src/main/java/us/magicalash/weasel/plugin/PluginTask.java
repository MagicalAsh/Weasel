package us.magicalash.weasel.plugin;

import lombok.Builder;
import lombok.Data;

import java.util.concurrent.Callable;

@Data
@Builder
public class PluginTask<V> {
    private String pluginName;
    private Callable<V> task;
}
