package com.example.common.session;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.jsonwebtoken.Claims;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Data
public class UserSession implements UserDetails {
    private Long userId;
    private String userName;
    private List<String> roles;
    private String state;
    private Collection<? extends GrantedAuthority> authorities;

    public UserSession(Claims claims, Collection<? extends GrantedAuthority> authorities) {
        Gson gson = new Gson();
        this.userId = claims.get("userId", Long.class);
        this.userName = claims.get("userName", String.class);
        String role = claims.get("roles", String.class);
        this.roles = gson.fromJson(role, new TypeToken<List<String>>(){}.getType());
        this.state = claims.get("state", String.class);
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return userName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return Objects.equals(state, "1");
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
