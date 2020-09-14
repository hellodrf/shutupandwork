package com.cervidae.cbookings;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan
@SpringBootApplication
public class CbookingsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CbookingsApplication.class, args);
    }

}
