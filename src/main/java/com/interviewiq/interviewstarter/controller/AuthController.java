package com.interviewiq.interviewstarter.controller;

import com.interviewiq.interviewstarter.dto.AuthDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.interviewiq.interviewstarter.service.AuthSerivce;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthSerivce authservice;

    public AuthController(AuthSerivce authService){
        this.authservice = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid  @RequestBody RegisterRequest request){
        String message = authservice.register(request.getName(), request.getEmail(), request.getPassword());
        if( message.equals("User registered successfully")){
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(new ApiResponse(true,message));
        };
        return  ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiResponse(false,message));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request){
        String token = authservice.login(
                request.getEmail(),
                request.getPassword()
        );
        return  ResponseEntity
                .status(HttpStatus.OK)
                .body(new LoginResponse(token));

    }

}
