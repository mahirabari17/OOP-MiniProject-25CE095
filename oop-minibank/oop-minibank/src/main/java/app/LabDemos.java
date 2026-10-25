package app;

import exception.BankException;
import exception.InsufficientFundsException;
import model.Account;
import model.BankInfo;
import model.CurrentAccount;
import model.Customer;
import model.FixedDepositAccount;
import model.MenuOption;
import model.Premium;
import model.SavingsAccount;
import model.WithdrawRule;
import service.AccountStore;
import service.AccountWorker;
import service.Repository;
import service.StatePersister;
import service.TransactionBuffer;
import service.TransactionProcessor;
import util.AnnotationValidator;
import util.CommandParser;
import util.DemoResource;
import util.StatementFormatter;
import util.Validator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs a small demo for every MiniBank lab so each concept can be shown to the faculty.
 * Usage: java -cp minibank.jar app.LabDemos [all | 1..13]
 */
public class LabDemos {

    public static void main(String[] args) throws Exception {
        String which = args.length > 0 ? args[0] : "all";
        for (int lab = 1; lab <= 13; lab++) {
            if (which.equals("all") || which.equals(String.valueOf(lab))) {
                System.out.println("\n################ LAB " + lab + " ################");
                switch (lab) {
                    case 1 -> lab1();
                    case 2 -> lab2();
                    case 3 -> lab3();
                    case 4 -> lab4();
                    case 5 -> lab5();
                    case 6 -> lab6();
                    case 7 -> lab7();
                    case 8 -> lab8();
                    case 9 -> lab9();
                    case 10 -> lab10();
                    case 11 -> lab11();
                    case 12 -> lab12();
                    case 13 -> lab13();
                    default -> { }
                }
            }
        }
        System.exit(0);     // daemon threads of the deadlock demo may still be stuck
    }

    static void lab1() {
        System.out.println("Record  : " + new BankInfo("MiniBank", "Main Branch"));
        for (MenuOption o : MenuOption.values()) {
            String text = switch (o) {          // switch expression
                case EXIT -> "goodbye";
                default -> o.label() + " - handled by BankService";
            };
            System.out.println((o.ordinal() + 1) + ". " + o + " -> " + text);
        }
    }

    static void lab2() throws Exception {
        Account a = new SavingsAccount("Riya", 5000);
        System.out.println("Opened " + a);
        a.deposit(2000);
        System.out.println("After deposit(2000)  : " + a.getBalance());
        System.out.println("withdraw(3000)       : " + a.withdraw(3000) + ", balance " + a.getBalance());
        try {
            a.withdraw(10000);
        } catch (InsufficientFundsException e) {
            System.out.println("withdraw(10000) fails: " + e.getMessage() + ", balance stays " + a.getBalance());
        }
        Customer c = new Customer("Riya", "riya@example.com", "9876543210");
        System.out.println("Customer id generated: " + c.getCustomerId());
    }

    static void lab3() {
        Account a1 = new SavingsAccount("AC0001", "Riya", 4000, 0);
        Account a2 = new SavingsAccount("AC0001", "Riya (copy)", 100, 0);
        System.out.println("toString        : " + a1);
        System.out.println("a1.equals(a2)   : " + a1.equals(a2) + "  (same account number)");
        System.out.println("hashCodes equal : " + (a1.hashCode() == a2.hashCode()));
        Object o = a1;
        System.out.println("instanceof Account : " + (o instanceof Account));
        Customer c = new Customer("Riya", "riya@example.com", "9876543210",
                new Customer.Address("12 MG Road", "Surat", "395001"));
        Customer copy = c.clone();
        System.out.println("Customer clone  : " + copy + " | " + copy.getAddress());
    }

    static void lab4() {
        System.out.println("isValidMobile(9876543210) : " + Validator.isValidMobile("9876543210"));
        System.out.println("isValidMobile(12345)      : " + Validator.isValidMobile("12345"));
        System.out.println("isValidEmail(abc@xyz.com) : " + Validator.isValidEmail("abc@xyz.com"));
        System.out.println("isValidEmail(abc@xyz)     : " + Validator.isValidEmail("abc@xyz"));
        System.out.println("isValidPan(ABCDE1234F)    : " + Validator.isValidPan("ABCDE1234F"));
        System.out.println("isValidPan(abcde1234f)    : " + Validator.isValidPan("abcde1234f"));
        System.out.println("isValidIfsc(SBIN0001234)  : " + Validator.isValidIfsc("SBIN0001234"));
        System.out.println("isValidIfsc(SBIN1001234)  : " + Validator.isValidIfsc("SBIN1001234"));
        var cmd = CommandParser.parse("DEPOSIT AC0001 500");
        System.out.println("parsed: type=" + cmd.type() + ", accountNumber=" + cmd.accountNumber()
                + ", amount=" + cmd.amount());
        try {
            CommandParser.parse("DEPOSIT AC0001");
        } catch (IllegalArgumentException e) {
            System.out.println("bad command rejected: " + e.getMessage());
        }
    }

