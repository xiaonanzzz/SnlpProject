/**
 * 
 */
package snlp.predict;

import java.util.List;

import snlp.data.yelp.entity.ReviewStars;

/**
 * @author Alex
 * evaluate yelp data
 */
public class YelpPredictionEvaluator{

	List<ReviewStars> testData;
	int[][] errorMatrix = new int[6][6];
	int[][] correctMatrix = new int[6][6];
	
	
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
			ps = Math.round(ps);
			int rs = review.getStars();
			error += Math.abs(ps - rs);
			if (ps != rs) {
				errorMatrix[rs][(int) Math.round(ps)]++;
			}else{
				correctMatrix[rs][(int) Math.round(ps)]++;
			}
			count++;
		}
		return error / count;
	}
	
	public void dump(){
		System.out.println("ErrorMatrix");
		for (int i = 1; i < 6; i++){
			for (int j = 1; j < 6;j++){
				System.out.print(errorMatrix[i][j] + "\t");
			}
			System.out.println("");
		}
		System.out.println("CorrectMatrix");
		for (int i = 1; i < 6; i++){
			for (int j = 1; j < 6;j++){
				System.out.print(correctMatrix[i][j] + "\t");
			}
			System.out.println("");
		}
		System.out.println("ErrorWeightedMatrix");
		for (int i = 1; i < 6; i++){
			for (int j = 1; j < 6;j++){
				System.out.print((errorMatrix[i][j]*Math.abs(i-j)) + "\t");
			}
			System.out.println("");
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
