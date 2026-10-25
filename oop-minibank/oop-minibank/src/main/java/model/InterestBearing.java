package model;

/** Accounts that earn interest. Default methods use interestRate() and the balance. */
public interface InterestBearing {
    double interestRate();

    long getBalance();

    default double yearlyInterest() {
        return getBalance() * interestRate() / 100.0;
    }

    default double monthlyInterest() {
        return yearlyInterest() / 12.0;
    }
}
