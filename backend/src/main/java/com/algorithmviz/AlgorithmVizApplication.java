package com.algorithmviz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AlgorithmVizApplication {
    public static void main(String[] args) {
        SpringApplication.run(AlgorithmVizApplication.class, args);
    }
}
