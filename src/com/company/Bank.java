//Peter Fröberg, pefr7147

package com.company;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class Bank {
    // Instance variables.
    /***
     * List is changed to ConcurrentHashMap to better support multi threading
     */
    private final ConcurrentHashMap<Integer, Account> accounts = new ConcurrentHashMap<>();

    /***
     * Creates a ConcurrentHashMap to store all account locks
     */
    private ConcurrentHashMap<Integer, ReentrantReadWriteLock> accountlocks = new ConcurrentHashMap<>();
    /***
     * Random added for sleep function to be randomized and not all threads retries at the same time. The Random
     * variable is used for thread sleep functions
     */

    Random random = new Random();
    private Object newAccountLock = new Object();
    private int highestAccountNumber = 0;

    // Instance methods.

    /***
     * Method to generate accountNumbers that is thread safe to avoid 2 accounts with the same accountNumber
     * @return - New accountNumber
     */
    int getNewAccountNumber(){
        synchronized (newAccountLock) {
            highestAccountNumber ++;
            return highestAccountNumber;
        }
    }

    /***
     * Changed so it use the thread safe method getNewAccountNumber to generate the new account number
     *
     * @param balance - Balance to set on new account
     * @return - accountNumber for the new account
     */
    int newAccount(int balance) {
        int accountId;
            accountId = getNewAccountNumber();
        accounts.put(accountId, new Account(accountId, balance));
        /***
         * add a lock for every account to accountLocks, The key is the accountId and a reentrantReadWriteLock as value
         */
        accountlocks.put(accountId, new ReentrantReadWriteLock());
        return accountId;
    }

    /***
     * getAccountBalance(int accountId)
     * @param accountId - recive which account to work with
     * @return - Returns the balanced of the account but if the needed lock not is acquired with 100 attempts returns null
     * This is not a safe return value as the account theoretically can have the balance of -999999999, the return type could be changed
     * to Integer and then return null if after 100 retries no lock was acquired
     *
     *  Have been modified to lock the account with an ReentrantReadWriteLock in read mode. To allow multiple threads to read the
     *   at the same time, but wait if any thread have a writelock
     *
     *  If the thread is unable to acquire the lock it retries 100 times with a random sleep of 1-100 ms between the tries
     *  to make sure there is no deadlocks.
     */
    int getAccountBalance(int accountId) {
        Account account = null;
        account = accounts.get(accountId);
        for (int i = 0; i < 100; i++) {
            if (accountlocks.get(account.getId()).readLock().tryLock()) {
                try {
                    return account.getBalance();
                } finally {
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
     *                  in the first try statement the operation is performed and in the finally statement the lock is released
     *                  The try stament ensures that the look i release even if an exception is thrown when performing the operation
     *                  in the else statement it makes the thread go to sleep in 1-100 ms by using the Random function if the look was not acquired
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
     * @return Returns true-if all locks was acquired, false-if not all locks was acquired
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
     *                   release the lock for the accounts in the list of operations held by this thread
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
     *                    loop through all operations in the transaction sent to the method.
     *                    First loop starts to loop all operations in the transaction
     *                    second loop  starts a loop for retries if not all account locks is acquired, it make 100 tries to acquire the locks
     *                    first if-statement tries to acquire all the account locks for all operations in the transaction

     *                    if all locks was acquired, it enters the try statement to perform the operations in the transaction.
     *                    when all the operations is done it release all the lock in the finally statement. The break statement
     *                    makes the program to exit the for-loop when all operations is performed
     *
     *                    if not all locks are acquired, all locks are released and the thread sleep for 1-100ms (random by Random function)
     *                    and retries to acquire the locks again, this is to avoid deadlocks. To make sure that all operations in one transaction
     *                    is performed it is required to acquire all locks before any operations is performed
     *
     *                    I have chosen this method to ensure that it is possible to do all operations in a row to ensure consistent account data
     *
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
