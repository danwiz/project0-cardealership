package com.revature.cardealer;

public class Offer {

	private String oAvail;
	private int oAmt;
	private int oMpay;
	private int oPrice;
	private boolean oAccept;
	private boolean oStatus;
	private boolean oPending;
	//private String status;
	private String oDetails;
	private Offer[] oOfferDB= new Offer[20];
	// private String offer; // Car - Make: model: Year: "+ Price: + Avail
	String[] offerDB = new String[20];
	private int oIndex = 0;
	private Car cDetails;
	private Car[] carDB = new Car[20];
	private String cName[] = new String[20];
	private int cpOffer[] = new int[20];
	private int cpPrice[] = new int[20];
	private int[] listingStock = new int[20];
	private int opIndex = 0;
	private String oCName;

	// private Offer[] oOfferDB = new Offer[20];
	// private int cIndex;

	/*
	 * public Offer() {
	 * 
	 * Offer[] offerDB = {registerOffer("Honda", "Accord", 2018, 15456,"Yes",7),
	 * registerOffer("Chevy", "Malibu", 2020, 17456,"Yes",4), registerOffer("Honda",
	 * "Accord", 2016, 10456,"Yes",6), registerOffer("Honda", "Accord", 2014,
	 * 13456,"Yes",3)};
	 * 
	 * for(int i=0; i>= offerDB.length;i++) {
	 * System.out.println(offerDB[i].toString());
	 * 
	 * } }
	 * 
	 * 
	 * 
	 * public void setOfferDB(Offer offer) {
	 * 
	 * 
	 * Offer[] offerDB = {registerOffer("Honda", "Accord", 2018, 15456,"Yes",7),
	 * registerOffer("Chevy", "Malibu", 2020, 17456,"Yes",4), registerOffer("Honda",
	 * "Accord", 2016, 10456,"Yes",6), registerOffer("Honda", "Accord", 2014,
	 * 13456,"Yes",3)};
	 * 
	 * 
	 * for(int i=0; i<= offerDB.length;i++) {
	 * System.out.println(offerDB[i].toString());
	 * 
	 * 
	 * 
	 * oOfferDB[oIndex]= offer; oIndex++;
	 * 
	 * 
	 * 
	 * }
	 */

	public synchronized void setpOffer(String cname, int onum) {

		if (cname == null || cname.trim().isEmpty()
				|| onum < 0 || onum >= offerDB.length
				|| offerDB[onum] == null || offerDB[onum].isEmpty()
				|| listingStock[onum] <= 0
				|| opIndex >= cpOffer.length) {
			throw new IllegalArgumentException("Invalid purchase request or request capacity exhausted");
		}
		cpOffer[opIndex] = onum;
		cName[opIndex] = cname;
		//status= "   Pending Offer: " + cName[onum];
		//oDetails = "["+onum+"]"+ offerDB[onum] + " Pending Offer: " + cName[onum];
		//2cpOffer[onum] =""+oDetails+ "   Pending Offer: " + cName[onum];
		
		System.out.println("["+onum+"]"+ offerDB[onum] + "     Pending Offer for : " + cName[opIndex]);
		oPending=true;
		opIndex++;  // Only the active request count changes; listing capacity is independent.

	}

	
	
	public void getpOffers() {
		if (opIndex == 0) {
			System.out.println("No pending purchase requests.");
			return;
		}
		for (int i = 0; i < opIndex; i++) {
			if (cName[i] != null && !cName[i].isEmpty()) {
				int listing = cpOffer[i];
				if (listing >= 0 && listing < offerDB.length && offerDB[listing] != null) {
					System.out.println(offerDB[listing] + "   |--->  Pending Offer:   " + cName[i]);
				}
			}
		}
	}

