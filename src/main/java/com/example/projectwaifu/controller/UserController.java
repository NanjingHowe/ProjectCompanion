package com.example.projectwaifu.controller;

import com.example.projectwaifu.models.UserData;
import com.example.projectwaifu.other.UserManager;
import com.example.projectwaifu.repositories.UserDataRepository;
import com.example.projectwaifu.security.CustomUserDetails;
import com.example.projectwaifu.repositories.UserRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.example.projectwaifu.other.SecurityMethods.validatePassword;

@RestController
@CrossOrigin(origins = {"http://localhost:5173"}, allowedHeaders = "*", allowCredentials = "true")
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserDataRepository userDataRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    SecurityContextRepository securityContextRepository;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserManager userManager;

    @GetMapping("/history")
    public List<Map<String, Object>> getUserHistory() {
        CustomUserDetails currUser = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.getChatLog(currUser.getId());
    }

    @GetMapping("/info")
    public Map<String, String> getUserInfo() {
        CustomUserDetails currUser = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Map.ofEntries(Map.entry("email", currUser.getEmail()), Map.entry("username", currUser.getUsername()));
    }

    @GetMapping("/coins")
    public Map<String, Object> getUserCoins() {
        CustomUserDetails currUser = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Map.of("coins", userDataRepository.getCoins(currUser.getId()));
    }


    @GetMapping("/profile")
    public Map<String, Object> getUserProfile() {
        CustomUserDetails currUser = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserData userData = userDataRepository.getUserData(currUser.getId());
        int totalMessages = userDataRepository.getUserTotalMessages(currUser.getId());
        return Map.ofEntries(Map.entry("profile_pic", userData.getProfile_pic()), Map.entry("description", userData.getDescription()), Map.entry("tag", userData.getTag()), Map.entry("total_messages", totalMessages));
    }

    @PostMapping("/change_username")
    public ResponseEntity<Object> changeUsername(@RequestBody Map<String, String> userInfo, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        CustomUserDetails currUser = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String newUsername = userInfo.get("username");

        if (newUsername == null) {
            return new ResponseEntity<>(Map.of("message", "No username provided"), HttpStatus.BAD_REQUEST);
        }

        if (userRepository.findByUsername(newUsername) != null) {
            return new ResponseEntity<>(Map.of("message", "Username already exists"), HttpStatus.BAD_REQUEST);
        }
        currUser.setUsername(newUsername);
        userRepository.changeUsername(newUsername, currUser.getEmail());
        userManager.updateContext(currUser.getEmail(), currUser.getPassword(), httpServletRequest, httpServletResponse, true);

        return new ResponseEntity<>(Map.of("message", "Username changed"), HttpStatus.OK);
    }

    @PostMapping("/change_password")
    public ResponseEntity<Object> changePassword(@RequestBody Map<String, String> userInfo, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        CustomUserDetails currUser = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String oldPassword = userInfo.get("oldpassword");
        String newPassword = userInfo.get("newpassword");

        if (oldPassword == null || newPassword == null) {
            return new ResponseEntity<>(Map.of("message", "Improper request body"), HttpStatus.BAD_REQUEST);
        } else if (!passwordEncoder.matches(oldPassword, currUser.getPassword())) {
            return new ResponseEntity<>(Map.of("message", "Incorrect old password"), HttpStatus.BAD_REQUEST);
        } else if (!validatePassword(newPassword)) {
            return new ResponseEntity<>(Map.of("message", "Invalid new password"), HttpStatus.BAD_REQUEST);
        }

        String encodedNewPassword = passwordEncoder.encode(newPassword);

        currUser.setPassword(newPassword);
        userRepository.changePassword(encodedNewPassword, currUser.getEmail());
        userManager.updateContext(currUser.getEmail(), newPassword, httpServletRequest, httpServletResponse, false);

        return new ResponseEntity<>(Map.of("message", "Password changed"), HttpStatus.OK);
    }



}
