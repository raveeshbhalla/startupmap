/*******************************************************************************
 * Copyright 2011-2013 Sergey Tarasevich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package co.raveesh.yspages;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Raveesh
 *
 * The following project scrapes YourStory Pages and retrieves a list of companies under their various categories.
 * It can also scrape an individual company's Page and retrieve data and store it. Currently, it has been restricted to
 * download only a company's address for a project of mine, in the future, detailed information would be provided.
 */


public class YSScraper {
	static String CURRENT = Constants.SOCIAL;
	public static void main(String [] args)
	{
		/**
		 * Un-comment the action you want to perform
		 */
		
		//scrapeListOfCompaniesFromYPages();
		
		//getListOfCompaniesFromFile();
		
		outputArrayWithAllAddresses();
	}
	
	
	/**
	 *	If list of companies have already been scraped and need to be accessed, use this method
	 */
	private static void getListOfCompaniesFromFile() {
		BufferedReader br = null;
		int addresses = 0;
		JSONObject companyWithAddresses = new JSONObject();
		try{
			String fileData=returnFileContents(Constants.OUTPUT_BASE+CURRENT+".json");
			JSONObject  fileObject = new JSONObject(fileData);
			JSONArray	companiesArray = fileObject.getJSONArray("companies");
			JSONArray	companyArray2 = new JSONArray();
			for (int i=0;i<companiesArray.length();i++){
				System.out.println("Done with:"+i+"/"+companiesArray.length());
				JSONObject company;
				try {
					company = scrapeIndividualCompanyInformation(companiesArray.getJSONObject(i));
				} catch (IOException e) {
					e.printStackTrace();
					i--;
					continue;
				}
				addresses += ((JSONArray)company.getJSONArray("addresses")).length();
				JSONObject company2 = new JSONObject();
				company2.put("name", company.getString("name"));
				company2.put("addresses", company.getJSONArray("addresses"));
				companyArray2.put(company2);
			}
			companyWithAddresses.put("companies", companyArray2);
			companyWithAddresses.put("number", companyArray2.length());
			companyWithAddresses.put("addresses", addresses);
			System.out.println("Retrieved addresses = "+addresses);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XPatherException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		writeToFile(CURRENT+"-with-addresses",companyWithAddresses);
	}
	
	/**
	 * Returns individual company information. Currently limited to returning only addresses
	 * @param company The company's individual JSONObject
	 * @return company JSONObject with obtained data added to it
	 * @throws IOException Thrown in case of error getting data from website
	 * @throws XPatherException Thrown in case of error with evaluation of XPath
	 * @throws JSONException Thrown in case of error putting or retrieving JSON data
	 */
	public static JSONObject scrapeIndividualCompanyInformation(JSONObject company) throws IOException, XPatherException, JSONException{
		HtmlCleaner cleaner = new HtmlCleaner();
		CleanerProperties props = cleaner.getProperties();
		props.setAllowHtmlInsideAttributes(true);
		props.setAllowMultiWordAttributes(true);
		props.setRecognizeUnicodeChars(true);
		props.setOmitComments(true);
		
		URL url = new URL(company.getString("page"));
		System.out.println("Getting data for company:"+company.getString("name"));
		System.out.println("Company page:"+company.getString("page"));
		URLConnection conn = url.openConnection();
		TagNode node = cleaner.clean(new InputStreamReader(conn.getInputStream()));
		Object[] companyAddresses = node.evaluateXPath("//div[@class='singleoffice']");
		
		JSONArray addresses = new JSONArray();
		for (int i=0;i<companyAddresses.length;i++){
			TagNode address = (TagNode)companyAddresses[i];
			Object[] spans = address.evaluateXPath("//span");
			String headline="",city="",detail="";
			if (spans.length>0){
				 headline =((TagNode)spans[0]).getText().toString().trim().replaceAll("\n", "").replaceAll("\r", "");
			}
			if (spans.length>1){
				 city =((TagNode)spans[1]).getText().toString().trim().replaceAll("\n", "").replaceAll("\r", "");
			}
			Object[] longaddress = address.evaluateXPath("//span[@class='longaddress']");
			if (longaddress.length>0){
				detail = ((TagNode)longaddress[0]).getText().toString().trim().replaceAll("\n", "").replaceAll("\r", "");
			}
			
			JSONObject individualAddress = new JSONObject();
			individualAddress.put("headline", headline);
			individualAddress.put("city", city);
			individualAddress.put("detail", detail);
			boolean blank = true;
			if (!detail.equals("")){
				JSONObject latlong = getLatLong(readJsonFromUrl(Constants.GOOGLE_GECODE_BASE_URL+detail.replace(".", "").replace(",", "%20").replace(" ", "%20").replace("#", "")));
				if (latlong.has("lat")){
						individualAddress.put("latlong",latlong);
						blank = false;
				}
			}
			if (!city.equals("") && blank){
				individualAddress.put("latlong", getLatLong(readJsonFromUrl(Constants.GOOGLE_GECODE_BASE_URL+city.replace(".", "").replace(",", "%20").replace(" ", "%20").replace("#", ""))));
			}
			addresses.put(individualAddress);
		}
		company.put("addresses", addresses);
		return company;
	}

	/**
	 * If the list of companies needs to be scraped from YourStory Pages, use this method. 
	 * IMPORTANT NOTE: Several 504 errors might occur, particularly as you go deeper into a list.
	 * The current handling is to attempt 3 times, before skipping a webpage. However, when dealing with
	 * the "Others" category, this doesn't help much beyond a certain degree. It is suggested to skip "Others"
	 * once you reach Page 50.
	 */
	public static void scrapeListOfCompaniesFromYPages(){
		JSONArray array = new JSONArray();
		
		int page;
		int trial =0;
		for(page=1;;page++){
			try {
				System.out.println(new Date(System.currentTimeMillis())+"Getting data from page:"+page);
				ArrayList<Company> companies = performScrapingffIndividualPageffList(Constants.YS_BASE_URL+Constants.CATEGORY_BASE_URL+CURRENT+"/"+page,page);
				if (companies.size() == 0)
					break;
				else{
					for (int i=0;i<companies.size();i++){
						try {
							array.put(companies.get(i).getCompany());
							trial =0;
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				if (trial<3){
					trial++;
					page--;
				}
				else{
					trial=0;
					System.out.println("Skipping page:"+page);
				}
			}
		}
		
		JSONObject object = new JSONObject();
		try {
			object.put("companies", array);
			object.put("number", array.length());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		writeToFile(CURRENT,object);
	}
	
	/**
	 * Performs the scraping of an individual page from a list on the YourStory Pages website.
	 * The information is gathered and an ArrayList of Company objects is returned with appropriate data.
	 * @param Url URL of the individual page on the YourStory website
	 * @param page The current page number from the list. Required so as to scrape Featured companies from the first page only
	 * @return ArrayList<Company> of the companies found on the webpage
	 * @throws IOException Thrown if there is an error accessing the webpage
	 * @throws XPatherException Thrown if there is a significant error with the evaluateXPath
	 */
	public static ArrayList<Company> performScrapingffIndividualPageffList(String Url, int page) throws XPatherException, IOException{
		List<Company> companies = new ArrayList<Company>();
		
		HtmlCleaner cleaner = new HtmlCleaner();
		CleanerProperties props = cleaner.getProperties();
		props.setAllowHtmlInsideAttributes(true);
		props.setAllowMultiWordAttributes(true);
		props.setRecognizeUnicodeChars(true);
		props.setOmitComments(true);
		
		URL url = new URL(Url);
		URLConnection conn = url.openConnection();
		TagNode node = cleaner.clean(new InputStreamReader(conn.getInputStream()));
		if (node.getText().toString().contains("No Companies Found")){
			System.out.println("Caught 404");
		}
		else{
			if (page == 1){
				Object[] companiesFeatured = node.evaluateXPath("//li[@class='companypanel']");
				System.out.println("Getting featured companies information");
				for (int i=0;i<companiesFeatured.length;i++){
					companies.add(new Company((TagNode)companiesFeatured[i]));
				}
			}
			Object[] companiesScraped = node.evaluateXPath("//li[@class='companylistitem']");
			System.out.println("Getting company information");
			for (int i=0;i<companiesScraped.length;i++){ 
				companies.add(new Company((TagNode)companiesScraped[i]));
			}
		}
		return (ArrayList<Company>) companies;
	}
	
	/**
	 * This function writes a JSONObject to the required output file
	 * @param fileName Output file name. The /output directory and .json extension are added by this method
	 * @param object The required output content in JSONObject form
	 */
	private static void writeToFile(String fileName,JSONObject object){
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(Constants.OUTPUT_BASE+fileName+".json", "UTF-8");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		writer.println(object.toString());
		writer.close();
	}
	
	/**
	 * Returns JSON data from a URL. Copied from Stack Overflow answer by Rolan Illig. URL:http://stackoverflow.com/questions/4308554/simplest-way-to-read-json-from-a-url-in-java 
	 * @param url URL from where to retrieve the data
	 * @return Retrieved data from JSONObject format
	 * @throws IOException Thrown if webpage cannot be accessed
	 * @throws JSONException Thrown in case of JSON error
	 */
	public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		System.out.println("Getting google maps data:"+url);
	    InputStream is = new URL(url).openStream();
	    try {
	      BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
	      String jsonText = readAll(rd);
	      JSONObject json = new JSONObject(jsonText);
	      return json;
	    } finally {
	      is.close();
	    }
	  }
	
	/**
	 * Returns String data read from URL. Copied from Stack Overflow answer by Rolan Illig. URL:http://stackoverflow.com/questions/4308554/simplest-way-to-read-json-from-a-url-in-java 
	 * @param rd
	 * @return
	 * @throws IOException
	 */
	private static String readAll(Reader rd) throws IOException {
	    StringBuilder sb = new StringBuilder();
	    int cp;
	    while ((cp = rd.read()) != -1) {
	      sb.append((char) cp);
	    }
	    return sb.toString();
	  }
	
	private static JSONObject getLatLong(JSONObject gMapsData) throws JSONException{
		JSONObject latLong = new JSONObject();
		JSONArray results = gMapsData.getJSONArray("results");
		if (results.length()>0){
			latLong = results.getJSONObject(0).getJSONObject("geometry").getJSONObject("location");
		}
		else{
			System.out.println("No result found");
			System.out.println(gMapsData.toString());
		}
		return latLong;
	}
	
	private static String returnFileContents(String fileName){
		try {
			String sCurrentLine,fileData="";
			try {
				BufferedReader br = new BufferedReader(new FileReader(fileName));
				while ((sCurrentLine = br.readLine()) != null) {
					fileData=sCurrentLine+"\n";
				}
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			fileData = fileData.trim();
			return fileData;
		} catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	private static void outputArrayWithAllAddresses(){
		ArrayList<String> files = getAllAddressFilesFromOutput();
		JSONObject output = new JSONObject();
		JSONArray outputMarkers = new JSONArray();
		int addressesMapped = 0, startupsMapped = 0, totalstartups = 0;
		for (int i=0;i<files.size();i++){
			try {
				JSONObject fileData = new JSONObject(returnFileContents(Constants.OUTPUT_BASE+files.get(i)));
				JSONArray companies = fileData.getJSONArray("companies");
				totalstartups += companies.length();
				for (int j=0;j<companies.length();j++){
					boolean mapped = false;
					JSONObject company = companies.getJSONObject(j);
					if (company.has("addresses")){
						JSONArray addresses = company.getJSONArray("addresses");
						if (addresses.length()>0){
							for (int k=0;k<addresses.length();k++){
								if (addresses.getJSONObject(k).has("latlong")){
									if (addresses.getJSONObject(k).getJSONObject("latlong").has("lat")){
										mapped = true;
										JSONObject mapAddress = new JSONObject();
										System.out.println(String.format("Checking %s, company %s",files.get(i),company.getString("name")));
										mapAddress.put("lat", addresses.getJSONObject(k).getJSONObject("latlong").getDouble("lat"));
										mapAddress.put("lng", addresses.getJSONObject(k).getJSONObject("latlong").getDouble("lng"));
										mapAddress.put("name", company.getString("name"));
										outputMarkers.put(mapAddress);
										addressesMapped++;
									}
								}
							}
						}
					}
					if (mapped){
						startupsMapped++;
					}	
				}
				output.put("companies", outputMarkers);
				output.put("total_startups", totalstartups);
				output.put("addresses_mapped", addressesMapped);
				output.put("startups_mapped",startupsMapped);
				writeToFile("alladdress",output);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	private static ArrayList<String> getAllAddressFilesFromOutput(){
		List<String> files = new ArrayList<String>();
		File folder = new File(Constants.OUTPUT_BASE);
		 for (final File fileEntry : folder.listFiles()) {
		        if (!fileEntry.isDirectory() && fileEntry.getName().contains("with-addresses")) {
		        	files.add(fileEntry.getName());
		        }
		    }
		 return (ArrayList)files;
	}
}
