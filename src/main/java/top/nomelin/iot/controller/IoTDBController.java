package top.nomelin.iot.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ApacheIotDBController
 *
 * @author nomelin
 * @since 2024/12/11 21:59
 **/
@RestController
@RequestMapping("/iotdb")
public class IoTDBController {
    @RequestMapping("")
    public String hello() {
        return "Hello, IoTDB!";
    }

}