	public synchronized void registerOffer(String make, String model, int year, int price, String avail, int amt) {
		if (oIndex >= offerDB.length || price < 0 || amt < 0
				|| make == null || make.trim().isEmpty()
				|| model == null || model.trim().isEmpty()) {
			throw new IllegalArgumentException("Invalid vehicle listing or listing capacity exhausted");
		}

		cDetails = new Car(make, model, year);
		setOffer(cDetails, price, avail, amt);
		carDB[oIndex] = cDetails;
		cpPrice[oIndex]=price;
		listingStock[oIndex] = amt;
		//offerDB[oIndex]=oDetails;
		offerDB[oIndex] = getOffer();
		
		/*
		 * Offer offer = new Offer(); offer.cDetails=cDetails; offer.oPrice = price;
		 * offer.oAvail=avail; offer.oAmt=amt; offer.oPending=false;
		 * offer.oAccept=false; offer.oStatus=true; offer.oIndex=oIndex;
		 */
		  
		//Used "this" store offer instend 
		  
		oOfferDB[oIndex]=this;   //stores this offer in offerDB
		oIndex++;

	}

	public void setOffer(Car details, int price, String avail, int amt) {

		cDetails = details;
		oPrice = price;
		oAvail = "yes";
		oAmt = amt;

		//oDetails = getOffer();
		// offer= cDetails.toString()+price+avail+;

	}

	public void setOffer(String make, String model, int year, int price, String avail, int amt) {
		cDetails = new Car(make, model, year);
		oPrice = price;
		oAvail = "yes";
		oAmt = amt;
		oStatus = true;
		oAccept = false;
		oPending=false;
		

		// offer= cDetails.getCarMake()+" "+cDetails.getCarModel()+"
		// "+cDetails.getCarYear() + " "+ price+" "+avail+" "+amt;
	}

	public void getOfferAll() {

		for (int i = 0; i <= offerDB.length-1; i++) {
			if(offerDB[i]!=null)
				System.out.println("[" + i + "]" + offerDB[i]);
		}
		System.out.println("\nWe have no Cars to at this Time!");
	}

	public String getOffer() {

		String offer = "[" + oIndex + "]   " + " Car:-  " + cDetails.getCarMake() + "   Model: "
				+ cDetails.getCarModel() + "   Year: " + cDetails.getCarYear() + "   Price: " + oPrice + "   Avail: "
				+ oAvail + "   Stock Qty: " + oAmt;
		System.out.println(offer);
		return offer;
	}

	public String toString() {
		return getOffer();
	}

	public void setPrice(int price) {
		oPrice = price;
	}

	public int getPrice() {
		return oPrice;
	}

	/** Returns the full purchase price for a registered listing, not its monthly installment. */
	/** Returns the buyer of the oldest matching pending request, without consuming it. */
	public String getPendingBuyer(int listing) {
		if (listing < 0 || listing >= offerDB.length || offerDB[listing] == null
				|| offerDB[listing].isEmpty()) {
			throw new IllegalArgumentException("Invalid listing");
		}
		for (int i = 0; i < opIndex; i++) {
			if (cpOffer[i] == listing && cName[i] != null && !cName[i].isEmpty()) {
				return cName[i];
			}
		}
		throw new IllegalArgumentException("No matching pending purchase request");
	}

	/** Remaining inventory for an individual registered listing. */
	public int getListingStock(int listing) {
		if (listing < 0 || listing >= offerDB.length || offerDB[listing] == null) {
			throw new IllegalArgumentException("Invalid listing");
		}
		return listingStock[listing];
	}

	public int getListingPrice(int listing) {
		if (listing < 0 || listing >= offerDB.length
				|| offerDB[listing] == null || offerDB[listing].isEmpty()
				|| carDB[listing] == null || cpPrice[listing] < 0) {
			throw new IllegalArgumentException("Invalid listing");
		}
		return cpPrice[listing];
	}

	public void setMpay(int mpay) {
		
		oMpay = mpay;
	}

	public int getMpay() {
		return oMpay;
	}

	public String getAvail() {
		return oAvail;
	}

	public void setAvail(String avail) {
		oAvail = avail;
	}

	public void setAmt(int amt) {
		oAmt = amt;
	}

	public int getAmt() {
		return oAmt;
	}

