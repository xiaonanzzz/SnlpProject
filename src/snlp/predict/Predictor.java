/**
 * 
 */
package snlp.predict;

/**
 * @author Alex
 *
 */
public interface Predictor {

	/**
	 * 
	 * @param userId
	 * @param businessId
	 * @return predicted stars can be a real number like 3.2
	 */
	double predictStars(String userId, String businessId);
	
}
