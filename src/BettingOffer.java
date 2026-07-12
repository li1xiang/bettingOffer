
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import data.Stake;
import data.UserInfo;

import javax.sound.midi.Soundbank;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class BettingOffer {

    private static final int  SERVER_PORT = 8001;
    private static final int TCP_CONNECTIONS = 100;
    private static final int BLOCK_QUEUE = 1000;
    private static final int TOP_STAKES_LIMIT = 20;
    private static final ConcurrentHashMap<String, UserInfo> loginMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, List<Stake>> stakeMap = new ConcurrentHashMap<>();
    private static final SecureRandom random = new SecureRandom();
    private static final String SESSION_DIC = "0123456789ABCDEFGHLMNOBQRSTUWWXYZ";
    private static final Object SESSION_LOCK = new Object();
    private static final String TIPS = "Please check your url and try again.";

    public static void main(String[] args) {
        System.out.println("Betting Offer Server started on port " + SERVER_PORT);
        Executor executor = executorThreadPoolExecutor();
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(SERVER_PORT), TCP_CONNECTIONS);
            server.setExecutor(executor);
            server.createContext("/", exchange -> {
                String requestMethodType = exchange.getRequestMethod();
                String path = exchange.getRequestURI().getPath();
                System.out.println("Path: " + path + " Request Type: " + requestMethodType);
                dealWithRequest(exchange, requestMethodType, path);
            });
            server.start();
        } catch (BindException bindException) {
            System.out.println("the server is already bound");
        } catch (IOException e) {
            System.out.println("an I/O error occurs");
        }
    }




    private static void dealWithRequest(HttpExchange exchange, String requestMethodType, String path) throws IOException{
        //get sessionKey
        if (requestMethodType.equalsIgnoreCase("GET") && path.endsWith("/session")) {
            String customerId = path.split("/")[1];
            String sessionKey = login(customerId);
            byte[] sessionkeyBytes = sessionKey.getBytes();
            setResponse(exchange, sessionkeyBytes.length, 200);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(sessionkeyBytes);
            }
            exchange.close();
        }
        //save stake for betting offerId
        else if (requestMethodType.equalsIgnoreCase("POST") && path.endsWith("/stake")) {

            String offerId = path.split("/")[1];
            System.out.println("Offer ID: " + offerId);

            String sessionKey = exchange.getRequestURI().getQuery().split("=")[1];
            System.out.println("Query: " + sessionKey);


            InputStream in = exchange.getRequestBody();
            String requestBody = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            int stakeAmount = parseStake(requestBody);

            int responseCode = submitStake(Integer.parseInt(offerId), stakeAmount, sessionKey);
            exchange.sendResponseHeaders(responseCode, 0);
            exchange.close();

        }
        //get top 20 stakes for offerId
        else if (requestMethodType.equalsIgnoreCase("GET") && path.endsWith("/highstakes")) {
            String offerId = path.split("/")[1];
            String topStakes = getTopStakes(Integer.parseInt(offerId));
            byte[] topStakesBytes = topStakes.getBytes();
            setResponse(exchange, topStakesBytes.length, 200);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(topStakesBytes);
            }
            exchange.close();
        }
        else {

            byte[] tipsBytes = TIPS.getBytes();
            setResponse(exchange, tipsBytes.length, 404);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(tipsBytes);
            }
            exchange.close();
        }
    }


    private static String login(String customerId) {
        synchronized (SESSION_LOCK) {
            UserInfo userInfo = loginMap.get(customerId);
            if (userInfo == null || userInfo.isSessionValid()) {
                userInfo = new UserInfo(customerId, generateSessionKey());
                loginMap.put(customerId, userInfo);
            } else {
                userInfo.resetLastValidTimeTime();
            }
            return userInfo.getSessionKey();
        }
    }

    private static int submitStake(int offerId, int stakeAmount, String sessionKey) {
        UserInfo userInfo = loginMap.values().stream()
                .filter(info -> info.getSessionKey().equals(sessionKey))
                .findFirst()
                .orElse(null);

        if (userInfo == null) {
            System.out.println("user not login in");
            return 401;
        }
        // Check if the session key is expired, if expired then remove from map
        if (userInfo.isSessionValid()) {
            System.out.println("session key expired");
            loginMap.remove(userInfo.getCustomerId());
            return 401;
        }
        userInfo.resetLastValidTimeTime();
        saveStake(offerId, stakeAmount, userInfo);
        System.out.println("Stake submitted for offerId: " + offerId + " by user: " + userInfo.getCustomerId());
        return 200;
    }

    private static String getTopStakes(int offerId) {
        return stakeMap.getOrDefault(offerId, new ArrayList<>())
                .stream()
                .map(stake -> stake.getUserInfo().getCustomerId() + "=" + stake.getStakeAmount())
                .collect(Collectors.joining(","));
    }

    private static void saveStake(int offerId, int stakeAmount, UserInfo userInfo) {

        List<Stake> newStakes = stakeMap.compute(offerId, (key, stakes) -> {
            if (null == stakes) {
                stakes = new ArrayList<>();
            }
            Stake existing = stakes.stream()
                    .filter(s -> s.getUserInfo().getCustomerId().equals(userInfo.getCustomerId()))
                    .findFirst()
                    .orElse(null);
            if (existing != null) {
                if (stakeAmount >= existing.getStakeAmount()) {
                    existing.setStakeAmount(stakeAmount);
                    existing.setStakeTime(LocalDateTime.now());
                }
            } else {
                stakes.add(new Stake(stakeAmount, LocalDateTime.now(), userInfo));
            }

            // Sort and limit to top 20 for current offer, base on amount and then time
            stakes.sort((s1, s2) -> {
                if (s2.getStakeAmount() != s1.getStakeAmount()) {
                    return Integer.compare(s2.getStakeAmount(), s1.getStakeAmount());
                } else {
                    return s1.getStakeTime().compareTo(s2.getStakeTime());
                }
            });
            if (stakes.size() > TOP_STAKES_LIMIT) {
                stakes = stakes.subList(0, TOP_STAKES_LIMIT);
            }
            return stakes;
        });
        stakeMap.put(offerId, newStakes);
    }

    private static String generateSessionKey() {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            str.append(SESSION_DIC.charAt(random.nextInt(SESSION_DIC.length())));
        }
        return str.toString();
    }

    private static int parseStake(String stakeBody) {
        int indexOfField = stakeBody.indexOf("\"stake\"");
        int indexOfValue = stakeBody.indexOf(':', indexOfField);
        int startIndex = indexOfValue + 1;
        while (startIndex < stakeBody.length() && Character.isWhitespace(stakeBody.charAt(startIndex))) startIndex++;
        int endIndex = startIndex;
        while (endIndex < stakeBody.length() && Character.isDigit(stakeBody.charAt(endIndex))) endIndex++;
        return Integer.parseInt(stakeBody.substring(startIndex, endIndex));
    }

    private static void setResponse(HttpExchange exchange, int length, int httpResponseCode) throws IOException {
        exchange.sendResponseHeaders(httpResponseCode, length);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
    }

    public static Executor executorThreadPoolExecutor() {
        int cpus = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(cpus+1, cpus+1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(BLOCK_QUEUE), new ThreadPoolExecutor.AbortPolicy());
    }

}