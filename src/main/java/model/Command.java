package model;

/** A parsed text command such as "DEPOSIT AC0001 500". */
public record Command(TransactionType type, String accountNumber, long amount) {
}
