package com.techelevator.tenmo.controller;


import com.techelevator.tenmo.dao.UserDao;
import com.techelevator.tenmo.model.Transfer;
import com.techelevator.tenmo.model.TransferHistoryDTO;
import com.techelevator.tenmo.model.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

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

    @GetMapping(path = "accounts")
    public List<User> getUsers() {
        return userDao.findAll();
    }

    @PutMapping(path = "transfer/{toId}")
    public Transfer send(@RequestBody Transfer transfer, @PathVariable long toId) {
        return userDao.send(transfer, toId);
    }
    @GetMapping(path = "history/{id}")
    public TransferHistoryDTO getHistory(@PathVariable long id) {
        return userDao.getHistory(id);
    }
}
