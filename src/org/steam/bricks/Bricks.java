/**
 * @author Raja SP
 * version 1.0.0.0
 */

package org.steam.bricks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class Bricks {

	List< Object[] > _allBricks;
	HashMap< String, String > _logicalOperatorsToJava;
	HashMap< String, String > _englishToRegex;
	JsonArray _allBricksJsonArray;
	
	public Bricks(){
		super();
		_allBricks = new ArrayList< Object[] >();
		_allBricksJsonArray = new JsonArray();
		
		_logicalOperatorsToJava = new LinkedHashMap< String, String >();
		_logicalOperatorsToJava.put( "string equal to", "equals" );
		_logicalOperatorsToJava.put( "string not equal to", "equals" );
		_logicalOperatorsToJava.put( "greater than or equal to", " >= " );
		_logicalOperatorsToJava.put( "less than or equal to", " <= " );
		_logicalOperatorsToJava.put( "greater than", " > " );
		_logicalOperatorsToJava.put( "less than", " < " );
		_logicalOperatorsToJava.put( "equal to", " == " );
		_logicalOperatorsToJava.put( "not equal to", " != " );
		_logicalOperatorsToJava.put( "contains", " contains " );
		_logicalOperatorsToJava.put( "starts with", " startsWith " );
		_logicalOperatorsToJava.put( "ends with", " endsWith " );
		
		
		_englishToRegex = new HashMap< String, String >();
		_englishToRegex.put( "NumericOperator", "(greater than|less than|greater than or equal to|less than or equal to|equal to|not equal to)" );
		_englishToRegex.put( "StringOperator", "(string equal to|string not equal to|contains|does not contain|starts with|ends with)" );
		_englishToRegex.put( "StringValue", "([a-zA-Z0-9\"]+)" );
		_englishToRegex.put( "NumericValue", "([0-9]+)" );
		_englishToRegex.put( "openBracketToRegex", "((\\(\\s?)*)" );
		_englishToRegex.put( "closeBracketToRegex", "\\s?((\\)\\s?)*)" );
		_englishToRegex.put( "spacesToRegex", "\\s?" );
		_englishToRegex.put( "logicalConnectorsToRegex", "(AND|OR)?" );
		_englishToRegex.put( "OfferId", "([a-zA-Z0-9]+)" );
	}

	public Function< String, String >normalizeSpaces = ( input ) -> input.replaceAll( "( )+", " " );
	
	public void addBrick( String annotatedStmt, String regex, String javaTemplate ) {
		Object[] aBrick = new Object[ 4 ];
		aBrick[ 0 ] = annotatedStmt;
		aBrick[ 1 ] = this.createRegex( annotatedStmt  );
//		aBrick[ 2 ] = Pattern.compile( regex );
		aBrick[ 2 ] = Pattern.compile( ( String )aBrick[ 1 ] );
		aBrick[ 3 ] = javaTemplate;
		_allBricks.add( aBrick );
		
		_allBricksJsonArray.add( new JsonPrimitive( annotatedStmt ) );
	}
	
	
	public JsonObject getTypeAheadMatches( String brickSnippet ) {
		String[] allStatements = brickSnippet.split( "\\n" );
		String brickStatement = allStatements[ allStatements.length - 1 ];
		List< Object[] > matchingList = _allBricks.stream()
				.filter( ( input ) -> ( ( ( String )input[ 0 ] ).toUpperCase().contains( brickStatement.toUpperCase() ) ) )
				.collect( Collectors.toList() );
		JsonObject result = new JsonObject();
		result.addProperty( "status", "success" );
		JsonArray resultArray = new JsonArray();
		if( matchingList != null && matchingList.size() == 0 )  
			result.add( "result", _allBricksJsonArray );
		else {
		    matchingList.forEach( ( aBrick ) -> resultArray.add( new JsonPrimitive( ( String )aBrick[ 0 ]) ) );
		    result.add( "result", resultArray );
		}    
		return result;
	}

	
	public boolean validateMatchingParanthesis( String input ) {
		input = normalizeSpaces.apply( input );
		Stack< Integer > stack = new Stack< Integer >();
		input.chars().forEach( ( ch ) -> {
			if( ch == '(' ) stack.push( ch );
			else if( ch == ')' && !stack.isEmpty() && stack.peek() == '(' ) stack.pop();
			else if( ch == ')' && stack.isEmpty() ) stack.push( ch );
		} );
		return stack.isEmpty();
	}
	
	
	public JsonObject validate( String brickSnippet ) {
		brickSnippet = normalizeSpaces.apply( brickSnippet );
		String[] allStatements = brickSnippet.split( "\\n" );
		Predicate< String > invalidStatementFilter = ( input ) ->  ! _allBricks.stream().
					anyMatch( ( aBrick ) -> ( ( Pattern ) aBrick[ 2 ] ).matcher( input ).matches()  );
		JsonArray errors = new JsonArray();
		if( ! validateMatchingParanthesis( brickSnippet ) )
			errors.add( new JsonPrimitive( "Paranthesis does not match" ) );	
		for( int i=0; i<allStatements.length; i++ ) {
			if( ( i == ( allStatements.length - 1 ) && ( allStatements[ i ].trim().endsWith( "AND" ) || allStatements[ i ].trim().endsWith( "OR" ) ) ) ||
			    ( i <  ( allStatements.length - 1 ) && ! ( allStatements[ i ].trim().endsWith( "AND" ) || allStatements[ i ].trim().endsWith( "OR" ) ) ) )  
				errors.add( new JsonPrimitive( "AND|OR connectors are invalid - " + allStatements[ i ] ) );
		}
		ArrayList< String > stmtErrors = new ArrayList< String >();
		stmtErrors.addAll( Arrays.asList( allStatements ).stream()
				.filter( invalidStatementFilter )
				.collect( Collectors.toList() ) );
		stmtErrors.forEach( ( anError ) -> errors.add( new JsonPrimitive( "Type mismatch or invalid statement - " + anError ) ) );
		JsonObject result = new JsonObject();
		if( errors != null && errors.size() == 0 )
		    result.addProperty( "status", "success" );
		else {
			result.addProperty( "status", "failure" );
			result.add( "errors", errors );
		}
		return result;
	}
	
	
	public JsonObject convertToJava( String brickSnippet ) {
		JsonObject validationResult = this.validate( brickSnippet );
		if( validationResult.get( "status" ).getAsString().equals( "failure" ) )
			return validationResult;
		brickSnippet = normalizeSpaces.apply( brickSnippet );
		String[] allStatements = brickSnippet.split( "\\n" );
		Function< String, String > replacer = ( brickStatement ) -> {
			Object[] matchingBrick = _allBricks.stream()
					.filter( ( aBrick ) -> ( ( Pattern ) aBrick[ 2 ] ).matcher( brickStatement ).matches() )
					.findFirst()
					.get();
			System.out.println(  " matching Brick fot stmt : " + brickStatement + " =  " + matchingBrick[ 1 ] );
			Matcher matcher = ( ( Pattern )matchingBrick[ 2 ] ).matcher( brickStatement );
			matcher.matches();
			String javaStatement = matchingBrick[ 3 ].toString();
			String partialStmt =  matcher.replaceAll( javaStatement );
			partialStmt = partialStmt.replaceAll( "AND$", "&&" ).replaceAll( "OR$", "||" );
			Set< String > keys = _logicalOperatorsToJava.keySet();
			Iterator< String > iterator = keys.iterator();
			while( iterator.hasNext() ) {
				String key = iterator.next();
				if( key.contains( " not " ) && partialStmt.contains(  key ) ) {
					Matcher stmtPattern = Pattern.compile( "((\\(\\s?)*)(.+)" ).matcher( partialStmt );
					stmtPattern.matches();
					partialStmt = stmtPattern.group( 1 ) + " ! " + stmtPattern.group( 3 );
				} 
				partialStmt = partialStmt.replace( key, _logicalOperatorsToJava.get( key ) );
			}			
			return partialStmt;
        	};
		String result = Arrays.asList( allStatements ).stream().map( replacer ).collect( Collectors.joining( "\n" ) );
		JsonObject jRes = new JsonObject();
		jRes.addProperty( "status", "success" );
		jRes.addProperty( "result", result );
		return jRes;
	}
	
	public String createRegex( String input ) {
		input = normalizeSpaces.apply( input );
		input = input.replaceAll( " ", "\\\\s?" );
		Pattern pattern = Pattern.compile( "\\{\\w+\\}" );
		Matcher matcher = pattern.matcher( input );
		matcher.matches();
		while( matcher.find() ) {
			input = input.replace( matcher.group(), _englishToRegex.get( matcher.group().replace( "{", "" ).replace( "}", "" ) ) );
		}	
		return _englishToRegex.get( "spacesToRegex" ) + _englishToRegex.get( "openBracketToRegex" )  +  input + _englishToRegex.get( "closeBracketToRegex" ) 
		                                              + _englishToRegex.get( "logicalConnectorsToRegex" ) 
		                                              + _englishToRegex.get( "spacesToRegex" ) ;
	}
	
			
	public static void main( String[ ] args ) {
		Bricks bricks = new Bricks();
		
		bricks.addBrick( "Current Balance is {NumericOperator} {NumericValue}", 
				         "\\s?((\\(\\s?)*)Current\\s?Balance\\s?is\\s?(greater than|less than|equal to)\\s?([0-9]+)\\s?((\\)\\s?)*)(AND|OR)?\\s?",
				         "$1 LATEST_BALANCE_Lifetime $3 $4 $5 $7" ); //First Group = open paranthesis, Last but one Group = close parnathesis, Last Group = Logical Connectors
		
		bricks.addBrick( "Been {NumericValue} days since the last {OfferId}", 
				          "\\s?((\\(\\s?)*)Been\\s?([0-9]+)\\s?days\\s?since\\s?the\\s?last\\s?([a-zA-Z0-9]+)\\s?((\\)\\s?)*)(AND|OR)?\\s?",
				         "$1 daysSinceLastOffer( $4 ) >= $3 $5 $7" );
		
		bricks.addBrick( "Average of last {NumericValue} Topup is {NumericOperator} {NumericValue}", 
				         "\\s?((\\(\\s?)*)Average\\s?of\\s?last\\s?([0-9]+)\\s?Topup\\s?is\\s?(greater than|less than|equal to)\\s?([0-9]+)\\s?((\\)\\s?)*)(AND|OR)?\\s?",
				         "$1 getAverageTopup( $3 ) $4 $5 $6 $8" );
		
		bricks.addBrick( "The value of offer attribute {StringValue} is {StringOperator} {StringValue}", 
		         "this will be autocreated",
		         "$1 getOfferAttribute( OFFER_ID, $3 ).$4( $5 ) $6 $8" );
	
		
		System.out.println( "---------Test for Typeahead----------------" );
		String input = "offer attribute";
		System.out.println(  "input : " + input );
		JsonObject typeAheadList = bricks.getTypeAheadMatches( input );
		System.out.println( "\noutput : " + typeAheadList.toString() );
		
		System.out.println( "\n\n----------Test for validation success----------------" );		
		input = "(        Current     Balance is greater than 10 AND\nBeen 10 days since the last OFFERABC12  )          OR    \nAverage of last 10 Topup is greater than 100        ";
		System.out.println(  "input : " + input );
		System.out.println( "\noutput : " +  bricks.validate( input ) );
		
		System.out.println( "\n\n----------Test for wrong parameter type---------------" );
		input = "( Current     Balance is greater than 10 AND\nBeen 10vg days since the last OFFERABC12        )  OR    \nAverage of last 10fd Topup is greater than 100        ";
		System.out.println(  "input : " + input );
		System.out.println( "\noutput : " +  bricks.validate( input ) );

		System.out.println( "\n\n----------Test for Mismatching Paranthesis---------------" );
		input = "(     ( (Current     Balance is greater than 10 AND\nBeen 10 days since the last OFFERABC12 )      \nAverage of last 10 Topup is greater than 100) )     )  ";
		System.out.println(  "input : " + input );
		System.out.println( "\noutput : " +  bricks.validate( input ) );
		
		System.out.println( "\n\n----------Test for Bricks to java Conversion ---------------" );
		input = "((Current     Balance is greater than 10 AND\n The value of offer attribute \"offerGroup\" is string not equal to \"refill\" AND\n Been 10 days since the last OFFERABC12 )    OR  \n"
				+ "( Average of last 10 Topup is greater than 100 ) )";
		System.out.println(  "input : " + input );
		System.out.println( "\noutput : " + bricks.convertToJava( input ).get( "result" ).getAsString() );
		
		System.out.println( "\n\n----------Test for creating Regex ---------------" );
		input = "Current Balance is {NumericOperator} {NumericValue}";
		System.out.println(  "input : " + input );
		String output = bricks.createRegex( input );
		System.out.println( "Eoutput : " + "\\s?((\\(\\s?)*)Current\\s?Balance\\s?is\\s?(greater than|less than|greater than or equal to|less than or equal to|equal to|not equal to)\\s?([0-9]+)\\s?((\\)\\s?)*)(AND|OR)?\\s?" );
		System.out.println( "Aoutput : " + output );

		System.out.println( "------------" );

		input = "Been {NumericValue} days since the last {OfferId}";
		System.out.println(  "input : " + input );
		output = bricks.createRegex( input );
		System.out.println( "Eoutput : " +  "\\s?((\\(\\s?)*)Been\\s?([0-9]+)\\s?days\\s?since\\s?the\\s?last\\s?([a-zA-Z0-9]+)\\s?((\\)\\s?)*)(AND|OR)?\\s?" );
		System.out.println( "Aoutput : " + output );
		
		System.out.println( "------------" );

		input = "Average of last {NumericValue} Topup is {NumericOperator} {NumericValue}";
		System.out.println(  "input : " + input );
		output = bricks.createRegex( input );
		System.out.println( "Eoutput : " +   "\\s?((\\(\\s?)*)Average\\s?of\\s?last\\s?([0-9]+)\\s?Topup\\s?is\\s?(greater than|less than|greater than or equal to|less than or equal to|equal to|not equal to)\\s?([0-9]+)\\s?((\\)\\s?)*)(AND|OR)?\\s?" );
		System.out.println( "Aoutput : " + output );
	}
}
