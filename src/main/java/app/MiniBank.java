package app;

import exception.BankException;
import exception.ValidationException;
import model.Account;
import model.BankInfo;
import model.MenuOption;
import service.BankService;
import util.Validator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Scanner;

/** MiniBank - menu driven console banking application. Run: java -jar minibank.jar [dataFolder] */
public class MiniBank {
    private static final Scanner SC = new Scanner(System.in);
    private static BankService service;

    public static void main(String[] args) {
        BankInfo info = new BankInfo("MiniBank", "Main Branch");
        System.out.println(info);                                    // record's generated toString
        System.out.println("=== Welcome to " + info.name() + " - " + info.branch() + " ===");

        try {
            service = new BankService(Path.of(args.length > 0 ? args[0] : "data"));
        } catch (IOException e) {
            System.out.println("Could not load saved data: " + e.getMessage());
            return;
        }

        MenuOption[] options = MenuOption.values();
        boolean running = true;
        while (running && SC.hasNextLine()) {
            printMenu(options);
            String input = SC.nextLine().trim();
            int choice;
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Please type a number between 1 and " + options.length + ".");
                continue;
            }
            if (choice < 1 || choice > options.length) {
                System.out.println("Invalid choice. Please type a number between 1 and " + options.length + ".");
                continue;
            }
            MenuOption option = options[choice - 1];
            try {
                // switch expression: every menu option returns the text to show
                String message = switch (option) {
                    case OPEN_ACCOUNT -> openAccount();
                    case DEPOSIT -> deposit();
                    case WITHDRAW -> withdraw();
                    case TRANSFER -> transfer();
                    case STATEMENT -> service.statement(ask("Account number: "));
                    case LIST_ACCOUNTS -> listAccounts();
                    case BATCH -> batch();
                    case REPORT -> service.report();
                    case EXIT -> "Saving data...";
                };
                System.out.println(message);
            } catch (BankException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("File problem: " + e.getMessage());
            } finally {
                System.out.println();
            }
            running = option != MenuOption.EXIT;
        }

        try {
            service.save();
        } catch (IOException e) {
            System.out.println("Could not save data: " + e.getMessage());
        }
        System.out.println("Thank you for banking with " + info.name() + ". Goodbye!");
    }

    private static void printMenu(MenuOption[] options) {
        System.out.println("---------- MENU ----------");
        for (int i = 0; i < options.length; i++) {
            System.out.println((i + 1) + ". " + options[i].label());
        }
        System.out.print("Choose an option: ");
    }

    private static String ask(String prompt) {
        System.out.print(prompt);
        return SC.hasNextLine() ? SC.nextLine().trim() : "";
    }

    private static long askAmount(String prompt) throws ValidationException {
        String text = ask(prompt);
        if (!Validator.isValidAmount(text)) {
            throw new ValidationException("Amount must be a positive whole number");
        }
        return Long.parseLong(text);
    }

    private static String openAccount() throws BankException, IOException {
        String name = ask("Customer name: ");
        String mobile = ask("Mobile (10 digits): ");
        String email = ask("Email: ");
        String type = ask("Type (SAVINGS / CURRENT / FD): ");
        String opening = ask("Opening balance: ");
        if (!opening.matches("[0-9]{1,15}")) {
            throw new ValidationException("Opening balance must be a whole number");
        }
        Account a = service.openAccount(name, email, mobile, type, Long.parseLong(opening));
        return "Account opened: " + a;
    }

    private static String deposit() throws BankException, IOException {
        String no = ask("Account number: ");
        long balance = service.deposit(no, askAmount("Amount: "));
        return "Deposit successful. New balance: " + balance;
    }

    private static String withdraw() throws BankException, IOException {
        String no = ask("Account number: ");
        long balance = service.withdraw(no, askAmount("Amount: "));
        return "Withdrawal successful. New balance: " + balance;
    }

    private static String transfer() throws BankException, IOException {
        String from = ask("From account: ");
        String to = ask("To account: ");
        service.transfer(from, to, askAmount("Amount: "));
        return "Transfer successful.";
    }

    private static String listAccounts() {
        if (service.listAccounts().isEmpty()) {
            return "No accounts yet.";
        }
        StringBuilder sb = new StringBuilder("Number | Owner | Balance | Status | Type\n");
        for (Account a : service.listAccounts()) {
            sb.append(a).append(" | ").append(a.getType()).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    private static String batch() {
        System.out.println("Enter one command per line, e.g. DEPOSIT AC0001 500 (empty line to finish):");
        ArrayList<String> lines = new ArrayList<>();
        while (true) {
            String line = ask("> ");
            if (line.isEmpty()) {
                break;
            }
            lines.add(line);
        }
        return lines.isEmpty() ? "Nothing to process." : service.runBatch(lines);
    }
}
