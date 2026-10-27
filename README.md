# MiniBank - OOP with Java mini project

Menu-driven console banking application built step by step over 14 labs.

**Student:** Mahi Rabari &nbsp;|&nbsp; **Roll No:** 25CE095 &nbsp;|&nbsp; **Repository:** `OOP-MiniProject-25CE095`

## Features
- Open Savings / Current / Fixed Deposit accounts (input validated with regular expressions)
- Deposit, withdraw, transfer (thread-safe, deadlock-free), account statement, list accounts
- Batch processing of text commands (`DEPOSIT AC0001 500`) on a thread pool
- Data saved on exit and loaded on start (serialization); transaction log + end-of-day report (NIO)
- Custom exceptions, custom annotations + reflection, generic `Repository<T>`, JUnit 5 tests

## Requirements
JDK 17 or newer (works in GitHub Codespaces). Maven is optional (only for running the tests).

## Build and run
```bash
bash build.sh                  # compiles and creates minibank.jar
java -jar minibank.jar     # starts the menu (data is stored in ./data)
```
Without the script:
```bash
mkdir out
javac -d out $(find src/main -name "*.java")
jar --create --file minibank.jar --main-class app.MiniBank -C out .
java -jar minibank.jar
```

## Run the tests (JUnit 5)
```bash
mvn test
```

## Lab demos
Every lab's concept can be shown separately (1-13, or `all`):
```bash
java -cp minibank.jar app.LabDemos all
java -cp minibank.jar app.LabDemos 9      # race condition vs synchronized
java -cp minibank.jar app.LabDemos 10     # thread pool, producer-consumer, deadlock and its fix
```

## Project structure
| Package | Contents |
|---|---|
| `app` | `MiniBank` (menu, main), `LabDemos` |
| `model` | `Account` (abstract), `SavingsAccount`, `CurrentAccount`, `FixedDepositAccount`, `Customer`, `BankInfo`, `MenuOption`, `Command`, interfaces |
| `model.annotation` | `@Id`, `@Positive`, `@MaxLength` |
| `exception` | `BankException` and its subclasses |
| `util` | `Validator`, `CommandParser`, `StatementFormatter`, `AnnotationValidator` |
| `service` | `BankService`, `Repository<T>`, `AccountStore`, `TransactionProcessor`, `StatePersister`, `TransactionLog`, `ReportGenerator` |

## Menu
1 Open account · 2 Deposit · 3 Withdraw · 4 Transfer · 5 Statement · 6 List accounts · 7 Batch · 8 Report · 9 Exit

## Rules of the account types
| Type | Interest | Withdrawal rule |
|---|---|---|
| Savings | 4% | balance must stay >= Rs. 500 |
| Current | 0% | may go down to -Rs. 5000 (overdraft) |
| Fixed Deposit | 7% | locked, no withdrawal |
