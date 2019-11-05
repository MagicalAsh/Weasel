package us.magicalash.weasel.plugin;

import lombok.Data;

import java.util.concurrent.Callable;

@Data
public class PluginTask<V> {
    private String pluginName;
    private Callable<V> task;
}
