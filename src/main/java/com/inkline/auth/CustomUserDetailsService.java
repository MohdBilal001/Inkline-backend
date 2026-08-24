package com.inkline.auth;

import com.inkline.user.User;
import com.inkline.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public CustomUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found: " + email
                        )
                );

        return new UserPrincipal(user);
    }
}