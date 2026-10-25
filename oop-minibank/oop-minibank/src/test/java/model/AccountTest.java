package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import exception.BankException;
import exception.InsufficientFundsException;
import exception.InvalidAmountException;
import org.junit.jupiter.api.Test;
import service.AccountWorker;

import java.util.ArrayList;
import java.util.List;

class AccountTest {

    @Test
    void depositIncreasesBalance() throws Exception {
        Account a = new SavingsAccount("Riya", 5000);
        a.deposit(2000);
        assertEquals(7000, a.getBalance());
    }

    @Test
    void withdrawReducesBalance() throws Exception {
        Account a = new SavingsAccount("Riya", 7000);
        assertTrue(a.withdraw(3000));
        assertEquals(4000, a.getBalance());
    }

    @Test
    void withdrawOverLimitThrowsAndKeepsBalance() {
        Account a = new SavingsAccount("Riya", 1000);
        InsufficientFundsException e = assertThrows(InsufficientFundsException.class, () -> a.withdraw(5000));
        assertEquals(4000, e.getShortfall());
        assertEquals(1000, a.getBalance());
    }

    @Test
    void nonPositiveAmountsAreRejected() {
        Account a = new SavingsAccount("Riya", 1000);
        assertThrows(InvalidAmountException.class, () -> a.deposit(0));
        assertThrows(InvalidAmountException.class, () -> a.deposit(-5));
        assertThrows(InvalidAmountException.class, () -> a.withdraw(-1));
        assertEquals(1000, a.getBalance());
    }

    @Test
    void savingsKeepsMinimumBalance() {
        Account a = new SavingsAccount("Riya", 1000, 500);
        assertThrows(InsufficientFundsException.class, () -> a.withdraw(600));
        assertTrue(a.canWithdraw(500));
    }

    @Test
    void currentAccountAllowsOverdraft() throws Exception {
        Account a = new CurrentAccount("Amit", 100, 500);
        a.withdraw(600);
        assertEquals(-500, a.getBalance());
        assertThrows(InsufficientFundsException.class, () -> a.withdraw(1));
    }

    @Test
    void fixedDepositIsLocked() {
        Account a = new FixedDepositAccount("Neha", 10000);
        assertFalse(a.canWithdraw(1));
        assertThrows(InsufficientFundsException.class, () -> a.withdraw(1));
    }

    @Test
    void polymorphicInterestRates() {
        Account[] accounts = {new SavingsAccount("A", 1), new CurrentAccount("B", 1, 0),
            new FixedDepositAccount("C", 1)};
        assertEquals(4.0, accounts[0].interestRate(), 0.0001);
        assertEquals(0.0, accounts[1].interestRate(), 0.0001);
        assertEquals(7.0, accounts[2].interestRate(), 0.0001);
    }

    @Test
    void accountsWithSameNumberAreEqual() {
        Account a1 = new SavingsAccount("AC7001", "Riya", 4000, 0);
        Account a2 = new SavingsAccount("AC7001", "Other", 1, 0);
        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
    }

    @Test
    void generatedAccountNumbersAreUnique() {
        Account a = new SavingsAccount("A", 0);
        Account b = new SavingsAccount("B", 0);
        assertFalse(a.getAccountNumber().equals(b.getAccountNumber()));
    }

    @Test
    void transferMovesMoney() throws Exception {
        Account a = new SavingsAccount("A", 1000);
        Account b = new SavingsAccount("B", 0);
        a.transfer(b, 400);
        assertEquals(600, a.getBalance());
        assertEquals(400, b.getBalance());
    }

    @Test
    void failedTransferChangesNothing() {
        Account a = new SavingsAccount("A", 100);
        Account b = new SavingsAccount("B", 0);
        assertThrows(BankException.class, () -> a.transfer(b, 500));
        assertEquals(100, a.getBalance());
        assertEquals(0, b.getBalance());
    }

    @Test
    void manyConcurrentDepositsKeepBalanceCorrect() throws Exception {
        Account a = new CurrentAccount("Shared", 0, 0);
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            threads.add(new Thread(new AccountWorker(a, 1000, 1)));
        }
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        assertEquals(10000, a.getBalance());
    }
}
