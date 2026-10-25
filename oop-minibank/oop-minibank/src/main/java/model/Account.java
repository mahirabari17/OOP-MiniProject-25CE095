package model;

import exception.BankException;
import exception.InsufficientFundsException;
import exception.InvalidAmountException;
import model.annotation.Id;
import model.annotation.MaxLength;
import model.annotation.Positive;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base class of every bank account.
 * The balance can change ONLY through deposit()/withdraw()/transfer() (encapsulation)
 * and those methods are synchronized, so many threads can use one account safely.
 */
public abstract class Account implements Transactable, InterestBearing, Comparable<Account>, Serializable {
    private static final long serialVersionUID = 1L;
    private static final int MAX_HISTORY = 500;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private static int counter = 0;                                   // used for account numbers
    private static final AtomicInteger transferAttempts = new AtomicInteger();

    @Id
    private final String accountNumber;
    @MaxLength(50)
    private String ownerName;
    @Positive
    private long balance;               // whole rupees; no public setter
    private boolean active = true;
    private final ArrayList<String> history = new ArrayList<>();

    /** Opens a new account with a generated number such as AC0001. */
    public Account(String ownerName, long openingBalance) {
        this(generateAccountNumber(), ownerName, openingBalance);
    }

    /** Opens a new account with zero balance (constructor chaining). */
    public Account(String ownerName) {
        this(ownerName, 0);
    }

    /** Re-creates an account with a known number (used for restore and tests). */
    protected Account(String accountNumber, String ownerName, long balance) {
        if (balance < 0) {
            throw new IllegalArgumentException("Opening balance cannot be negative");
        }
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.balance = balance;
        ensureCounterAtLeast(accountNumber);
        record("OPEN", balance);
    }

    // ---------- account number generation (static, shared by all objects) ----------
    private static synchronized String generateAccountNumber() {
        counter++;
        return String.format("AC%04d", counter);
    }

    /** After loading saved accounts, make sure new numbers do not repeat old ones. */
    public static synchronized void ensureCounterAtLeast(String accountNumber) {
        try {
            int n = Integer.parseInt(accountNumber.substring(2));
            if (n > counter) {
                counter = n;
            }
        } catch (RuntimeException ignored) {
            // number not in ACnnnn form: nothing to adjust
        }
    }

    public static int getTransferAttempts() {
        return transferAttempts.get();
    }

    // ---------- rules every account type must provide ----------
    public abstract double interestRate();

    public abstract boolean canWithdraw(long amount);

    /** Lowest balance this account may reach after a withdrawal. */
    protected long lowestAllowedBalance() {
        return 0;
    }

    /** How much money is missing for a withdrawal that is refused. */
    protected long shortfallFor(long amount) {
        return Math.max(0, lowestAllowedBalance() - (getBalance() - amount));
    }

    // ---------- the only ways to change the balance ----------
    @Override
    public void deposit(long amount) throws InvalidAmountException {
        credit(amount, "DEPOSIT");
    }

    @Override
    public boolean withdraw(long amount) throws InsufficientFundsException, InvalidAmountException {
        debit(amount, "WITHDRAW");
        return true;
    }

    private synchronized void credit(long amount, String label) throws InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException(amount);
        }
        balance += amount;
        record(label, amount);
    }

    private synchronized void debit(long amount, String label)
            throws InsufficientFundsException, InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException(amount);
        }
        if (!canWithdraw(amount)) {
            throw new InsufficientFundsException(shortfallFor(amount));
        }
        balance -= amount;
        record(label, amount);
    }

    /** Moves money to another account; if the deposit fails the money is given back. */
    public void transfer(Account to, long amount) throws BankException {
        boolean debited = false;
        try {
            debit(amount, "TRANSFER OUT to " + to.accountNumber);
            debited = true;
            to.credit(amount, "TRANSFER IN from " + accountNumber);
        } catch (BankException e) {
            if (debited) {
                credit(amount, "TRANSFER REFUND");
            }
            throw e;                         // re-throw to the caller
        } finally {
            transferAttempts.incrementAndGet();
        }
    }

    private synchronized void record(String what, long amount) {
        if (history.size() >= MAX_HISTORY) {
            history.remove(0);
        }
        history.add(LocalDateTime.now().format(TIME) + " | " + what + " " + amount
                + " | balance " + balance);
    }

    // ---------- getters (no setter for balance!) ----------
    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    @Override
    public synchronized long getBalance() {
        return balance;
    }

    public synchronized boolean isActive() {
        return active;
    }

    public synchronized void setActive(boolean active) {
        this.active = active;
    }

    public synchronized ArrayList<String> getHistory() {
        return new ArrayList<>(history);
    }

    public String getType() {
        return getClass().getSimpleName();
    }

    // ---------- Object methods ----------
    @Override
    public synchronized String toString() {
        return accountNumber + " | " + ownerName + " | " + balance + " | " + (active ? "active" : "closed");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof Account other && accountNumber.equals(other.accountNumber);
    }

    @Override
    public int hashCode() {
        return accountNumber.hashCode();
    }

    @Override
    public int compareTo(Account other) {
        return accountNumber.compareTo(other.accountNumber);
    }
}
