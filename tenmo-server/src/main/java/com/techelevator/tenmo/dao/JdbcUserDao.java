package com.techelevator.tenmo.dao;

import com.techelevator.tenmo.model.Transfer;
import com.techelevator.tenmo.model.User;
import org.jboss.logging.BasicLogger;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class JdbcUserDao implements UserDao {

    private static final BigDecimal STARTING_BALANCE = new BigDecimal("1000.00");
    private JdbcTemplate jdbcTemplate;

    public JdbcUserDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int findIdByUsername(String username) {
        String sql = "SELECT user_id FROM tenmo_user WHERE username ILIKE ?;";
        Integer id = jdbcTemplate.queryForObject(sql, Integer.class, username);
        if (id != null) {
            return id;
        } else {
            return -1;
        }
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id, username, password_hash FROM tenmo_user;";
        SqlRowSet results = jdbcTemplate.queryForRowSet(sql);
        while(results.next()) {
            User user = mapRowToUser(results);
            users.add(user);
        }
        return users;
    }

    public BigDecimal viewCurrentBalance(long id) {
        BigDecimal balance = null;
        String sql = "Select balance from account " +
                "JOIN tenmo_user t using (user_id) " +
                "where t.user_id = ?;";
        try {
         balance = jdbcTemplate.queryForObject(sql, BigDecimal.class, id);

        } catch (DataAccessException  | NullPointerException e) {
            System.out.println("Error accessing database");
        }
        return balance;
    }

    @Override
    public Transfer send(Transfer transfer, long toId) {

        BigDecimal balance = viewCurrentBalance(transfer.getTransfer_id());
        // checks if amount to send is greater than balance in account
        if (balance.subtract(transfer.getBalance()).compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Insufficient funds");

            BigDecimal newBalance = balance.subtract(transfer.getBalance());

            String sql = "UPDATE account set balance = ? where user_id = ?;";
        int success = jdbcTemplate.update(sql, newBalance, transfer.getTransfer_id());
        if (success == 1) {
            String sqlTransfer = "Select user_id, username, y.transfer_type_desc, s.transfer_status_desc, a.balance " +
                    "From tenmo_user " +
                    "Join account a using (user_id) " +
                    "Join transfer t on a.account_id = t.account_to " +
                    "Join transfer_status s using (transfer_status_id) " +
                    "join transfer_type y using (transfer_type_id) " +
                    "where user_id = ?";

            SqlRowSet results = jdbcTemplate.queryForRowSet(sqlTransfer, toId);
            while (results.next()) {
                transfer = mapRowToTransfer(results);
            }
            String sqlSend = "UPDATE account set balance = balance + ? where user_id = ?;";
            int send = jdbcTemplate.update(sqlSend, transfer.getBalance(), toId);
            if (send == 1) {
                return transfer;
            } else {throw new IllegalArgumentException("Failed balance update sql");}

        } else { throw new IllegalArgumentException("Failed transfer sql");}


    }

    @Override
    public User findByUsername(String username) throws UsernameNotFoundException {
        String sql = "SELECT user_id, username, password_hash FROM tenmo_user WHERE username ILIKE ?;";
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql, username);
        if (rowSet.next()){
            return mapRowToUser(rowSet);
        }
        throw new UsernameNotFoundException("User " + username + " was not found.");
    }

    @Override
    public boolean create(String username, String password) {

        // create user
        String sql = "INSERT INTO tenmo_user (username, password_hash) VALUES (?, ?) RETURNING user_id";
        String password_hash = new BCryptPasswordEncoder().encode(password);
        Integer newUserId;
        try {
            newUserId = jdbcTemplate.queryForObject(sql, Integer.class, username, password_hash);
        } catch (DataAccessException e) {
            return false;
        }

        // create account
        sql = "INSERT INTO account (user_id, balance) values(?, ?)";
        try {
            jdbcTemplate.update(sql, newUserId, STARTING_BALANCE);
        } catch (DataAccessException e) {
            return false;
        }

        return true;
    }

    private User mapRowToUser(SqlRowSet rs) {
        User user = new User();
        user.setId(rs.getLong("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password_hash"));
        user.setActivated(true);
        user.setAuthorities("USER");
        return user;
    }
    private Transfer mapRowToTransfer(SqlRowSet rs) {
        Transfer transfer = new Transfer();
        transfer.setUsername(rs.getString("username"));
        transfer.setTransfer_id(rs.getInt("transfer_id"));
        transfer.setTransfer_status_desc(rs.getString("transfer_status_desc"));
        transfer.setTransfer_type_desc(rs.getString("transfer_type_desc"));
        transfer.setBalance(rs.getBigDecimal("balance"));
        return transfer;
    }
}
