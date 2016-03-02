/**
 * TAC AgentWare
 * http://www.sics.se/tac        tac-dev@sics.se
 *
 * Copyright (c) 2001-2005 SICS AB. All rights reserved.
 *
 * SICS grants you the right to use, modify, and redistribute this
 * software for noncommercial purposes, on the conditions that you:
 * (1) retain the original headers, including the copyright notice and
 * this text, (2) clearly document the difference between any derived
 * software and the original, and (3) acknowledge your use of this
 * software in pertaining publications and reports.  SICS provides
 * this software "as is", without any warranty of any kind.  IN NO
 * EVENT SHALL SICS BE LIABLE FOR ANY DIRECT, SPECIAL OR INDIRECT,
 * PUNITIVE, INCIDENTAL OR CONSEQUENTIAL LOSSES OR DAMAGES ARISING OUT
 * OF THE USE OF THE SOFTWARE.
 *
 * -----------------------------------------------------------------
 *
 * Author  : Joakim Eriksson, Niclas Finne, Sverker Janson
 * Created : 23 April, 2002
 * Updated : $Date: 2005/06/07 19:06:16 $
 *	     $Revision: 1.1 $
 * ---------------------------------------------------------
 * DummyAgent is a simplest possible agent for TAC. It uses
 * the TACAgent agent ware to interact with the TAC server.
 *
 * Important methods in TACAgent:
 *
 * Retrieving information about the current Game
 * ---------------------------------------------
 * int getGameID()
 *  - returns the id of current game or -1 if no game is currently plaing
 *
 * getServerTime()
 *  - returns the current server time in milliseconds
 *
 * getGameTime()
 *  - returns the time from start of game in milliseconds
 *
 * getGameTimeLeft()
 *  - returns the time left in the game in milliseconds
 *
 * getGameLength()
 *  - returns the game length in milliseconds
 *
 * int getAuctionNo()
 *  - returns the number of auctions in TAC
 *
 * int getClientPreference(int client, int type)
 *  - returns the clients preference for the specified type
 *   (types are TACAgent.{ARRIVAL, DEPARTURE, HOTEL_VALUE, E1, E2, E3}
 *
 * int getAuctionFor(int category, int type, int day)
 *  - returns the auction-id for the requested resource
 *   (categories are TACAgent.{CAT_FLIGHT, CAT_HOTEL, CAT_ENTERTAINMENT
 *    and types are TACAgent.TYPE_INFLIGHT, TACAgent.TYPE_OUTFLIGHT, etc)
 *
 * int getAuctionCategory(int auction)
 *  - returns the category for this auction (CAT_FLIGHT, CAT_HOTEL,
 *    CAT_ENTERTAINMENT)
 *
 * int getAuctionDay(int auction)
 *  - returns the day for this auction.
 *
 * int getAuctionType(int auction)
 *  - returns the type for this auction (TYPE_INFLIGHT, TYPE_OUTFLIGHT, etc).
 *
 * int getOwn(int auction)
 *  - returns the number of items that the agent own for this
 *    auction
 *
 * Submitting Bids
 * ---------------------------------------------
 * void submitBid(Bid)
 *  - submits a bid to the tac server
 *
 * void replaceBid(OldBid, Bid)
 *  - replaces the old bid (the current active bid) in the tac server
 *
 *   Bids have the following important methods:
 *    - create a bid with new Bid(AuctionID)
 *
 *   void addBidPoint(int quantity, float price)
 *    - adds a bid point in the bid
 *
 * Help methods for remembering what to buy for each auction:
 * ----------------------------------------------------------
 * int getAllocation(int auctionID)
 *   - returns the allocation set for this auction
 * void setAllocation(int auctionID, int quantity)
 *   - set the allocation for this auction
 *
 *
 * Callbacks from the TACAgent (caused via interaction with server)
 *
 * bidUpdated(Bid bid)
 *  - there are TACAgent have received an answer on a bid query/submission
 *   (new information about the bid is available)
 * bidRejected(Bid bid)
 *  - the bid has been rejected (reason is bid.getRejectReason())
 * bidError(Bid bid, int error)
 *  - the bid contained errors (error represent error status - commandStatus)
 *
 * quoteUpdated(Quote quote)
 *  - new information about the quotes on the auction (quote.getAuction())
 *    has arrived
 * quoteUpdated(int category)
 *  - new information about the quotes on all auctions for the auction
 *    category has arrived (quotes for a specific type of auctions are
 *    often requested at once).

 * auctionClosed(int auction)
 *  - the auction with id "auction" has closed
 *
 * transaction(Transaction transaction)
 *  - there has been a transaction
 *
 * gameStarted()
 *  - a TAC game has started, and all information about the
 *    game is available (preferences etc).
 *
 * gameStopped()
 *  - the current game has ended
 *
 */

