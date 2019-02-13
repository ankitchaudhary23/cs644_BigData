# Hadoop-MapReduce
Map Reduce program to find the missing cards in a deck.
# How to Run?
.pull the code

.copy the testfile or any other file you want to test on the hdfs using the command :
" hadoop fs -put missingCards\ Input\ testFile.txt "

.make the jar file of the program or use the one provided
.run the jar file with the text file as the input file using the command :
" hadoop jar missingCards.jar Poker\ Input\ testFile.txt output "
