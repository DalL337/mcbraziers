package com.dai.mcbraziers.gametest;

import java.util.List;

/**
 * Which client test scenarios run. Owner's rule (2026-10-25): visual development tests run only the scenario
 * asked for, in an isolated world, and the historical scenes run only when a regression run is asked for. A
 * full run, the regression check, runs every scenario. During focused visual work, {@code ./gradlew
 * runClientGameTest -Pscenario=<name>} runs only that one, in a fresh world with only its own scene.
 */
final class ClientScenarios {

    /** The system property build.gradle sets from {@code -Pscenario}. */
    private static final String PROPERTY = "mcbraziers.scenario";

    /**
     * Every scenario: {@code placement}, {@code fire-particles} and {@code art} are the whole client tests;
     * the rest are scenes of the art test that also run on their own.
     */
    static final List<String> ALL = List.of("placement", "fire-particles", "art", "wall-corners");

    private ClientScenarios() {
    }

    /** The scenario asked for, or null for a full run. A name not in {@link #ALL} fails rather than running nothing. */
    static String focused() {
        String name = System.getProperty(PROPERTY, "").trim();
        if (name.isEmpty()) {
            return null;
        }
        if (!ALL.contains(name)) {
            throw new AssertionError("unknown client test scenario '" + name + "'; known: " + String.join(", ", ALL));
        }
        return name;
    }

    /** Whether the whole of {@code name}, one of {@link #ALL}, runs: in a full run, or when it's the one asked for. */
    static boolean runs(String name) {
        String focused = focused();
        return focused == null || focused.equals(name);
    }
}
