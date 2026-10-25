package service;

import static util.Validator.isValidEmail;
import static util.Validator.isValidMobile;

import exception.AccountNotFoundException;
import exception.BankException;
import exception.InvalidAmountException;
import exception.ValidationException;
import model.Account;
import model.CurrentAccount;
import model.Customer;
import model.FixedDepositAccount;
import model.SavingsAccount;
import util.AnnotationValidator;
import util.StatementFormatter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** Business layer: wires Repository + Validator + exceptions together. */
public class BankService {
    public static final long MIN_SAVINGS_BALANCE = 500;
    public static final long OVERDRAFT_LIMIT = 5000;

    private final Path accountsFile;
    private final Path logsDir;
    private final Path logFile;
    private final Path reportFile;
    private final Repository<Account> accounts = new Repository<>(new ConcurrentHashMap<>());
    private final Repository<Customer> customers = new Repository<>();

    /** dataDir holds accounts.dat, logs/transactions.log and report.txt. */
    public BankService(Path dataDir) throws IOException {
        this.accountsFile = dataDir.resolve("accounts.dat");
        this.logsDir = dataDir.resolve("logs");
        this.logFile = logsDir.resolve("transactions.log");
        this.reportFile = dataDir.resolve("report.txt");
        if (Files.exists(accountsFile)) {
            for (Account a : StatePersister.load(accountsFile)) {
                accounts.save(a);
            }
        }
    }

    // ---------------- operations ----------------
    public Account openAccount(String name, String email, String mobile, String type, long opening)
            throws BankException, IOException {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Name cannot be empty");
        }
        if (!isValidMobile(mobile)) {
            throw new ValidationException("Invalid mobile number (10 digits, starting with 6-9)");
        }
        if (!isValidEmail(email)) {
            throw new ValidationException("Invalid email address");
        }
        Customer customer = new Customer(name.trim(), email, mobile);
        String[] problems = AnnotationValidator.validate(customer);
        if (problems.length > 0) {
            throw new ValidationException("Invalid customer: " + String.join(", ", problems));
        }
        Account account = switch (type.toUpperCase()) {
            case "SAVINGS" -> {
                if (opening < MIN_SAVINGS_BALANCE) {
                    throw new ValidationException("Savings account needs at least " + MIN_SAVINGS_BALANCE);
                }
                yield new SavingsAccount(name.trim(), opening, MIN_SAVINGS_BALANCE);
            }
            case "CURRENT" -> new CurrentAccount(name.trim(), opening, OVERDRAFT_LIMIT);
            case "FD" -> {
                if (opening <= 0) {
                    throw new InvalidAmountException(opening);
                }
                yield new FixedDepositAccount(name.trim(), opening);
            }
            default -> throw new ValidationException("Account type must be SAVINGS, CURRENT or FD");
        };
        customers.save(customer);
        accounts.save(account);
        log("OPEN " + account.getAccountNumber() + " " + opening);
        return account;
    }

    public Account find(String accountNumber) throws AccountNotFoundException {
        String key = accountNumber == null ? "" : accountNumber.trim().toUpperCase();
        Account a = accounts.findById(key);
        if (a == null) {
            throw new AccountNotFoundException(key);
        }
        return a;
    }

    public long deposit(String accountNumber, long amount) throws BankException, IOException {
        Account a = find(accountNumber);
        a.deposit(amount);
        log("DEPOSIT " + a.getAccountNumber() + " " + amount);
        return a.getBalance();
    }

    public long withdraw(String accountNumber, long amount) throws BankException, IOException {
        Account a = find(accountNumber);
        a.withdraw(amount);
        log("WITHDRAW " + a.getAccountNumber() + " " + amount);
        return a.getBalance();
    }

    /** Locks the two accounts in a fixed order (smaller number first) so transfers cannot deadlock. */
    public void transfer(String from, String to, long amount) throws BankException, IOException {
        Account a = find(from);
        Account b = find(to);
        if (a.equals(b)) {
            throw new ValidationException("Cannot transfer to the same account");
        }
        Account first = a.compareTo(b) < 0 ? a : b;
        Account second = first == a ? b : a;
        synchronized (first) {
            synchronized (second) {
                a.transfer(b, amount);
            }
        }
        log("TRANSFER " + a.getAccountNumber() + " " + b.getAccountNumber() + " " + amount);
    }

    public String statement(String accountNumber) throws AccountNotFoundException {
        return StatementFormatter.buildStatement(find(accountNumber));
    }

    public List<Account> listAccounts() {
        ArrayList<Account> list = accounts.findAll();
        Collections.sort(list);                // natural order = account number
        return list;
    }

    public List<Account> searchByOwner(String text) {
        ArrayList<Account> result = new ArrayList<>();
        for (Account a : listAccounts()) {
            if (a.getOwnerName().toLowerCase().contains(text.toLowerCase())) {
                result.add(a);
            }
        }
        return result;
    }

    /** Runs lines like "DEPOSIT AC0001 500" on a thread pool and returns a short summary. */
    public String runBatch(List<String> lines) {
        TransactionProcessor processor = new TransactionProcessor(cmd -> {
            try {
                log(cmd.type() + " " + cmd.accountNumber() + " " + cmd.amount());
            } catch (IOException e) {
                System.out.println("Could not write log: " + e.getMessage());
            }
        });
        for (Account a : accounts.findAll()) {
            processor.register(a);
        }
        for (String line : lines) {
            processor.submitLine(line);
        }
        processor.stop();
        StringBuilder sb = new StringBuilder();
        sb.append("Batch finished: ").append(processor.getSucceeded()).append(" succeeded, ")
                .append(processor.getFailed()).append(" failed");
        for (String err : processor.getErrors()) {
            sb.append("\n  - ").append(err);
        }
        return sb.toString();
    }

    public String report() throws IOException {
        return ReportGenerator.generate(logsDir, reportFile);
    }

    public void save() throws IOException {
        StatePersister.save(accounts.findAll().toArray(new Account[0]), accountsFile);
    }

    private void log(String line) throws IOException {
        TransactionLog.append(logFile, line);
    }
}
