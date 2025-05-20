import static spark.Spark.*;
import com.google.gson.Gson;
import java.util.*;
import java.io.*;

class City {
    String name;
    Map<City, Integer> neighbors;

    public City(String name) {
        this.name = name;
        this.neighbors = new HashMap<>();
    }

    public void addNeighbor(City city, int weight) {
        neighbors.put(city, weight);
    }
}

class PathResult {
    List<String> cities;
    int totalDistance;
    double estimatedTime;
    String googleMapsUrl;

    public PathResult(List<String> cities, int totalDistance, double estimatedTime) {
        this.cities = cities;
        this.totalDistance = totalDistance;
        this.estimatedTime = estimatedTime;
        this.googleMapsUrl = generateMapsUrl(cities);
    }

    private String generateMapsUrl(List<String> cities) {
        if (cities.size() < 2) return "";
        StringBuilder url = new StringBuilder("https://www.google.com/maps/dir/");
        for (String city : cities) {
            url.append(city.replace(" ", "+")).append("/");
        }
        return url.toString();
    }
}

public class TravelPlanner {
    Map<String, City> cities = new HashMap<>();

    public void addCity(String name) {
        cities.putIfAbsent(name.toLowerCase(), new City(name));
    }

    public void addRoute(String from, String to, int weight) {
        City fromCity = cities.get(from.toLowerCase());
        City toCity = cities.get(to.toLowerCase());
        if (fromCity != null && toCity != null) {
            fromCity.addNeighbor(toCity, weight);
            toCity.addNeighbor(fromCity, weight); // Undirected graph
        }
    }

    public PathResult dijkstra(String start, String end) {
        City startCity = cities.get(start.toLowerCase());
        City endCity = cities.get(end.toLowerCase());

        if (startCity == null || endCity == null) return null;

        Map<City, Integer> distance = new HashMap<>();
        Map<City, City> prev = new HashMap<>();
        PriorityQueue<City> pq = new PriorityQueue<>(Comparator.comparingInt(distance::get));

        for (City city : cities.values()) distance.put(city, Integer.MAX_VALUE);
        distance.put(startCity, 0);
        pq.add(startCity);

        while (!pq.isEmpty()) {
            City current = pq.poll();
            if (current == endCity) break;

            for (Map.Entry<City, Integer> entry : current.neighbors.entrySet()) {
                City neighbor = entry.getKey();
                int newDist = distance.get(current) + entry.getValue();
                if (newDist < distance.get(neighbor)) {
                    distance.put(neighbor, newDist);
                    prev.put(neighbor, current);
                    pq.add(neighbor);
                }
            }
        }

        List<String> path = new LinkedList<>();
        for (City at = endCity; at != null; at = prev.get(at)) path.add(0, at.name);

        int totalDistance = distance.get(endCity);
        double estimatedTime = totalDistance / 50.0; // Assume 50 km/h average speed

        return new PathResult(path, totalDistance, estimatedTime);
    }

    public void loadCitiesFromFile(String path) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split(",");
            if (parts.length == 3) {
                String from = parts[0].trim();
                String to = parts[1].trim();
                int weight = Integer.parseInt(parts[2].trim());
                addCity(from);
                addCity(to);
                addRoute(from, to, weight);
            }
        }
        br.close();
    }

    public static void main(String[] args) throws IOException {
        TravelPlanner planner = new TravelPlanner();
        planner.loadCitiesFromFile("cities.csv"); // Make sure cities.csv is in your project folder
        Gson gson = new Gson();

        port(4567);

        // Enable CORS (allow frontend requests from any origin)
        options("/*", (request, response) -> {
            String headers = request.headers("Access-Control-Request-Headers");
            if (headers != null) response.header("Access-Control-Allow-Headers", headers);
            String method = request.headers("Access-Control-Request-Method");
            if (method != null) response.header("Access-Control-Allow-Methods", method);
            return "OK";
        });

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
        });

        get("/shortest-path", (req, res) -> {
            String sourceRaw = req.queryParams("source");
            String destinationRaw = req.queryParams("destination");

            if (sourceRaw == null || destinationRaw == null) {
                res.status(400);
                return gson.toJson("Source or destination not provided.");
            }

            String source = sourceRaw.trim().toLowerCase();
            String destination = destinationRaw.trim().toLowerCase();

            if (!planner.cities.containsKey(source) || !planner.cities.containsKey(destination)) {
                res.status(400);
                return gson.toJson("Invalid source or destination city.");
            }

            PathResult result = planner.dijkstra(source, destination);
            if (result == null || result.cities.isEmpty()) {
                res.status(404);
                return gson.toJson("No path found.");
            }

            return gson.toJson(result);
        });
    }
}
