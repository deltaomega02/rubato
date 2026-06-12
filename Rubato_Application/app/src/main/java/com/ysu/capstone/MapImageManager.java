package com.ysu.capstone;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.util.Arrays;

//이 놈이 뭐하는 놈이냐고요? 폰에 사진 쌓이면 지워주는 놈이에요! 와아!
public class MapImageManager {
    private static final int MAX_IMAGES = 10;
    private static final long DELETE_INTERVAL = 30 * 60 * 1000; // 30분
    private final Context context;
    private Handler cleanupHandler;
    private Runnable cleanupRunnable;

    public MapImageManager(Context context) {
        this.context = context;
        setupPeriodicCleanup();
    }

    // 정기적인 정리 설정
    private void setupPeriodicCleanup() {
        cleanupHandler = new Handler(Looper.getMainLooper());
        cleanupRunnable = new Runnable() {
            @Override
            public void run() {
                cleanupOldImages();
                cleanupHandler.postDelayed(this, DELETE_INTERVAL);
            }
        };
    }

    // 정기적인 정리 시작
    public void startPeriodicCleanup() {
        cleanupHandler.postDelayed(cleanupRunnable, DELETE_INTERVAL);
    }

    // 정기적인 정리 중지
    public void stopPeriodicCleanup() {
        cleanupHandler.removeCallbacks(cleanupRunnable);
    }

    // 개수 기반 정리
    public void cleanupByCount() {
        File mapsDir = new File(context.getExternalFilesDir(null), "maps");
        if (!mapsDir.exists()) return;

        File[] files = mapsDir.listFiles();
        if (files == null || files.length <= MAX_IMAGES) return;

        // 파일들을 수정 시간 기준으로 정렬
        Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

        // MAX_IMAGES개를 제외한 나머지 삭제
        for (int i = MAX_IMAGES; i < files.length; i++) {
            boolean deleted = files[i].delete();
            Log.d("MapImageManager", "Deleted old image: " + files[i].getName() +
                    " - Success: " + deleted);
        }
    }

    // 시간 기반 정리
    public void cleanupOldImages() {
        File mapsDir = new File(context.getExternalFilesDir(null), "maps");
        if (!mapsDir.exists()) return;

        File[] files = mapsDir.listFiles();
        if (files == null) return;

        long currentTime = System.currentTimeMillis();
        long deleteBeforeTime = currentTime - DELETE_INTERVAL;

        for (File file : files) {
            if (file.lastModified() < deleteBeforeTime) {
                boolean deleted = file.delete();
                Log.d("MapImageManager", "Deleted expired image: " + file.getName() +
                        " - Success: " + deleted);
            }
        }
    }

    // 모든 이미지 삭제
    public void deleteAllImages() {
        File mapsDir = new File(context.getExternalFilesDir(null), "maps");
        if (!mapsDir.exists()) return;

        File[] files = mapsDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            boolean deleted = file.delete();
            Log.d("MapImageManager", "Deleted image: " + file.getName() +
                    " - Success: " + deleted);
        }
    }
}