    static void lab5() {
        Account[] accounts = {
            new SavingsAccount("Riya", 5000, 500),
            new CurrentAccount("Amit", 2000, 5000),
            new FixedDepositAccount("Neha", 10000)
        };
        for (Account a : accounts) {
            System.out.println(a.getType() + " interestRate() = " + a.interestRate()
                    + ", yearly interest = " + a.yearlyInterest());
            if (a instanceof Premium) {
                System.out.println("   -> " + a.getAccountNumber() + " is a Premium account");
            }
            if (a instanceof FixedDepositAccount fd) {      // pattern matching instanceof
                System.out.println("   -> FD locked, canWithdraw(1) = " + fd.canWithdraw(1));
            }
        }
    }

    static void lab6() {
        Account acc = new SavingsAccount("Riya", 5000, 500);
        WithdrawRule anonymous = new WithdrawRule() {
            @Override
            public boolean allow(Account account, long amount) {
                return amount <= 10000;
            }
        };
        WithdrawRule lambda = (account, amount) -> amount <= 10000;
        for (long amt : new long[] {500, 10000, 20000}) {
            System.out.println("amount " + amt + " -> anonymous: " + anonymous.allow(acc, amt)
                    + ", lambda: " + lambda.allow(acc, amt));
        }
    }

    static void lab7() {
        Account bad = new SavingsAccount("AC9000", "Riya", 0, 0);
        System.out.println("errors for balance 0   : " + String.join(",", AnnotationValidator.validate(bad)));
        Account good = new SavingsAccount("AC9001", "Riya", 5000, 0);
        System.out.println("errors for balance 5000: " + AnnotationValidator.validate(good).length);
        String longName = "X".repeat(60);
        Account longOwner = new SavingsAccount("AC9002", longName, 100, 0);
        System.out.println("long owner name: " + String.join(",", AnnotationValidator.validate(longOwner)));
    }

    static void lab8() {
        Account acc = new SavingsAccount("Riya", 1000, 0);
        try {
            acc.withdraw(5000);
        } catch (InsufficientFundsException e) {
            System.out.println(e.getMessage() + "  (shortfall field = " + e.getShortfall() + ")");
        } catch (BankException e) {
            System.out.println("other bank error: " + e.getMessage());
        } finally {
            System.out.println("finally block always runs; balance is still " + acc.getBalance());
        }
        try {
            acc.deposit(-5);
        } catch (BankException e) {
            System.out.println("deposit(-5): " + e.getMessage());
        }
        Account other = new CurrentAccount("Amit", 0, 0);
        try {
            acc.transfer(other, 400);
            System.out.println("transfer 400 ok: " + acc.getBalance() + " / " + other.getBalance());
            acc.transfer(other, 9999);
        } catch (BankException e) {
            System.out.println("transfer 9999 failed: " + e.getMessage());
        }
        System.out.println("try-with-resources:");
        try (DemoResource r = new DemoResource("FileHandle")) {
            r.use(true);
        } catch (IllegalStateException e) {
            System.out.println("  caught after close: " + e.getMessage());
        }
    }

    /** Deliberately NOT thread-safe, to show the race condition. */
    static class UnsafeBalance {
        long value;

        void add(long amount) {
            long temp = value;      // read
            if (temp % 1000 == 0) {
                Thread.yield();     // lets another thread run between read and write, so the race shows every time
            }
            value = temp + amount;  // write - another thread may have changed value in between
        }
    }

