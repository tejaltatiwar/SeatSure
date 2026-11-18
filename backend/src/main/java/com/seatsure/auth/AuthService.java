package com.seatsure.auth;

import com.seatsure.auth.dto.AuthResponse;
import com.seatsure.auth.dto.LoginRequest;
import com.seatsure.auth.dto.RegisterRequest;
import com.seatsure.auth.security.AuthenticatedUser;
import com.seatsure.auth.security.InvalidTokenException;
import com.seatsure.auth.security.JwtService;
import com.seatsure.common.exception.ConflictException;
import com.seatsure.user.Role;
import com.seatsure.user.User;
import com.seatsure.user.UserRepository;
import com.seatsure.user.UserResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw emailAlreadyRegistered();
        }
        User user = new User(request.fullName().trim(), email, passwordEncoder.encode(request.password()), Role.CUSTOMER);
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException raceOnUniqueEmail) {
            throw emailAlreadyRegistered();
        }
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        AuthenticatedUser identity = jwtService.parseRefreshToken(refreshToken);
        User user = userRepository.findById(identity.userId()).orElseThrow(InvalidTokenException::new);
        return issueTokens(user);
    }

    private AuthResponse issueTokens(User user) {
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                jwtService.accessTokenTtlSeconds(),
                UserResponse.from(user));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private static ConflictException emailAlreadyRegistered() {
        return new ConflictException("EMAIL_ALREADY_REGISTERED", "An account with this email already exists");
    }
}
