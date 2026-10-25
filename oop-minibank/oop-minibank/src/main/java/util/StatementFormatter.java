package util;

import model.Account;

/** Builds a multi-line account statement with a StringBuilder. */
public class StatementFormatter {
    private StatementFormatter() {
    }

    public static String buildStatement(Account account) {
        StringBuilder sb = new StringBuilder();
        sb.append("========== ACCOUNT STATEMENT ==========\n");
        sb.append("Account No : ").append(account.getAccountNumber()).append('\n');
        sb.append("Owner      : ").append(account.getOwnerName()).append('\n');
        sb.append("Type       : ").append(account.getType()).append('\n');
        sb.append("Status     : ").append(account.isActive() ? "ACTIVE" : "CLOSED").append('\n');
        sb.append("Balance    : Rs. ").append(account.getBalance()).append('\n');
        sb.append(String.format("Interest   : %.1f%% p.a. (about Rs. %.2f per year)%n",
                account.interestRate(), account.yearlyInterest()));
        sb.append("---------- Transactions ----------\n");
        for (String line : account.getHistory()) {
            sb.append(line).append('\n');
        }
        sb.append("=======================================");
        return sb.toString();
    }
}
