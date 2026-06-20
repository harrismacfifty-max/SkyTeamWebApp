package de.skyteam.flightschool.service;

final class TestRunner {
    private int passed;
    private int failed;

    void test(String name, ThrowingRunnable runnable) {
        try {
            runnable.run();
            passed++;
            System.out.println("[PASS] " + name);
        } catch (Throwable throwable) {
            failed++;
            System.out.println("[FAIL] " + name + " -> " + throwable.getMessage());
            throwable.printStackTrace(System.out);
        }
    }

    void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " Expected <" + expected + "> but was <" + actual + ">.");
        }
    }

    <T extends Throwable> T expectThrows(Class<T> type, ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            if (type.isInstance(throwable)) {
                return type.cast(throwable);
            }
            throw new AssertionError("Expected " + type.getSimpleName() + " but got " + throwable.getClass().getSimpleName(), throwable);
        }
        throw new AssertionError("Expected " + type.getSimpleName() + " but no exception was thrown.");
    }

    void summary() {
        System.out.println("Tests passed: " + passed + ", failed: " + failed);
        if (failed > 0) {
            throw new AssertionError("Backend tests failed.");
        }
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }
}
