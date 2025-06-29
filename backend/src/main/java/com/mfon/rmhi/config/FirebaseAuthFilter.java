package com.mfon.rmhi.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class FirebaseAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest req,
            HttpServletResponse res,
            FilterChain chain) throws IOException, ServletException {

        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String idToken = header.substring(7);
            try {
                FirebaseAuth.getInstance().verifyIdToken(idToken, true);
            } catch (FirebaseAuthException e) {
                res.setStatus(HttpStatus.UNAUTHORIZED.value());
                return;
            }
        }
        chain.doFilter(req, res);
    }
}
