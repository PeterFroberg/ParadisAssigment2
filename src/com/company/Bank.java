// Peter Idestam-Almquist, 2020-01-31.

package com.company;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class Bank {
    // Instance variables.
    //private final List<Account> accounts = new ArrayList<Account>();
    private final ConcurrentHashMap<Integer, Account> accounts = new ConcurrentHashMap<>();

    /***
     * Creates a concurentHashMap to store all account locks
      */
    private ConcurrentHashMap<Integer, ReentrantReadWriteLock> accountlocks = new ConcurrentHashMap<>();


    Random random = new Random();
    // Instance methods.

    int newAccount(int balance) {
        int accountId;
        accountId = accounts.size(); // FIX ORIGINAL
        accounts.put(accountId, new Account(accountId, balance));
        accountlocks.put(accountId, new ReentrantReadWriteLock());
        return accountId;
    }

    /***
     * getAccountBalance(int accountId)
     * @param accountId - recive which account to work with
     * @return - Returns the balanace of the account but if the needed lock not is acquired with 100 attempts reurns null
     * This is not a safe return value as the account theoretically can have the balance of -999999999
     *
     *  Have been modified to lock the account with an ReentrantReadWriteLock in read mode. To allow multiple threads to read the
     *  value
     *
     *  If the thread is unable to acquire the lock it retries 100 times with a random sleep of 1-100 ms between the tries
     */
    int getAccountBalance(int accountId) {
        Account account = null;
        account = accounts.get(accountId);
        for (int i = 0; i < 100; i++) {
            if (accountlocks.get(account.getId()).readLock().tryLock()) {
                try {
                    return account.getBalance();
                }finally {
                    accountlocks.get(account.getId()).readLock().unlock();
                }
            } else {
                try {
                    Thread.sleep(random.nextInt(100));
                } catch (InterruptedException e) {
                    System.out.println(e);
                }
            }
        }
        return -999999999;
    }

    /***
     * help function to get indiviual lock in write-mode
     * @param account the account for which account the lock should be acquired
     * @return boolean value true-lock acquired, false-lock not acquired
     */
    private boolean getCurrentLock(Account account) {
        return accountlocks.get(account.getId()).writeLock().tryLock();
    }

    /***
     * release/unlock the account
     * @param account
     */
    private void releaseCurrentLock(Account account) {
        accountlocks.get(account.getId()).writeLock().unlock();
    }

    /***
     *
     * @param operation - which operation that shold be run
     *
     *                  Tries 100 times to acquire the lock for the specified account
     *                  in the first try statment the operation is performed and in the finally statment the lock is released
     *                  The try stament ensures that the look i release even if an exception is thrown when performing the operation
     *                  in the else stament it makes the thread go to sleep in 1-100 ms by using the Random function if the look was not acquired
     */
    void runOperation(Operation operation) {
        Account account = null;
        account = accounts.get(operation.getAccountId());

        //Lås account{

        for (int i = 0; i < 100; i++) {
            if (getCurrentLock(account)) {
                try {
                    int balance = account.getBalance();
                    balance = balance + operation.getAmount();
                    account.setBalance(balance);

                } finally {
                    releaseCurrentLock(account);
                }
                break;
            } else {
                try {
                    Thread.sleep(random.nextInt(100));
                } catch (InterruptedException e) {
                    System.out.println(e);
                }
            }
        }
    }

    /***
     *
     * @param operations - a List of operations to work on
     * @return  Returns true-if all locks was acquired, false-if not all locks was acquired
     *
     * Tries to acquire the locks for all the lock the account in the list of operations
     */
    private boolean lockOperations(List<Operation> operations) {
        for (Operation o : operations) {
            if (!accountlocks.get(o.getAccountId()).writeLock().tryLock()) {
                return false;
            }
        }
        return true;
    }

    /***
     *
     * @param operations - a list of operations to work on
     *
     *                   relaese the lock for the accouts in the list of operations held by this thread
     *
     */
    private void releaseCurrentLockOperations(List<Operation> operations) {
        for (Operation o : operations) {
            if (accountlocks.get(o.getAccountId()).writeLock().isHeldByCurrentThread())
                accountlocks.get(o.getAccountId()).writeLock().unlock();
        }
    }

    /***
     *
     * @param transaction a transaction to work on
     *                    loopd through all operations in the transaction sent.First starts to loop all operations in the transaction
     *                    secondly starts a loop for retries if not all
     *                    account locks is acquired, it make 100 tries to acquire the locks
     *                    first if-statment tries to acquire all the account locks for all operations in the transaction
     *                    if not all locks are acquired the thread sleep for 1-100ms (random by Random function) and retries to acquire the locks again
     *                    if all locks was acquired, enters the try stament to perform the operations.
     *                    when all the operations is done release all the lock in the finally statment
     */
    void runTransaction(Transaction transaction) {
        List<Operation> currentOperations = transaction.getOperations();

        for (Operation operation : currentOperations) {
            for (int i = 0; i < 100; i++) {
                if (lockOperations(currentOperations)) {
                    try {
                        runOperation(operation);
                    } finally {
                        releaseCurrentLockOperations(currentOperations);
                    }
                    break;
                } else {
                    releaseCurrentLockOperations(currentOperations);
                }
                try {
                    Thread.sleep(random.nextInt(50)); // Random wait before retry.
                } catch (InterruptedException e) {
                    System.out.println(e);
                }
            }
        }

    }

    public ConcurrentHashMap<Integer, Account> getAccounts() {
        return accounts;
    }
}
