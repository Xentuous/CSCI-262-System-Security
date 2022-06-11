======================================
Requirements:
Java version: 1.8
Java runtime env: jdk1.8.0_291
Text editor: Visual Studio Code
IDE: IntelliJ IDEA Commmunity Edition
Supporting OS: Windows OS 32/64bit
======================================

Program created, runned and tested on: Windows 10 x64 based processor, IDEA Commmunity Edition, Visual Studio Code 
Java program file name: Rainbow.java
**Note**: This program will create and output a Rainbow.txt file at /Desktop/Rainbow.txt


Using CMD / Windows Terminal:
1) Navigate to the correct file path containing Rainbow.java and Wordlist.txt
2) Enter the following command
    java Rainbow.java Wordlist.txt

Code-explanation:

private static String reduct(String hex) 

This methods takes in a 32 character long, which then converts it into a BigInteger(Base 16).
The BigInteger value will then mod the total number of passwords inside the passwordList.
The resultant value will be the index which is use to retrieve the corresponding element from the passwordList.