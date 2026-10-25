package model;

/** Earns 4% interest; the balance may not fall below minBalance. */
public class SavingsAccount extends Account {
    private static final long serialVersionUID = 1L;
    private final long minBalance;

    public SavingsAccount(String ownerName, long openingBalance, long minBalance) {
        super(ownerName, openingBalance);
        this.minBalance = minBalance;
    }

    public SavingsAccount(String ownerName, long openingBalance) {
        this(ownerName, openingBalance, 0);
    }

    /** Restore constructor with a known account number. */
    public SavingsAccount(String accountNumber, String ownerName, long balance, long minBalance) {
        super(accountNumber, ownerName, balance);
        this.minBalance = minBalance;
    }

    public long getMinBalance() {
        return minBalance;
    }

    @Override
    public double interestRate() {
        return 4.0;
    }

    @Override
    public boolean canWithdraw(long amount) {
        return getBalance() - amount >= minBalance;
    }

    @Override
    protected long lowestAllowedBalance() {
        return minBalance;
    }
}
