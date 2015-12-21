package snlp.sentiment;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeSet;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
	private double avgOffset;
	private String trainingDataFile;
	
	public BagOfWords(String trainingDataFile) throws Exception {
		wordCount = new HashMap<String, Double>();
		wordScore = new HashMap<String, Double>();
		starCount = new HashMap<Double, Double>();
		this.trainingDataFile = trainingDataFile;
		totalTrainingDataCount = 0;
		avgOffset = 0.0;
		readFromFile();
		System.out.println("Finish reading from file ..");
		normalize();
		System.out.println("Finish normalizing ..");
		generateOffset();//add
		System.out.println(avgOffset + " !!!!");
		System.out.println("Finish calculating offset ..");
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
	    TreeSet<Double> ts = new TreeSet<Double>(starCount.keySet());
	    double cumulativeCount = 0;
	    for(double star : ts) {
	    	double count = starCount.get(star);
			cumulativeCount += count;
	    	starCount.put(star, cumulativeCount / totalTrainingDataCount);
	    }
	}
	
	//add
	private void generateOffset() {
		JSONParser parser = new JSONParser(); 
		try {
			Stopword sw = new Stopword("./data/stopwords.txt");
			Scanner sc = new Scanner(new File(trainingDataFile));
			while(sc.hasNextLine()) {
				String line = sc.nextLine();
				JSONObject jsonObject = (JSONObject) parser.parse(line);
				String review = (String) jsonObject.get("text");
				double effectiveWordNum = 0.0;
				double score = Double.valueOf(jsonObject.get("stars").toString());
				review = review.replaceAll("[^A-Za-z0-9 ]", " ").toLowerCase();
				String[] words = review.split(" ");
				double predictedScore = 0.0;
				for(String word: words) {
					if(!sw.isStopword(word) && !word.equals("")) {
						if(wordScore.containsKey(word)) {
							effectiveWordNum ++;
							predictedScore += wordScore.get(word);
						}
					}
				}
				if(effectiveWordNum != 0) {
					avgOffset += (score - predictedScore / effectiveWordNum);
					//System.out.println("score: " + score + "; predict: " + predictedScore / effectiveWordNum);
					//avgOffset += Math.pow(predictedScore / effectiveWordNum - score, 2);
				}
			}
			sc.close();
			avgOffset /= totalTrainingDataCount;
			//avgOffset = Math.sqrt(avgOffset);
			avgOffset = (int)Math.round(avgOffset * 10000)/(double)10000;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void generateScore(String inputJsonFile, String outputJsonFile) throws Exception {
		Scanner sc = new Scanner(new File(inputJsonFile));
		JSONParser parser = new JSONParser();
		Stopword sw = new Stopword("./data/stopwords.txt");
		PrintWriter pw = new PrintWriter(new File(outputJsonFile));
		while(sc.hasNextLine()) {
			JSONObject job = (JSONObject) parser.parse(sc.nextLine());
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
			double avgScore = (int)Math.round(score / count * 10000)/(double)10000;
			job.put("star_predicted", avgScore);
			//job.put("star_predicted", Math.ceil(score / count * 2) / 2);
			
			pw.write(job.toJSONString() + "\n");
		}
		pw.flush(); pw.close();
	}
	
	public void generateScore_Heurustic(String inputJsonFile, String outputJsonFile) throws IOException, ParseException {
		Scanner sc = new Scanner(new File(inputJsonFile));
		JSONParser parser = new JSONParser();
		Stopword sw = new Stopword("./data/stopwords.txt");
		PrintWriter pw = new PrintWriter(new File(outputJsonFile));
		Map<Double, Double> values = new HashMap<Double, Double>();
		List<JSONObject> js = new ArrayList<JSONObject>();
		
		while(sc.hasNextLine()) {
			JSONObject job = (JSONObject) parser.parse(sc.nextLine());
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
			//double avgScore = Math.ceil(score / count * 2) / 2;
			double avgScore = (int)Math.round(score / count * 10000)/(double)10000;
			job.put("star_predicted", avgScore);
			js.add(job); 
			values.put(avgScore, 0.0);
		}
		
		updateValues(values);
		
		for(JSONObject job : js) {
			job.replace("star_predicted", values.get(job.get("star_predicted")));
			pw.write(job.toJSONString() + "\n");
		}
		pw.flush(); pw.close();
	}
	
	private void updateValues(Map<Double, Double> values) {
		int size = values.size();
		double i = 0.0;
		TreeSet<Double> sortedSet = new TreeSet<Double>(values.keySet());
		for (double key : sortedSet) {
			double j = 1.0;
			while(i / size > starCount.get(j) && j <= 5.0) {
				j += 1.0;
			}
			values.put(key, j);
			i += 1.0;
		}
	}
	
	
	private void generateScore_withOffset(String inputJsonFile, String outputJsonFile) throws Exception {
		Scanner sc = new Scanner(new File(inputJsonFile));
		JSONParser parser = new JSONParser();
		Stopword sw = new Stopword("./data/stopwords.txt");
		PrintWriter pw = new PrintWriter(new File(outputJsonFile));
		while(sc.hasNextLine()) {
			JSONObject job = (JSONObject) parser.parse(sc.nextLine());
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
			double avgScore = (int)Math.round(score / count * 10000)/(double)10000;
			job.put("star_predicted", avgScore + avgOffset);
			
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
	
	public double evaluate(String testReviewPredictFile) throws FileNotFoundException, ParseException {
		Scanner sc = new Scanner(new File(testReviewPredictFile));
		JSONParser parser = new JSONParser();
		int count = 0;
		double totalDiffCumulative = 0.0;
		
		while(sc.hasNextLine()) {
			JSONObject jsonObject = (JSONObject) parser.parse(sc.nextLine());
			double stars = Double.valueOf(jsonObject.get("stars").toString());
			double predictedStars = Double.valueOf(jsonObject.get("star_predicted").toString());
			count++;
			totalDiffCumulative += Math.abs(predictedStars - stars);
		}
		
		return totalDiffCumulative / count;
	}
	
	public static void main(String args[]) throws Exception{
		if(args[0].equals("train")) {
			System.out.println("Training ..");
			BagOfWords bow = new BagOfWords("./data/train-review.json");
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("./model/bow.model"));
			oos.writeObject(bow);
			oos.close();
			System.out.println("Finish training.");
		} else if(args[0].equals("test")) {
			System.out.println("Testing ..");
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream("./model/bow.model"));
			BagOfWords bow = (BagOfWords) ois.readObject();
			ois.close();
			if(args[1].equals("heuristic")){
				bow.generateScore_Heurustic("./data/test-review.json", "./data/test-review-predict.json");
			} else if(args[1].equals("offset")){
				bow.generateScore_withOffset("./data/test-review.json", "./data/test-review-predict.json");
			} else {
				bow.generateScore("./data/test-review.json", "./data/test-review-predict.json");
			}
			System.out.println("Finish testing and write results to file.");
			System.out.println("Evaluating ..");
			double resultOffset = bow.evaluate("./data/test-review-predict.json");
			System.out.println("Predicted score offset: " + resultOffset);
		}
	}
}
