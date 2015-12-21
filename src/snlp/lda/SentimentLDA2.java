

package snlp.lda;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.plaf.synth.SynthSpinnerUI;

import org.apache.commons.io.filefilter.AndFileFilter;
import org.w3c.dom.css.Counter;

/**
 * jLDADMM: A Java package for the LDA and DMM topic models
 * 
 * Implementation of the Latent Dirichlet Allocation topic model, using
 * collapsed Gibbs sampling, as described in:
 * 
 * Thomas L. Griffiths and Mark Steyvers. 2004. Finding scientific topics.
 * Proceedings of the National Academy of Sciences of the United States of
 * America, 101(Suppl 1):5228–5235.
 * 
 * @author: Dat Quoc Nguyen
 * 
 * @modifier: Qiming Chen
 */

public class SentimentLDA2
{
//	public double alpha; // Hyper-parameter alpha
//	public double beta; // Hyper-parameter alpha
	
	public double[] alpha; // Hyper-parameter alpha- doc topic
	public double[][] beta; // Hyper-parameter beta- topic word

	public double[] gamma; //Hyper-parameter for sentiment
	public double gammaSum;
	
	public int numDocuments; // Number of documents in the corpus
	public int numTopics; // Number of topics
	public int numIterations; // Number of Gibbs sampling iterations
	public int numTopWords; // Number of most probable words for each topic

	public int numSentiment;
	
	public double alphaSum; // alpha * numTopics
	public double[] betaSum; // beta * vocabularySize

	public List<List<Integer>> corpus; // Word ID-based corpus
	public List<List<Integer>> topicAssignments; // Topics assignments for words in the corpus
	public int[][] sentimentAssignment;
	
	public int numWordsInCorpus; // Number of words in the corpus

	public HashMap<String, Integer> word2IdVocabulary; // Vocabulary to get ID given a word
	public HashMap<Integer, String> id2WordVocabulary; // Vocabulary to get word given an ID
	public int vocabularySize; // The number of word types in the corpus

	public HashMap<String, String> topic2word;
	public HashMap<String, String> word2topic;
	
	public List<List<Integer>> sentimentLabel;
	
	// numDocuments * numTopics matrix
		// Given a document: number of its words assigned to each topic
		public int[][] docTopicCount;
		// Number of words in every document
		public int[] sumDocTopicCount;
		// numTopics * vocabularySize matrix
		// Given a topic: number of times a word type assigned to the topic
//		public int[][] topicWordCount;
//		// Total number of words assigned to a topic
//		public int[] sumTopicWordCount;
		
		public int[][][] docTopicSentimentCount;
		public int[][] sumDocTopicSentimentCount;
		
		public int[][][] topicSentimentWordCount;
		public int[][] sumTopicSentimentWordCount;

	// Double array used to sample a topic
	public double[] multiPros;

	// Path to the directory containing the corpus
	public String folderPath;
	// Path to the topic modeling corpus
	public Vector<String> corpusPath;

	public String expName = "LDAmodel";
	public String orgExpName = "LDAmodel";
	public String tAssignsFilePath = "";
	public int savestep = 0;
	
	public Vector<String> review_id;

	public SentimentLDA2(Vector<String> pathToCorpus, String pathToVocab, int inNumTopics,
			double inAlpha, double inBeta, int inNumIterations, int inTopWords,
			String inExpName, String pathToOutput)
		throws Exception
	{
		this(pathToCorpus, pathToVocab,inNumTopics, inAlpha, inBeta, inNumIterations,
			inTopWords, inExpName,pathToOutput, "", 0);
	}

	public SentimentLDA2(Vector<String> pathToCorpus, String pathToVocab, int inNumTopics,
			double inAlpha, double inBeta, int inNumIterations, int inTopWords,
			String inExpName, String pathToOutput, int inSaveStep)
		throws Exception
	{
		this(pathToCorpus, pathToVocab,inNumTopics, inAlpha, inBeta, inNumIterations,
			inTopWords, inExpName,pathToOutput, "", inSaveStep);
	}
	
