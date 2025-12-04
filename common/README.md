Mapping coordinates to country
--------------

`Geo.getCountry(coordinate)` is used for mapping coordinates to country. This is backed by
[countryboundaries](https://github.com/westnordost/countryboundaries) and 
[country-boundaries.osm](https://github.com/entur/geocoder-data/blob/main/country-boundaries.osm) from the 
[geocoder-data](https://github.com/entur/geocoder-data) repository.

The `resources/countryboundaries/boundaries60x30.ser` file is created this way: 

```
git clone https://github.com/westnordost/countryboundaries
cd countryboundaries/generator
../gradlew build
java -jar build/libs/generator-all.jar path/to/country-boundaries.osm 60 30
```
