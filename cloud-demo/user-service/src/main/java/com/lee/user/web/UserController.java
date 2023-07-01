package com.lee.user.web;

import com.lee.user.config.PatternProperties;
import com.lee.user.pojo.User;
import com.lee.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequestMapping("/user")
//@RefreshScope
public class UserController {

    @Autowired
    private UserService userService;

//    @Value("${pattern.dateformat}")
//    private String dateformat;

    @Autowired
    private PatternProperties patternProperties;

    @GetMapping("now")
    public String now() {
//        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(dateformat));
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(patternProperties.getDateformat()));
    }

    @GetMapping("prop")
    public PatternProperties prop() {
        return patternProperties;
    }

    /**
     * 路径： /user/110
     *
     * @param id 用户id
     * @return 用户
     */
    @GetMapping("/{id}")
    public User queryById(@PathVariable("id") Long id,
                          @RequestHeader(value = "truth", required = false) String truth) {
        System.out.println("请求头truth:" + truth);
        return userService.queryById(id);
    }
}
