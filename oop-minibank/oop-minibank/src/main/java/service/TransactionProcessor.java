package service;

import exception.BankException;
import model.Account;
import model.Command;
import util.CommandParser;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** Runs transactions on a fixed pool of 4 threads over a thread-safe account map. */
public class TransactionProcessor {
    private final ExecutorService pool = Executors.newFixedThreadPool(4);
    private final ConcurrentHashMap<String, Account> accounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> tasksPerThread = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<String> errors = new ConcurrentLinkedQueue<>();
    private final AtomicInteger succeeded = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private final Consumer<Command> onSuccess;

    public TransactionProcessor() {
        this(c -> { });
    }

    /** onSuccess is called (from a pool thread) for every transaction that worked. */
    public TransactionProcessor(Consumer<Command> onSuccess) {
        this.onSuccess = onSuccess;
    }

    public void register(Account account) {
        accounts.put(account.getAccountNumber(), account);
    }

    public void submit(Runnable task) {
        pool.execute(() -> {
            tasksPerThread.computeIfAbsent(Thread.currentThread().getName(), k -> new AtomicInteger())
                    .incrementAndGet();
            task.run();
        });
    }

    /** Parses one text line and runs it as a task. */
    public void submitLine(String line) {
        final Command cmd;
        try {
            cmd = CommandParser.parse(line);
        } catch (IllegalArgumentException e) {
            failed.incrementAndGet();
            errors.add("'" + line + "' -> " + e.getMessage());
            return;
        }
        submit(() -> execute(cmd));
    }

    private void execute(Command cmd) {
        Account account = accounts.get(cmd.accountNumber());
        if (account == null) {
            failed.incrementAndGet();
            errors.add(cmd + " -> account not found");
            return;
        }
        try {
            switch (cmd.type()) {
                case DEPOSIT -> account.deposit(cmd.amount());
                case WITHDRAW -> account.withdraw(cmd.amount());
                case TRANSFER -> throw new BankException("TRANSFER needs two accounts - use the menu");
            }
            succeeded.incrementAndGet();
            onSuccess.accept(cmd);
        } catch (BankException e) {
            failed.incrementAndGet();
            errors.add(cmd.type() + " " + cmd.accountNumber() + " " + cmd.amount() + " -> " + e.getMessage());
        }
    }

    /** Stops accepting tasks and waits until the running ones finish. */
    public void stop() {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(1, TimeUnit.MINUTES)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public int getSucceeded() {
        return succeeded.get();
    }

    public int getFailed() {
        return failed.get();
    }

    public List<String> getErrors() {
        return List.copyOf(errors);
    }

    public ConcurrentHashMap<String, AtomicInteger> getTasksPerThread() {
        return tasksPerThread;
    }
}
