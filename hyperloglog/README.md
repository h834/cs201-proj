# Probabilistic Set Analysis (`my-app`)

This project provides Java classes for performance and accuracy testing of a probabilistic set data structure, `ProbabilisticSet`, which uses the Apache DataSketches HLL (HyperLogLog) library.

The project contains two main executable classes:
1.  `ProbabilisticTester`: Runs performance benchmarks for `add` and `compareOverlap` operations.
2.  `AccuracyTester`: Tests the accuracy of the `compareOverlap` method against known set sizes.

---

## Prerequisites

Before you can run this project, you will need the following software installed on your system:

* **Java Development Kit (JDK) 11**: The project is configured to use Java 11.
* **Apache Maven**: This is used to compile the code, manage dependencies, and run the applications.

---

## How to Run

### 1. File Placement

The `ProbabilisticTester` class requires a data file named `airline_users.csv` to run its performance tests.

* **Place `airline_users.csv` in the `my-app/` directory.** This is the root directory where the `pom.xml` file is located.

### 2. Compile the Project

Navigate to the `my-app` directory in your terminal (the one containing `pom.xml`) and run the Maven `compile` command. This will download the required DataSketches dependency and compile all Java source files.

```bash
cd path/to/cs201-proj/my-app
mvn compile exec:java -Dexec.mainClass="com.mycompany.app.AccuracyTester" 
mvn compile exec:java -Dexec.mainClass="com.mycompany.app.ProbabilisticTester"