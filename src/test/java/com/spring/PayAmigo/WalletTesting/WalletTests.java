package com.spring.PayAmigo.WalletTesting;

import com.spring.PayAmigo.entities.Wallet;
import com.spring.PayAmigo.entities.WalletDTO;
import com.spring.PayAmigo.services.TransactionService;
import com.spring.PayAmigo.services.UserService;
import com.spring.PayAmigo.services.WalletService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.util.AssertionErrors.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(locations = "classpath:application.yaml")
@AutoConfigureMockMvc
public class WalletTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private WalletService walletService;
    @Autowired
    private UserService userService;
    @Autowired
    private TransactionService transactionService;

    @Test
    void createWallet () throws Exception {
        String jsonContent = "{"
                + "\"name\": \"johnny_wallet\","
                + "\"balance\": 9000.22,"
                + "\"user_id\": 52"
                + "}";

        MvcResult result = mockMvc.perform(post("/api/add_wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andReturn();

        int httpStatus = result.getResponse().getStatus();
        assertEquals(httpStatus, 201);
    }
    @Test
    void getWalletByName () {
        Wallet wallet = walletService.getWalletByName("vasile_wallet");
        assertEquals("vasile_wallet", wallet.getName());
    }


    @Test
    void createWalletWithAlreadyExistentName () throws Exception {
        String jsonContent = "{"
                + "\"name\": \"vasile_wallet\","
                + "\"balance\": 100.22,"
                + "\"user_id\": 102"
                + "}";

        try {
            MvcResult result = mockMvc.perform(post("/api/add_wallet")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonContent))
                    .andReturn();
            fail("No ServletException");
            //A test that should throw an ServletException because the name already exists in the database
        }catch (ServletException e) {
            System.out.println("NAME ALREADY EXISTS");
            e.getStackTrace();
        }
    }

    //Creating a wallet assigned to a user of which ID doesn't exist
    //It should return BAD REQUEST (400)
    @Test
    void createWalletAssociatedWithNonExistentUser() throws Exception {
        String jsonContent = "{"
                + "\"name\": \"John\","
                + "\"balance\": 100.22,"
                + "\"user_id\": 27"
                + "}";

        MvcResult result = mockMvc.perform(post("/api/add_wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andReturn();

        assertEquals("The userID that is assign to this wallet doesn't exist", result.getResponse().getContentAsString());
    }
    @Test
    void getAllWalletsByUserId () {

//		Get the ID for the user "Vasile" that we already created
        Long user_id = userService.getIdByName("Vasile");

//		Create a fist wallet for "Vasile"
        WalletDTO walletDTO1 = new WalletDTO();
        walletDTO1.setUser_id(user_id);
        walletDTO1.setName("vasile_wallet");
        walletDTO1.setBalance(230.0);
        Wallet wallet1 = walletService.addWallet(walletDTO1);

//		Create the second wallet for "Vasile"
        WalletDTO walletDTO2 = new WalletDTO();
        walletDTO2.setUser_id(user_id);
        walletDTO2.setName("vasile_wallet2");
        walletDTO2.setBalance(230.0);
        Wallet wallet2 = walletService.addWallet(walletDTO2);

//		Concatenate these 2 wallets into a list
        List<Wallet> wallets = Arrays.asList(wallet1, wallet2);
//		System.out.println(wallets);

//		Create a list with the wallets Ids
        List<Long> walletsIds = new ArrayList<>();
        for (Wallet wallet : wallets) {
            walletsIds.add(wallet.getId());
        }

//		Get from the database all the wallets assigned to Vasile
        List<Wallet> wallets_from_db = walletService.findByUserId(user_id);
//		System.out.println(wallets_from_db);
//		Create a list with the wallets Ids from db
        List<Long> wallets_from_dbIds = new ArrayList<>();
        for (Wallet wallet : wallets_from_db) {
            wallets_from_dbIds.add(wallet.getId());
        }

        assertArrayEquals(walletsIds.stream().mapToLong(l -> l).toArray(), wallets_from_dbIds.stream().mapToLong(l -> l).toArray());

    }

    @Test
    void getEmptyWallets () {
        WalletDTO walletDTO1 = new WalletDTO();
        walletDTO1.setUser_id(1L);
        walletDTO1.setName("empty_wallet");
        walletDTO1.setBalance(0.0);
        Wallet wallet1 = walletService.addWallet(walletDTO1);

        List<Wallet> wallets_from_db = walletService.getEmptyWallets();

        List<Wallet> wallets = Arrays.asList(wallet1);

        List<Long> walletsIds = new ArrayList<>();
        for (Wallet wallet : wallets) {
            walletsIds.add(wallet.getId());
        }

        List<Long> wallets_from_dbIds = new ArrayList<>();
        for (Wallet wallet : wallets_from_db) {
            wallets_from_dbIds.add(wallet.getId());

        }
        assertArrayEquals(walletsIds.stream().mapToLong(l -> l).toArray(), wallets_from_dbIds.stream().mapToLong(l -> l).toArray());
    }

    @Test
    void addMoney () throws Exception {
        Double value = 5.0;
        Double initial_balance = walletService.getById(1L).getBalance();
        walletService.addMoney(1L, value);
        Double after_add = walletService.getById(1L).getBalance();

        assertEquals(initial_balance + value, after_add);
    }

    @Test
    void withdrawMoney () throws Exception {
        Double value = 5.0;
        Double initial_balance = walletService.getById(1L).getBalance();
        walletService.withdrawMoney(1L, value);
        Double after_add = walletService.getById(1L).getBalance();

        assertEquals(initial_balance - value, after_add);
    }

    @Test
    void withdrawMoreMoneyThanYouHave () throws Exception {
        MvcResult result = mockMvc.perform(post("/api/wallets/withdraw_money")
                        .param("value", "1000000.0")
                        .param("id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        assertEquals(result.getResponse().getContentAsString(), "Insufficient funds");
    }


    @Test
    void addMoneyNegativeAmount () throws Exception {
        MvcResult result = mockMvc.perform(post("/api/wallets/add_money")
                        .param("value", "-10.0")
                        .param("id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        assertEquals(result.getResponse().getContentAsString(), "No negative amounts");
    }

}
