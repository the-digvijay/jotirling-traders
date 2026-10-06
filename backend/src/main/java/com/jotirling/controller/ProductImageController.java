package com.jotirling.controller;
import com.jotirling.entity.*;
import com.jotirling.repository.*;
import com.jotirling.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.util.*;
@RestController @RequestMapping("/api")
public class ProductImageController {
 private final AuthService auth; private final ProductRepository products; private final ProductImageRepository images;
 public ProductImageController(AuthService a,ProductRepository p,ProductImageRepository i){auth=a;products=p;images=i;}
 private Product product(Long id){return products.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found"));}
 @PostMapping("/admin/products/{id}/image") @Transactional
 public Map<String,String> upload(@PathVariable Long id,@RequestParam("file") MultipartFile file,HttpServletRequest req) throws IOException {
  auth.admin(req); Product p=product(id);
  if(file.isEmpty()||file.getSize()>2*1024*1024) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a JPG or PNG image up to 2 MB");
  byte[] bytes=file.getBytes(); String type;
  try(ImageInputStream stream=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){
   var readers=ImageIO.getImageReaders(stream);
   if(!readers.hasNext())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid image. Use JPG or PNG");
   ImageReader reader=readers.next();
   try{
    reader.setInput(stream);String format=reader.getFormatName().toLowerCase(Locale.ROOT);
    if(!Set.of("jpeg","jpg","png").contains(format))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use JPG or PNG only");
    int w=reader.getWidth(0),h=reader.getHeight(0);
    if(w<1||h<1||w>4096||h>4096||(long)w*h>12000000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Image too large. Maximum 4096 pixels and 12 megapixels");
    if(reader.read(0)==null)throw new IOException("Invalid image");
    type=format.equals("png")?"image/png":"image/jpeg";
   }finally{reader.dispose();}
  }catch(IOException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cannot read image. Use a valid JPG or PNG");}
  ProductImage image=new ProductImage();image.id=id;image.data=bytes;image.mediaType=type;images.save(image);
  p.imageVersion=UUID.randomUUID().toString();products.save(p);
  return Map.of("imageUrl","/products/"+id+"/image?v="+p.imageVersion);
 }
 @GetMapping("/products/{id}/image") @Transactional(readOnly=true)
 public ResponseEntity<byte[]> show(@PathVariable Long id,HttpServletRequest req){
  Product p=product(id);if(!p.active)auth.admin(req);
  ProductImage image=images.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Image not found"));
  return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.mediaType)).header("X-Content-Type-Options","nosniff").header("Cache-Control","no-cache").body(image.data);
 }
}
