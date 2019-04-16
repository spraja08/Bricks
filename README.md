# Bricks
Convert English Statements to Java

If you are building an analytical engine or any application that involves executing some dynamic (user provided) rules, you would have 2 options

   1) To let the users code the rules in a programming language (say, java or python) and execute those snippets in runtime.
   2) Or let the users create the rules in natural language, translate those rules into a programming language (java, python etc) and then execute those snippers in runtime.
   
Obviously, option 2 is better as it does not require any skills from the users. In this contribution, I have given some clues about how to use natural language templates to enable users to create the rules and convert the same into java dynamically.

For example, you have the following java rule based on an already available function and you want to enable a correspending natural language equivalent for this

getAverageBalance( int customerId ) > 100

You may configure the natural language temmplate as below 

Average Balance of Customer is greater than 100

Using a GUI, you may allow user to markup certain portions in the above statement as dynamic values that will be supplied by the users later.

Average Balance of {CustomerId} is {NumericOperator} {NumericValue}

This would become a template for capturing all the rules of this form. 

In the above statement, {Numeric Operator} represents a regular expression "(greater than|less than|equal to)" and {NumnericValue} represents ([0-9]+). And the {CustomerId} is an integer value.

You may autoamtically generate a corresponding regular expression that captures the abvoe syntax as below.

\s?((\(\s?)*)Average\s?Balance\s?of\s?([0-9]+)\s?is\s?(greater than|less than|equal to)\s?([0-9]+)\s?((\)\s?)*)(AND|OR)?\s?

In the above the first regex group is the open paranthesis, the last but one Group is the close parnathesis and the last group is the Logical Connectors. You may also generate a corresponding java template automatically as below 

$1 getAverageBalance( $3 ) $4 $5 $6 $8.

Thats it. You can pop the template to the user and make them customise the portions within {} by providing actual values. From there, you may validate and generate the java code. Check my code for the implementation.
