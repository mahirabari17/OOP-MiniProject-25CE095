package model;

import exception.InsufficientFundsException;
import exception.InvalidAmountException;

/** Anything that can receive and pay out money. */
public interface Transactable {
    void deposit(long amount) throws InvalidAmountException;

    boolean withdraw(long amount) throws InsufficientFundsException, InvalidAmountException;
}
