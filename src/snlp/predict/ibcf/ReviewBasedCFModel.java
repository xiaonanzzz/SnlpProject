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
import snlp.data.yelp.ReviewTopicFile;
import snlp.data.yelp.TopicBasedBusinessSimilarDataReader;
import snlp.data.yelp.entity.ReviewStars;
import snlp.predict.Predictor;
import snlp.predict.YelpPredictionEvaluator;

/**
 * @author Alex
 *
 */
public class ReviewBasedCFModel implements Predictor{
	
	List<ReviewStars> trainData;
	Map<String, List<ReviewStars>> usersToReviews;
	Map<String, List<ReviewStars>> businessToReviews;
	ItemSimilarityModel reviewSimilarityModel;
	ReviewStatistic reviewStatistic;
	double defaultStar = 4;
	int newUserCount = 0;
	int neverReviewCount = 0;
	int newBusinessCount = 0;
	
	
	public ReviewBasedCFModel(List<ReviewStars> trainData, ItemSimilarityModel itemSimilarityModel,
			ReviewStatistic reviewStatistic, double defaultStar) {
		super();
		this.trainData = trainData;
		this.reviewSimilarityModel = itemSimilarityModel;
		this.reviewStatistic = reviewStatistic;
		this.defaultStar = defaultStar;
	}

	@Override
	public double predictStars(String userId, String businessId) {
		if (reviewSimilarityModel == null)
			throw new RuntimeException("reviewSimilarityModel == null");
		if (usersToReviews == null)
			throw new RuntimeException("usersToReviews == null");
		if (businessToReviews == null)
			throw new RuntimeException("businessToReviews == null");
		
		
		double businessAvg = reviewStatistic.businessAvg(businessId);
		
		List<ReviewStars> userReviews = usersToReviews.get(userId);
		if (userReviews == null){
			newUserCount++;
			return businessAvg;
		}
		if (userReviews.size() == 0){
			neverReviewCount++;
			return businessAvg;
		}
		List<ReviewStars> businessReviews = businessToReviews.get(businessId);
		if (businessReviews == null || businessReviews.size() == 0){
			newBusinessCount++;
			return businessAvg;
		}
		double similaritySum = 0;
		double devRatioSum = 0;
		for (ReviewStars oneur : userReviews){
			
			// For each review by the user
			// 1, find the most similar review in the business
			// 2, use the similarity as weight
			
			double similarity = 0;
			
			for (ReviewStars onebr : businessReviews){
				double sim = reviewSimilarityModel.cosineSimilarity(onebr.getReview_id(), oneur.getReview_id());
				if (sim > similarity)
					similarity = sim;
			}
			// compute dev
			double dev = oneur.getStars() - reviewStatistic.businessAvg(oneur.getBusiness_id());
			double devRatio = dev / reviewStatistic.businessStdev(oneur.getBusiness_id());
			if (Double.isNaN(devRatio))
				throw new RuntimeException("Double.isNaN(devRatio)");
			devRatioSum += similarity * devRatio;
			similaritySum += similarity;
		}
		
		
		if (similaritySum == 0){
			throw new RuntimeException("similaritySum == 0");
		}
		
		double finalDevRatio = devRatioSum / similaritySum;
		double predictedRate = businessAvg + finalDevRatio * reviewStatistic.businessStdev(businessId);

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
		
		usersToReviews = new HashMap<String, List<ReviewStars>>();
		businessToReviews = new HashMap<String, List<ReviewStars>>();
		seperateByUsers();
		seperateByBusiness();
		trainData = null; // release memory
	}
	
	void seperateByUsers(){
		// seperate training data by users 
		for (ReviewStars review : trainData){
			String user = review.getUser_id();
			if (!usersToReviews.containsKey(user))
				usersToReviews.put(user, new ArrayList<ReviewStars>());
			List<ReviewStars> userReviews = usersToReviews.get(user);
			
			userReviews.add(review);
		}
		
	}
	void seperateByBusiness(){
		// seperate training data by business
		for (ReviewStars review : trainData){
			String business = review.getBusiness_id();
			if (!businessToReviews.containsKey(business))
				businessToReviews.put(business, new ArrayList<ReviewStars>());
			List<ReviewStars> businessReviews = businessToReviews.get(business);
			
			businessReviews.add(review);
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
		ReviewTopicFile reviewTopicFile = new ReviewTopicFile("./dirty data/reviews.topicdist", 0);
		
		try {
			System.out.println("Reading training data...");
			List<ReviewStars> trainData = reviewReader.readReviewStars(-1);
			
			ReviewStatistic reviewStatistic = new ReviewStatistic(trainData);
			reviewStatistic.train();
			
			ItemSimilarityModel itemSimilarityModel = new ItemSimilarityModel(
					reviewTopicFile.read());
			
			ReviewBasedCFModel itemBasedCFModel = new ReviewBasedCFModel(trainData, itemSimilarityModel, reviewStatistic, 4);
			
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
