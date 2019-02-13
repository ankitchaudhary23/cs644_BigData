#ALGORITHM

Step-1: Splitting all the words in the file keeping the delimiter as "\\s".
Step-2: Collect all the words which are obtained after splitting in the Step-1 and put them in a String Array.
Step-3: Parse all the words in the file.
Step-4: Collect pair of consecutive words.
Step-5: Now the first step of the reducer is to put them in a key value pair. All word pairs are the key and their value is set to 1.
Step-6: Mapper will bring together the same pair of words.
Step-7: Now the reducer will total the value of the word pair with and put them in a Tree Set with the key and value i.e. key being the word pair and the value as the frequency.
Step-8: Finally consider only the top 100 word pair and write the word pair to the output file.
