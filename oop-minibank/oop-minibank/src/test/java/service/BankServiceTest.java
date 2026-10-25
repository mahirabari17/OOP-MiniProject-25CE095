package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import exception.AccountNotFoundException;
import exception.InsufficientFundsException;
import exception.ValidationException;
import model.Account;
import model.Customer;
import model.SavingsAccount;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class BankServiceTest {

    private BankService newBank() throws Exception {
        return new BankService(Files.createTempDirectory("minibank-test"));
    }

    @Test
    void openDepositWithdraw() throws Exception {
        BankService bank = newBank();
        Account a = bank.openAccount("Riya", "riya@example.com", "9876543210", "SAVINGS", 5000);
        assertEquals(7000, bank.deposit(a.getAccountNumber(), 2000));
        assertEquals(4000, bank.withdraw(a.getAccountNumber(), 3000));
    }

    @Test
    void withdrawingTooMuchThrows() throws Exception {
        BankService bank = newBank();
        Account a = bank.openAccount("Riya", "riya@example.com", "9876543210", "SAVINGS", 1000);
        assertThrows(InsufficientFundsException.class, () -> bank.withdraw(a.getAccountNumber(), 5000));
        assertEquals(1000, a.getBalance());
    }

    @Test
    void invalidInputIsRejected() throws Exception {
        BankService bank = newBank();
        assertThrows(ValidationException.class,
                () -> bank.openAccount("Riya", "riya@example.com", "12345", "SAVINGS", 1000));
        assertThrows(ValidationException.class,
                () -> bank.openAccount("Riya", "bad-email", "9876543210", "SAVINGS", 1000));
        assertThrows(ValidationException.class,
                () -> bank.openAccount("Riya", "riya@example.com", "9876543210", "GOLD", 1000));
        assertThrows(AccountNotFoundException.class, () -> bank.deposit("AC9999", 10));
    }

    @Test
    void transferBetweenAccounts() throws Exception {
        BankService bank = newBank();
        Account a = bank.openAccount("Riya", "riya@example.com", "9876543210", "SAVINGS", 5000);
        Account b = bank.openAccount("Amit", "amit@example.com", "9123456789", "CURRENT", 0);
        bank.transfer(a.getAccountNumber(), b.getAccountNumber(), 1500);
        assertEquals(3500, a.getBalance());
        assertEquals(1500, b.getBalance());
    }

    @Test
    void dataSurvivesRestart() throws Exception {
        Path dir = Files.createTempDirectory("minibank-restart");
        BankService first = new BankService(dir);
        Account a = first.openAccount("Riya", "riya@example.com", "9876543210", "SAVINGS", 5000);
        first.deposit(a.getAccountNumber(), 250);
        first.save();

        BankService second = new BankService(dir);          // "restart"
        Account loaded = second.find(a.getAccountNumber());
        assertEquals(5250, loaded.getBalance());
        assertEquals("Riya", loaded.getOwnerName());
        assertEquals(1, second.listAccounts().size());
    }

    @Test
    void batchOnThreadPoolIsCorrect() throws Exception {
        BankService bank = newBank();
        Account a = bank.openAccount("Riya", "riya@example.com", "9876543210", "CURRENT", 0);
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            lines.add("DEPOSIT " + a.getAccountNumber() + " 10");
        }
        lines.add("DEPOSIT " + a.getAccountNumber());       // bad line
        lines.add("DEPOSIT AC9999 10");                       // unknown account
        String summary = bank.runBatch(lines);
        assertEquals(2000, a.getBalance());
        assertTrue(summary.contains("200 succeeded, 2 failed"));
    }

    @Test
    void reportMatchesLoggedTransactions() throws Exception {
        BankService bank = newBank();
        Account a = bank.openAccount("Riya", "riya@example.com", "9876543210", "SAVINGS", 5000);
        bank.deposit(a.getAccountNumber(), 500);
        bank.withdraw(a.getAccountNumber(), 200);
        String report = bank.report();
        assertTrue(report.contains("Total deposits     : 500"));
        assertTrue(report.contains("Total withdrawals  : 200"));
    }

    @Test
    void searchByOwner() throws Exception {
        BankService bank = newBank();
        bank.openAccount("Riya Shah", "riya@example.com", "9876543210", "SAVINGS", 5000);
        bank.openAccount("Amit", "amit@example.com", "9123456789", "SAVINGS", 5000);
        assertEquals(1, bank.searchByOwner("riya").size());
    }

    @Test
    void repositoryWorksForAnyTypeWithId() {
        Repository<Account> accounts = new Repository<>();
        accounts.saveAll(new SavingsAccount("AC7301", "A", 1, 0), new SavingsAccount("AC7302", "B", 2, 0));
        assertEquals("B", accounts.findById("AC7302").getOwnerName());
        assertEquals(2, accounts.findAll().size());
        assertTrue(accounts.delete("AC7301"));

        Repository<Customer> customers = new Repository<>();
        Customer c = customers.save(new Customer("Riya", "r@example.com", "9876543210"));
        assertNotNull(customers.findById(c.getCustomerId()));
        assertEquals(2.5, Repository.average(new Integer[] {1, 2, 3, 4}), 0.0001);
    }

    @Test
    void sortedByBalanceIsHighestFirst() {
        AccountStore store = new AccountStore();
        store.add(new SavingsAccount("AC7401", "A", 5000, 0));
        store.add(new SavingsAccount("AC7402", "B", 2000, 0));
        store.add(new SavingsAccount("AC7403", "C", 9000, 0));
        assertEquals(9000, store.sortedByBalance().get(0).getBalance());
        assertEquals(2000, store.sortedByBalance().get(2).getBalance());
        assertEquals("B", store.get("AC7402").getOwnerName());
    }
}
