package models;

import com.aliyun.credentials.models.Config;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ConfigTest {
    @Test
    public void buildTest() {
        Map<String, String> map = new HashMap<>();
        map.put("accessKeyId", "test");
        Config config = Config.build(map);
        Assert.assertEquals("test", config.accessKeyId);
    }
}
