// Peter Idestam-Almquist, 2020-01-31.

package com.company;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class Bank {
	// Instance variables.
	private final List<Account> accounts = new ArrayList<Account>();

	private HashMap<Integer,ReentrantLock> Accountlocks = new HashMap<>();
	ReentrantLock transactionLock = new ReentrantLock();
	
	// Instance methods.

	int newAccount(int balance) {
		int accountId;
		accountId = accounts.size(); // FIX ORIGINAL
		accounts.add(new Account(accountId, balance));
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
		int balance = account.getBalance();
		balance = balance + operation.getAmount();
		account.setBalance(balance);
		//lås upp acoount
	}
		
	void runTransaction(Transaction transaction) {
		List<Operation> currentOperations = transaction.getOperations();

		boolean allLocksAcquiered = true;
		// Låsa alla konton med reentrant lock som finns i current opperations
		//For loop lock
		for (Operation o : currentOperations){
			if(accounts.get(o.getAccountId())){

			}else{
				allLocksAcquiered = false;
			}
		}
		for (Operation operation : currentOperations) {
			runOperation(operation);
		}

		//for loop unlock
	}
}
