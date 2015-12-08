/**
 * 
 */
package nlp.predict;

import java.util.List;

import snlp.data.yelp.entity.ReviewStars;

/**
 * @author Alex
 * evaluate yelp data
 */
public class YelpPredictionEvaluator{

	List<ReviewStars> testData;
	
	
	
	public YelpPredictionEvaluator(List<ReviewStars> testData) {
		super();
		this.testData = testData;
	}

	/**
	 * 
	 * @param predictor
	 * @return compute the mean absolute error
	 */
	public double copmuteMAE(Predictor predictor){
		int count = 0;
		double error = 0;
		for (ReviewStars review : this.testData){
			double ps = predictor.predictStars(review.getUser_id(), review.getBusiness_id());
			double rs = review.getStars();
			error += Math.abs(ps - rs);
			count++;
		}
		return error / count;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
