/**
 * 
 */
package snlp.predict.ibcf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nlp.util.Dumper;
import snlp.data.yelp.entity.ReviewStars;

/**
 * @author Alex
 *
 */
public class ReviewStatistic {

	List<ReviewStars> data;
	Map<String, int[]> businessStarsDistribution;
	Map<String, Double> businessAvgStars;
	Map<String, Double> businessStdev;
	Map<String, Integer> businessReviewMode;
	double totalAvg;
	double stdevAvg;
	
	/**
	 * @param data
	 */
	public ReviewStatistic(List<ReviewStars> data) {
		super();
		this.data = data;
	}

	/**
	 * 
	 */
	public void train(){
		if (data == null)
			throw new RuntimeException("data == null");
		//computeMode();
		computeAverage();
		this.data = null; //release memory
	}
	
	/***
	 * 
	 * @param businessId
	 * @return If business average does not exists, return overall average stars. 
	 * If business overall average stars does not exists, return 0.
	 * CAN USE 
	 */
	public double businessAvg(String businessId){
		
		if (isBusinessDataInsufficient(businessId)){
//			System.out.println("Warning: business don't have sufficient data to compute avg give total avg");
			return totalAvg;
		}
		
		return businessAvgStars.get(businessId);
	}
	
	/**
	 * 
	 * @param businessId
	 * @return
	 */
	public boolean isBusinessDataInsufficient(String businessId){
		if (!businessStarsDistribution.containsKey(businessId))
			return true;
		
		int[] busDist = businessStarsDistribution.get(businessId);
		if (busDist[0] == 0)
			return true;
		
		return false;
	}
	
	/**
	 * 
	 * @param businessId
	 * @return
	 */
	public double businessStdev(String businessId){
		if (isBusinessDataInsufficient(businessId)){
//			System.out.println("Warning: business don't have sufficient data to compute avg give total avg");
			return stdevAvg;
		}
		
		return businessStdev.get(businessId);
	}
	
	/**
	 * 
	 * @param businessId
	 * @return the star amount of each level(1-5) [0] is the total number of stars. 
	 * But if business doesn't exist, return null.
	 */
	public int[] businessStarsDistribution(String businessId){
		
		return businessStarsDistribution.get(businessId);
	}

	void computeMode(){
		computeStarsDistribution();
		businessReviewMode = new HashMap<String, Integer>();
		for (Map.Entry<String, int[]> entry : businessStarsDistribution.entrySet()){
			String id = entry.getKey();
			int[] dist = entry.getValue();
			
			int max = 0;
			int maxindex = 0;
			for (int i = 1; i <= 5; i++){
				if (dist[i] > max){
					max = dist[i];
					maxindex = i;
				}
					
			}
			
			businessReviewMode.put(id, maxindex);
			
		}
		
	}

	void computeStarsDistribution(){
		businessStarsDistribution = new HashMap<String, int[]>();
		int[] totalDist = new int[6];
		for (ReviewStars oneReview : data){
			if (!businessStarsDistribution.containsKey(oneReview.getBusiness_id()))
				businessStarsDistribution.put(oneReview.getBusiness_id(), new int[6]);
			int[] busDist = businessStarsDistribution.get(oneReview.getBusiness_id());
			busDist[0]++;
			busDist[oneReview.getStars()]++;
			totalDist[oneReview.getStars()]++;
		}
		System.out.println("Training business count: " + businessStarsDistribution.size());
		System.out.println("Total distribution:");
		for(int i = 0; i <= 5; i++){
			System.out.printf("%d ", totalDist[i]);
		}
		System.out.println();
	}

	void dump(){
		Dumper dump = new Dumper("./dump", "starsDistribution");
		for (Map.Entry<String, int[]> entry : businessStarsDistribution.entrySet()){
			String id = entry.getKey();
			int[] dist = entry.getValue();
			
			for(int i = 1; i <= 5; i++){
				dump.dumpf("%.2f ", (dist[i]*1.0)/(dist[0]));
			}
			dump.dumpln(dist[0] + " " + id);
			
		}
	}
	
	void computeAverage(){
		computeStarsDistribution();
		businessAvgStars = new HashMap<String, Double>();
		businessStdev = new HashMap<String, Double>();
		
		totalAvg = 0;
		// compute average and stdev
		for (Map.Entry<String, int[]> entry : businessStarsDistribution.entrySet()){
			String id = entry.getKey();
			int[] dist = entry.getValue();
			// average
			double sum = 0;
			int count = 0;
			for(int i = 1; i <= 5; i++){
				sum += i * dist[i];
				count += dist[i];
			}
			if (count < 1 || count != dist[0])
				throw new RuntimeException("count < 1 || count != dist[0]");
			double avg = sum / count;
			businessAvgStars.put(id, avg);
			
			double dev = 0;
			// compute stdev
			for(int i = 1; i <= 5; i++){
				dev += dist[i] * Math.pow(i - avg, 2);
			}
			double stdev = count > 0 ? Math.sqrt(dev / (count)) : 1;
			if (stdev < 0.5)
				stdev = 0.5;
			businessStdev.put(id, stdev);
			
			// total
			totalAvg += avg;
			stdevAvg += stdev;
		}
		totalAvg /= businessStarsDistribution.size();
		stdevAvg /= businessStarsDistribution.size();
		
		System.out.println("Review total average stars: " + totalAvg);
		System.out.println("Review total average stdev: " + stdevAvg);
		
	}

	public double getTotalAvg() {
		return totalAvg;
	}

	public double getStdevAvg() {
		return stdevAvg;
	}
	
}
