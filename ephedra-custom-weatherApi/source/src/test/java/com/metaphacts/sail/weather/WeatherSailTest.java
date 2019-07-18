package com.metaphacts.sail.weather;

import java.util.Date;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.junit.Test;

public class WeatherSailTest {

    // TODO ADD YOUR DARK SKY API KEY HERE
    public static final String API_KEY = "YOUR-API-KEY-HERE";

    @Test
    public void testWeatherWalldorf() throws Exception {

        String longitude = "8.65";
        String latitude = "49.3";

        WeatherApiSail sail = new WeatherApiSail();
        sail.setApiKey(API_KEY);
        SailRepository repo = new SailRepository(sail);
        repo.initialize();

        try (RepositoryConnection conn = repo.getConnection()) {

            String queryString = "" + "PREFIX weather: <http://www.metaphacts.com/ontologies/weather#> "
                    + "SELECT * WHERE { " 
                    + "  ?item weather:longitude ?longitude . "
                    + "  ?item weather:latitude ?latitude . " 
                    + "  ?item weather:dataPointObject 'summary' . "
                    + "  ?item weather:dataPointObject 'time' . "
                    + "  ?item weather:dataPointObject 'apparentTemperatureMin' . "
                    + "  ?item weather:dataPointObject 'apparentTemperatureMax' . "
                    + "  ?item weather:dataPointObject 'icon' . " 
                    + "}";

            TupleQuery tq = conn.prepareTupleQuery(queryString);
            tq.setBinding("longitude", SimpleValueFactory.getInstance().createLiteral(longitude));
            tq.setBinding("latitude", SimpleValueFactory.getInstance().createLiteral(latitude));
            try (TupleQueryResult tqRes = tq.evaluate()) {
                while (tqRes.hasNext()) {
                    System.out.println(tqRes.next());
                }
            }
        }
        repo.shutDown();
    }

    @Test
    public void testWeatherWalldorf_Hourly() throws Exception {

        String longitude = "8.65";
        String latitude = "49.3";

        WeatherApiSail sail = new WeatherApiSail();
        sail.setApiKey(API_KEY);
        SailRepository repo = new SailRepository(sail);
        repo.initialize();

        try (RepositoryConnection conn = repo.getConnection()) {

            String queryString = "" + "PREFIX weather: <http://www.metaphacts.com/ontologies/weather#> "
                    + "SELECT * WHERE { " 
                    + "  ?item weather:longitude ?longitude . "
                    + "  ?item weather:latitude ?latitude . " 
                    + "  ?item weather:block 'hourly' . "
                    + "  ?item weather:dataPointObject 'summary' . " 
                    + "  ?item weather:dataPointObject 'time' . "
                    + "  ?item weather:dataPointObject 'apparentTemperature' . "
                    + "  ?item weather:dataPointObject 'icon' . " 
                    + "}";

            TupleQuery tq = conn.prepareTupleQuery(queryString);
            tq.setBinding("longitude", SimpleValueFactory.getInstance().createLiteral(longitude));
            tq.setBinding("latitude", SimpleValueFactory.getInstance().createLiteral(latitude));
            try (TupleQueryResult tqRes = tq.evaluate()) {
                while (tqRes.hasNext()) {
                    System.out.println(tqRes.next());
                }
            }
        }
        repo.shutDown();
    }

    @Test
    public void testTime() throws Exception {

        int timestampSeconds = 1558044000;
        long timestampMs = timestampSeconds * 1000L;
        Date date = new Date(timestampMs);
        System.out.println(date);
    }
}
