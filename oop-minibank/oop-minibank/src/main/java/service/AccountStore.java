package service;

import model.Account;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

/** Collections based store: HashMap for look-up, ArrayList for history. */
public class AccountStore {
    private final HashMap<String, Account> accounts = new HashMap<>();
    private final ArrayList<String> transactionHistory = new ArrayList<>();

    public void add(Account account) {
        accounts.put(account.getAccountNumber(), account);
        transactionHistory.add("ADDED " + account.getAccountNumber());
    }

    public Account get(String accountNumber) {
        return accounts.get(accountNumber);
    }

    public ArrayList<Account> getAll() {
        return new ArrayList<>(accounts.values());
    }

    /** Highest balance first. */
    public ArrayList<Account> sortedByBalance() {
        ArrayList<Account> list = getAll();
        list.sort(Comparator.comparingLong(Account::getBalance).reversed());
        return list;
    }

    public ArrayList<Account> balanceAbove(long limit) {
        ArrayList<Account> result = new ArrayList<>();
        for (Account a : accounts.values()) {
            if (a.getBalance() > limit) {
                result.add(a);
            }
        }
        return result;
    }

    public ArrayList<String> getTransactionHistory() {
        return transactionHistory;
    }
}
