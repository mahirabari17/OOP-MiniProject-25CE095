package model;

/** Actions offered by the MiniBank menu (menu number = ordinal + 1). */
public enum MenuOption {
    OPEN_ACCOUNT("Open account"),
    DEPOSIT("Deposit"),
    WITHDRAW("Withdraw"),
    TRANSFER("Transfer"),
    STATEMENT("Account statement"),
    LIST_ACCOUNTS("List all accounts"),
    BATCH("Process a batch of transactions"),
    REPORT("End-of-day report"),
    EXIT("Exit");

    private final String label;

    MenuOption(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
