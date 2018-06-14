package com.company;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) throws IOException {
        //String carUrl = "https://autogidas.lt/skelbimas/volkswagen-passat-benzinas-1997-m-b4-0129301767.html";
        //System.out.println(getParameters(carUrl));

        String pageUrl = "https://autogidas.lt/skelbimai/automobiliai/?f_1=Asia";
        getUrls(pageUrl);
    }

    public static void getUrls(String page) throws IOException {
        Document d = Jsoup.connect(page + "&page=1").timeout(6000).get();

        //Gets the number of pages in the search
        Element paginatorElement = d.getElementsByClass("paginator").get(0);
        String[] pages = paginatorElement.text().split(" ");
        int noOfPages = Integer.parseInt(pages[pages.length-1]);

        ArrayList<String> carAdUrls = new ArrayList<String>();

        for (int i = 1; i <= noOfPages; i++) {
            Document p = Jsoup.connect(page + "&page=" + i).timeout(6000).get();
            Element getUrls = p.getElementsByClass("all-ads-block").get(0);
            Elements getUrlsUrls = getUrls.getElementsByClass("item-link");

            //TODO check if URL's are unique
            for (Element temp : getUrlsUrls) {
                carAdUrls.add("https://autogidas.lt" + temp.attr("href"));
            }
        }

        System.out.println(carAdUrls);
    }

    public static Map<String, String> getParameters(String url) throws IOException {
        Document d = Jsoup.connect(url).timeout(6000).get();

        Map<String, String> allParameters = new HashMap<String, String>();

        //Gets the make and model of the car
        String carMake = "";
        String carModel = "";
        Elements makeModel = d.getElementsByAttributeValue("itemprop", "name");
        int i = 0;
        for (Element temp : makeModel) {
            if (i == 2) {
                carMake = temp.text();
            }
            else if (i == 3) {
                carModel = temp.text();
            }
            i++;
        }
        allParameters.put("make", carMake);
        allParameters.put("model", carModel);

        //Gets the parameters of the car
        Map<String, String> parameters = new HashMap<String, String>();
        Elements getParams = d.getElementsByClass("param");
        for (Element temp : getParams) {
            Elements param = temp.getElementsByClass("left");
            Elements value = temp.getElementsByClass("right");
            parameters.put(param.text(), value.text());
        }

        //Find the production year
        if (parameters.containsKey("Metai")) {
            Pattern yearPattern = Pattern.compile("(\\d{4})/\\d{2} m\\.");
            Matcher yearMatcher = yearPattern.matcher(parameters.get("Metai"));
            if (yearMatcher.matches()) {
                allParameters.put("year", yearMatcher.group(1));
            }
        }

        //Find the mileage
        if (parameters.containsKey("Rida, km")) {
            Pattern mileagePattern = Pattern.compile("(\\d+) km");
            Matcher mileageMatcher = mileagePattern.matcher(parameters.get("Rida, km"));
            if (mileageMatcher.matches()) {
                allParameters.put("mileage", mileageMatcher.group(1));
            }
        }

        //Find the technical check-up availability
        if (parameters.containsKey("TA iki")) {
            allParameters.put("techCheckUpAvailable", "true");

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM");

            Date todayDate = new Date();
            Date techDate; //The date until which the tech check-up is valid
            try {
                techDate = df.parse(parameters.get("TA iki"));
                long diffInMillis = Math.abs(techDate.getTime() - todayDate.getTime());
                allParameters.put("techValidMonths", String.valueOf(Math.round(diffInMillis/(1000*60*60*24*30.44))));
            } catch (ParseException e) {
                System.out.println("Tech check-up date unparsable using " + df);
            }
        }
        else {
            allParameters.put("techCheckUpAvailable", "false");
        }

        //Gets the engine details
        if (parameters.containsKey("Variklis")) {
            Pattern enginePattern = Pattern.compile("((\\d\\.\\d)l\\.)? ?((\\d+)kW \\(\\d+Ag\\))?");
            Matcher engineMatcher = enginePattern.matcher(parameters.get("Variklis"));
            if (engineMatcher.matches()) {
                if (engineMatcher.group(2) != null) {
                    allParameters.put("engineSize", engineMatcher.group(2));
                }
                if (engineMatcher.group(4) != null) {
                    allParameters.put("enginePower", engineMatcher.group(4));
                }
            }
        }

        //Gets the price of the car
        if (parameters.containsKey("Kaina")) {
            String price = parameters.get("Kaina");
            price = price.replace(" ", "");
            price = price.replace("€", "");
            allParameters.put("price", price);
        }

        //Gets the ad id
        if (parameters.containsKey("Skelbimo ID:")) {
            allParameters.put("id", parameters.get("Skelbimo ID:"));
        }

        //Gets the faults
        if (parameters.containsKey("Defektai")) {
            allParameters.put("fault", parameters.get("Defektai"));
        }

        //Gets the addons of the car
        ArrayList<String> addons = new ArrayList<String>();
        Elements getAddons = d.getElementsByClass("addons").get(0).getElementsByClass("addon");
        for (Element temp : getAddons) {
            addons.add(temp.text().substring(2)); //substring(2) needed to remove "» "
        }
        allParameters.put("numOfAddons", String.valueOf(addons.size()));

        //Gets the sold value of car
        boolean sold = false;
        Elements getSold = d.getElementsByClass("sold");
        if (!getSold.isEmpty()) {
            sold = true;
        }
        allParameters.put("sold", String.valueOf(sold));

        System.out.println(parameters);

        return allParameters;
    }
}
