# Akka-Streams-proc

Project developed as part of the interview process for Simplaex.
The task fundamentals can be found at: https://github.com/scravy/exercise 

## Getting Started

The project was developed on top of akka-actors/ akka-stream along with other libs like alpakka
Scala 2.12.8


### Prerequisites

To run the Project sbt(1.0+) is needed

## Running the tests

To run the tests run the command:

```
sbt test
```

This will run the test class and print the steps and what should happen.

## Deployment

Within the project folder:

To run the project on your terminal, with sbt installed, just run:

```
sbt run
```

To exit just press ```Ctrl + C```

You can also run ```sbt assembly``` and create the .jar file.
Th file will be created at: ```target/scala-2.12/akka-streams-proc-assembly-1.0.jar```

To run this .jar, you are going to need scala/java installed.

```scala target/scala-2.12/akka-streams-proc-assembly-1.0.jar```

OR

```java -jar target/scala-2.12/akka-streams-proc-assembly-1.0.jar```


## Next Steps

There is a couple changes that can be done to improve:
 1- The aggregation uses groupBy and foldLeft, but those can not maintain ordering of the keys when
 grouping, due the Map implementation, as was not clear on the task, I left this for a later stage.
 
 2- Verify if its possible to run on one stream only instead of broadcast.

## Authors

* **Bruno Rossi** -  [GitHub](https://github.com/brunobrossi)


