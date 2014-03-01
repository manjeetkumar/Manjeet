import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.resource.ClientResource;

public class SelectGifts {

	public static void main(String[] args) throws IOException, JSONException {
		SelectGifts z = new SelectGifts();
		String s = "";
		do {
			System.out.println("Please enter the number of items: ");
			BufferedReader br = new BufferedReader(new InputStreamReader(
					System.in));
			s = br.readLine();
		} while (!z.isNumber(s));

		Integer numberOfItems = Integer.valueOf(s);

		do {
			System.out.println("Please enter the totalValue: ");
			BufferedReader br = new BufferedReader(new InputStreamReader(
					System.in));
			s = br.readLine();
		} while (!z.isNumber(s));

		System.out.println("Please enter a valid API key: ");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		key = br.readLine();

		double totalValue = Double.valueOf(s);
		System.out.println("Following are suggested for finding "
				+ numberOfItems + " items for $" + totalValue + ":");

		Map<Map<String, Map<String, Double>>, Integer> productsList = new HashMap<Map<String, Map<String, Double>>, Integer>(
				10);

		int currentItemCount = 0;

		for (int i = numberOfItems; i > 0; i = i - currentItemCount) {
			double[] itemDetails = setNextProduct(i, z, totalValue,
					productsList);
			totalValue = totalValue - itemDetails[0];
			currentItemCount = (int) itemDetails[1];
		}

		for (Map.Entry<Map<String, Map<String, Double>>, Integer> mapItems : productsList
				.entrySet()) {
			System.out.println("**************************************");
			System.out.println(mapItems.getValue()
					+ " "
					+ mapItems.getKey().entrySet().iterator().next().getKey()
							.replace("%20", " ") + " from this list");
			z.printProducts(mapItems.getKey().entrySet().iterator().next()
					.getValue());
		}
	}

	private void printProducts(Map<String, Double> products)
			throws IOException, JSONException {
		StringBuffer productIds = new StringBuffer();
		for (Map.Entry<String, Double> product : products.entrySet()) {
			productIds.append(",");
			productIds.append(product.getKey());
		}

		String url = "http://api.zappos.com/Product/"
				+ productIds.toString().substring(1) + "?key=" + key;
		ClientResource resource = new ClientResource(url);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		resource.get().write(baos);
		String resultString = baos.toString();
		JSONObject j = (JSONObject) new JSONObject(resultString).getJSONArray(
				"product").get(0);
		System.out.println(j.toString(4));
	}

	private static double[] setNextProduct(int numberOfItems, SelectGifts z,
			double totalValue,
			Map<Map<String, Map<String, Double>>, Integer> productsList)
			throws IOException, JSONException {
		List<String> existingItems = new ArrayList<String>();
		for (Map.Entry<Map<String, Map<String, Double>>, Integer> mapItems : productsList
				.entrySet()) {
			existingItems.add(mapItems.getKey().entrySet().iterator().next()
					.getKey());
		}
		Map<String, Integer> items = z.getItemType(numberOfItems,
				existingItems, false);
		Double units = 0.0;
		String item = "";
		for (String key : items.keySet()) {
			item = key;
			units = units + itemTypes.get(key) * items.get(key);
		}

		double unitPrice = totalValue / units;

		if (unitPrice < 25) {
			items = z.getItemType(numberOfItems, existingItems, true);
			units = 0.0;
			for (String key : items.keySet()) {
				item = key;
				units = units + itemTypes.get(key) * items.get(key);
			}
			unitPrice = totalValue / units;
		}

		Map<Integer, Map.Entry<String, Double>> allProducts;

		Double price = unitPrice * itemTypes.get(item);
		allProducts = z.getItemList(price, item);
		Map<String, Double> products = z.findItem(allProducts, price,
				items.get(item));
		double itemPrice = products.values().iterator().next();
		double itemValue = itemPrice * items.get(item);
		Map<String, Map<String, Double>> productMap = new HashMap<String, Map<String, Double>>();
		productMap.put(item, products);
		productsList.put(productMap, items.get(item));

		return new double[] { itemValue, items.get(item) };

	}

