package com.jotirling.controller;
import com.jotirling.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.util.*;
@RestController @RequestMapping("/api") public class PaymentClaimController {
 private final AuthService auth;private final PaymentClaimService service;
 public PaymentClaimController(AuthService a,PaymentClaimService s){auth=a;service=s;}
 @GetMapping("/orders/{id}/payment-submissions") public Object list(@PathVariable("id") Long id,HttpServletRequest r){return service.list(auth.current(r),id);}
 @GetMapping("/admin/payment-submissions") public Object all(HttpServletRequest r){return service.all(auth.admin(r));}
 // Legacy order submissions retained for reviewing existing entries; new submissions use simple-payments.
 public Object submit(@PathVariable("id") Long id,@Valid @RequestPart("details") PaymentClaimService.Input in,@RequestPart("file") MultipartFile file,HttpServletRequest r)throws IOException{
 var customer=auth.current(r);if(file.isEmpty()||file.getSize()>2*1024*1024)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a JPG/PNG screenshot up to 2 MB");
 byte[] bytes=file.getBytes();String type;
 try(ImageInputStream stream=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){var readers=ImageIO.getImageReaders(stream);if(!readers.hasNext())throw new IOException();ImageReader reader=readers.next();try{reader.setInput(stream);String f=reader.getFormatName().toLowerCase(Locale.ROOT);int w=reader.getWidth(0),h=reader.getHeight(0);if(!Set.of("png","jpeg","jpg").contains(f)||w<1||h<1||w>4096||h>4096||(long)w*h>12000000||reader.read(0)==null)throw new IOException();type=f.equals("png")?"image/png":"image/jpeg";}finally{reader.dispose();}}catch(IOException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use a valid JPG/PNG screenshot (max 4096 pixels / 12 MP)");}
 return service.submit(customer,id,in,bytes,type);
 }
 @PostMapping("/admin/payment-submissions/{id}/review") public Object review(@PathVariable("id") Long id,@Valid @RequestBody PaymentClaimService.Review in,HttpServletRequest r){return service.review(auth.admin(r),id,in);}
 @GetMapping("/payment-submissions/{id}/image") @Transactional(readOnly=true) public ResponseEntity<byte[]> image(@PathVariable("id") Long id,HttpServletRequest r){var c=service.image(auth.current(r),id);return ResponseEntity.ok().contentType(MediaType.parseMediaType(c.mediaType)).header("X-Content-Type-Options","nosniff").header("Cache-Control","no-store").body(c.screenshot);}
}
