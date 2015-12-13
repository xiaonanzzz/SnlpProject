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

/**
 * @author Alex
 *
 */
public class ItemBasedCFModel implements Predictor{
	
	List<ReviewStars> trainData;
	Map<String, List<ReviewStars>> usersExperience;
	ItemSimilarityModel itemSimilarityModel;
	ReviewStatistic reviewStatistic;
	double defaultStar = 4;
	int newUserCount = 0;
	int neverReviewCount = 0;
	int newBusinessCount = 0;
	
	
	public ItemBasedCFModel(List<ReviewStars> trainData, ItemSimilarityModel itemSimilarityModel,
			ReviewStatistic reviewStatistic, double defaultStar) {
		super();
		this.trainData = trainData;
		this.itemSimilarityModel = itemSimilarityModel;
		this.reviewStatistic = reviewStatistic;
		this.defaultStar = defaultStar;
	}

	@Override
	public double predictStars(String userId, String businessId) {
		if (itemSimilarityModel == null)
			throw new RuntimeException("itemSimilarityModel == null");
		if (usersExperience == null)
			throw new RuntimeException("usersExperience == null");
		
		double businessAvg = reviewStatistic.businessAvg(businessId);
		
		List<ReviewStars> pastReviews = usersExperience.get(userId);
		if (pastReviews == null){
			newUserCount++;
			return businessAvg;
		}
		if (pastReviews.size() == 0){
			neverReviewCount++;
			return businessAvg;
		}
		double similaritySum = 0; // normalizer
		double userDevRatioSum = 0;
//		double devSum = 0;
//		double userRateSum = 0;
		
		for (ReviewStars oneReview : pastReviews){
			double similarity = itemSimilarityModel.cosineSimilarity(businessId, oneReview.getBusiness_id());
			double dev = oneReview.getStars() - reviewStatistic.businessAvg(oneReview.getBusiness_id());
			double busdev = reviewStatistic.businessStdev(oneReview.getBusiness_id());
			double devRatio = busdev != 0? dev / busdev : 0;
//			double stars = oneReview.getStars();
			
			userDevRatioSum += similarity * devRatio;
//			devSum += similarity * dev;
//			userRateSum += similarity * stars;
			similaritySum += Math.abs(similarity);
		}
		if (similaritySum == 0){
			newBusinessCount++;
			return defaultStar;
		}
		double userDevRatio = userDevRatioSum / similaritySum;
		double predictedRate = businessAvg + userDevRatio * reviewStatistic.businessStdev(businessId);
//		devSum /= similaritySum;
//		userRateSum /= similaritySum;
//		double predictedRate = businessAvg + userRateSum;
		if (Double.isNaN(predictedRate))
			throw new RuntimeException("NaN");
		if (predictedRate > 5)
			return 5;
		if (predictedRate < 1)
			return 1;
		return predictedRate;
	}

	public void train(){
		if (trainData == null)
			throw new RuntimeException("Training data is null!");
		
		usersExperience = new HashMap<String, List<ReviewStars>>();
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
		
		ReviewReader reviewReader = new ReviewReader("./dirty data/train-rest-review-stars.json");
		ReviewReader testReader = new ReviewReader("./dirty data/validate-rest-review-stars.json");
		TopicBasedBusinessSimilarDataReader topicVectorReader = 
				new TopicBasedBusinessSimilarDataReader(
						"./topicDistribution/50 Topic 21799 restaurants count one word 3 groups/idList", 
						"./topicDistribution/50 Topic 21799 restaurants count one word 3 groups/positive.theta", 0);
		
		try {
			System.out.println("Reading training data...");
			List<ReviewStars> trainData = reviewReader.readReviewStars(-1);
			
			ItemSimilarityModel itemSimilarityModel = new ItemSimilarityModel(
					topicVectorReader.retrieveBusinessTopicVector());
			ReviewStatistic reviewStatistic = new ReviewStatistic(trainData);
			
			reviewStatistic.train();
			ItemBasedCFModel itemBasedCFModel = new ItemBasedCFModel(trainData, itemSimilarityModel, reviewStatistic, 4);
			
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
