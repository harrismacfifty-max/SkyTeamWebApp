package de.skyteam.flightschool.service;

public final class BackendTestRunner {
    private BackendTestRunner() {
    }

    public static void main(String[] args) {
        TestRunner runner = new TestRunner();
        TheorieServiceTest.run(runner);
        PraxisServiceTest.run(runner);
        PruefungsServiceTest.run(runner);
        AusbildungsstatusServiceTest.run(runner);
        runner.summary();
    }
}
