package com.revature.cardealer;

public class Carlot {
	
	private Car[] carDB;
	private String carLotDB[]= new String[20];
	private Car cDetails;
	private String carlot;
	private String cName[] = new String[20];
	private int cpCars[] = new int[20];
	private int caCars[] = new int[20];
	private int cIndex = 0;
	private String oCName;
	int cPrice, cAvail, cAmt;
	
	
	public Carlot() {
		
		
	}
	
	public String getList() {

		String carlot = "[" + cIndex + "]   " + " Car:-  " + cDetails.getCarMake() + "   Model: "
				+ cDetails.getCarModel() + "   Year: " + cDetails.getCarYear() + "   Price: " + cPrice + "   Avail: "
				+ cAvail + "    Stock Qty: " + cAmt;
		System.out.println(carlot);
		return carlot;
	}
	
	public void getListAll() {

		for (int i = 0; i <= carLotDB.length; i++) {

			System.out.println("[" + 1 + "]" + carLotDB[i]);
		}
	}


}