	public SentimentLDA2(Vector<String> pathToCorpus, String pathToVocab, int inNumTopics,
		double inAlpha, double inBeta, int inNumIterations, int inTopWords,
		String inExpName, String pathToOutput, String pathToTAfile, int inSaveStep)
		throws Exception
	{

		numSentiment =2;
		
		//numDocuments=pathToCorpus.size();
		numDocuments=990626;
		numDocuments=500000;
		//numDocuments=100;
		
		numTopics = inNumTopics;
		numIterations = inNumIterations;
		numTopWords = inTopWords;
		savestep = inSaveStep;
		expName = inExpName;
		orgExpName = expName;
		corpusPath = pathToCorpus;
		folderPath = pathToOutput + "/";

		//System.out.println("Reading topic modeling corpus: " + pathToCorpus);

		sentimentAssignment = new int[numDocuments][numTopics];
		
		word2IdVocabulary = new HashMap<String, Integer>();
		id2WordVocabulary = new HashMap<Integer, String>();
		//get review_id
		BufferedReader brid = new BufferedReader(new FileReader("/Users/QimingChen/Desktop/Yelp_Review2/review_id.txt"));
		review_id=new Vector<String>();
		for (int i = 0; i < numDocuments; i++) {
			String id=brid.readLine();
			review_id.add(id);
		}
		
		//get vocabulary
		BufferedReader br = null;
		try {
			int indexWord = -1;
			br = new BufferedReader(new FileReader(pathToVocab));
			for (String word; (word = br.readLine()) != null;) {
				indexWord ++;
				word2IdVocabulary.put(word.trim(), indexWord);
				id2WordVocabulary.put(indexWord, word.trim());
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		vocabularySize = word2IdVocabulary.size();
		
		//////////////////////////////////////////////////////////////////////////
		//get sentiment label
		Vector<String> inPathSentiment=new Vector<String>();
		inPathSentiment.add("/Users/QimingChen/Desktop/label/negative-words.txt");
		inPathSentiment.add("/Users/QimingChen/Desktop/label/positive-words.txt");
		
		sentimentLabel=new ArrayList<List<Integer>>();
		try {
			for (int i = 0; i < numSentiment; i++) {
				System.out.println(i);
				br = new BufferedReader(new FileReader(inPathSentiment.get(i)));
				List<Integer> sentimentWords = new ArrayList<Integer>();
			
				for (String word; (word = br.readLine()) != null;) {
					word=word.trim();
					if (word.length()!=0) {
							if (word2IdVocabulary.containsKey(word)) {
								sentimentWords.add(word2IdVocabulary.get(word));
							}
					}
				}
				sentimentLabel.add(sentimentWords);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		//////////////////////////////////////////////////////////////////////////

		corpus = new ArrayList<List<Integer>>();
		
		numWordsInCorpus = 0;
		corpus = new ArrayList<List<Integer>>();
		br = new BufferedReader(new FileReader("/Users/QimingChen/Desktop/Yelp_Review2/review.txt"));
		
		try {
			//for (int i = 0; i < numDocuments; i++) {System.out.println(i);
				//br = new BufferedReader(new FileReader(pathToCorpus.get(i)));
				int count=0;
				for (String doc; (doc = br.readLine()) != null && count < numDocuments;) {
					System.out.println(count); count++;
					
					List<Integer> document = new ArrayList<Integer>();
					
					if (doc.trim().length() == 0)
						continue;
					
					String[] words = doc.trim().split("\\s+");
					for (String word : words) {
						String[] words2 = word.trim().split("[\\W&&[^\"']]");
						for (String word2 : words2) {
							word2=word2.replaceAll("[0-9]", "");
							word2=word2.toLowerCase();
							
							if (!sentimentLabel.get(0).contains(word2) || !sentimentLabel.get(1).contains(word2)) {
								continue;
							}
							//if (word2IdVocabulary.containsKey(word2) && !document.contains(word2)) {
							if (word2IdVocabulary.containsKey(word2)) {
								document.add(word2IdVocabulary.get(word2));
							}else{
								//not in vocabulary
								//document.add(word2IdVocabulary.get(word2));
							}
	
						}
					}
					numWordsInCorpus += document.size();
					corpus.add(document);
				//}
				
			}
			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		//write 
//		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath + "corpus."+ expName));
//		for (int i=0;i<numDocuments;i++) {
//			for(int j=0;j<corpus.get(i).size();j++){
//				writer.write(corpus.get(i).get(j)+" ");
//			}
//			writer.write("\n");
//		}
//		writer.close();
		
		//get in hyper parameters
		
		alpha = new double[numDocuments];
		alphaSum=0;
		for (int i = 0; i < alpha.length; i++) {
			alpha[i]=inAlpha;
			alphaSum+=alpha[i];
		}
		
		beta=new double[numTopics][vocabularySize];
		betaSum=new double[numTopics];
		for (int i = 0; i < numTopics; i++) {
			for (int j = 0; j < vocabularySize; j++) {
				beta[i][j]=1.00/numTopics;
				betaSum[i]=betaSum[i]+beta[i][j];
			}
		}
		
		gamma=new double[numSentiment];
		gammaSum=0.0;
		for (int i = 0; i < gamma.length; i++) {
			gamma[i]=1.0/numSentiment;
			gammaSum+=gamma[i];
		}
		
		docTopicCount = new int[numDocuments][numTopics];
		//topicWordCount = new int[numTopics][vocabularySize];
		docTopicSentimentCount = new int[numDocuments][numTopics][numSentiment];
		topicSentimentWordCount = new int[numTopics][numSentiment][vocabularySize];
				
		sumDocTopicCount = new int[numDocuments];
		//sumTopicWordCount = new int[numTopics];
		sumDocTopicSentimentCount = new int[numDocuments][numTopics];
		sumTopicSentimentWordCount = new int[numTopics][numSentiment];

		multiPros = new double[numTopics*numSentiment];
		for (int i = 0; i < numTopics*numSentiment; i++) {
			multiPros[i] = 1.0 / (numTopics*numSentiment);
		}

		

//		System.out.println("Corpus size: " + numDocuments + " docs, "
//			+ numWordsInCorpus + " words");
//		System.out.println("Vocabuary size: " + vocabularySize);
//		System.out.println("Number of topics: " + numTopics);
//		System.out.println("alpha: " + alpha);
//		System.out.println("beta: " + beta);
//		System.out.println("Number of sampling iterations: " + numIterations);
//		System.out.println("Number of top topical words: " + numTopWords);

		tAssignsFilePath = pathToTAfile;
		if (tAssignsFilePath.length() > 0)
			initialize(tAssignsFilePath);
		else
			initialize();
	}

	
	/**
	 * Randomly initialize topic assignments
	 */
	public void initialize()
		throws IOException
	{
		System.out.println("Randomly initializing topic assignments ...");

		topicAssignments = new ArrayList<List<Integer>>();

		for (int i = 0; i < numDocuments; i++) {
			List<Integer> topics = new ArrayList<Integer>();
			int docSize = corpus.get(i).size();
			for (int j = 0; j < docSize; j++) {
				int temp = nextDiscrete(multiPros);
				int topic =temp/numSentiment; // Sample a topic from theta_doc
				//int sentiment = nextDiscrete(gamma); // Sample a sentiment
				int sentiment=0;
				
				if (sentimentLabel.get(1).contains(corpus.get(i).get(j))) {
					//System.out.println(id2WordVocabulary.get(corpus.get(i).get(j)));
					sentiment=1;
				}
				
				// Increase counts
				docTopicCount[i][topic] += 1;
				//topicWordCount[topic][corpus.get(i).get(j)] += 1;
				docTopicSentimentCount[i][topic][sentiment] +=1;
				topicSentimentWordCount[topic][sentiment][corpus.get(i).get(j)] +=1;
				
				sumDocTopicCount[i] += 1;
				//sumTopicWordCount[topic] += 1;
				sumDocTopicSentimentCount[i][topic]+=1;
				sumTopicSentimentWordCount[topic][sentiment] +=1;
				
				//sentimentAssignment[i][topic]=sentiment;
				topics.add(topic);
			}
			topicAssignments.add(topics);
		}
		
		for (int i = 0; i < numDocuments; i++) {
			for (int j = 0; j < numTopics; j++) {
				if (docTopicSentimentCount[i][j][0]>docTopicSentimentCount[i][j][1]) {
					sentimentAssignment[i][j]=0;
				}else{
					sentimentAssignment[i][j]=1;
				}
			}
			
		}
	}

	/**
	 * Initialize topic assignments from a given file
	 */
	public void initialize(String pathToTopicAssignmentFile)
	{
		System.out.println("Reading topic-assignment file: "
			+ pathToTopicAssignmentFile);

		topicAssignments = new ArrayList<List<Integer>>();

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(pathToTopicAssignmentFile));
			int docID = 0;
			int numWords = 0;
			for (String line; (line = br.readLine()) != null;) {
				String[] strTopics = line.trim().split("\\s+");
				System.out.println(docID);
				List<Integer> topics = new ArrayList<Integer>();
				if (line.length()==0) {
					topicAssignments.add(topics);
					docID++;
					continue;
				}
				for (int j = 0; j < strTopics.length; j++) {
					int topic = Integer.parseInt(strTopics[j]);
					// Increase counts
					docTopicCount[docID][topic] += 1;
					//topicWordCount[topic][corpus.get(docID).get(j)] += 1;
					sumDocTopicCount[docID] += 1;
					//sumTopicWordCount[topic] += 1;

					topics.add(topic);
					numWords++;
				}
				topicAssignments.add(topics);
				docID++;
			}

			if ((docID != numDocuments) || (numWords != numWordsInCorpus)) {
				System.out
					.println("The topic modeling corpus and topic assignment file are not consistent!!!");
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void inference()
		throws IOException
	{
		System.out.println("Running Gibbs sampling inference: ");

		for (int iter = 1; iter <= numIterations; iter++) {

			System.out.println("\tSampling iteration: " + (iter));

			sampleInSingleIteration();

			//System.out.println(computePerplexity());
			
			if ((savestep > 0) && (iter % savestep == 0)
				&& (iter < numIterations)) {
				System.out.println("\t\tSaving the output from the " + iter
					+ "^{th} sample");
				expName = orgExpName + "-" + iter;
				write();
				System.out.println(computePerplexity());
			}
		}
		expName = orgExpName;

		writeParameters();
		System.out.println("Writing output from the last sample ...");
		write();

		System.out.println("Sampling completed!");

	}

	public void sampleInSingleIteration()
	{
		for (int dIndex = 0; dIndex < numDocuments; dIndex++) {
			int docSize = corpus.get(dIndex).size();
			for (int wIndex = 0; wIndex < docSize; wIndex++) {
				// Get current word and its topic
				int topic = topicAssignments.get(dIndex).get(wIndex);
				int word = corpus.get(dIndex).get(wIndex);
				int sentiment=sentimentAssignment[dIndex][topic];

				if (docTopicCount[dIndex][topic]==0 || docTopicSentimentCount[dIndex][topic][sentiment]==0 ||topicSentimentWordCount[topic][sentiment][word] ==0) {
					continue;
					//System.out.println(docTopicCount[dIndex][topic]);
					//System.out.println(docTopicSentimentCount[dIndex][topic][sentiment]);
					//System.out.println(topicSentimentWordCount[topic][sentiment][word]);
				}
				
				// Decrease counts
				docTopicCount[dIndex][topic] -= 1;
				sumDocTopicCount[dIndex] -= 1;
				//topicWordCount[topic][word] -= 1;
				//sumTopicWordCount[topic] -= 1;
				
				docTopicSentimentCount[dIndex][topic][sentiment] -=1;
				sumDocTopicSentimentCount[dIndex][topic] -=1;
				
				topicSentimentWordCount[topic][sentiment][word] -=1;
				sumTopicSentimentWordCount[topic][sentiment] -=1;
				
				
				double Vbeta = vocabularySize * beta[topic][word];
				double Kalpha = numTopics * alpha[dIndex];
				
				// Sample a topic
				// Sample a topic
				for (int tIndex = 0; tIndex < numTopics; tIndex++) {
					for (int sIndex = 0; sIndex < numSentiment; sIndex++) {
						multiPros[tIndex+sIndex] =  
								(docTopicCount[dIndex][tIndex] + alpha[dIndex]) /(sumDocTopicCount[dIndex]+Kalpha) *
								(docTopicSentimentCount[dIndex][tIndex][sentiment] + gamma[sentiment]) / ( sumDocTopicSentimentCount[dIndex][tIndex] + gammaSum) *
								(topicSentimentWordCount[tIndex][sentiment][word] + beta[tIndex][word])/ (sumTopicSentimentWordCount[tIndex][sentiment]+Vbeta);
							
					}
					//System.out.println((docTopicCount[dIndex][tIndex] + alpha[dIndex]) /(sumDocTopicCount[dIndex]+Kalpha));
					//System.out.println((docTopicSentimentCount[dIndex][tIndex][sentiment] + gamma[sentiment]) / ( sumDocTopicSentimentCount[dIndex][tIndex] + gammaSum));
					//System.out.println((topicSentimentWordCount[tIndex][sentiment][word] + beta[tIndex][word])/ (sumTopicSentimentWordCount[tIndex][sentiment]+Vbeta));
				}
				
				int tsIndex = nextDiscrete(multiPros);
				int tsIndex2=tsIndex;
				topic=tsIndex/numSentiment;
				sentiment=tsIndex2%numSentiment;

				//given topic sample a sentiment
//				double[] multisentiment=new double[numSentiment];
//				for (int sIndex = 0; sIndex < numSentiment; sIndex++) {
//					multisentiment[sIndex] =  
//					(docTopicCount[dIndex][topic] + alpha[dIndex]) /(sumDocTopicCount[dIndex]+Kalpha) *
//					(docTopicSentimentCount[dIndex][topic][sIndex] + gamma[sIndex]) / ( sumDocTopicSentimentCount[dIndex][topic] + gammaSum) *
//					(topicSentimentWordCount[topic][sIndex][word] + beta[topic][word])/ (sumTopicSentimentWordCount[topic][sIndex]+Vbeta);
//				}
//				sentiment=nextDiscrete(multisentiment);
				
				// Increase counts
				docTopicCount[dIndex][topic] += 1;
				sumDocTopicCount[dIndex] += 1;
				//topicWordCount[topic][word] += 1;
				//sumTopicWordCount[topic] += 1;
				docTopicSentimentCount[dIndex][topic][sentiment] +=1;
				sumDocTopicSentimentCount[dIndex][topic] +=1;
				
				topicSentimentWordCount[topic][sentiment][word] +=1;
				sumTopicSentimentWordCount[topic][sentiment] +=1;

				// Update topic assignments
				//sentimentAssignment[dIndex][topic]=sentiment;
				topicAssignments.get(dIndex).set(wIndex, topic);
			}
		}
		
		for (int i = 0; i < numDocuments; i++) {
			for (int j = 0; j < numTopics; j++) {
				if (docTopicSentimentCount[i][j][0]>docTopicSentimentCount[i][j][1]) {
					sentimentAssignment[i][j]=0;
				}else{
					sentimentAssignment[i][j]=1;
				}
			}
			
		}
	}
	
	public static int nextDiscrete(double[] probs)
    {
        double sum = 0.0;
        for (int i = 0; i < probs.length; i++)
            sum += probs[i];

        double r= (new Random()).nextDouble() *sum;

        sum = 0.0;
        for (int i = 0; i < probs.length; i++) {
            sum += probs[i];
            if (sum > r)
                return i;
        }
        return probs.length - 1;
    }
	
	 public double computePerplexity()
	 {
		 double logliCorpus = 0.0;
		 for (int dIndex = 0; dIndex < numDocuments; dIndex++) {
			 int docSize = corpus.get(dIndex).size();
			 double logliDoc = 0.0;
			 for (int wIndex = 0; wIndex < docSize; wIndex++) {
				 int word = corpus.get(dIndex).get(wIndex);
				 double likeWord = 0.0;
				 for (int tIndex = 0; tIndex < numTopics; tIndex++) {
					 int sentiment= sentimentAssignment[dIndex][tIndex];
			//		 likeWord += ((docTopicCount[dIndex][tIndex] + alpha[dIndex]) /
			//		 (sumDocTopicCount[dIndex] + alphaSum))
			//		 * ((topicWordCount[tIndex][word] + beta[tIndex][word]) / (sumTopicWordCount[tIndex] +
			//		 betaSum[tIndex]));
					 likeWord+=  
								(docTopicCount[dIndex][tIndex] + alpha[dIndex]) /(sumDocTopicCount[dIndex]+alphaSum) *
								(docTopicSentimentCount[dIndex][tIndex][sentiment] + gamma[sentiment]) / ( sumDocTopicSentimentCount[dIndex][tIndex] + gammaSum) *
								(topicSentimentWordCount[tIndex][sentiment][word] + beta[tIndex][word])/ (sumTopicSentimentWordCount[tIndex][sentiment]+betaSum[tIndex]);
							
					 
				 }
				 logliDoc += Math.log(likeWord);
			 }
			 logliCorpus += logliDoc;
		 }
		 double perplexity = Math.exp(-1.0 * logliCorpus / numWordsInCorpus);
		 if (perplexity < 0)
		 throw new RuntimeException("Illegal perplexity value: " + perplexity);
		 return perplexity;
	 }

	public void writeParameters()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".paras"));
		writer.write("-model" + "\t" + "LDA");
		writer.write("\n-corpus" + "\t" + corpusPath);
		writer.write("\n-ntopics" + "\t" + numTopics);
		writer.write("\n-alpha" + "\t" + alpha);
		writer.write("\n-beta" + "\t" + beta);
		writer.write("\n-niters" + "\t" + numIterations);
		writer.write("\n-twords" + "\t" + numTopWords);
		writer.write("\n-name" + "\t" + expName);
		if (tAssignsFilePath.length() > 0)
			writer.write("\n-initFile" + "\t" + tAssignsFilePath);
		if (savestep > 0)
			writer.write("\n-sstep" + "\t" + savestep);

		writer.close();
	}

	public void writeDictionary()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".vocabulary"));
		for (String word : word2IdVocabulary.keySet()) {
			writer.write(word + " " + word2IdVocabulary.get(word) + "\n");
		}
		writer.close();
	}

