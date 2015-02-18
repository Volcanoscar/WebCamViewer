package cz.yetanotherview.webcamviewer.app.helper;

import java.util.Comparator;

import cz.yetanotherview.webcamviewer.app.model.Country;

public class CountryNameComparator implements Comparator<Country> {
    @Override
    public int compare(Country country1, Country country2) {
        String countryName1 = country1.getCountryName();
        String countryName2 = country2.getCountryName();

        return countryName1.compareToIgnoreCase(countryName2);
    }
}
