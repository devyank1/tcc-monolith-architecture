package com.yankdev.brtickets;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = "jwt.secret=VwjZLRV0UswHzhavZnR1hHCCNnyIQAvz+AOaCTYWfbc=")
@Import(TestContainersConfiguration.class)
class BrticketsApplicationTests {

    @Test
    void contextLoads() {
    }

}
