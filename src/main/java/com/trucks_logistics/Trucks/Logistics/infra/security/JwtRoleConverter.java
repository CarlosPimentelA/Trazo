package com.trucks_logistics.Trucks.Logistics.infra.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class JwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        String role = jwt.getClaim("role");
        if (role == null)
            return List.of();
        return List.of(new SimpleGrantedAuthority(role));
    }

}