package com.microservices.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@RestController
public class UserJPAResource {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/users")
    public List<User> retrieveAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/count")
    public Integer getCount() {
        return userRepository.findAll().toArray().length;
    }
    
    @GetMapping("/users/{id}")
    public EntityModel<User> retrieveUser(@PathVariable int id) {
        Optional<User> user = userRepository.findById(id);
        
        if(!user.isPresent())
            throw new UserNotFoundException("id-"+id);
        
        EntityModel<User> resource = new EntityModel<User>(user.get()); 
        WebMvcLinkBuilder linkTo = linkTo(methodOn(this.getClass()).retrieveAllUsers());
        resource.add(linkTo.withRel("all-users"));
        return resource;
    }

    @PostMapping("/users")
    public ResponseEntity<Object> createUserSource(@Valid @RequestBody User user) {
        User savedUser = userRepository.save(user);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedUser.getId())
                .toUri();
        return ResponseEntity.created(location).build();
    }

    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable int id) {
        userRepository.deleteById(id);
    }
    
    @PutMapping("/users/{id}")
    Optional<Object> replaceEmployee(@RequestBody User newUser, @PathVariable Integer id) {
      
      return userRepository.findById(id)
        .map(user -> {
        	user.setName(newUser.getName());
        	user.setBirthDate(newUser.getBirthDate());
          return userRepository.save(user);
        });
//        .orElseGet(() -> {
//        	newUser.setId(id);
//          return userRepository.save(newUser);
//        });
    }

    @RequestMapping(value = "/upload", method = RequestMethod.POST, consumes = { "multipart/form-data" })
    public void upload(@RequestPart("file") @Valid @NotNull @NotBlank MultipartFile file) throws IOException {
        System.out.println("Uploaded File: ");
        System.out.println("Name : " + file.getName());
        System.out.println("Type : " + file.getContentType());
        System.out.println("Name : " + file.getOriginalFilename());
        System.out.println("Size : " + file.getSize());

        File convFile = new File(System.getProperty("java.io.tmpdir")+"/"+file.getOriginalFilename());
        file.transferTo(convFile);
        String content = Files.readString(Path.of(convFile.getPath()), StandardCharsets.US_ASCII);
        ObjectMapper objectMapper = new ObjectMapper();
        User[] user = objectMapper.readValue(content, User[].class);
        for(int i = 0; i<user.length; i++)
            userRepository.save(user[i]);
    }
}
