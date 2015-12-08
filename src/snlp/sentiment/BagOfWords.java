package snlp.sentiment;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import snlp.data.Stopword;

public class BagOfWords implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Map<String, Double> wordCount;
	private Map<String, Double> wordScore;
	private Map<Double, Double> starCount;
	private int totalTrainingDataCount;
	private String trainingDataFile;
	
	public BagOfWords(String trainingDataFile) throws Exception {
		wordCount = new HashMap<String, Double>();
		wordScore = new HashMap<String, Double>();
		starCount = new HashMap<Double, Double>();
		this.trainingDataFile = trainingDataFile;
		totalTrainingDataCount = 0;
		readFromFile();
		normalize();
	}
	
	private void readFromFile() {
		JSONParser parser = new JSONParser(); 
		try {
			Stopword sw = new Stopword("./data/stopwords.txt");
			Scanner sc = new Scanner(new File(trainingDataFile));
			while(sc.hasNextLine()) {
				String line = sc.nextLine();
				totalTrainingDataCount++;
				JSONObject jsonObject = (JSONObject) parser.parse(line);
				String review = (String) jsonObject.get("text");
				double score = Double.valueOf(jsonObject.get("stars").toString());
				review = review.replaceAll("[^A-Za-z0-9 ]", " ").toLowerCase();
				String[] words = review.split(" ");
				if(starCount.containsKey(score)) {
					starCount.put(score, starCount.get(score) + 1.0);
				} else {
					starCount.put(score, 1.0);
				}
				for(String word: words) {
					if(!sw.isStopword(word)) {
						if(wordCount.containsKey(word)) {
							wordCount.put(word, wordCount.get(word) + 1.0);
						} else {
							wordCount.put(word, 1.0);
						}
						if(wordScore.containsKey(word)) {
							wordScore.put(word, wordScore.get(word) + score);
						} else{
							wordScore.put(word, score);
						}
					}
				}
			}
			sc.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void normalize() {
		Iterator<String> it = wordScore.keySet().iterator();
	    while (it.hasNext()) {
	    	String keyword = it.next();
	    	double wordCnt = wordCount.get(keyword);
	    	wordScore.put(keyword, wordScore.get(keyword)/wordCnt);
	    }
	    Iterator<Double> it2 = starCount.keySet().iterator();
	    while(it2.hasNext()) {
	    	double star = it2.next();
	    	starCount.put(star, starCount.get(star)/totalTrainingDataCount);
	    }
	}
	
	private void generateScore(String inputJsonFile, String outputJsonFile) throws Exception {
		Scanner sc = new Scanner(new File(inputJsonFile));
		JSONParser parser = new JSONParser();
		Stopword sw = new Stopword("./data/stopwords.txt");
		PrintWriter pw = new PrintWriter(new File(outputJsonFile));
		while(sc.hasNextLine()) {
			JSONObject job = (JSONObject) parser.parse(sc.nextLine());
			//String business_id = (String) job.get("business_id");
			String review = (String) job.get("text");
			review = review.replaceAll("[^A-Za-z0-9 ]", " ").toLowerCase();
			double score = 0.0;
			int count = 0;
			for(String word : review.split(" ")) {
				if(!sw.isStopword(word) && !word.equals("")) {
					if(wordScore.containsKey(word)) {
						count++;
						score += wordScore.get(word);
					}
				}
			}
			//job.put("star_predicted", Math.ceil(score / count * 2) / 2);
			job.put("star_predicted", score / count);
			
			pw.write(job.toJSONString() + "\n");
		}
		pw.flush(); pw.close();
	}
	
	public boolean hasWord(String word) {
		return wordScore.containsKey(word);
	}
	
	public double getWordScore(String word) {
		return wordScore.get(word);
	}
	
	public double getStarPercent(double star) {
		return starCount.get(star);
	}
	
	public static void main(String args[]) throws Exception{
		if(args[0].equals("train")) {
			System.out.println("Training ..");
			BagOfWords bow = new BagOfWords("./data/train-review.json");
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("./model/bow.model"));
			oos.writeObject(bow);
			oos.close();
		} else if(args[0].equals("test")) {
			System.out.println("Testing ..");
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream("./model/bow.model"));
			BagOfWords bow = (BagOfWords) ois.readObject();
			ois.close();
			bow.generateScore("./data/test-review.json", "./data/test-review-predict.json");
		}
	}

	
}
