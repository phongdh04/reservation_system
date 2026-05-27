package com.example.qlnh.helpers;

import com.example.qlnh.models.entities.User;
import com.example.qlnh.services.interfaces.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserResolver {

    private final IUserService userService;

    public User resolve() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        return userService.getUserByEmail(auth.getName());
    }

    public User resolve(Authentication auth) {
        if (auth == null || auth.getName() == null) return null;
        return userService.getUserByEmail(auth.getName());
    }
}
