#! /bin/bash
# This script is used to start the client
#

app=Client

function compile {
	javac $app.java
}

function run {
	java $app
}

function main {
	compile
	run
	rm -f */*.class *.class
}

main
