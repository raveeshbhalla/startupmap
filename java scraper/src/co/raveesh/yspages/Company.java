package co.raveesh.yspages;

import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.json.JSONException;
import org.json.JSONObject;

public class Company {
	private String Name,ImageUrl,PagesUrl;

	public Company(TagNode company){
		Object[] browsercompanyname = null;
		try {
			browsercompanyname = company.evaluateXPath("//div[@class='browsercompanyname']");
		} catch (XPatherException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		if (browsercompanyname.length>0 && browsercompanyname !=null){
			setName(((TagNode)browsercompanyname[0]).getText().toString());
		}
		else{
			setName(company.getText().toString());
		}
		try {
			Object[] anchors = company.evaluateXPath("//a");
			TagNode pageURL = (TagNode)anchors[0];
			setPagesUrl(pageURL.getAttributeByName("href"));
		} catch (XPatherException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			Object[] anchors = company.evaluateXPath("//img");
			TagNode imgURL = (TagNode)anchors[0];
			setImageUrl(imgURL.getAttributeByName("src"));
		} catch (XPatherException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String getPagesUrl() {
		return PagesUrl;
	}

	public void setPagesUrl(String pagesUrl) {
		PagesUrl = pagesUrl.replaceAll("\r\n", "").trim();
	}

	public String getImageUrl() {
		return ImageUrl;
	}

	public void setImageUrl(String imageUrl) {
		ImageUrl = imageUrl.replaceAll("\r\n", "").trim();
	}

	public String getName() {
		return Name;
	}

	public void setName(String name) {
		Name = name.replaceAll("\r\n", "").trim();
	}
	
	public JSONObject getCompany() throws JSONException{
		JSONObject company = new JSONObject();
		company.put("name", Name);
		company.put("icon", ImageUrl);
		company.put("page", PagesUrl);
		return company;
	}
	
	public String toString(){
		try {
			return getCompany().toString();
		} catch (JSONException e) {
			e.printStackTrace();
			return "Error with company information";
		}
	}
}
