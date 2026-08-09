package com.yan.campuspass;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan({
        "com.yan.campuspass.activity.mapper",
        "com.yan.campuspass.user.mapper",
        "com.yan.campuspass.registration.mapper",
        "com.yan.campuspass.waitlist.mapper",
        "com.yan.campuspass.notification.mapper",
        "com.yan.campuspass.checkin.mapper"
})
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
public class CampusPassApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusPassApplication.class, args);
    }
}
