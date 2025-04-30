import java.util.*;

public class DijkstraAlgorithm {
    static class Node implements Comparable<Node> {
        String vertex;
        int weight;

        public Node(String vertex, int weight) {
            this.vertex = vertex;
            this.weight = weight;
        }

        @Override
        public int compareTo(Node other) {
            return Integer.compare(this.weight, other.weight);
        }
    }

    public static int dijkstra(Map<String, List<Node>> graph, String startingVertex, String EndingVertex) {
        String selectedVertex = "";
        String prevSelectedVertex = startingVertex;
        int smallestWeight;
        List<Node> edges = graph.get(startingVertex);
        Map<String, Integer> minNodes = new HashMap<>();

        for(Node edge : edges){
            minNodes.put(edge.vertex, Integer.MAX_VALUE);
        }

        while (!graph.isEmpty()) {
            smallestWeight = Integer.MAX_VALUE;
            
            for(Node edge : edges){
                if(edge.weight < smallestWeight){
                    smallestWeight = edge.weight;
                    selectedVertex = edge.vertex;
                }
            }

            if(smallestWeight < minNodes.getOrDefault(selectedVertex, 0)){
                minNodes.put(selectedVertex, smallestWeight);
            }

            edges = graph.get(selectedVertex);

            graph.remove(prevSelectedVertex);
            prevSelectedVertex = selectedVertex;
        }
        
        return minNodes.get(selectedVertex);
    }

    public static void main(String[] args) {
        Map<String, List<Node>> graph = new HashMap<>();
        String[] vertices = {"A", "B", "C", "D", "E"};
        for (String vertex : vertices) {
            graph.put(vertex, new ArrayList<>());
        }
        graph.get("A").add(new Node("B", 10));
        graph.get("A").add(new Node("D", 5));
        graph.get("B").add(new Node("C", 1));
        graph.get("B").add(new Node("D", 2));
        graph.get("C").add(new Node("E", 4));
        graph.get("D").add(new Node("B", 3));
        graph.get("D").add(new Node("C", 9));
        graph.get("D").add(new Node("E", 2));
        graph.get("E").add(new Node("A", 7));
        graph.get("E").add(new Node("C", 6));

        int shortestDistance = dijkstra(graph, "A", "E");
        System.out.println("Shortest distance: " + shortestDistance);
    }
}