package se.sics.tac.aw;
import java.util.logging.*;

import se.sics.tac.util.ArgEnumerator;

public class Pascal extends AgentImpl {

  private static final Logger log =
    Logger.getLogger(DummyAgent.class.getName());

  private static final boolean DEBUG = false;
  private static final int CLIENTNO = 8;
  
  private float[] prices;

  private boolean[] packages; // The packages we want to buy
  private int[] dayPackages; // Store the auction number for a client
  
  private int[] arrivals; // The arrivals of the clients
  private int[] departures; // The departures of the clients
  
  private float bestValue=250;//Value which we directly buy a flight 
  private float[] limitsSup;
  private float[] limitsInf;
  private boolean[] updated;
  
  private float hotelPricePremium=50;
  private float hotelPriceCheap=50;
  //Delta is used for hotels 
  private float[] deltas;
  private float[] oldPricesHotel;
  
  protected void init(ArgEnumerator args) {
    prices = new float[agent.getAuctionNo()];
    deltas = new float[agent.getAuctionNo()];
    packages = new boolean[CLIENTNO * 3];
    dayPackages = new int[CLIENTNO * 3];
    for (int i = 0; i < CLIENTNO*3; packages[i++] = true);
    arrivals = new int[CLIENTNO];
    departures = new int[CLIENTNO];
    oldPricesHotel = new float[agent.getAuctionNo()];
    limitsSup = new float[agent.getAuctionNo()];
    limitsInf = new float[agent.getAuctionNo()];
    updated =new boolean[agent.getAuctionNo()];
    for(boolean val : updated)
    {
    	val = false;
    }
  }

  public void quoteUpdated(Quote quote) {
    int auction = quote.getAuction();
    int auctionCategory = agent.getAuctionCategory(auction);
    if (auctionCategory == TACAgent.CAT_HOTEL) {
    	int alloc = agent.getAllocation(auction);
    	if (alloc > 0 && quote.hasHQW(agent.getBid(auction)) &&
	  	quote.getHQW() < alloc) {
    		Bid bid = new Bid(auction);
	// Can not own anything in hotel auctions...
	//the average of deltas that is to say on average we calculate how evolved the AskPRice
    		deltas[auction]=(deltas[auction]+(quote.getAskPrice()-oldPricesHotel[auction]))/2;
	
	//so our price is the price asked Superior + Delta they put for a day
	//otherwise it is left unchanged
			if(prices[auction]< quote.getAskPrice()+deltas[auction])
			{
				prices[auction]=quote.getAskPrice()+deltas[auction];
				bid.addBidPoint(alloc, prices[auction]);
				agent.submitBid(bid);
			}
		
			if (DEBUG) {
			  log.finest("submitting bid with alloc="
				     + agent.getAllocation(auction)
				     + " own=" + agent.getOwn(auction));
			}
    	}
    } else if (auctionCategory == TACAgent.CAT_ENTERTAINMENT) {
    	int alloc = agent.getAllocation(auction) - agent.getOwn(auction);
    	if (alloc != 0) {
		Bid bid = new Bid(auction);
		if (alloc < 0)
			prices[auction] = 200f - (agent.getGameTime() * 120f) / 720000;
		else
			prices[auction] = 50f + (agent.getGameTime() * 100f) / 720000;
		bid.addBidPoint(alloc, prices[auction]);
		if (DEBUG) {
			log.finest("submitting bid with alloc"
			     + agent.getAllocation(auction)
			     + " own=" + agent.getOwn(auction));
		}
		agent.submitBid(bid);
	    }
	} else if (auctionCategory == TACAgent.CAT_FLIGHT) {
    	float askPrice=quote.getAskPrice();
    	int alloc = agent.getAllocation(auction);
    	limitsInf[auction]=(float)(agent.getQuote(auction).getAskPrice()-0.1*agent.getQuote(auction).getAskPrice());
    	if(alloc>0)
    	{
    		if(!updated[auction])
    		{
    			limitsSup[auction]=(float)(agent.getQuote(auction).getAskPrice()+0.1*agent.getQuote(auction).getAskPrice());
    			Bid oldbid =quote.getBid();
    			Bid bid = new Bid(auction);
    			bid.addBidPoint(alloc, limitsInf[auction]);
    			agent.replaceBid(oldbid, bid);
    			updated[auction]=true;
    		}
    		
    		//if there is a price above the initial price + 20 % we buy
    		if(askPrice>limitsSup[auction])
    		{
    			Bid oldbid =quote.getBid();
    			Bid bid = new Bid(auction);
    			bid.addBidPoint(alloc, 1000);
    			agent.replaceBid(oldbid, bid);
    		}
    		//If there is less than a minute we buy everything
    		else if(agent.getGameTimeLeft() < 60000)

    		{
    			Bid oldbid =quote.getBid();
    			Bid bid = new Bid(auction);
    			bid.addBidPoint(alloc, 1000);
    			agent.replaceBid(oldbid, bid);
    		}
    	}
    	
    }
  }

