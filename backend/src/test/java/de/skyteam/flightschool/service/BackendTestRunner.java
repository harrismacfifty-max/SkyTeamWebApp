package de.skyteam.flightschool.service;

public final class BackendTestRunner {
    private BackendTestRunner() {
    }

    public static void main(String[] args) {
        TestRunner runner = new TestRunner();
        AuthServiceTest.run(runner);
        ApiRoleSecurityTest.run(runner);
        SchuelerServiceTest.run(runner);
        AbschlussServiceTest.run(runner);
        TheorieServiceTest.run(runner);
        PraxisServiceTest.run(runner);
        PruefungsServiceTest.run(runner);
        AusbildungsstatusServiceTest.run(runner);
        runner.summary();
    }
}
