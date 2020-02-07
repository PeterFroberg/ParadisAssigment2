// Peter Idestam-Almquist, 2020-01-31.

package com.company;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class Bank {
    // Instance variables.
    //private final List<Account> accounts = new ArrayList<Account>();
    private final ConcurrentHashMap<Integer, Account> accounts = new ConcurrentHashMap<>();
    private Hashtable<Integer, ReentrantReadWriteLock> accountlocks = new Hashtable<>();


    Random random = new Random();
    // Instance methods.

    int newAccount(int balance) {
        int accountId;
        accountId = accounts.size(); // FIX ORIGINAL
        accounts.put(accountId, new Account(accountId, balance));
        accountlocks.put(accountId, new ReentrantReadWriteLock());
        return accountId;
    }

    int getAccountBalance(int accountId) {
        Account account = null;
        account = accounts.get(accountId);
        for (int i = 0; i < 100; i++) {
            if(accountlocks.get(account.getId()).readLock().tryLock()) {
                return account.getBalance();
            }else {
                try{
                    Thread.sleep(random.nextInt(50));
                }catch (InterruptedException e){
                    System.out.println(e);
                }
            }
        }
        return -1;
    }
    private boolean getCurrentLock(Account account) {
        return accountlocks.get(account.getId()).writeLock().tryLock();
    }
    private void releaseCurrentLock(Account account) {
        accountlocks.get(account.getId()).writeLock().unlock();
    }

    void runOperation(Operation operation) {
        Account account = null;
        account = accounts.get(operation.getAccountId());
        synchronized (account){
            int balance = account.getBalance();
            balance = balance + operation.getAmount();
            account.setBalance(balance);
        }
        /*//Lås account{

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
            }else{
                try{
                    Thread.sleep(random.nextInt(50));
                }catch (InterruptedException e){
                    System.out.println(e);
                }
            }
        }*/
    }

    private boolean lockOperations(List<Operation> operations) {
        for (Operation o : operations) {
            if (!accountlocks.get(o.getAccountId()).writeLock().tryLock()) {
                return false;
            }
        }
        return true;
    }

    private void releaseCurrentLockOperations(List<Operation> operations) {
        for (Operation o : operations) {
            if (accountlocks.get(o.getAccountId()).writeLock().isHeldByCurrentThread())
                accountlocks.get(o.getAccountId()).writeLock().unlock();
        }
    }

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
