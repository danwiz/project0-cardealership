package com.revature.cardealer;

public class Customer extends User {
	
private Car[ ] cOwnedDB = new Car[20];
private int cOwnedIndex=0;
private String [] cPayments;



public void setCarsOwned (Car owned, int mpay) {
	
	if(cOwnedIndex <= cOwnedDB.length)
		cOwnedDB[cOwnedIndex] = owned;
	
	cOwnedIndex++;
	
}

public void setPayments(String payments) {
	
	cPayments[cOwnedIndex]=payments;		
		System.out.println(payments);
	
}

public void setPayments(String [] payaments) {
	

		/*
		 * for(int i= 0; i<= cPayments.length; i++) { payments = cPayments[i];
		 * System.out.println(payments); }
		 */
}

public void getCarsOwned() {
	
	Car cDetails = new Car();
	for(int i= 0; i<= cOwnedDB.length; i++) {		
		cDetails= cOwnedDB[i];
		
		System.out.println(cDetails.getCar());
	}
	
	
	
}

}
