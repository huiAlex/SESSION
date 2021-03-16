# SESSION
SESSION is a sentiment analysis tool of our ICPC'2021 paper " 
**[Exploiting the Unique Expression for Improved Sentiment Analysis in Software Engineering Text]**".
It uses sentence structure information to improve the sentiment analysis in SE texts. 
It contains a set of filter and adjust rules based on sentence structures inside SE texts, 
and combines these heuristics with the mainstream dictionary-based approach called SentiStrength. 
Our evaluation based on four different datasets showed that SESSION has the overall better performance 
and generalizability than three baseline approaches(SentiStrength, SentiStrength-SE, and Senti4SD). 

## Overview
1. ```src/main/java``` contains the codes of SESSION. 
  * ```sentstrength``` contains the main codes for sentiment analysis.The Main-Class is SentiStrength.
    The class ClassificationOptions contains initial settings for various rules. 
    The code about adjust rules is in the class Sentence.
    The code where Trigger start to work is in the class Paragraph.
  * ```preprocess``` contains the main codes for preprocessing and segment. 
    The inlet of preprocessing and segment is in the class PreProcess.
    The code about the part of speech tagging is in the class POSTagging.

  
2. ```data``` contains data used in SESSION.
  * ```benchmark_data``` contains benchmark datasets we used to evaluate SESSION. 
  As they were not generated and released by us, we do not provide them here. 
  If want to you use any of them, you should fulfill the licenses that they were released with and consider citing the original papers, and you can download original datasets at [Senti4SD](https://github.com/collab-uniba/Senti4SD)and [Lin et.al@ICSE2018](https://sentiment-se.github.io/replication.zip).

  * ```sentistrength_data```  contains SentiStrength's various lists used for sentiment analysis.


## Running SESSION
1. Introduce dependencies described in ```pom.xml```.
2. Set arguments in ```main``` function of ```sentistength/SentiStrength.java```, e.g., ```"-input data/benchmark_data/*.txt trinary"```, where 'trinary' means that report positive-negative-neutral classifcation.
    By default, the positive and negative scores for the input text are output, where + 1 ≤ the positive score ≤ + 5 and − 5 ≤ the negative score≤ − 1.
    The result file will be outputted under the same directory of the input file by default when SESSION executed successfully, and you can set output directory in arguments. 
  
    Using "help" in program arguments，you can get command line option lists, 
    which conains our new options and the original options of SentiStrength. 
    Here are some key options related to our work：
    * noDealWithPreprocess(don't preprocess)
    * noDealWithRule_Trigger(shut down Rule_Trigger)
    * noDealWithSubjunctiveMood (don't deal with subjunctive mood)
    * noDealWithPolysemy (don't deal with polysemy)
    * noDealWithNegative (don't deal with negative word)

