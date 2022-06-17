package com.techelevator.tenmo.controller;


import com.techelevator.tenmo.dao.UserDao;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("account/")
@PreAuthorize("isAuthenticated()")
public class AccountController {

    private UserDao userDao;

    public AccountController(UserDao userDao) {
        this.userDao = userDao;
    }

    @GetMapping(path = "{id}/balance")
    public BigDecimal viewCurrentBalance(@PathVariable long id) {
        return userDao.viewCurrentBalance(id);
    }

    @PutMapping(path = "transferTo/{toId}", consumes = "application/json", produces = "application/json")
    public boolean send(@RequestBody BigDecimal amount, long user_id,  @PathVariable long toId) {
        return userDao.send(amount, user_id, toId);
    }
}
