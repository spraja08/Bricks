/**
 * @author Raja SP
 * version 1.0.0.0
 */

package org.steam.bricks;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import junit.framework.Assert;

class BricksTest {

	Bricks bricks = new Bricks();

	@BeforeEach
	void setUp() throws Exception {
		bricks.addBrick( "Current Balance is {NumericOperator} {NumericValue}", "\\s?((\\(\\s?)*)Current\\s?Balance\\s?is\\s?(greater than|less than|equal to)\\s?([0-9]+)\\s?((\\)\\s?)*)(AND|OR)?\\s?", "$1 LATEST_BALANCE_Lifetime $3 $4 $5 $7" ); // First
		bricks.addBrick( "Been {NumericValue} days since the last {OfferId}", "\\s?((\\(\\s?)*)Been\\s?([0-9]+)\\s?days\\s?since\\s?the\\s?last\\s?([a-zA-Z0-9]+)\\s?((\\)\\s?)*)(AND|OR)?\\s?", "$1 daysSinceLastOffer( $4 ) >= $3 $5 $7" );
		bricks.addBrick( "Average of last {NumericValue} Topup is {NumericOperator} {NumericValue}", "\\s?((\\(\\s?)*)Average\\s?of\\s?last\\s?([0-9]+)\\s?Topup\\s?is\\s?(greater than|less than|equal to)\\s?([0-9]+)\\s?((\\)\\s?)*)(AND|OR)?\\s?", "$1 getAverageTopup( $3 ) $4 $5 $6 $8" );
		bricks.addBrick( "The value of offer attribute {StringValue} is {StringOperator} {StringValue}", "this will be autocreated", "$1 getOfferAttribute( OFFER_ID, $3 ).$4( $5 ) $6 $8" );
	}

	@Test
	void testGetTypeAheadMatches() {
		String input = "offer attribute";
		JsonObject typeAheadList = bricks.getTypeAheadMatches( input );
		String typeAheadString = typeAheadList.toString();
		assertEquals( typeAheadString, "{\"status\":\"success\",\"result\":[\"The value of offer attribute {StringValue} is {StringOperator} {StringValue}\"]}", "matched" );
	}

	@Test
	void testValidateHappyScenario() {
		String input = "(        Current     Balance is greater than 10 AND\nBeen 10 days since the last OFFERABC12  )          OR    \nAverage of last 10 Topup is greater than 100        ";
		JsonObject result = bricks.validate( input );
		assertEquals( result.toString(), "{\"status\":\"success\"}", "matched" );
	}

	@Test
	void testValidateWrongParameterType() {
		String input = "( Current     Balance is greater than 10 AND\nBeen 10vg days since the last OFFERABC12        )  OR    \nAverage of last 10fd Topup is greater than 100        ";
		JsonObject result = bricks.validate( input );
		assertEquals( result.toString(), "{\"status\":\"failure\",\"errors\":[\"Type mismatch or invalid statement - Been 10vg days since the last OFFERABC12 ) OR \",\"Type mismatch or invalid statement - Average of last 10fd Topup is greater than 100 \"]}", "matched" );
	}

	@Test
	void testValidateMismatchingParanthesis() {
		String input = "(     ( (Current     Balance is greater than 10 AND\nBeen 10 days since the last OFFERABC12 )      \nAverage of last 10 Topup is greater than 100) )     )  ";
		JsonObject result = bricks.validate( input );
		assertEquals( result.toString(), "{\"status\":\"failure\",\"errors\":[\"Paranthesis does not match\",\"AND|OR connectors are invalid - Been 10 days since the last OFFERABC12 ) \"]}", "matched" );
	}

	@Test
	void testConvertToJava() {
		String input = "((Current     Balance is greater than 10 AND\n The value of offer attribute \"offerGroup\" is string not equal to \"refill\" AND\n Been 10 days since the last OFFERABC12 )    OR  \n" + "( Average of last 10 Topup is greater than 100 ) )";
		String result = bricks.convertToJava( input ).get( "result" ).getAsString();
		assertEquals( result, "(( LATEST_BALANCE_Lifetime  >  10  &&\n" + " !  getOfferAttribute( OFFER_ID, \"offerGroup\" ).equals( \"refill\" )  &&\n" + " daysSinceLastOffer( OFFERABC12 ) >= 10 )  ||\n" + "(  getAverageTopup( 10 )  >  100 ) ) ", "matched" );
	}

	@Test
	void testCreateRegex() {
		String input = "Current Balance is {NumericOperator} {NumericValue}";
		String output = bricks.createRegex( input );
		assertEquals( output, "\\s?((\\(\\s?)*)Current\\s?Balance\\s?is\\s?(greater than|less than|greater than or equal to|less than or equal to|equal to|not equal to)\\s?([0-9]+)\\s?((\\)\\s?)*)(AND|OR)?\\s?", "matched" );
	}
}