	private Map<Integer, Map.Entry<String, Double>> getItemList(double amount,
			String searchKey) throws IOException, JSONException {
		if (amount <= 50)
			searchKey = "$50.00%20and%20Under";
		else if (amount <= 100)
			searchKey = "$100.00%20and%20Under";
		else if (amount < 200)
			searchKey = "$200.00%20and%20Under";
		else
			searchKey = "$200.00%20and%20Over";

		String url = "http://api.zappos.com/Search?term=shirts&filters=%7B\"priceFacet\"%3A[\""
				+ searchKey
				+ "\"],\"colorFacet\"%3A[\"Red\"]%7D&excludes=[\"styleId\",\"colorId\",\"brandName\",\"productName\",\"productUrl\",\"thumbnailImageUrl\",\"originalPrice\",\"percentOff\"]&sort=%7B\"price\"%3A\"desc\"%7D&limit=100&key="
				+ key;
		ClientResource resource = new ClientResource(url);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		resource.get().write(baos);
		String resultString = baos.toString();
		JSONArray jsonObj = new JSONObject(resultString)
				.getJSONArray("results");
		Map<Integer, Map.Entry<String, Double>> t = new HashMap<Integer, Map.Entry<String, Double>>(
				100);

		for (int i = 0; i < jsonObj.length(); i++) {
			JSONObject j = (JSONObject) jsonObj.get(i);
			t.put(i + 1,
					new AbstractMap.SimpleEntry<String, Double>(j
							.getString("productId"), new Double(j.getString(
							"price").substring(1))));
		}

		return t;
	}

	private Map<String, Integer> getItemType(int qty,
			List<String> existingItems, boolean economical) {

		List<String> nonEconomicalItems = new ArrayList<String>(10);
		nonEconomicalItems.add("Shirt");
		nonEconomicalItems.add("pant");
		nonEconomicalItems.add("Handbag");
		nonEconomicalItems.add("tie");
		nonEconomicalItems.add("coat");
		nonEconomicalItems.add("Jeans");
		nonEconomicalItems.add("Blazer");
		nonEconomicalItems.add("Jeans");
		nonEconomicalItems.add("Hair%20Dryer");
		nonEconomicalItems.add("tshirt");

		List<String> economicalItems = new ArrayList<String>(5);
		economicalItems.add("tshirt");
		economicalItems.add("Jewelery");
		economicalItems.add("Nail%20Polish");
		economicalItems.add("Lipstick");
		economicalItems.add("Eye%20Liner");
		nonEconomicalItems.removeAll(existingItems);
		economicalItems.removeAll(existingItems);

		Map<String, Integer> itemsMap = new HashMap<String, Integer>(qty);
		if (!economical) {
			for (int i = 0; i < qty; i++) {
				int arrayIndex = i % nonEconomicalItems.size();
				int prvQty = itemsMap.get(nonEconomicalItems.get(arrayIndex)) == null ? 0
						: itemsMap.get(nonEconomicalItems.get(arrayIndex));
				itemsMap.put(nonEconomicalItems.get(arrayIndex), prvQty + 1);
			}
		} else {
			for (int i = 0; i < qty; i++) {
				int arrayIndex = i % economicalItems.size();
				int prvQty = itemsMap.get(economicalItems.get(arrayIndex)) == null ? 0
						: itemsMap.get(economicalItems.get(arrayIndex));
				itemsMap.put(economicalItems.get(arrayIndex), prvQty + 1);
			}
		}
		return itemsMap;
	}

	Map<String, Double> findItem(
			Map<Integer, Map.Entry<String, Double>> allItems, Double price,
			int count) {
		Map<String, Double> productsList = new HashMap<String, Double>(10);
		double basePrice = price;
		int index = 1;

		if (price > allItems.get(1).getValue()) {
			basePrice = allItems.get(1).getValue();
			index = index + count * 3 - 1;
		} else if (price < allItems.get(allItems.size()).getValue()) {
			basePrice = allItems.get(allItems.size()).getValue();
			index = allItems.size();
		} else {
			for (int i = 1; i < allItems.size(); i++) {
				if (allItems.get(i).getValue() < basePrice) {
					basePrice = allItems.get(i).getValue();
					index = i;
					break;
				}
			}
		}

		for (int i = index - count * 3 - 1; i <= index; i++) {
			productsList.put(allItems.get(i).getKey(), allItems.get(i)
					.getValue());
		}

		return productsList;
	}

	private boolean isNumber(String str) {
		return str.matches("-?\\d+");
	}

	private static String key = "12c3302e49b9b40ab8a222d7cf79a69ad11ffd78";
	private static final Map<String, Float> itemTypes = new HashMap<String, Float>();
	static {
		itemTypes.put("Shirt", 1f);
		itemTypes.put("pant", 1.5f);
		itemTypes.put("Handbag", 2f);
		itemTypes.put("tie", 0.75f);
		itemTypes.put("coat", 2.5f);
		itemTypes.put("Jeans", 1.5f);
		itemTypes.put("Blazer", 1.5f);
		itemTypes.put("Jeans", 1.5f);
		itemTypes.put("Hair%20Dryer", 2.5f);
		itemTypes.put("tshirt", 0.75f);
		itemTypes.put("Jewelery", 0.50f);
		itemTypes.put("Nail%20Polish", 0.25f);
		itemTypes.put("Lipstick", 0.25f);
		itemTypes.put("Eye%20Liner", 0.50f);
	}
}
