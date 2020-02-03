// Peter Idestam-Almquist, 2020-01-31.

package com.company;

import java.util.ArrayList;
import java.util.List;

class Transaction implements Runnable {
	private List<Operation> operations = new ArrayList<Operation>();
	private List<Integer> accountIds = new ArrayList<Integer>();
	private final Bank bank;
	
	Transaction(Bank bank) {
		this.bank = bank;
	}

	void add(Operation operation) {
		operations.add(operation);
		accountIds.add(operation.getAccountId());
	}
	
	List<Integer> getAccountIds() {
		return accountIds;
	}
	
	List<Operation> getOperations() {
		return operations;
	}
	
	public void run() {
		bank.runTransaction(this);
	}
}	
