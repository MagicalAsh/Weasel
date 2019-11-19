package us.magicalash.weasel.common.dummyplugin;

import java.util.Properties;

public class TestPlugin implements ITestPlugin {
    @Override
    public String getName() {
        return "Test plugin";
    }

    @Override
    public String[] requestProperties() {
        return new String[0];
    }

    @Override
    public void load(Properties properties) {

    }
}
