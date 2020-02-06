// Peter Idestam-Almquist, 2020-01-31.

package com.company;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class Bank {
    // Instance variables.
    private final List<Account> accounts = new ArrayList<Account>();

    private Hashtable<Integer, ReentrantReadWriteLock> accountlocks = new Hashtable<>();
    
    Random random = new Random();
    // Instance methods.

    int newAccount(int balance) {
        int accountId;
        accountId = accounts.size(); // FIX ORIGINAL
        accounts.add(new Account(accountId, balance));
        accountlocks.put(accountId, new ReentrantReadWriteLock());
        return accountId;
    }

    int getAccountBalance(int accountId) {
        Account account = null;
        account = accounts.get(accountId);
        return account.getBalance();
    }

    void runOperation(Operation operation) {
        Account account = null;
        account = accounts.get(operation.getAccountId());
        //Lås account
        for (int i = 0; i < 100; i++) {
            if (accountlocks.get(operation.getAccountId()).writeLock().tryLock()) {
                try {
                    int balance = account.getBalance();
                    balance = balance + operation.getAmount();
                    account.setBalance(balance);

                } finally {
                    accountlocks.get(operation.getAccountId()).writeLock().unlock();
                }
                break;
            }else{
                try{
                    Thread.sleep(random.nextInt(100));
                }catch (InterruptedException e){
                    System.out.println(e);
                }
            }

        }
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
            if (accountlocks.get(o.getAccountId()).isWriteLockedByCurrentThread())
                accountlocks.get(o.getAccountId()).writeLock().unlock();
        }
    }

    void runTransaction(Transaction transaction) {
        List<Operation> currentOperations = transaction.getOperations();

       /* boolean allLocksAcquiered = true;
        boolean operationsCompleted = false;*/
        // Låsa alla konton med reentrant lock som finns i currentOperations



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
                    Thread.sleep(random.nextInt(100)); // Random wait before retry.
                } catch (InterruptedException e) {
                    System.out.println(e);
                }
            }
        }


        /*int i = 0;
        for (int x = 0; x < 100; x++) {
            try {
                while (allLocksAcquiered && !operationsCompleted) {
                    if (!accountlocks.get(currentOperations.get(i).getAccountId()).writeLock().tryLock()) {
                        allLocksAcquiered = false;
                    }
                    i++;
                    if (i >= currentOperations.size()) {
                        operationsCompleted = true;
                    }
                }
                if (allLocksAcquiered) {
                    for (Operation operation : currentOperations) {
                        runOperation(operation);
                    }
                }
            } finally {
                for (Operation o : currentOperations) {
                    if (accountlocks.get(o.getAccountId()).isWriteLockedByCurrentThread()) {
                        accountlocks.get(o.getAccountId()).writeLock().unlock();
                    }
                }
            }

            try {
                Thread.sleep(random.nextInt(100));
            } catch (InterruptedException e) {

            }

        }*/
    }

    public List<Account> getAccounts() {
        return accounts;
    }

	/*public static void main(String[] args) throws InterruptedException {
		Bank bank = new Bank();

		Account account1 = bank.getAccounts().get(bank.newAccount(0));
		Account account2 = bank.getAccounts().get(bank.newAccount(0));
		System.out.println(account1.getBalance());
		System.out.println(account2.getBalance());
		//OPERATION

		long startTime = System.nanoTime();

		//parralelize
		int numThreads = 9;
		int numTransactions = 1000;
		Transaction[] transactions = new Transaction[numTransactions];
		Thread[] threads = new Thread[numThreads];

		for (int i = 0; i < numThreads; i++) {
			transactions[i] = new Transaction(bank);

			for (int j = 0; j < numTransactions / numThreads; j++) {
				transactions[i].add(new Operation(bank, account1.getId(), 100));
			}

			threads[i] = new Thread(transactions[i]);
		}
		for (int i = 0; i < numThreads; i++) {
			threads[i].start();
		}

		for (int i = 0; i < numThreads; i++) {
			threads[i].join();
		}


		System.out.println(account1.getBalance());
		System.out.println(account2.getBalance());

		long end = System.nanoTime();


		System.out.println("It took time: " + (end - startTime) / 1000000000.0);


	}*/
}
