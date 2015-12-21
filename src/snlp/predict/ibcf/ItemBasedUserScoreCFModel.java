/**
 * 
 */
package snlp.predict.ibcf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import snlp.data.yelp.ReviewReader;
import snlp.data.yelp.TopicBasedBusinessSimilarDataReader;
import snlp.data.yelp.entity.ReviewStars;
import snlp.predict.Predictor;
import snlp.predict.YelpPredictionEvaluator;
import snlp.predict.baseline.StatisticPrediction;

/**
 * @author Alex
 *
 */
public class ItemBasedUserScoreCFModel implements Predictor{
	
	List<ReviewStars> trainData;
	Map<String, List<ReviewStars>> usersExperience;
	Map<String, Double> usersAvgScores;
	ItemSimilarityModel itemSimilarityModel;
	double defaultStar = 4;
	int newUserCount = 0;
	int neverReviewCount = 0;
	int newBusinessCount = 0;
	
	
	public ItemBasedUserScoreCFModel(List<ReviewStars> trainData, ItemSimilarityModel itemSimilarityModel, double defaultStar) {
		super();
		this.trainData = trainData;
		this.itemSimilarityModel = itemSimilarityModel;
		this.defaultStar = defaultStar;
	}

	@Override
	public double predictStars(String userId, String businessId) {
		if (itemSimilarityModel == null)
			throw new RuntimeException("itemSimilarityModel == null");
		if (usersExperience == null)
			throw new RuntimeException("usersExperience == null");
		if (usersAvgScores == null)
			throw new RuntimeException("usersAvgScores == null");
		
		List<ReviewStars> pastReviews = usersExperience.get(userId);
		if (pastReviews == null){
			newUserCount++;
			return defaultStar;
		}
		if (pastReviews.size() == 0){
			neverReviewCount++;
			return defaultStar;
		}
		
		double userAvg = usersAvgScores.get(userId);
		double weightSum = 0;
		double similaritySum = 0;
		for (ReviewStars oneReview : pastReviews){
			double similarity = itemSimilarityModel.cosineSimilarity(businessId, oneReview.getBusiness_id());
			
			weightSum += similarity * (oneReview.getStars() - userAvg);
			similaritySum += Math.abs(similarity);
		}
		if (similaritySum == 0){
			newBusinessCount++;
			return userAvg;
		}
		return userAvg + weightSum / similaritySum;
	}

	public void train(){
		if (trainData == null)
			throw new RuntimeException("Training data is null!");
		
		usersExperience = new HashMap<String, List<ReviewStars>>();
		usersAvgScores = new HashMap<String, Double>();
		seperateByUsers();
		
		trainData = null; // release memory and prevent second train
	}
	
	void seperateByUsers(){
		// seperate training data by users 
		for (ReviewStars review : trainData){
			String user = review.getUser_id();
			if (!usersExperience.containsKey(user))
				usersExperience.put(user, new ArrayList<ReviewStars>());
			List<ReviewStars> userReviews = usersExperience.get(user);
			
			userReviews.add(review);
		}
		
		// compute average scores users give
		for (Map.Entry<String, List<ReviewStars>> entry : usersExperience.entrySet()){
			String user = entry.getKey();
			List<ReviewStars> reviews = entry.getValue();
			double sum = 0;
			for (ReviewStars oneReview: reviews){
				sum += oneReview.getStars();
			}
			usersAvgScores.put(user, sum / reviews.size());
		}
	}
	
	void dump(){
		
		System.out.printf("newUserCount = %d\n", newUserCount);
		System.out.printf("neverReviewCount = %d\n", neverReviewCount);
		System.out.printf("newBusinessCount = %d\n", newBusinessCount);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		ReviewReader reviewReader = new ReviewReader("./data/train-reviewstars.json");
		ReviewReader testReader = new ReviewReader("./data/test-reviewstars.json");
		TopicBasedBusinessSimilarDataReader topicVectorReader = 
				new TopicBasedBusinessSimilarDataReader(
						"./topicDistribution/10 topic count all words/idList", 
						"./topicDistribution/10 topic count all words/testLDA.theta", 0);
		
		try {
			System.out.println("Reading training data...");
			List<ReviewStars> trainData = reviewReader.readReviewStars(-1);
			
			ItemSimilarityModel itemSimilarityModel = new ItemSimilarityModel(
					topicVectorReader.retrieveBusinessTopicVector());
			
			ItemBasedUserScoreCFModel itemBasedCFModel = new ItemBasedUserScoreCFModel(trainData, itemSimilarityModel, 4);
			
			System.out.println("Training...");
			itemBasedCFModel.train();
			
			System.out.println("Reading test data...");
			YelpPredictionEvaluator yelpPredictionEvaluator = new YelpPredictionEvaluator(testReader.readReviewStars(-1));
			
			System.out.println("Computing MAE...");
			double mae = yelpPredictionEvaluator.copmuteMAE(itemBasedCFModel);
			
			itemBasedCFModel.dump();
			System.out.printf("MAE=%f\n", mae);
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}

	
}