    static void lab9() throws Exception {
        int threads = 10;
        UnsafeBalance unsafe = new UnsafeBalance();
        List<Thread> list = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                for (int k = 0; k < 200_000; k++) {
                    unsafe.add(1);
                }
            }, "Worker-" + (i + 1));
            list.add(t);
        }
        System.out.println("Thread state before start: " + list.get(0).getState());
        for (Thread t : list) {
            t.start();
        }
        for (Thread t : list) {
            t.join();
        }
        System.out.println("Thread state after join  : " + list.get(0).getState());
        System.out.println("WITHOUT synchronization: expected " + (threads * 200_000L) + ", got " + unsafe.value
                + (unsafe.value < threads * 200_000L ? "  <-- race condition!" : "  (lucky run, try again)"));

        Account account = new CurrentAccount("Shared", 0, 0);
        List<Thread> safeThreads = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            safeThreads.add(new Thread(new AccountWorker(account, 1000, 1), "Teller-" + (i + 1)));
        }
        for (Thread t : safeThreads) {
            t.start();
        }
        for (Thread t : safeThreads) {
            t.join();
        }
        System.out.println("WITH synchronized deposit(): expected 10000, got " + account.getBalance());
    }

    static void lab10() throws Exception {
        // 1. thread pool
        Account acc = new CurrentAccount("Pool", 0, 0);
        TransactionProcessor p = new TransactionProcessor();
        for (int i = 0; i < 20; i++) {
            p.submit(new AccountWorker(acc, 100, 1));
        }
        p.stop();
        System.out.println("Pool of 4 threads, 20 tasks -> balance " + acc.getBalance() + " (expected 2000)");
        System.out.println("Tasks per pool thread: " + new java.util.TreeMap<>(p.getTasksPerThread()));

        // 2. producer - consumer
        TransactionBuffer buffer = new TransactionBuffer(3);
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 8; i++) {
                    buffer.put("TXN-" + i);
                    System.out.println("  produced TXN-" + i);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Producer");
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 8; i++) {
                    System.out.println("  consumed " + buffer.take());
                    Thread.sleep(20);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Consumer");
        producer.start();
        consumer.start();
        producer.join();
        consumer.join();

        // 3. deadlock and fix
        System.out.println("Opposite lock order : " + (transferDemo(false) ? "DEADLOCK (threads stuck)" : "finished"));
        System.out.println("Ordered lock order  : " + (transferDemo(true) ? "DEADLOCK (threads stuck)" : "finished OK"));
    }

    /** returns true when the two threads are still stuck after 2 seconds */
    private static boolean transferDemo(boolean ordered) throws InterruptedException {
        Account a = new CurrentAccount("A", 1000, 0);
        Account b = new CurrentAccount("B", 1000, 0);
        Thread t1 = new Thread(() -> move(a, b, 100, ordered), "A-to-B");
        Thread t2 = new Thread(() -> move(b, a, 50, ordered), "B-to-A");
        t1.setDaemon(true);
        t2.setDaemon(true);
        t1.start();
        t2.start();
        t1.join(2000);
        t2.join(2000);
        boolean stuck = t1.isAlive() || t2.isAlive();
        if (!stuck) {
            System.out.println("   balances A=" + a.getBalance() + " B=" + b.getBalance() + " (expected 950 / 1050)");
        }
        return stuck;
    }

    private static void move(Account from, Account to, long amount, boolean ordered) {
        Account first = from;
        Account second = to;
        if (ordered && from.compareTo(to) > 0) {    // always lock the smaller account number first
            first = to;
            second = from;
        }
        synchronized (first) {
            try {
                Thread.sleep(200);                  // give the other thread time to take its first lock
                synchronized (second) {
                    from.withdraw(amount);
                    to.deposit(amount);
                }
            } catch (BankException | InterruptedException e) {
                System.out.println("transfer failed: " + e.getMessage());
            }
        }
    }

    static void lab11() throws Exception {
        Path dir = Files.createTempDirectory("minibank-demo");
        Account[] accounts = {
            new SavingsAccount("Riya", 5000, 500),
            new CurrentAccount("Amit", 2000, 5000),
            new FixedDepositAccount("Neha", 9000)
        };
        Path file = dir.resolve("accounts.dat");
        StatePersister.save(accounts, file);
        Account[] loaded = StatePersister.load(file);
        System.out.println("Saved " + accounts.length + " accounts, reloaded " + loaded.length + ":");
        for (Account a : loaded) {
            System.out.println("  " + a + "  (" + a.getType() + ")");
        }
        service.BankService bank = new service.BankService(dir);
        bank.deposit(loaded[0].getAccountNumber(), 500);
        bank.withdraw(loaded[1].getAccountNumber(), 200);
        bank.transfer(loaded[0].getAccountNumber(), loaded[1].getAccountNumber(), 300);
        System.out.println(bank.report());
    }

    static void lab12() {
        AccountStore store = new AccountStore();
        store.add(new SavingsAccount("AC0001", "Riya", 5000, 0));
        store.add(new SavingsAccount("AC0002", "Amit", 2000, 0));
        store.add(new SavingsAccount("AC0003", "Neha", 9000, 0));
        System.out.println("get(AC0002) -> " + store.get("AC0002"));
        System.out.print("sortedByBalance (high to low):");
        for (Account a : store.sortedByBalance()) {
            System.out.print(" " + a.getBalance());
        }
        System.out.println();
        System.out.println("balance above 3000: " + store.balanceAbove(3000));
        ArrayList<Account> byNumber = store.getAll();
        java.util.Collections.sort(byNumber);       // Comparable: by account number
        System.out.println("natural order (Comparable): " + byNumber);
    }

    static void lab13() {
        Repository<Account> accounts = new Repository<>();
        accounts.saveAll(new SavingsAccount("AC0001", "Riya", 5000, 0),
                new CurrentAccount("AC0002", "Amit", 2000, 100),
                new FixedDepositAccount("AC0003", "Neha", 9000));
        System.out.println("findById(AC0002) -> " + accounts.findById("AC0002"));
        System.out.println("findAll() -> " + accounts.findAll().size() + " accounts, delete(AC0001) = "
                + accounts.delete("AC0001") + ", count now " + accounts.count());
        Repository<Customer> customers = new Repository<>();
        Customer c = customers.save(new Customer("Riya", "riya@example.com", "9876543210"));
        System.out.println("Same Repository class for Customer -> " + customers.findById(c.getCustomerId()));
        System.out.println("average(Integer[1,2,3,4]) = " + Repository.average(new Integer[] {1, 2, 3, 4}));
        System.out.println("average(Double[1.5,2.5])  = " + Repository.average(new Double[] {1.5, 2.5}));
        System.out.println("statement preview:\n" + StatementFormatter.buildStatement(new SavingsAccount("AC0010", "Riya", 4000, 0)));
    }
}
