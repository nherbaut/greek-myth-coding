package top.nextnet.greekmythcoding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.shell.command.annotation.CommandScan;

@SpringBootApplication
@CommandScan(value = "top.nextnet.greekmythcoding.cmd")
public class GreekMythCodingApplication {

    public static void main(String[] args) {
        SpringApplication.run(GreekMythCodingApplication.class, args);
    }

}
