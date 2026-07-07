package com.algorithmviz.service;

import com.algorithmviz.dto.CompetitionSubmitRequest;
import com.algorithmviz.dto.CompetitionSubmitResponse;
import com.algorithmviz.dto.CreateCompetitionRoomRequest;
import com.algorithmviz.dto.JoinCompetitionRoomRequest;
import com.algorithmviz.model.CompetitionPlayer;
import com.algorithmviz.model.CompetitionQuestion;
import com.algorithmviz.model.CompetitionRoom;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CompetitionRoomService {
    private static final Set<String> SUPPORTED_ALGORITHMS = Set.of(
            "quick-sort", "merge-sort", "bubble-sort", "heap-sort", "insertion-sort",
            "binary-search", "dijkstra", "bfs", "dfs", "prim", "kruskal", "astar",
            "knapsack", "n-queens", "karatsuba"
    );

    private final Map<String, CompetitionRoom> rooms = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> answerKeys = new ConcurrentHashMap<>();
    private final Map<String, Map<Long, List<String>>> submittedQuestions = new ConcurrentHashMap<>();
    private final Map<String, Map<Long, Map<String, CompetitionSubmitResponse>>> submissionResults =
            new ConcurrentHashMap<>();
    private final Map<String, Map<Long, Instant>> questionStartedAt = new ConcurrentHashMap<>();

    public CompetitionRoom createRoom(CreateCompetitionRoomRequest request) {
        if (request.getAlgorithm() == null || request.getAlgorithm().isBlank()) {
            throw new IllegalArgumentException("请选择竞赛算法");
        }
        if (!SUPPORTED_ALGORITHMS.contains(request.getAlgorithm())) {
            throw new IllegalArgumentException("不支持该竞赛算法");
        }
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("用户信息无效，请重新登录");
        }

        CompetitionRoom room = new CompetitionRoom();
        room.setRoomId(newRoomId());
        room.setAlgorithm(request.getAlgorithm());
        room.setStatus("waiting");
        Instant now = Instant.now();
        room.setCreatedAt(now);
        room.setLastActivityAt(now);
        room.setQuestions(createQuestions(request.getAlgorithm()));
        room.setQuestionCount(room.getQuestions().size());
        room.getPlayers().add(new CompetitionPlayer(request.getUserId(), displayName(request.getDisplayName())));

        rooms.put(room.getRoomId(), room);
        answerKeys.put(room.getRoomId(), createAnswerKey(request.getAlgorithm(), room.getQuestions()));
        submittedQuestions.put(room.getRoomId(), new ConcurrentHashMap<>());
        submissionResults.put(room.getRoomId(), new ConcurrentHashMap<>());
        questionStartedAt.put(room.getRoomId(), new ConcurrentHashMap<>());
        return room;
    }

    public synchronized CompetitionRoom joinRoom(String roomId, JoinCompetitionRoomRequest request) {
        CompetitionRoom room = getRoomOrThrow(normalizeRoomId(roomId));
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("用户信息无效，请重新登录");
        }

        boolean alreadyJoined = room.getPlayers().stream()
                .anyMatch(player -> player.getUserId().equals(request.getUserId()));
        if (!alreadyJoined) {
            if (room.getPlayers().size() >= 2) {
                throw new IllegalStateException("房间人数已满");
            }
            if (!List.of("waiting", "ready-check").contains(room.getStatus())) {
                throw new IllegalStateException("比赛已经开始，无法加入");
            }
            room.getPlayers().add(new CompetitionPlayer(request.getUserId(), displayName(request.getDisplayName())));
        }

        if (room.getPlayers().size() == 2 && "waiting".equals(room.getStatus())) {
            room.setStatus("ready-check");
        }
        touch(room);
        return room;
    }

    public synchronized CompetitionRoom markReady(String roomId, Long userId) {
        CompetitionRoom room = getRoomOrThrow(normalizeRoomId(roomId));
        if (room.getPlayers().size() < 2) {
            throw new IllegalStateException("请等待另一名玩家加入");
        }
        if (!List.of("ready-check", "waiting").contains(room.getStatus())) {
            throw new IllegalStateException("比赛已经开始");
        }

        playerOrThrow(room, userId).setReady(true);
        if (room.getPlayers().stream().allMatch(CompetitionPlayer::isReady)) {
            room.setStatus("playing");
            Instant startedAt = Instant.now();
            room.setStartedAt(startedAt);
            room.getPlayers().forEach(player -> questionStartedAt.get(room.getRoomId())
                    .put(player.getUserId(), startedAt));
        }
        touch(room);
        return room;
    }

    public synchronized CompetitionRoom leaveRoom(String roomId, Long userId) {
        CompetitionRoom room = getRoomOrThrow(normalizeRoomId(roomId));
        CompetitionPlayer player = playerOrThrow(room, userId);

        if (List.of("waiting", "ready-check").contains(room.getStatus())) {
            room.getPlayers().remove(player);
            if (room.getPlayers().isEmpty()) {
                removeRoom(room.getRoomId());
                return room;
            }
            room.setStatus("waiting");
            room.getPlayers().forEach(remaining -> remaining.setReady(false));
            touch(room);
            return room;
        }

        if ("playing".equals(room.getStatus())) {
            player.setFinished(true);
            player.setForfeited(true);
            player.setSubmittedCount(room.getQuestions().size());
            room.setStatus("finished");
        }
        touch(room);
        return room;
    }

    public synchronized CompetitionSubmitResponse submit(String roomId, CompetitionSubmitRequest request) {
        CompetitionRoom room = getRoomOrThrow(normalizeRoomId(roomId));
        if (!"playing".equals(room.getStatus())) {
            throw new IllegalStateException("当前比赛不能提交答案");
        }

        CompetitionPlayer player = playerOrThrow(room, request.getUserId());
        String expected = answerKeys.get(room.getRoomId()).get(request.getQuestionId());
        if (expected == null) {
            throw new IllegalArgumentException("题目不属于当前房间");
        }

        Map<Long, List<String>> submittedByPlayer = submittedQuestions.get(room.getRoomId());
        List<String> userSubmissions = submittedByPlayer.computeIfAbsent(
                request.getUserId(),
                ignored -> new ArrayList<>()
        );

        if (userSubmissions.contains(request.getQuestionId())) {
            CompetitionSubmitResponse previous = submissionResults.get(room.getRoomId())
                    .getOrDefault(request.getUserId(), Map.of())
                    .get(request.getQuestionId());
            if (previous != null) {
                return new CompetitionSubmitResponse(
                        previous.isCorrect(),
                        previous.getCorrectAnswer(),
                        0,
                        player.getScore(),
                        true
                );
            }
            return new CompetitionSubmitResponse(false, expected, 0, player.getScore(), true);
        }

        int expectedQuestionIndex = userSubmissions.size();
        if (expectedQuestionIndex >= room.getQuestions().size()
                || !room.getQuestions().get(expectedQuestionIndex).getId().equals(request.getQuestionId())) {
            throw new IllegalStateException("请按顺序提交题目");
        }

        Instant now = Instant.now();
        Instant startedAt = questionStartedAt.get(room.getRoomId())
                .getOrDefault(request.getUserId(), room.getStartedAt());
        long elapsedMs = Math.max(0, Duration.between(startedAt, now).toMillis());

        userSubmissions.add(request.getQuestionId());
        boolean correct = normalize(expected).equals(normalize(request.getAnswer()));
        int basePoints = room.getQuestions().stream()
                .filter(question -> question.getId().equals(request.getQuestionId()))
                .map(CompetitionQuestion::getPoints)
                .findFirst()
                .orElse(100);
        int speedBonus = correct ? Math.max(0, 20 - (int) (elapsedMs / 5000)) : 0;
        int awarded = correct ? basePoints + speedBonus : 0;

        player.setScore(player.getScore() + awarded);
        player.setTotalTimeMs(player.getTotalTimeMs() + elapsedMs);
        player.setSubmittedCount(userSubmissions.size());
        player.setFinished(userSubmissions.size() >= room.getQuestions().size());
        if (correct) {
            player.setCorrectCount(player.getCorrectCount() + 1);
        }
        questionStartedAt.get(room.getRoomId()).put(request.getUserId(), now);

        CompetitionSubmitResponse response =
                new CompetitionSubmitResponse(correct, expected, awarded, player.getScore());
        submissionResults.get(room.getRoomId())
                .computeIfAbsent(request.getUserId(), ignored -> new ConcurrentHashMap<>())
                .put(request.getQuestionId(), response);

        if (room.getPlayers().size() == 2 && room.getPlayers().stream().allMatch(CompetitionPlayer::isFinished)) {
            room.setStatus("finished");
        }
        touch(room);
        return response;
    }

    public CompetitionRoom getRoom(String roomId) {
        CompetitionRoom room = getRoomOrThrow(normalizeRoomId(roomId));
        touch(room);
        return room;
    }

    public CompetitionRoom publicView(CompetitionRoom room) {
        CompetitionRoom view = new CompetitionRoom();
        view.setRoomId(room.getRoomId());
        view.setAlgorithm(room.getAlgorithm());
        view.setStatus(room.getStatus());
        view.setCreatedAt(room.getCreatedAt());
        view.setStartedAt(room.getStartedAt());
        view.setLastActivityAt(room.getLastActivityAt());
        view.setQuestionCount(room.getQuestionCount());
        view.setPlayers(new ArrayList<>(room.getPlayers()));
        if (List.of("playing", "finished").contains(room.getStatus())) {
            view.setQuestions(room.getQuestions());
        }
        return view;
    }

    public boolean isPlayerInRoom(String roomId, Long userId) {
        if (userId == null) return false;
        CompetitionRoom room = rooms.get(normalizeRoomId(roomId));
        return room != null && room.getPlayers().stream()
                .anyMatch(player -> player.getUserId().equals(userId));
    }

    public List<CompetitionPlayer> getRankings(String roomId) {
        CompetitionRoom room = getRoomOrThrow(normalizeRoomId(roomId));
        if (!"finished".equals(room.getStatus())) {
            throw new IllegalStateException("比赛尚未结束");
        }
        touch(room);
        return room.getPlayers().stream()
                .sorted(Comparator.comparing(CompetitionPlayer::isForfeited)
                        .thenComparing(Comparator.comparingInt(CompetitionPlayer::getScore).reversed())
                        .thenComparing(CompetitionPlayer::getCorrectCount, Comparator.reverseOrder())
                        .thenComparing(CompetitionPlayer::getTotalTimeMs))
                .toList();
    }

    private CompetitionPlayer playerOrThrow(CompetitionRoom room, Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户信息无效，请重新登录");
        }
        return room.getPlayers().stream()
                .filter(player -> player.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("当前用户不在该房间中"));
    }

    private CompetitionRoom getRoomOrThrow(String roomId) {
        CompetitionRoom room = rooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("房间不存在或已失效");
        }
        return room;
    }

    private void removeRoom(String roomId) {
        rooms.remove(roomId);
        answerKeys.remove(roomId);
        submittedQuestions.remove(roomId);
        submissionResults.remove(roomId);
        questionStartedAt.remove(roomId);
    }

    @Scheduled(fixedDelay = 300_000)
    public synchronized void removeExpiredRooms() {
        Instant now = Instant.now();
        rooms.values().stream()
                .filter(room -> isExpired(room, now))
                .map(CompetitionRoom::getRoomId)
                .toList()
                .forEach(this::removeRoom);
    }

    private boolean isExpired(CompetitionRoom room, Instant now) {
        Duration idle = Duration.between(room.getLastActivityAt(), now);
        if ("finished".equals(room.getStatus())) return idle.toHours() >= 1;
        if ("playing".equals(room.getStatus())) return idle.toHours() >= 2;
        return idle.toMinutes() >= 30;
    }

    private void touch(CompetitionRoom room) {
        room.setLastActivityAt(Instant.now());
    }

    private String newRoomId() {
        String roomId;
        do {
            roomId = UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        } while (rooms.containsKey(roomId));
        return roomId;
    }

    private String normalizeRoomId(String roomId) {
        return roomId == null ? "" : roomId.trim().toUpperCase(Locale.ROOT);
    }

    private String displayName(String value) {
        return value == null || value.isBlank() ? "Player" : value.trim();
    }

    private Map<String, String> createAnswerKey(String algorithm, List<CompetitionQuestion> questions) {
        Map<String, String> key = new HashMap<>();
        for (CompetitionQuestion question : questions) {
            String answer = question.getId().endsWith("-q1")
                    ? complexityFor(algorithm)
                    : question.getId().endsWith("-q2")
                    ? answerFor(algorithm)
                    : useCaseAnswerFor(algorithm);
            key.put(question.getId(), answer);
        }
        return key;
    }

    private List<CompetitionQuestion> createQuestions(String algorithm) {
        String label = labelFor(algorithm);
        return List.of(
                new CompetitionQuestion(
                        algorithm + "-q1",
                        algorithm,
                        label + " 基础判断",
                        "该算法的典型时间复杂度最接近哪一项？",
                        List.of("O(1)", "O(log n)", "O(n)", "O(n log n)", "O(n^2)", "O(n!)",
                                "O(V+E)", "O(E log V)", "O(E log E)", "O(nW)", "O(n^1.585)"),
                        Map.of(),
                        100
                ),
                new CompetitionQuestion(
                        algorithm + "-q2",
                        algorithm,
                        label + " 状态推演",
                        promptFor(algorithm),
                        optionsFor(algorithm),
                        Map.of(),
                        120
                ),
                new CompetitionQuestion(
                        algorithm + "-q3",
                        algorithm,
                        label + " 应用选择",
                        "这个算法最适合解决下面哪类问题？",
                        useCaseOptionsFor(algorithm),
                        Map.of(),
                        100
                )
        );
    }

    private String labelFor(String algorithm) {
        return switch (algorithm) {
            case "quick-sort" -> "快速排序";
            case "merge-sort" -> "归并排序";
            case "bubble-sort" -> "冒泡排序";
            case "heap-sort" -> "堆排序";
            case "insertion-sort" -> "插入排序";
            case "binary-search" -> "二分查找";
            case "dijkstra" -> "Dijkstra";
            case "bfs" -> "BFS";
            case "dfs" -> "DFS";
            case "prim" -> "Prim";
            case "kruskal" -> "Kruskal";
            case "astar" -> "A*";
            case "knapsack" -> "0/1 背包";
            case "n-queens" -> "N 皇后";
            case "karatsuba" -> "Karatsuba";
            default -> algorithm;
        };
    }

    private String complexityFor(String algorithm) {
        return switch (algorithm) {
            case "bubble-sort", "insertion-sort" -> "O(n^2)";
            case "binary-search" -> "O(log n)";
            case "bfs", "dfs" -> "O(V+E)";
            case "knapsack" -> "O(nW)";
            case "n-queens" -> "O(n!)";
            case "karatsuba" -> "O(n^1.585)";
            case "dijkstra", "prim", "astar" -> "O(E log V)";
            case "kruskal" -> "O(E log E)";
            default -> "O(n log n)";
        };
    }

    private String promptFor(String algorithm) {
        return switch (algorithm) {
            case "quick-sort" -> "数组 [5,2,8,1] 以 5 为 pivot 完成一次分区后，pivot 左侧应都是比它怎样的元素？";
            case "merge-sort" -> "归并排序的核心操作是先递归拆分，再执行什么操作？";
            case "bubble-sort" -> "冒泡排序一轮扫描后，最大元素通常会移动到哪里？";
            case "heap-sort" -> "堆排序首先通常要把数组调整成什么结构？";
            case "insertion-sort" -> "插入排序维护的是数组左侧哪种状态？";
            case "binary-search" -> "二分查找要求输入数组满足什么条件？";
            case "dijkstra" -> "Dijkstra 每轮选择哪个未确定节点？";
            case "bfs" -> "BFS 通常使用哪种数据结构管理待访问节点？";
            case "dfs" -> "DFS 通常优先沿着路径向哪个方向探索？";
            case "prim" -> "Prim 每次选择连接当前生成树和外部节点的什么边？";
            case "kruskal" -> "Kruskal 会先按照什么顺序处理所有边？";
            case "astar" -> "A* 的优先级通常由实际代价 g 和什么组成？";
            case "knapsack" -> "0/1 背包中每个物品最多可以选择几次？";
            case "n-queens" -> "N 皇后要求任意两个皇后不能处于同一行、列或什么线上？";
            case "karatsuba" -> "Karatsuba 通过减少哪类运算来优化大整数乘法？";
            default -> "选择最符合该算法特点的一项。";
        };
    }

    private List<String> optionsFor(String algorithm) {
        return switch (algorithm) {
            case "quick-sort" -> List.of("更小", "更大", "相等", "无序");
            case "merge-sort" -> List.of("合并有序子数组", "随机打乱", "建堆", "回溯");
            case "bubble-sort" -> List.of("数组末尾", "数组开头", "中间位置", "随机位置");
            case "heap-sort" -> List.of("堆", "队列", "哈希表", "链表");
            case "insertion-sort" -> List.of("有序", "完全随机", "堆结构", "图结构");
            case "binary-search" -> List.of("有序", "无序", "必须为图", "必须为树");
            case "dijkstra" -> List.of("当前距离最小", "入度最大", "编号最大", "随机");
            case "bfs" -> List.of("队列", "栈", "堆", "集合");
            case "dfs" -> List.of("更深层节点", "同层所有节点", "权重最大边", "随机节点");
            case "prim" -> List.of("最小权重边", "最大权重边", "任意边", "自环");
            case "kruskal" -> List.of("边权从小到大", "边权从大到小", "节点编号", "访问时间");
            case "astar" -> List.of("启发式估价 h", "数组长度 n", "栈深度", "哈希值");
            case "knapsack" -> List.of("一次", "无限次", "两次", "不能选择");
            case "n-queens" -> List.of("对角线", "颜色", "权重", "编号");
            case "karatsuba" -> List.of("乘法", "加法", "比较", "入栈");
            default -> List.of("选项 A", "选项 B", "选项 C", "选项 D");
        };
    }

    private String answerFor(String algorithm) {
        return switch (algorithm) {
            case "quick-sort" -> "更小";
            case "merge-sort" -> "合并有序子数组";
            case "bubble-sort" -> "数组末尾";
            case "heap-sort" -> "堆";
            case "insertion-sort", "binary-search" -> "有序";
            case "dijkstra" -> "当前距离最小";
            case "bfs" -> "队列";
            case "dfs" -> "更深层节点";
            case "prim" -> "最小权重边";
            case "kruskal" -> "边权从小到大";
            case "astar" -> "启发式估价 h";
            case "knapsack" -> "一次";
            case "n-queens" -> "对角线";
            case "karatsuba" -> "乘法";
            default -> "选项 A";
        };
    }

    private List<String> useCaseOptionsFor(String algorithm) {
        return switch (algorithm) {
            case "binary-search" -> List.of("在有序数组中定位目标", "给图染色", "压缩图片", "生成随机数");
            case "dijkstra", "astar" -> List.of("寻找带权图路径", "数组原地排序", "字符串匹配", "矩阵转置");
            case "bfs", "dfs" -> List.of("图或树的遍历", "浮点数运算", "数据库索引", "图片滤镜");
            case "prim", "kruskal" -> List.of("构造最小生成树", "求最长公共子序列", "数组去重", "用户认证");
            case "knapsack" -> List.of("容量约束下做最优选择", "排序日志", "查找中位数", "解析 URL");
            case "n-queens" -> List.of("约束满足搜索", "哈希冲突处理", "缓存淘汰", "负载均衡");
            case "karatsuba" -> List.of("大整数乘法", "图遍历", "最短路径", "队列调度");
            default -> List.of("对一组元素排序", "寻找最短路径", "递归摆放棋子", "容量规划");
        };
    }

    private String useCaseAnswerFor(String algorithm) {
        return switch (algorithm) {
            case "binary-search" -> "在有序数组中定位目标";
            case "dijkstra", "astar" -> "寻找带权图路径";
            case "bfs", "dfs" -> "图或树的遍历";
            case "prim", "kruskal" -> "构造最小生成树";
            case "knapsack" -> "容量约束下做最优选择";
            case "n-queens" -> "约束满足搜索";
            case "karatsuba" -> "大整数乘法";
            default -> "对一组元素排序";
        };
    }

    private String normalize(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC);
        return normalized.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