	public boolean isAvail() {
		if (oAmt == 0)
			oStatus = false;
		return oStatus;
	}

	public void setStatus(boolean status) {
		oStatus = status;

	}

	public boolean getStatus() {
		return oStatus;
	}

	public void getAccept(Offer[] offerdb) {
		for (int i = 0; i >= offerdb.length; i++) {
			if (offerdb[i].oAccept)
				System.out.println(offerdb[i].oDetails + "   Offer Accepted: " + offerdb[i].oCName);
			oAccept = false;
		}

	}

	public void setAccept() {
		oAccept=true;	
	}
	// serches
	/**
	 * Performs a reversible approval when called under the shared Offer monitor.
	 * A failure after any inventory or request mutation restores the captured
	 * fields before propagating the original exception.
	 */
	public synchronized int acceptWithRollback(int onum, int mth) {
		int[] stockBefore = listingStock.clone();
		int[] requestsBefore = cpOffer.clone();
		String[] buyersBefore = cName.clone();
		int pendingCountBefore = opIndex;
		int amountBefore = oAmt;
		int monthlyBefore = oMpay;
		boolean acceptedBefore = oAccept;
		String buyerBefore = oCName;
		String detailsBefore = oDetails;
		try {
			return setAccept(onum, mth, true);
		} catch (RuntimeException | Error failure) {
			System.arraycopy(stockBefore, 0, listingStock, 0, listingStock.length);
			System.arraycopy(requestsBefore, 0, cpOffer, 0, cpOffer.length);
			System.arraycopy(buyersBefore, 0, cName, 0, cName.length);
			opIndex = pendingCountBefore;
			oAmt = amountBefore;
			oMpay = monthlyBefore;
			oAccept = acceptedBefore;
			oCName = buyerBefore;
			oDetails = detailsBefore;
			throw failure;
		}
	}

	public synchronized int setAccept(int onum, int mth, boolean accept) {
		if (!accept || onum < 0 || onum >= offerDB.length || mth <= 0
				|| offerDB[onum] == null || offerDB[onum].isEmpty()) {
			throw new IllegalArgumentException("Invalid approval request");
		}
		for (int i = 0; i < opIndex; i++) {
			if (cpOffer[i] == onum && cName[i] != null) {
				if (listingStock[onum] <= 0) {
					throw new IllegalStateException("No inventory available for approval");
				}
				String buyer = cName[i];
				int monthlyPayment = cpPrice[onum] / mth;
				listingStock[onum]--;
				oAmt = listingStock[onum];
				oAccept = true;
				oMpay = monthlyPayment;
				oCName = buyer;
				oDetails = offerDB[onum] + "   Offer Accepted: " + buyer
						+ "   Monthly Payment:  " + monthlyPayment;
				// Compact the FIFO queue; otherwise completed requests exhaust
				// fixed capacity and newly reused holes overtake older buyers.
				for (int pending = i; pending < opIndex - 1; pending++) {
					cName[pending] = cName[pending + 1];
					cpOffer[pending] = cpOffer[pending + 1];
				}
				cName[opIndex - 1] = null;
				cpOffer[opIndex - 1] = 0;
				opIndex--;
				System.out.println(oDetails);
				return monthlyPayment;
			}
		}
		throw new IllegalArgumentException("No matching pending purchase request");
	}

	public synchronized void rejectAllOffers() {
		for (int i = 0; i < opIndex; i++) {
			cpOffer[i] = 0;
			cName[i] = null;
		}
		opIndex = 0;
		oPending = false;
		System.out.println("\\nMessage: All Pending Offers REJECTED!! ");
	}

	public synchronized void removeOffer(int onum) {
		
		System.out.println(offerDB[onum]+"\nMessage: Car Listing Removed!! ");
		if(offerDB[onum]!=null)
			offerDB[onum] = "";
		//consider removing car for carDB?
	    //Offer ObjectDB  [onum].status=false
			
	}
	
	public boolean getAccept() {
		return oAccept;
	}

	public Car getCarDB(int pnum) {
		return carDB[pnum];
	}

}
