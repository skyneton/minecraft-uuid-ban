package net.mpoisv.ban;

import org.bukkit.Bukkit;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadManager {
    private boolean isRunnable = true;
    private ExecutorService es = Executors.newSingleThreadExecutor();
    public void startWorker() {
        es.submit(new Worker(60, 10000));
    }

    public void stop() {
        isRunnable = false;
        es.shutdownNow();
        while(!es.isShutdown());
    }

    public boolean isRunnable() {
        return isRunnable;
    }

    private class Worker implements Runnable {
        private long workTimer;
        private long logTime;
        private long currentLogTimer;
        public Worker(long logTime, long workTimer) {
            this.logTime = logTime;
            this.currentLogTimer = logTime;
            this.workTimer = workTimer;
        }

        @Override
        public void run() {
            Timer timer = new Timer();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    if(Main.instance == null || !isRunnable) {
                        timer.cancel();
                        return;
                    }

                    try {
                        Main.instance.databaseManager.clean();
                    }catch (Exception e) {
                        Bukkit.getConsoleSender().sendMessage("§bː§f UUID §bː §rDB 작업을 실패했습니다. "
                                + e.getLocalizedMessage() + " : " + e.getCause());
                    }

                    if(++currentLogTimer >= logTime) {
                        Bukkit.broadcastMessage("§m                                                ");
                        Bukkit.broadcastMessage("§bː§f UUID §bː §f Server UUID Ban System");
                        Bukkit.broadcastMessage("");
                        Bukkit.broadcastMessage("Version: §a" + Main.instance.getDescription().getVersion());
                        Bukkit.broadcastMessage("Made By MPoint(skyneton)");
                        Bukkit.broadcastMessage("");
                        Bukkit.broadcastMessage("Last Update Date: 2023/07/17");
                        Bukkit.broadcastMessage("§m                                                ");
                        currentLogTimer = 0;
                    }
                }
            };
            timer.schedule(task, 10000, this.workTimer);
        }
    }
}
