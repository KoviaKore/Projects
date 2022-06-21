package com.techelevator.tenmo.dao;

import com.techelevator.tenmo.model.Transfer;
import com.techelevator.tenmo.model.TransferDTO;
import com.techelevator.tenmo.model.User;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

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
        int accountFrom = 0;
        int accountTo = 0;

        String sql = "UPDATE account set balance = ? where user_id = ?;";
        int complete = jdbcTemplate.update(sql, newBalance, transfer.getTransfer_id());
        if (complete == 1) {
            String sqlFrom = "Select account_id from account where user_id = ?;";
            try {
                accountFrom = jdbcTemplate.queryForObject(sqlFrom, int.class, transfer.getTransfer_id());
            } catch (DataAccessException | NullPointerException e) {
                System.out.println("Error in retrieval of account id");
            }
            String sqlSend = "UPDATE account set balance = balance + ? where user_id = ?;";
            int success = jdbcTemplate.update(sqlSend, transfer.getBalance(), toId);
            if (success == 1) {
                String sqlTo = "Select account_id from account where user_id = ?;";

                try {
                    accountTo = jdbcTemplate.queryForObject(sqlTo, int.class,(int) toId);
                } catch (DataAccessException | NullPointerException e) {
                    System.out.println("Error in retrieval of account id"); }

                String sqlTransfer = "INSERT into transfer(transfer_type_id, transfer_status_id, account_from, account_to, amount) " +
                        "VALUES ((Select transfer_type_id from transfer_type where transfer_type_id = ?), " +
                        "(Select transfer_status_id from transfer_status where transfer_status_id = ?), " +
                        "(Select account_id from account where account_id = ?), " +
                        "(Select account_id from account where account_id = ?), " +
                        "?) Returning transfer_id";

                try {
                    int transferId = jdbcTemplate.queryForObject(sqlTransfer, int.class, 2, 2, accountFrom, accountTo, transfer.getBalance());
                    transfer.setTransfer_id(transferId);
                } catch (NullPointerException e) {
                    System.out.println("Failed to post transfer information");
                }
                return transfer;
            } else {
                throw new IllegalArgumentException("Failed sending transaction");
            }
        } else {
            throw new IllegalArgumentException("Failed starting transaction");
        }
    }

    @Override
    public List<TransferDTO> getHistory(long id) {
        List<TransferDTO> list = new ArrayList<>();
        String sqlOtherUser = "select tr.transfer_id, " +
                "fu.username as account_from, " +
                "tu.username as account_to, " +
                "t.transfer_type_desc as transfer_type, " +
                "s.transfer_status_desc as transfer_status, " +
                "amount as amount " +
                "from transfer tr " +
                "join account fr on tr.account_from = fr.account_id " +
                "join account re on tr.account_to = re.account_id " +
                "join tenmo_user fu on fr.user_id = fu.user_id " +
                "join tenmo_user tu on re.user_id = tu.user_id " +
                "join transfer_type t using (transfer_type_id) " +
                "join transfer_status s using (transfer_status_id) " +
                "where (account_from in (select account_id from account where user_id = ?)) " +
                "or (account_to in (select account_id from account where user_id = ?)); ";

            SqlRowSet results = jdbcTemplate.queryForRowSet(sqlOtherUser, id, id);
            while (results.next()) {
                TransferDTO listDTO = mapRowToTransferDTO(results);
                list.add(listDTO);
            }return list;
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
    private TransferDTO mapRowToTransferDTO(SqlRowSet rs) {
        TransferDTO transfer = new TransferDTO();
        transfer.setFromName(rs.getString("account_from"));
        transfer.setToName(rs.getString("account_to"));
        transfer.setTransferId(rs.getInt("transfer_id"));
        transfer.setTransactionStatus(rs.getString("transfer_status"));
        transfer.setTransactionType(rs.getString("transfer_type"));
        transfer.setAmount(rs.getBigDecimal("amount"));
        return transfer;
    }



}
