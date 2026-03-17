package org.dragon;

import org.dragon.config.config.ConfigAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(ConfigAutoConfiguration.class)
public class DragonHeadApplication {

    public static void main(String[] args) {
        SpringApplication.run(DragonHeadApplication.class, args);
    }

}
