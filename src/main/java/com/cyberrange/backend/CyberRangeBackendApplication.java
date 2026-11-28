package com.cyberrange.backend;

import com.cyberrange.backend.runtime.ManagedRuntime;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CyberRangeBackendApplication {

    public static void main(String[] args) {
        var application = new SpringApplication(CyberRangeBackendApplication.class);
        ManagedRuntime.configure(application);
        application.run(args);
    }
}
