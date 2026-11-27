package com.hexisnutrition.backend.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Supporto ai test di concorrenza: richieste HTTP (MockMvc) lanciate su thread diversi nello stesso istante. */
public final class Concorrenza {

    private Concorrenza() {
    }

    /** Fa partire le richieste insieme (per quanto possibile) e ne raccoglie gli esiti, nell'ordine dato. */
    @SafeVarargs
    public static List<Integer> inParallelo(Callable<Integer>... richieste) throws Exception {
        ExecutorService esecutore = Executors.newFixedThreadPool(richieste.length);
        try {
            CountDownLatch via = new CountDownLatch(1);
            List<Future<Integer>> futuri = new ArrayList<>();
            for (Callable<Integer> richiesta : richieste) {
                futuri.add(esecutore.submit(() -> {
                    via.await();
                    return richiesta.call();
                }));
            }
            via.countDown();
            List<Integer> esiti = new ArrayList<>();
            for (Future<Integer> futuro : futuri) {
                esiti.add(futuro.get(30, TimeUnit.SECONDS));
            }
            return esiti;
        } finally {
            esecutore.shutdownNow();
        }
    }
}
