package com.devanshbhosale.channelhub.web;

import com.devanshbhosale.channelhub.domain.Reseller;
import com.devanshbhosale.channelhub.domain.User;
import com.devanshbhosale.channelhub.domain.UserRole;
import com.devanshbhosale.channelhub.dto.LoginReq;
import com.devanshbhosale.channelhub.dto.RegisterReq;
import com.devanshbhosale.channelhub.dto.TokenRes;
import com.devanshbhosale.channelhub.exception.ApiException;
import com.devanshbhosale.channelhub.repository.ResellerRepository;
import com.devanshbhosale.channelhub.repository.UserRepository;
import com.devanshbhosale.channelhub.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
public class AuthController {

    private static final BigDecimal DEFAULT_CREDIT_LIMIT = new BigDecimal("200000.00");

    private final UserRepository userRepository;
    private final ResellerRepository resellerRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository,
                          ResellerRepository resellerRepository,
                          JwtService jwtService,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.resellerRepository = resellerRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/api/auth/register")
    public ResponseEntity<TokenRes> register(@Valid @RequestBody RegisterReq request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setRole(UserRole.RESELLER);
        user.setDemo(false);
        userRepository.save(user);

        Reseller reseller = new Reseller();
        reseller.setUserId(user.getId());
        reseller.setCompanyName(request.companyName());
        reseller.setGstin(request.gstin());
        reseller.setPhone(request.phone());
        reseller.setCreditLimit(DEFAULT_CREDIT_LIMIT);
        resellerRepository.save(reseller);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new TokenRes(jwtService.issue(user),
                new TokenRes.UserRes(user.getEmail(), user.getFullName(), user.getRole().name(), null)));
    }

    @PostMapping("/api/auth/login")
    public TokenRes login(@Valid @RequestBody LoginReq request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return new TokenRes(jwtService.issue(user),
            new TokenRes.UserRes(user.getEmail(), user.getFullName(), user.getRole().name(), user.isDemo()));
    }
}
