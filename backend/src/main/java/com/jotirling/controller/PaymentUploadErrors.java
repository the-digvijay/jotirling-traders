package com.jotirling.controller;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
@RestControllerAdvice public class PaymentUploadErrors {
 @ExceptionHandler(MaxUploadSizeExceededException.class) public ResponseEntity<?> size(MaxUploadSizeExceededException e){return ResponseEntity.status(413).body(Map.of("message","Choose a JPG/PNG screenshot or image up to 2 MB"));}
}
