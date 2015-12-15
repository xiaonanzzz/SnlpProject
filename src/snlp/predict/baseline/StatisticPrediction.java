/**
 * 
 */
package snlp.predict.baseline;

import java.io.IOException;
import java.util.List;

import nlp.util.Counter;
import snlp.data.yelp.ReviewReader;
import snlp.data.yelp.entity.ReviewStars;
import snlp.predict.Predictor;
import snlp.predict.YelpPredictionEvaluator;

/**
 * @author Alex
 * A statistic way to predict the stars given a business
 */
public class StatisticPrediction implements Predictor{

	Counter<String> businessAvgStars = new Counter<String>();
	double avgBusinessStars = 0;
	List<ReviewStars> trainData;
	
	public StatisticPrediction(List<ReviewStars> trainData){
		this.trainData = trainData;
	}
	
	public void train(){
		Counter<String> starsCount = new Counter<String>();
		Counter<String> reviewCount = new Counter<String>();
		double starsSum = 0;
		int count = 0;
		for (ReviewStars review : this.trainData){
			starsSum += review.getStars();
			count++;
			starsCount.incrementCount(review.getBusiness_id(), review.getStars());
			reviewCount.incrementCount(review.getBusiness_id(), 1.0);
		}
		for (String bid : starsCount.keySet()){
			businessAvgStars.setCount(bid, starsCount.getCount(bid) / reviewCount.getCount(bid));
		}
		avgBusinessStars = starsSum / count;
		System.out.println("Business avg star=" + avgBusinessStars);
	}
	
	
	@Override
	public double predictStars(String userId, String businessId) {
		if (businessAvgStars.containsKey(businessId))
			return businessAvgStars.getCount(businessId);
		System.out.println("Warning: business not in train data set!");
		return this.avgBusinessStars;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		ReviewReader reviewReader = new ReviewReader("./dirty data/train-rest-review-stars.json");
		ReviewReader testReader = new ReviewReader("./dirty data/validate-rest-review-stars.json");
		
		try {
			System.out.println("Reading training data...");
			StatisticPrediction statisticPrediction = new StatisticPrediction(reviewReader.readReviewStars(-1));
			
			System.out.println("Training...");
			statisticPrediction.train();
			
			System.out.println("Reading test data...");
			YelpPredictionEvaluator yelpPredictionEvaluator = new YelpPredictionEvaluator(testReader.readReviewStars(-1));
			
			System.out.println("Computing MAE...");
			
			double mae = yelpPredictionEvaluator.copmuteMAE(statisticPrediction);
			yelpPredictionEvaluator.dump();
			System.out.printf("MAE=%f\n", mae);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}

}
