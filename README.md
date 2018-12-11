# IntentResolutionApi

Aim : Recognizing intents with slots using OpenNLP to convert natural language into structured commands with arguments.
```
> Show me some flights from Atlanta to Minneapolis on monday?
> {'name' : 'flightCityToCity' , 'entities' : {'city' : ['Atlanta','Minneapolis'] , 'dateTime' : ['monday'] }}
```
The example system uses document categorization to determine the intent (command) and entity recognition to determine the slots (arguments) of the natural language text input.

The training system uses a directory containing separate files for each possible action, in this case the actions in a fictitious flight application:
```
- IntentResolutionApi\src\main\resources\training
  - flightCityToCity.txt - get the flight list between two cities
  - flightDtlsCity.txt - get the flight list from one city
 ```
Each training file contains at least one example per line with any possible arguments surrounded by mark up to indicate the name of the parameter:

file: *flightCityToCity.txt*
```
flight from <START:city> Atlanta <END> to <START:city> Minneapolis <END> on <START:dateTime> monday <END>?
...
```

There is a dictionary file in the following path : *-IntentResolutionApi\dictionaries\dict.txt*
Any number of words can be added to the file along with its type in the following format :
```
city=delhi,kolkata,mumbai,hyderabad
```

These words will be added to the parameter list as dictionary words.
```
> {'name' : 'flightCityToCity' , 'entities' : {'dateTime' : ['monday'] } , 'dictionaryWords' : { 'city' : ['Atlanta','Minneapolis'] } }
```

# Running the Application:
- Run the IntentResolutionApiApplication.java file as java application 

# Test the Application:
*Rest Endpoint URL:*
```
>http://localhost:8080/nlp/intent/getAllIntentNames [GET]
```
```
>http://localhost:8080/nlp/intent/create [POST]
{
  "intent" : "flightCityToCity",
  "desc": "the type of <START:movieName> predator <END>"
}
```
```
>http://localhost:8080/nlp/intent/update [POST]
{
  "intent" : "flightCityToCity",
  "desc": "the type of <START:movieName> predator <END>"
}
```
```
>http://localhost:8080/nlp/intent/get [POST]
{
  "desc":"Show me some flights from Atlanta to Minneapolis on monday?"
}
```