	public void writeIDbasedCorpus()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".IDcorpus"));
		for (int dIndex = 0; dIndex < numDocuments; dIndex++) {
			int docSize = corpus.get(dIndex).size();
			for (int wIndex = 0; wIndex < docSize; wIndex++) {
				writer.write(corpus.get(dIndex).get(wIndex) + " ");
			}
			writer.write("\n");
		}
		writer.close();
	}

	public void writeTopicAssignments()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".topicAssignments"));
		for (int dIndex = 0; dIndex < numDocuments; dIndex++) {
			int docSize = corpus.get(dIndex).size();
			for (int wIndex = 0; wIndex < docSize; wIndex++) {
				writer.write("["+topicAssignments.get(dIndex).get(wIndex) + ","+sentimentAssignment[dIndex][topicAssignments.get(dIndex).get(wIndex)]+"] ");
			}
			writer.write("\n");
		}
		writer.close();
	}

	public void writeTopTopicalWords()
			throws IOException
		{
			BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
				+ "numTopWords."+ expName));

			for (int tIndex = 0; tIndex < numTopics; tIndex++) {
				//writer.write("Topic" + new Integer(tIndex) + " ");

				////
				for (int sentiment = 0; sentiment < numSentiment; sentiment++) {
					writer.write("Topic" + new Integer(tIndex) + " "+"Sentiment" + new Integer(sentiment) + ":");
					
					Map<Integer, Integer> wordCount = new TreeMap<Integer, Integer>();
					for (int wIndex = 0; wIndex < vocabularySize; wIndex++) {
						wordCount.put(wIndex, topicSentimentWordCount[tIndex][sentiment][wIndex]);

					}
					
					//wordCount = FuncUtils.sortByValueDescending(wordCount);
			        List<Map.Entry<Integer, Integer>> list = new LinkedList<Map.Entry<Integer, Integer>>(wordCount.entrySet());
			        Collections.sort(list, new Comparator<Map.Entry<Integer, Integer>>()
			        {
			            public int compare(Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2)
			            {
			                int compare = (o1.getValue()).compareTo(o2.getValue());
			                return -compare;
			            }
			        });

			        Map<Integer, Integer> result = new LinkedHashMap<Integer, Integer>();
			        for (Map.Entry<Integer, Integer> entry : list) {
			            result.put(entry.getKey(), entry.getValue());
			        }
			        wordCount= result;
					
					Set<Integer> mostLikelyWords = wordCount.keySet();
					int count = 0;
					for (Integer index : mostLikelyWords) {
						if (count < numTopWords) {
							writer.write(" " + id2WordVocabulary.get(index));
							count += 1;
						}
						else {
							writer.write("\n\n");
							break;
						}
					}
				}
				
//				Map<Integer, Integer> wordCount = new TreeMap<Integer, Integer>();
//				for (int wIndex = 0; wIndex < vocabularySize; wIndex++) {
//					for (int label = 0; label < numSentiment; label++) {
//						if (label==0) {
//							wordCount.put(wIndex, topicSentimentWordCount[tIndex][label][wIndex]);
//						}
//						else{
//							wordCount.put(wIndex, wordCount.get(wIndex)+topicSentimentWordCount[tIndex][label][wIndex]);
//						}
//					}
//					
//				}
//				
//				//wordCount = FuncUtils.sortByValueDescending(wordCount);
//		        List<Map.Entry<Integer, Integer>> list = new LinkedList<Map.Entry<Integer, Integer>>(wordCount.entrySet());
//		        Collections.sort(list, new Comparator<Map.Entry<Integer, Integer>>()
//		        {
//		            public int compare(Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2)
//		            {
//		                int compare = (o1.getValue()).compareTo(o2.getValue());
//		                return -compare;
//		            }
//		        });
//
//		        Map<Integer, Integer> result = new LinkedHashMap<Integer, Integer>();
//		        for (Map.Entry<Integer, Integer> entry : list) {
//		            result.put(entry.getKey(), entry.getValue());
//		        }
//		        wordCount= result;
//				
//				Set<Integer> mostLikelyWords = wordCount.keySet();
//				int count = 0;
//				for (Integer index : mostLikelyWords) {
//					if (count < numTopWords) {
//						writer.write(" " + id2WordVocabulary.get(index));
//						count += 1;
//					}
//					else {
//						writer.write("\n\n");
//						break;
//					}
//				}
				////
			}
			writer.close();
		}

	public void writeTopicWordPros()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".phi"));
		for (int i = 0; i < numTopics; i++) {
			for (int j = 0; j < numSentiment; j++) {
				for (int k = 0; k < vocabularySize; k++) {
//					double pro = (topicWordCount[i][j] + beta[i][j])
//						/ (sumTopicWordCount[i] + betaSum[i]);
					double pro=(topicSentimentWordCount[i][j][k] + beta[i][k])/ (sumTopicSentimentWordCount[i][j]+betaSum[i]);
					
					writer.write(pro + " ");
				}
			}
			
			writer.write("\n");
		}
		writer.close();
	}

	public void writeTopicWordCount()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".WTcount"));
		for (int i = 0; i < numTopics; i++) {
			for (int j = 0; j < vocabularySize; j++) {
				//writer.write(topicWordCount[i][j] + " ");
			}
			writer.write("\n");
		}
		writer.close();

	}
	
	public void writeDocTopicSentimentPros()
			throws IOException
		{
			BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
				+ expName + ".pi"));
			for (int i = 0; i < numDocuments; i++) {
				for (int j = 0; j < numTopics; j++) {
					double pro = (docTopicCount[i][j] + alpha[i])
						/ (sumDocTopicCount[i] + alphaSum);
					writer.write(pro + " ");
				}
				writer.write("\n");
			}
			writer.close();
		}

	public void writeDocTopicPros()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".theta"));
		for (int dIndex = 0; dIndex < numDocuments; dIndex++) {
			for (int tIndex = 0; tIndex < numTopics; tIndex++) {
				for (int sentiment = 0; sentiment < numSentiment; sentiment++) {
					//double pro = (docTopicCount[i][j] + alpha[i]) / (sumDocTopicCount[i] + alphaSum);
					double pro=(docTopicCount[dIndex][tIndex] + alpha[dIndex]) /(sumDocTopicCount[dIndex]+alpha[dIndex]*numTopics) *
							(docTopicSentimentCount[dIndex][tIndex][sentiment] + gamma[sentiment]) / ( sumDocTopicSentimentCount[dIndex][tIndex] + gammaSum);
					//System.out.println((docTopicCount[dIndex][tIndex] + alpha[dIndex]) /(sumDocTopicCount[dIndex]+alphaSum));
					//System.out.println(docTopicSentimentCount[dIndex][tIndex][sentiment]);
					//System.out.println(sumDocTopicSentimentCount[dIndex][tIndex]);
					//System.out.println((docTopicSentimentCount[dIndex][tIndex][sentiment] + gamma[sentiment]) / ( sumDocTopicSentimentCount[dIndex][tIndex] + gammaSum));
					writer.write(pro + " ");
				}
				
			}
			writer.write("\n");
		}
		writer.close();
	}
	
	
	public void writeDocTopicPros2()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".theta2"));
		for (int i = 0; i < numDocuments; i++) {
			for (int j = 0; j < numTopics; j++) {
				for (int k = 0; k < numSentiment; k++) {
					double pro = (docTopicCount[i][j] + alpha[i]) / (sumDocTopicCount[i] + alphaSum) *
							(docTopicSentimentCount[i][j][k] + gamma[k]) / ( sumDocTopicSentimentCount[i][j] + gammaSum);
					writer.write(pro + " ");
				}			
			}
			writer.write("\n");
		}
		writer.close();
	}
	
	public void writeDocTopicCount()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".DTcount"));
		for (int i = 0; i < numDocuments; i++) {
			for (int j = 0; j < numTopics; j++) {
				writer.write(docTopicCount[i][j] + " ");
			}
			writer.write("\n");
		}
		writer.close();
	}

	public void write()
		throws IOException
	{
		writeTopTopicalWords();
		writeDocTopicPros();
		writeDocTopicPros2();
		writeTopicAssignments();
		writeTopicWordPros();
	}
	
	public static Vector<String> getFileName(String filePath) throws IOException{
		List<String> restaurant_id = new ArrayList<String>();
		restaurant_id=getRestaurantId();
		
		
//		Vector<String> fileName = new Vector<String>();
//		File folder1 = new File(filePath);
//		String[] list1 = folder1.list();
//		for (int i = 0; i < list1.length; i++) {
//			if (i>0) {
//				
//				fileName.addElement(filePath+"/"+list1[i]);
//			}
//			
//		}
		
		Vector<String> fileName = new Vector<String>();
		File folder1 = new File(filePath);
		String[] list1 = folder1.list();//990626
		for (int i = 0; i < list1.length; i++) {
			if (i>0) {
				String temp=list1[i].substring(6, list1[i].length()-4);
				if (restaurant_id.contains(temp)) {
					fileName.addElement(filePath+"/"+list1[i]);
				}	
			}
			
		}
		
		return fileName;
	}
	
	public static List<String> getRestaurantId() throws IOException{
		
		String pathToId="/Users/QimingChen/Desktop/restaurant_id";
		List<String> id = new ArrayList<String>();
		
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(pathToId));
			for (String word; (word = br.readLine()) != null;) {
				id.add(word);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		br.close();
		return id;
	}
	
	public double sumDouble(double[] input){
		double sum=0;
		for (int i = 0; i < input.length; i++) {
			sum=sum+input[i];
		}
		return sum;
	}

	public static void main(String args[])
		throws Exception
	{
		Vector<String> fileName = new Vector<String>();
		//fileName=getFileName("/Users/QimingChen/Desktop/Yelp_Review");
		System.out.println(fileName.size());
		String pathToVocab="/Users/QimingChen/Desktop/vocabulary28482";
		String pathToOutput="/Users/QimingChen/Desktop/output";
		String pathToTA="";
		//String pathToTA="/Users/QimingChen/Desktop/output/reviews1-450.topicAssignments";
		int numOfTopic=30;
		int numOfWord=100;
		int numOfIter=1000;
		double alpha=0.01;
		double beta=0.33; // 1/numoftopic
		SentimentLDA2 lda=new SentimentLDA2(fileName,pathToVocab, numOfTopic, alpha, beta, numOfIter, numOfWord, "label",pathToOutput,pathToTA,20); //save every 20 iteration
//"/Users/QimingChen/Desktop/output/reviews1-450.topicAssignments
		//GibbsSamplingLDA lda = new GibbsSamplingLDA("/Users/QimingChen/Desktop/Statistical NLP/project/jLDADMM_v1.0/src/models/test/corpus.txt", 7, 0.1,
		//	0.01, 2000, 20, "testLDA");
		lda.inference();
	}
}


