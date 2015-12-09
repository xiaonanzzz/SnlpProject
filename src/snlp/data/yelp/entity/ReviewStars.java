/**
 * 
 */
package snlp.data.yelp.entity;

/**
 * @author Alex
 * 
 */
public class ReviewStars {
	String business_id;
	String user_id;
	int stars;
	public String getBusiness_id() {
		return business_id;
	}
	public void setBusiness_id(String business_id) {
		this.business_id = business_id;
	}
	public String getUser_id() {
		return user_id;
	}
	public void setUser_id(String user_id) {
		this.user_id = user_id;
	}
	public int getStars() {
		return stars;
	}
	public void setStars(int stars) {
		this.stars = stars;
	}
	
	
}
