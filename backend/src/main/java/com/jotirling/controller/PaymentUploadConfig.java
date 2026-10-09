package com.jotirling.controller;
import jakarta.servlet.MultipartConfigElement;
import org.springframework.context.annotation.*;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.util.unit.DataSize;
@Configuration public class PaymentUploadConfig {
 @Bean public MultipartConfigElement multipartConfigElement(){MultipartConfigFactory f=new MultipartConfigFactory();f.setMaxFileSize(DataSize.ofMegabytes(2));f.setMaxRequestSize(DataSize.ofMegabytes(3));return f.createMultipartConfig();}
}
