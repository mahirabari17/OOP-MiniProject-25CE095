package service;

import exception.InvalidAmountException;
import model.Account;

/** Runnable that deposits 'amount' into one shared account 'times' times. */
public class AccountWorker implements Runnable {
    private final Account account;
    private final int times;
    private final long amount;

    public AccountWorker(Account account, int times, long amount) {
        this.account = account;
        this.times = times;
        this.amount = amount;
    }

    @Override
    public void run() {
        for (int i = 0; i < times; i++) {
            try {
                account.deposit(amount);
            } catch (InvalidAmountException e) {
                System.out.println(Thread.currentThread().getName() + ": " + e.getMessage());
                return;
            }
        }
    }
}
