package model;

/** Extra rule deciding whether a withdrawal may go ahead. */
@FunctionalInterface
public interface WithdrawRule {
    boolean allow(Account account, long amount);
}
