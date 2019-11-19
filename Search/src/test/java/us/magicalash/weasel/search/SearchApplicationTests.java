package us.magicalash.weasel.search;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SearchApplicationTests {

    @Test
    public void contextLoads() {
    }

    // TODO: figure out how to test this
    // since elasticsearch's client can't be mocked, we can't properly search this  without a
    // live elasticsearch instance. The options are either to require an elasticsearch instance
    // in order to test, or run one inside the unit tests somehow. Neither option is nice. :(
}