  public void quoteUpdated(int auctionCategory) {
    log.fine("All quotes for "
	     + agent.auctionCategoryToString(auctionCategory)
	     + " has been updated");
  }

  public void bidUpdated(Bid bid) {
    log.fine("Bid Updated: id=" + bid.getID() + " auction="
	     + bid.getAuction() + " state="
	     + bid.getProcessingStateAsString());
    log.fine("       Hash: " + bid.getBidHash());
  }

  public void bidRejected(Bid bid) {
    log.warning("Bid Rejected: " + bid.getID());
    log.warning("      Reason: " + bid.getRejectReason()
		+ " (" + bid.getRejectReasonAsString() + ')');
  }

  public void bidError(Bid bid, int status) {
    log.warning("Bid Error in auction " + bid.getAuction() + ": " + status
		+ " (" + agent.commandStatusToString(status) + ')');
  }

  public void gameStarted() {
    log.fine("Game " + agent.getGameID() + " started!");

    calculateAllocation();
    sendBids();
  }

  public void gameStopped() {
    log.fine("Game Stopped!");
  }

  public void auctionClosed(int auction) {
    log.fine("*** Auction " + auction + " closed!");
  }

  private void sendBids() {
    for (int i = 0, n = agent.getAuctionNo(); i < n; i++) {
      int alloc = agent.getAllocation(i) - agent.getOwn(i);
      float price = -1f;
      switch (agent.getAuctionCategory(i)) {
      case TACAgent.CAT_FLIGHT:
	if (alloc > 0) {
		
	  price = 100;
	}
	break;
      case TACAgent.CAT_HOTEL:
	if (alloc > 0) {
		if(agent.getAuctionType(i)== TACAgent.TYPE_CHEAP_HOTEL)
		{
			price = hotelPriceCheap;
			prices[i] = hotelPriceCheap;
		}
		else
		{
			price = hotelPricePremium;
			prices[i]=hotelPricePremium;
		}
	}
	break;
    case TACAgent.CAT_ENTERTAINMENT:
    // Starting prices : we want to buy at 200
    if (alloc > 0 && isWanted(i) == 1) {
		price = 200;
		prices[i] = 200f;
	}
	break;
      default:
	break;
      }
      if (price > 0) {
	Bid bid = new Bid(i);
	bid.addBidPoint(alloc, price);
	if (DEBUG) {
	  log.finest("submitting bid with alloc=" + agent.getAllocation(i)
		     + " own=" + agent.getOwn(i));
	}
	agent.submitBid(bid);
      }
    }
  }

