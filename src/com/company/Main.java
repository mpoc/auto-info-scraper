package com.company;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws IOException {
        String carUrl = "https://autogidas.lt/skelbimas/bmw-728-benzinasdujos-1998-m-e38-0129898274.html";
        Document d = Jsoup.connect(carUrl).timeout(6000).get();

        Map<String, String> parameters = new HashMap<String, String>();

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
        parameters.put("make", carMake);
        parameters.put("model", carModel);

        //Gets the parameters of the car
        Elements getParams = d.getElementsByClass("param");
        for (Element temp : getParams) {
            Elements param = temp.getElementsByClass("left");
            Elements value = temp.getElementsByClass("right");
            parameters.put(param.text(), value.text());
        }

        //Gets the addons of the car
        ArrayList<String> addons = new ArrayList<String>();
        Elements getAddons = d.getElementsByClass("addons").get(0).getElementsByClass("addon");
        for (Element temp : getAddons) {
            addons.add(temp.text().substring(2)); //substring(2) needed to remove "» "
        }
        parameters.put("numOfAddons", String.valueOf(addons.size()));

        //Gets the sold value of car
        boolean sold = false;
        Elements getSold = d.getElementsByClass("sold");
        if (!getSold.isEmpty()) {
            sold = true;
        }
        parameters.put("sold", String.valueOf(sold));

        System.out.println(parameters);
        System.out.println(addons);


    }
}
