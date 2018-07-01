package com.company;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) throws IOException {
        //String carUrl = "https://autogidas.lt/skelbimas/volkswagen-passat-benzinas-1997-m-b4-0129301767.html";
        //System.out.println(getParameters(carUrl));

        String pageUrl = "https://autogidas.lt/skelbimai/automobiliai/?f_1=Volvo&f_model_14=C30&f_215=&f_216=&f_41=&f_42=&f_3=&f_2=&f_376=";
        ArrayList<String> urls = getUrls(pageUrl);

        //System.out.println(getParameters("https://autogidas.lt/skelbimas/dodge-grand-caravan-benzinasdujos-2008-m-vienaturis-0129960053.html"));

        StringBuilder sqlinsert = new StringBuilder();
        sqlinsert.append("INSERT INTO data (id, url, make, model, year, price, extraTaxes, mileage, sold, techCheckUpAvailable, techValidMonths, engineSize, fault, enginePower, numOfAddons) VALUES ");

        Iterator itr = urls.iterator();
        while(itr.hasNext()) {
            Object element = itr.next();
            Map<String, String> prmtrs = getParameters(element.toString());
            sqlinsert.append("(");
            sqlinsert.append(prmtrs.get("id"));
            sqlinsert.append(", ");
            sqlinsert.append("'");
            sqlinsert.append(prmtrs.get("url"));
            sqlinsert.append("'");
            sqlinsert.append(", ");
            sqlinsert.append("'");
            sqlinsert.append(prmtrs.get("make"));
            sqlinsert.append("'");
            sqlinsert.append(", ");
            sqlinsert.append("'");
            sqlinsert.append(prmtrs.get("model"));
            sqlinsert.append("'");
            sqlinsert.append(", ");
            sqlinsert.append(prmtrs.get("year"));
            sqlinsert.append(", ");
            sqlinsert.append(prmtrs.get("price"));
            sqlinsert.append(", ");
            sqlinsert.append(prmtrs.get("extraTaxes"));
            sqlinsert.append(", ");
            sqlinsert.append(prmtrs.get("mileage"));
            sqlinsert.append(", ");
            sqlinsert.append(prmtrs.get("sold"));
            sqlinsert.append(", ");
            sqlinsert.append(prmtrs.get("techCheckUpAvailable"));
            sqlinsert.append(", ");
            sqlinsert.append(prmtrs.get("techValidMonths"));
            sqlinsert.append(", ");
            sqlinsert.append(prmtrs.get("engineSize"));
            sqlinsert.append(", ");
            if (prmtrs.get("fault") == null) {
                sqlinsert.append(prmtrs.get("fault"));
            }
            else {
                sqlinsert.append("'");
                sqlinsert.append(prmtrs.get("fault"));
                sqlinsert.append("'");
            }
            sqlinsert.append(", ");
            sqlinsert.append(prmtrs.get("enginePower"));
            sqlinsert.append(", ");
            sqlinsert.append(prmtrs.get("numOfAddons"));
            sqlinsert.append(")");

            if (!itr.hasNext()) {
                sqlinsert.append(";");
            }
            else {
                sqlinsert.append(", ");
            }
        }
        System.out.println(sqlinsert);

        Statement stmt = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/autoinfo","autoinfouser","autoinfouserpw");
            stmt = con.createStatement();
            stmt.executeUpdate(sqlinsert.toString());
            stmt.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static ArrayList<String> getUrls(String page) throws IOException {
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

        return carAdUrls;
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
            allParameters.put("techValidMonths", "0");
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

        //Gets the price and extra tax availability of the car
        if (parameters.containsKey("Kaina")) {
            String price = parameters.get("Kaina");
            price = price.replace(" ", "");
            price = price.replace("€", "");
            if (price.contains("+mokesčiai")) {
                allParameters.put("extraTaxes", "true");
                price = price.replace("+mokesčiai", "");
            }
            else {
                allParameters.put("extraTaxes", "false");
            }
            allParameters.put("price", price);
        }

        //Gets the ad id
        if (parameters.containsKey("Skelbimo ID:")) {
            allParameters.put("id", parameters.get("Skelbimo ID:"));
        }

        //Gets the faults
        if (parameters.containsKey("Defektai")) {
            if (parameters.get("Defektai").equals("Be defektų")) {
                allParameters.put("fault", null);
            }
            else {
                allParameters.put("fault", parameters.get("Defektai"));
            }
        }
        else {
            allParameters.put("fault", null);
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

        allParameters.put("url", url);

        //All the parameters from the webpage
        //System.out.println(parameters);

        return allParameters;
    }
}