  private void calculateAllocation() {
    for (int i = 0; i < 8; i++) {
      int inFlight = agent.getClientPreference(i, TACAgent.ARRIVAL);
      int outFlight = agent.getClientPreference(i, TACAgent.DEPARTURE);
      int hotel = agent.getClientPreference(i, TACAgent.HOTEL_VALUE);
      int type;
      arrivals[i] = inFlight;
      departures[i] = outFlight;

      // Get the flight preferences auction and remember that we are
      // going to buy tickets for these days. (inflight=1, outflight=0)
      int auction = agent.getAuctionFor(TACAgent.CAT_FLIGHT,
					TACAgent.TYPE_INFLIGHT, inFlight);
      agent.setAllocation(auction, agent.getAllocation(auction) + 1);
      auction = agent.getAuctionFor(TACAgent.CAT_FLIGHT,
				    TACAgent.TYPE_OUTFLIGHT, outFlight);
      agent.setAllocation(auction, agent.getAllocation(auction) + 1);

      // if the hotel value is greater than 70 we will select the
      // expensive hotel (type = 1)
      if (hotel > 70) {
	type = TACAgent.TYPE_GOOD_HOTEL;
      } else {
	type = TACAgent.TYPE_CHEAP_HOTEL;
      }
      // allocate a hotel night for each day that the agent stays
      for (int d = inFlight; d < outFlight; d++) {
	auction = agent.getAuctionFor(TACAgent.CAT_HOTEL, type, d);
	log.finer("Adding hotel for day: " + d + " on " + auction);
	agent.setAllocation(auction, agent.getAllocation(auction) + 1);
      }

      bestEntDay(inFlight, outFlight, i);
    }
  }

  private void bestEntDay(int inFlight, int outFlight, int client) {
	int[] types = getPreferences(client);
	int currentIdx = 0;
    for (int i = inFlight; i < outFlight && currentIdx < types.length; i++) {
      int auction = TACAgent.getAuctionFor(TACAgent.CAT_ENTERTAINMENT,
					types[currentIdx], i);
      agent.setAllocation(auction, agent.getAllocation(auction) + 1);
      currentIdx++;
    }
  }
  
  private int[] getPreferences(int client) {
	  int[] types = new int[3];
	  int e1 = agent.getClientPreference(client, TACAgent.E1);
	  int e2 = agent.getClientPreference(client, TACAgent.E2);
	  int e3 = agent.getClientPreference(client, TACAgent.E3);
	    
	  if (e1 > e2 && e1 > e3) {
		  types[0] = TACAgent.TYPE_ALLIGATOR_WRESTLING;
		  if (e2 > e3) {
			  types[1] = TACAgent.TYPE_AMUSEMENT;
			  types[2] = TACAgent.TYPE_MUSEUM;
		  } else {
			  types[2] = TACAgent.TYPE_AMUSEMENT;
			  types[1] = TACAgent.TYPE_MUSEUM;
		  }
	  } else if (e2 > e1 && e2 > e3) {
		  types[0] = TACAgent.TYPE_AMUSEMENT;
		  if (e1 > e3) {
			  types[1] = TACAgent.TYPE_ALLIGATOR_WRESTLING;
			  types[2] = TACAgent.TYPE_MUSEUM;
		  } else {
			  types[2] = TACAgent.TYPE_ALLIGATOR_WRESTLING;
			  types[1] = TACAgent.TYPE_MUSEUM;
		  }
	  } else {
		  types[0] = TACAgent.TYPE_MUSEUM;
		  if (e1 > e2) {
			  types[1] = TACAgent.TYPE_ALLIGATOR_WRESTLING;
			  types[2] = TACAgent.TYPE_AMUSEMENT;
		  } else {
			  types[2] = TACAgent.TYPE_ALLIGATOR_WRESTLING;
			  types[1] = TACAgent.TYPE_AMUSEMENT;
		  }
	  }
	  return types;
  }

  // Return the client interested by this entertainment auction, -1 if none
  private int isWanted(int auction) {
	  if (agent.getAuctionCategory(auction) != TACAgent.CAT_ENTERTAINMENT) {
		  return -1;
	  }
	  int type = agent.getAuctionType(auction);
	  int arr, dep;
	  int day = agent.getAuctionDay(auction);
	  for (int i = 0; i < CLIENTNO; i++) {
		  arr = arrivals[i];
		  dep = departures[i];
		  // Check that this option is feasible and that the client still not have this kind of entertainement
		  if (day >= arr && day < dep && packages[i*3 + type -1]) {
			  // Check the client has nothing planned for this day
			  if (dayPackages[i*3] != day && dayPackages[i*3+1] != day && dayPackages[i*3+2] != day) {
				  return i;
			  }
		  }
	  }
	  return -1;
  }

  // -------------------------------------------------------------------
  // Only for backward compability
  // -------------------------------------------------------------------

  public static void main (String[] args) {
    TACAgent.main(args);
  }

} // DummyAgent
