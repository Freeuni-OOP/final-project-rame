package com.serialtracker.backend;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:5173") // უფლებას ვაძლევთ მხოლოდ ჩვენს ფრონტენდს
public class HelloController {

    @GetMapping("/api/hello")
    public String sayHello() {
        return "გამარჯობა ფრონტენდო, მე ბექენდი ვარ!";
    }
}