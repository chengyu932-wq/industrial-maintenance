package com.cq.maintenance.security;

import com.cq.maintenance.system.entity.SysUser;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseAuthenticationProvider implements AuthenticationProvider {
    private final AuthMapper authMapper;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;

    public DatabaseAuthenticationProvider(AuthMapper authMapper, CurrentUserService currentUserService,
                                          PasswordEncoder passwordEncoder) {
        this.authMapper = authMapper;
        this.currentUserService = currentUserService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName().trim();
        SysUser user = authMapper.findByUsername(username);
        if (user == null || !passwordEncoder.matches(String.valueOf(authentication.getCredentials()), user.getPasswordHash())) {
            throw new BadCredentialsException("Bad credentials");
        }
        if (!"ENABLED".equals(user.getStatus())) throw new DisabledException("Account disabled");
        LoginUser loginUser = currentUserService.load(user.getId());
        return UsernamePasswordAuthenticationToken.authenticated(loginUser, null, loginUser.authorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
