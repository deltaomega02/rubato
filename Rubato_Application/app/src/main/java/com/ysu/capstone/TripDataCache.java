package com.ysu.capstone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TripDataCache {
    private static TripDataCache instance;
    private final Map<Integer, List<String>> dayWiseDestinations = new HashMap<>();
    private final Map<Integer, ArrayList<String>> dayWiseDistances = new HashMap<>();

    private TripDataCache() {}

    public static TripDataCache getInstance() {
        if (instance == null) {
            synchronized (TripDataCache.class) {
                if (instance == null) {
                    instance = new TripDataCache();
                }
            }
        }
        return instance;
    }

    public void setDestinations(int day, List<String> destinations) {
        dayWiseDestinations.put(day, new ArrayList<>(destinations));
    }

    public List<String> getDestinations(int day) {
        return dayWiseDestinations.getOrDefault(day, new ArrayList<>());
    }

    public void setDistances(int day, ArrayList<String> distances) {
        dayWiseDistances.put(day, new ArrayList<>(distances));
    }

    public ArrayList<String> getDistances(int day) {
        return dayWiseDistances.getOrDefault(day, new ArrayList<>());
    }

    // 모든 데이터 초기화
    public void clearAll() {
        dayWiseDestinations.clear();
        dayWiseDistances.clear();

        // 메모리 최적화를 위해 새로운 맵으로 초기화
        instance = null;
    }

    // 디버깅을 위한 메서드
    public void printCache() {
        System.out.println("=== Destinations ===");
        for (Map.Entry<Integer, List<String>> entry : dayWiseDestinations.entrySet()) {
            System.out.println("Day " + entry.getKey() + ": " + entry.getValue());
        }

        System.out.println("=== Distances ===");
        for (Map.Entry<Integer, ArrayList<String>> entry : dayWiseDistances.entrySet()) {
            System.out.println("Day " + entry.getKey() + ": " + entry.getValue());
        }
    }
}