package com.algorithmviz.service;

import com.algorithmviz.dto.GraphRequest;
import com.algorithmviz.model.GraphStep;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GraphService {

    private static final double INF = Double.MAX_VALUE / 2;

    public List<GraphStep> generateSteps(String algorithm, GraphRequest.GraphData graph,
                                         String startId, String endId) {
        return switch (algorithm) {
            case "dijkstra" -> dijkstra(graph, startId, endId);
            case "bfs"      -> bfs(graph, startId, endId);
            case "dfs"      -> dfs(graph, startId, endId);
            case "prim"     -> prim(graph, startId);
            case "kruskal"  -> kruskal(graph);
            case "astar"    -> aStar(graph, startId, endId);
            default -> throw new IllegalArgumentException("Unknown graph algorithm: " + algorithm);
        };
    }

    private String edgeKey(String from, String to) {
        return from + "-" + to;
    }

    private Map<String, String> initNodeStates(GraphRequest.GraphData g, String startId, String endId) {
        Map<String, String> ns = new LinkedHashMap<>();
        g.getNodes().forEach(n -> ns.put(n.getId(), "unvisited"));
        if (startId != null) ns.put(startId, "start");
        if (endId != null && !endId.isEmpty()) ns.put(endId, "end");
        return ns;
    }

    private Map<String, String> initEdgeStates(GraphRequest.GraphData g) {
        Map<String, String> es = new LinkedHashMap<>();
        g.getEdges().forEach(e -> {
            es.put(edgeKey(e.getFrom(), e.getTo()), "default");
            if (!g.isDirected()) es.put(edgeKey(e.getTo(), e.getFrom()), "default");
        });
        return es;
    }

    private Map<String, List<GraphRequest.GraphEdgeDto>> buildAdj(GraphRequest.GraphData g) {
        Map<String, List<GraphRequest.GraphEdgeDto>> adj = new HashMap<>();
        g.getNodes().forEach(n -> adj.put(n.getId(), new ArrayList<>()));
        g.getEdges().forEach(e -> {
            adj.get(e.getFrom()).add(e);
            if (!g.isDirected()) {
                GraphRequest.GraphEdgeDto rev = new GraphRequest.GraphEdgeDto();
                rev.setFrom(e.getTo());
                rev.setTo(e.getFrom());
                rev.setWeight(e.getWeight());
                adj.get(e.getTo()).add(rev);
            }
        });
        return adj;
    }

    // ==================== DIJKSTRA ====================
    private List<GraphStep> dijkstra(GraphRequest.GraphData g, String startId, String endId) {
        List<GraphStep> steps = new ArrayList<>();
        List<String> nodes = g.getNodes().stream().map(GraphRequest.GraphNodeDto::getId).collect(Collectors.toList());
        Map<String, List<GraphRequest.GraphEdgeDto>> adj = buildAdj(g);

        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        Set<String> visited = new HashSet<>();
        int[] comparisons = {0};

        nodes.forEach(n -> { dist.put(n, INF); prev.put(n, null); });
        dist.put(startId, 0.0);

        Map<String, String> nodeStates = initNodeStates(g, startId, endId);
        Map<String, String> edgeStates = initEdgeStates(g);

        steps.add(GraphStep.builder()
                .nodeStates(new LinkedHashMap<>(nodeStates))
                .edgeStates(new LinkedHashMap<>(edgeStates))
                .distances(new HashMap<>(dist))
                .queue(List.of(startId)).stack(List.of())
                .current(null).path(List.of())
                .description("初始化：dist[" + startId + "]=0，其余节点距离为 ∞")
                .codeLine(1).visitedCount(0).pathLength(0).comparisons(0).phase("init").build());

        while (true) {
            String u = null;
            double minD = INF;
            for (String n : nodes) {
                if (!visited.contains(n) && dist.get(n) < minD) {
                    minD = dist.get(n); u = n;
                }
            }
            if (u == null) break;

            visited.add(u);
            comparisons[0]++;
            if (!u.equals(startId) && !u.equals(endId)) nodeStates.put(u, "current");

            final String current = u;
            List<String> queue = nodes.stream().filter(n -> !visited.contains(n) && dist.get(n) < INF)
                    .collect(Collectors.toList());

            steps.add(GraphStep.builder()
                    .nodeStates(new LinkedHashMap<>(nodeStates))
                    .edgeStates(new LinkedHashMap<>(edgeStates))
                    .distances(new HashMap<>(dist))
                    .queue(queue).stack(List.of())
                    .current(current).path(List.of())
                    .description("访问距离最小的未访问节点 " + current + "（距离=" + minD + "）")
                    .codeLine(2).visitedCount(visited.size()).pathLength(0).comparisons(comparisons[0]).phase("select_min").build());

            if (current.equals(endId)) break;

            for (GraphRequest.GraphEdgeDto edge : adj.getOrDefault(current, List.of())) {
                String v = edge.getTo();
                double alt = dist.get(current) + edge.getWeight();
                comparisons[0]++;

                edgeStates.put(edgeKey(current, v), "exploring");
                if (!g.isDirected()) edgeStates.put(edgeKey(v, current), "exploring");

                steps.add(GraphStep.builder()
                        .nodeStates(new LinkedHashMap<>(nodeStates))
                        .edgeStates(new LinkedHashMap<>(edgeStates))
                        .distances(new HashMap<>(dist))
                        .queue(queue).stack(List.of())
                        .current(current).path(List.of())
                        .description("探索边 " + current + "→" + v + "（权重=" + edge.getWeight()
                                + "），alt=" + alt + " vs dist[" + v + "]=" + dist.get(v))
                        .codeLine(3).visitedCount(visited.size()).pathLength(0).comparisons(comparisons[0]).phase("explore_edge").build());

                if (alt < dist.get(v)) {
                    dist.put(v, alt);
                    prev.put(v, current);
                    edgeStates.put(edgeKey(current, v), "tree");
                    if (!g.isDirected()) edgeStates.put(edgeKey(v, current), "tree");
                    if (!v.equals(startId) && !v.equals(endId) && !visited.contains(v)) {
                        nodeStates.put(v, "in-queue");
                    }
                    steps.add(GraphStep.builder()
                            .nodeStates(new LinkedHashMap<>(nodeStates))
                            .edgeStates(new LinkedHashMap<>(edgeStates))
                            .distances(new HashMap<>(dist))
                            .queue(nodes.stream().filter(n -> !visited.contains(n) && dist.get(n) < INF).collect(Collectors.toList()))
                            .stack(List.of()).current(current).path(List.of())
                            .description("更新 dist[" + v + "] = " + alt)
                            .codeLine(4).visitedCount(visited.size()).pathLength(0).comparisons(comparisons[0]).phase("update_dist").build());
                } else {
                    edgeStates.put(edgeKey(current, v), "default");
                    if (!g.isDirected()) edgeStates.put(edgeKey(v, current), "default");
                }
            }
            if (!current.equals(startId) && !current.equals(endId)) nodeStates.put(current, "visited");
        }

        // Reconstruct path
        List<String> path = new ArrayList<>();
        if (endId != null && !endId.isEmpty() && dist.get(endId) < INF) {
            String cur = endId;
            while (cur != null) { path.add(0, cur); cur = prev.get(cur); }
            for (int i = 0; i < path.size() - 1; i++) {
                edgeStates.put(edgeKey(path.get(i), path.get(i + 1)), "path");
                if (!g.isDirected()) edgeStates.put(edgeKey(path.get(i + 1), path.get(i)), "path");
            }
            path.forEach(n -> { if (!n.equals(startId) && !n.equals(endId)) nodeStates.put(n, "path"); });
        }

        double finalDist = (endId != null && !endId.isEmpty()) ? dist.getOrDefault(endId, -1.0) : -1;
        steps.add(GraphStep.builder()
                .nodeStates(new LinkedHashMap<>(nodeStates))
                .edgeStates(new LinkedHashMap<>(edgeStates))
                .distances(new HashMap<>(dist))
                .queue(List.of()).stack(List.of()).current(null).path(path)
                .description(path.isEmpty()
                        ? "✓ Dijkstra 完成，无法到达终点"
                        : "✓ 最短路径：" + String.join(" → ", path) + "（距离=" + finalDist + "）")
                .codeLine(5).visitedCount(visited.size()).pathLength(finalDist < 0 ? 0 : finalDist)
                .comparisons(comparisons[0]).phase("reconstruct_path").build());

        return steps;
    }

    // ==================== BFS ====================
    private List<GraphStep> bfs(GraphRequest.GraphData g, String startId, String endId) {
        List<GraphStep> steps = new ArrayList<>();
        Map<String, List<GraphRequest.GraphEdgeDto>> adj = buildAdj(g);
        Map<String, String> nodeStates = initNodeStates(g, startId, endId);
        Map<String, String> edgeStates = initEdgeStates(g);
        Map<String, String> prev = new HashMap<>();
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        int[] comparisons = {0};

        queue.add(startId);
        visited.add(startId);
        nodeStates.put(startId, "start");

        steps.add(GraphStep.builder()
                .nodeStates(new LinkedHashMap<>(nodeStates)).edgeStates(new LinkedHashMap<>(edgeStates))
                .distances(Map.of()).queue(new ArrayList<>(queue)).stack(List.of())
                .current(null).path(List.of())
                .description("BFS 初始化，将起点 " + startId + " 加入队列")
                .codeLine(1).visitedCount(0).pathLength(0).comparisons(0).phase("init").build());

        while (!queue.isEmpty()) {
            String u = queue.poll();
            comparisons[0]++;
            if (!u.equals(startId) && !u.equals(endId)) nodeStates.put(u, "visiting");

            steps.add(GraphStep.builder()
                    .nodeStates(new LinkedHashMap<>(nodeStates)).edgeStates(new LinkedHashMap<>(edgeStates))
                    .distances(Map.of()).queue(new ArrayList<>(queue)).stack(List.of())
                    .current(u).path(List.of())
                    .description("出队节点 " + u + "，开始探索其邻居")
                    .codeLine(2).visitedCount(visited.size()).pathLength(0).comparisons(comparisons[0]).phase("dequeue").build());

            if (u.equals(endId)) break;

            for (GraphRequest.GraphEdgeDto edge : adj.getOrDefault(u, List.of())) {
                String v = edge.getTo();
                comparisons[0]++;
                edgeStates.put(edgeKey(u, v), "exploring");
                if (!g.isDirected()) edgeStates.put(edgeKey(v, u), "exploring");

                if (!visited.contains(v)) {
                    visited.add(v);
                    prev.put(v, u);
                    queue.add(v);
                    edgeStates.put(edgeKey(u, v), "tree");
                    if (!g.isDirected()) edgeStates.put(edgeKey(v, u), "tree");
                    if (!v.equals(endId)) nodeStates.put(v, "in-queue");

                    steps.add(GraphStep.builder()
                            .nodeStates(new LinkedHashMap<>(nodeStates)).edgeStates(new LinkedHashMap<>(edgeStates))
                            .distances(Map.of()).queue(new ArrayList<>(queue)).stack(List.of())
                            .current(u).path(List.of())
                            .description("发现新节点 " + v + "，加入队列")
                            .codeLine(3).visitedCount(visited.size()).pathLength(0).comparisons(comparisons[0]).phase("discover_neighbor").build());
                } else {
                    edgeStates.put(edgeKey(u, v), "default");
                    if (!g.isDirected()) edgeStates.put(edgeKey(v, u), "default");
                }
            }
            if (!u.equals(startId) && !u.equals(endId)) nodeStates.put(u, "visited");
        }

        List<String> path = reconstructPath(prev, startId, endId, nodeStates, edgeStates, g.isDirected());
        steps.add(GraphStep.builder()
                .nodeStates(new LinkedHashMap<>(nodeStates)).edgeStates(new LinkedHashMap<>(edgeStates))
                .distances(Map.of()).queue(List.of()).stack(List.of()).current(null).path(path)
                .description(path.isEmpty()
                        ? "✓ BFS 完成，无法到达终点"
                        : "✓ BFS 完成，路径：" + String.join(" → ", path))
                .codeLine(4).visitedCount(visited.size()).pathLength(path.size() - 1.0).comparisons(comparisons[0]).phase("reconstruct_path").build());
        return steps;
    }

    // ==================== DFS ====================
    private List<GraphStep> dfs(GraphRequest.GraphData g, String startId, String endId) {
        List<GraphStep> steps = new ArrayList<>();
        Map<String, List<GraphRequest.GraphEdgeDto>> adj = buildAdj(g);
        Map<String, String> nodeStates = initNodeStates(g, startId, endId);
        Map<String, String> edgeStates = initEdgeStates(g);
        Map<String, String> prev = new HashMap<>();
        Set<String> visited = new HashSet<>();
        Deque<String> stack = new ArrayDeque<>();
        int[] comparisons = {0};

        stack.push(startId);

        steps.add(GraphStep.builder()
                .nodeStates(new LinkedHashMap<>(nodeStates)).edgeStates(new LinkedHashMap<>(edgeStates))
                .distances(Map.of()).queue(List.of()).stack(new ArrayList<>(stack))
                .current(null).path(List.of())
                .description("DFS 初始化，将起点 " + startId + " 压入栈")
                .codeLine(1).visitedCount(0).pathLength(0).comparisons(0).phase("init").build());

        while (!stack.isEmpty()) {
            String u = stack.pop();
            if (visited.contains(u)) continue;
            visited.add(u);
            comparisons[0]++;
            if (!u.equals(startId) && !u.equals(endId)) nodeStates.put(u, "visiting");

            steps.add(GraphStep.builder()
                    .nodeStates(new LinkedHashMap<>(nodeStates)).edgeStates(new LinkedHashMap<>(edgeStates))
                    .distances(Map.of()).queue(List.of()).stack(new ArrayList<>(stack))
                    .current(u).path(List.of())
                    .description("出栈节点 " + u + "，标记为已访问")
                    .codeLine(2).visitedCount(visited.size()).pathLength(0).comparisons(comparisons[0]).phase("pop_stack").build());

            if (u.equals(endId)) break;

            List<GraphRequest.GraphEdgeDto> neighbors = new ArrayList<>(adj.getOrDefault(u, List.of()));
            Collections.reverse(neighbors);
            for (GraphRequest.GraphEdgeDto edge : neighbors) {
                String v = edge.getTo();
                comparisons[0]++;
                if (!visited.contains(v)) {
                    prev.put(v, u);
                    stack.push(v);
                    edgeStates.put(edgeKey(u, v), "tree");
                    if (!g.isDirected()) edgeStates.put(edgeKey(v, u), "tree");
                    if (!v.equals(endId)) nodeStates.put(v, "in-queue");

                    steps.add(GraphStep.builder()
                            .nodeStates(new LinkedHashMap<>(nodeStates)).edgeStates(new LinkedHashMap<>(edgeStates))
                            .distances(Map.of()).queue(List.of()).stack(new ArrayList<>(stack))
                            .current(u).path(List.of())
                            .description("发现邻居 " + v + "，压入栈")
                            .codeLine(3).visitedCount(visited.size()).pathLength(0).comparisons(comparisons[0]).phase("discover_neighbor").build());
                }
            }
            if (!u.equals(startId) && !u.equals(endId)) nodeStates.put(u, "visited");
        }

        List<String> path = reconstructPath(prev, startId, endId, nodeStates, edgeStates, g.isDirected());
        steps.add(GraphStep.builder()
                .nodeStates(new LinkedHashMap<>(nodeStates)).edgeStates(new LinkedHashMap<>(edgeStates))
                .distances(Map.of()).queue(List.of()).stack(List.of()).current(null).path(path)
                .description(path.isEmpty()
                        ? "✓ DFS 完成，无法到达终点"
                        : "✓ DFS 完成，路径：" + String.join(" → ", path))
                .codeLine(4).visitedCount(visited.size()).pathLength(path.size() - 1.0).comparisons(comparisons[0]).phase("reconstruct_path").build());
        return steps;
    }

    // ==================== PRIM ====================
    private List<GraphStep> prim(GraphRequest.GraphData g, String startId) {
        List<GraphStep> steps = new ArrayList<>();
        Map<String, List<GraphRequest.GraphEdgeDto>> adj = buildAdj(g);
        Map<String, String> nodeStates = initNodeStates(g, startId, null);
        Map<String, String> edgeStates = initEdgeStates(g);
        Set<String> inMST = new HashSet<>();
        int[] comparisons = {0};
        double[] mstCost = {0};

        inMST.add(startId);
        nodeStates.put(startId, "start");

        steps.add(GraphStep.builder()
                .nodeStates(new LinkedHashMap<>(nodeStates)).edgeStates(new LinkedHashMap<>(edgeStates))
                .distances(Map.of()).queue(List.of()).stack(List.of()).current(startId).path(List.of())
                .description("Prim 初始化，从节点 " + startId + " 开始构建 MST")
                .codeLine(1).visitedCount(1).pathLength(0).comparisons(0).mstCost(0.0).phase("init").build());

        int nodeCount = g.getNodes().size();
        while (inMST.size() < nodeCount) {
            double minW = INF;
            String bestU = null, bestV = null;
            for (String u : inMST) {
                for (GraphRequest.GraphEdgeDto e : adj.getOrDefault(u, List.of())) {
                    comparisons[0]++;
                    if (!inMST.contains(e.getTo()) && e.getWeight() < minW) {
                        minW = e.getWeight(); bestU = u; bestV = e.getTo();
                    }
                }
            }
            if (bestV == null) break;

            inMST.add(bestV);
            mstCost[0] += minW;
            edgeStates.put(edgeKey(bestU, bestV), "mst");
            edgeStates.put(edgeKey(bestV, bestU), "mst");
            nodeStates.put(bestV, "mst");

            steps.add(GraphStep.builder()
                    .nodeStates(new LinkedHashMap<>(nodeStates)).edgeStates(new LinkedHashMap<>(edgeStates))
                    .distances(Map.of()).queue(List.of()).stack(List.of()).current(bestV).path(List.of())
                    .description("添加最小权边 " + bestU + "-" + bestV + "（权重=" + minW + "），MST 总权重=" + mstCost[0])
                    .codeLine(2).visitedCount(inMST.size()).pathLength(mstCost[0]).comparisons(comparisons[0]).mstCost(mstCost[0]).phase("add_to_mst").build());
        }

        steps.add(GraphStep.builder()
                .nodeStates(new LinkedHashMap<>(nodeStates)).edgeStates(new LinkedHashMap<>(edgeStates))
                .distances(Map.of()).queue(List.of()).stack(List.of()).current(null).path(List.of())
                .description("✓ Prim's MST 完成！最小生成树总权重 = " + mstCost[0])
                .codeLine(3).visitedCount(inMST.size()).pathLength(mstCost[0]).comparisons(comparisons[0]).mstCost(mstCost[0]).phase("done").build());
        return steps;
    }

    // ==================== KRUSKAL ====================
    private List<GraphStep> kruskal(GraphRequest.GraphData g) {
        List<GraphStep> steps = new ArrayList<>();
        Map<String, String> nodeStates = new LinkedHashMap<>();
        g.getNodes().forEach(n -> nodeStates.put(n.getId(), "unvisited"));
        Map<String, String> edgeStates = initEdgeStates(g);

        List<GraphRequest.GraphEdgeDto> edges = new ArrayList<>(g.getEdges());
        edges.sort(Comparator.comparingDouble(GraphRequest.GraphEdgeDto::getWeight));

        Map<String, String> parent = new HashMap<>();
        g.getNodes().forEach(n -> parent.put(n.getId(), n.getId()));

        int[] comparisons = {0};
        double[] mstCost = {0};

        steps.add(GraphStep.builder()
                .nodeStates(new LinkedHashMap<>(nodeStates)).edgeStates(new LinkedHashMap<>(edgeStates))
                .distances(Map.of()).queue(List.of()).stack(List.of()).current(null).path(List.of())
                .description("Kruskal 初始化：将 " + edges.size() + " 条边按权重升序排列")
                .codeLine(1).visitedCount(0).pathLength(0).comparisons(0).mstCost(0.0).phase("init").build());

        for (GraphRequest.GraphEdgeDto e : edges) {
            comparisons[0]++;
            String pu = find(parent, e.getFrom());
            String pv = find(parent, e.getTo());

            edgeStates.put(edgeKey(e.getFrom(), e.getTo()), "exploring");
            edgeStates.put(edgeKey(e.getTo(), e.getFrom()), "exploring");

            steps.add(GraphStep.builder()
                    .nodeStates(new LinkedHashMap<>(nodeStates)).edgeStates(new LinkedHashMap<>(edgeStates))
                    .distances(Map.of()).queue(List.of()).stack(List.of()).current(null).path(List.of())
                    .description("检查边 " + e.getFrom() + "-" + e.getTo() + "（权重=" + e.getWeight() + "）：是否形成环？")
                    .codeLine(2).visitedCount(0).pathLength(mstCost[0]).comparisons(comparisons[0]).mstCost(mstCost[0]).phase("check_cycle").build());

            if (!pu.equals(pv)) {
                union(parent, pu, pv);
                mstCost[0] += e.getWeight();
                edgeStates.put(edgeKey(e.getFrom(), e.getTo()), "mst");
                edgeStates.put(edgeKey(e.getTo(), e.getFrom()), "mst");
                nodeStates.put(e.getFrom(), "mst");
                nodeStates.put(e.getTo(), "mst");

                steps.add(GraphStep.builder()
                        .nodeStates(new LinkedHashMap<>(nodeStates)).edgeStates(new LinkedHashMap<>(edgeStates))
                        .distances(Map.of()).queue(List.of()).stack(List.of()).current(null).path(List.of())
                        .description("✓ 加入 MST：边 " + e.getFrom() + "-" + e.getTo() + "，MST 总权重=" + mstCost[0])
                        .codeLine(3).visitedCount(0).pathLength(mstCost[0]).comparisons(comparisons[0]).mstCost(mstCost[0]).phase("add_to_mst").build());
            } else {
                edgeStates.put(edgeKey(e.getFrom(), e.getTo()), "default");
                edgeStates.put(edgeKey(e.getTo(), e.getFrom()), "default");
                steps.add(GraphStep.builder()
                        .nodeStates(new LinkedHashMap<>(nodeStates)).edgeStates(new LinkedHashMap<>(edgeStates))
                        .distances(Map.of()).queue(List.of()).stack(List.of()).current(null).path(List.of())
                        .description("✗ 跳过：边 " + e.getFrom() + "-" + e.getTo() + " 会形成环")
                        .codeLine(4).visitedCount(0).pathLength(mstCost[0]).comparisons(comparisons[0]).mstCost(mstCost[0]).phase("skip_edge").build());
            }
        }

        steps.add(GraphStep.builder()
                .nodeStates(new LinkedHashMap<>(nodeStates)).edgeStates(new LinkedHashMap<>(edgeStates))
                .distances(Map.of()).queue(List.of()).stack(List.of()).current(null).path(List.of())
                .description("✓ Kruskal's MST 完成！最小生成树总权重 = " + mstCost[0])
                .codeLine(5).visitedCount(0).pathLength(mstCost[0]).comparisons(comparisons[0]).mstCost(mstCost[0]).phase("done").build());
        return steps;
    }

    private String find(Map<String, String> parent, String x) {
        if (!parent.get(x).equals(x)) parent.put(x, find(parent, parent.get(x)));
        return parent.get(x);
    }

    private void union(Map<String, String> parent, String x, String y) {
        parent.put(find(parent, x), find(parent, y));
    }

    // ==================== A* ====================
    private List<GraphStep> aStar(GraphRequest.GraphData g, String startId, String endId) {
        List<GraphStep> steps = new ArrayList<>();
        Map<String, List<GraphRequest.GraphEdgeDto>> adj = buildAdj(g);
        Map<String, String> nodeStates = initNodeStates(g, startId, endId);
        Map<String, String> edgeStates = initEdgeStates(g);
        Map<String, Double> gCost = new HashMap<>();
        Map<String, Double> fCost = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        Set<String> closed = new HashSet<>();
        int[] comparisons = {0};

        Map<String, double[]> positions = new HashMap<>();
        g.getNodes().forEach(n -> positions.put(n.getId(), new double[]{n.getX(), n.getY()}));

        g.getNodes().forEach(n -> { gCost.put(n.getId(), INF); fCost.put(n.getId(), INF); });
        gCost.put(startId, 0.0);
        fCost.put(startId, heuristic(positions, startId, endId));

        PriorityQueue<String> open = new PriorityQueue<>(Comparator.comparingDouble(fCost::get));
        open.add(startId);

        steps.add(GraphStep.builder()
                .nodeStates(new LinkedHashMap<>(nodeStates)).edgeStates(new LinkedHashMap<>(edgeStates))
                .distances(new HashMap<>(gCost)).queue(new ArrayList<>(open)).stack(List.of())
                .current(null).path(List.of())
                .description("A* 初始化，g[" + startId + "]=0，h=" + String.format("%.1f", fCost.get(startId)))
                .codeLine(1).visitedCount(0).pathLength(0).comparisons(0).phase("init").build());

        while (!open.isEmpty()) {
            String u = open.poll();
            comparisons[0]++;
            if (!u.equals(startId) && !u.equals(endId)) nodeStates.put(u, "current");
            List<String> openList = new ArrayList<>(open);

            steps.add(GraphStep.builder()
                    .nodeStates(new LinkedHashMap<>(nodeStates)).edgeStates(new LinkedHashMap<>(edgeStates))
                    .distances(new HashMap<>(gCost)).queue(openList).stack(List.of())
                    .current(u).path(List.of())
                    .description("从 Open 列表中取出 f 值最小节点 " + u + "（g=" + gCost.get(u) + ", f=" + String.format("%.1f", fCost.get(u)) + "）")
                    .codeLine(2).visitedCount(closed.size()).pathLength(0).comparisons(comparisons[0]).phase("select_min").build());

            if (u.equals(endId)) break;
            closed.add(u);

            for (GraphRequest.GraphEdgeDto edge : adj.getOrDefault(u, List.of())) {
                String v = edge.getTo();
                if (closed.contains(v)) continue;
                comparisons[0]++;
                double tentativeG = gCost.get(u) + edge.getWeight();

                edgeStates.put(edgeKey(u, v), "exploring");
                if (!g.isDirected()) edgeStates.put(edgeKey(v, u), "exploring");

                if (tentativeG < gCost.get(v)) {
                    gCost.put(v, tentativeG);
                    double h = heuristic(positions, v, endId);
                    fCost.put(v, tentativeG + h);
                    prev.put(v, u);
                    if (!open.contains(v)) {
                        open.add(v);
                        if (!v.equals(endId)) nodeStates.put(v, "in-queue");
                    }
                    edgeStates.put(edgeKey(u, v), "tree");
                    if (!g.isDirected()) edgeStates.put(edgeKey(v, u), "tree");

                    steps.add(GraphStep.builder()
                            .nodeStates(new LinkedHashMap<>(nodeStates)).edgeStates(new LinkedHashMap<>(edgeStates))
                            .distances(new HashMap<>(gCost)).queue(new ArrayList<>(open)).stack(List.of())
                            .current(u).path(List.of())
                            .description("更新 " + v + "：g=" + tentativeG + ", h=" + String.format("%.1f", h) + ", f=" + String.format("%.1f", fCost.get(v)))
                            .codeLine(3).visitedCount(closed.size()).pathLength(0).comparisons(comparisons[0]).phase("update_dist").build());
                } else {
                    edgeStates.put(edgeKey(u, v), "default");
                    if (!g.isDirected()) edgeStates.put(edgeKey(v, u), "default");
                }
            }
            if (!u.equals(startId) && !u.equals(endId)) nodeStates.put(u, "visited");
        }

        List<String> path = reconstructPath(prev, startId, endId, nodeStates, edgeStates, g.isDirected());
        double pathLen = path.isEmpty() ? 0 : gCost.getOrDefault(endId, 0.0);
        steps.add(GraphStep.builder()
                .nodeStates(new LinkedHashMap<>(nodeStates)).edgeStates(new LinkedHashMap<>(edgeStates))
                .distances(new HashMap<>(gCost)).queue(List.of()).stack(List.of()).current(null).path(path)
                .description(path.isEmpty()
                        ? "✓ A* 完成，无法到达终点"
                        : "✓ A* 完成，最优路径：" + String.join(" → ", path) + "（代价=" + pathLen + "）")
                .codeLine(4).visitedCount(closed.size()).pathLength(pathLen).comparisons(comparisons[0]).phase("reconstruct_path").build());
        return steps;
    }

    private double heuristic(Map<String, double[]> positions, String a, String b) {
        if (b == null || b.isEmpty() || !positions.containsKey(a) || !positions.containsKey(b)) return 0;
        double[] pa = positions.get(a), pb = positions.get(b);
        return Math.sqrt(Math.pow(pa[0] - pb[0], 2) + Math.pow(pa[1] - pb[1], 2));
    }

    private List<String> reconstructPath(Map<String, String> prev, String startId, String endId,
                                          Map<String, String> nodeStates, Map<String, String> edgeStates,
                                          boolean directed) {
        List<String> path = new ArrayList<>();
        if (endId == null || endId.isEmpty() || !prev.containsKey(endId)) return path;
        String cur = endId;
        while (cur != null) { path.add(0, cur); cur = prev.get(cur); }
        if (!path.isEmpty() && path.get(0).equals(startId)) {
            for (int i = 0; i < path.size() - 1; i++) {
                edgeStates.put(edgeKey(path.get(i), path.get(i + 1)), "path");
                if (!directed) edgeStates.put(edgeKey(path.get(i + 1), path.get(i)), "path");
            }
            path.forEach(n -> { if (!n.equals(startId) && !n.equals(endId)) nodeStates.put(n, "path"); });
        }
        return path;
    }
}
