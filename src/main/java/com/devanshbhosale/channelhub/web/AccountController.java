package com.devanshbhosale.channelhub.web;

import com.devanshbhosale.channelhub.domain.User;
import com.devanshbhosale.channelhub.dto.AccountRes;
import com.devanshbhosale.channelhub.service.AccountService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/api/account")
    public AccountRes account(@AuthenticationPrincipal User user) {
        return accountService.load(user.getId());
    }
}
